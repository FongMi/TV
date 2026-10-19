import builtins
import hashlib
import json
import os
import shutil
import sys
import threading
import types
import uuid
from importlib.machinery import ModuleSpec
from urllib.parse import urljoin, urlsplit


class SourceNotFoundError(FileNotFoundError):
    pass


class _Load:
    def __init__(self):
        self.owner = threading.get_ident()
        self.done = threading.Event()
        self.value = None
        self.error = None


class Runtime:
    def __init__(self, cache, source, site_key, read):
        identity = hashlib.sha256(json.dumps([site_key, source], ensure_ascii=False).encode()).hexdigest()
        generation = uuid.uuid4().hex
        self._cache = os.path.realpath(cache)
        self.directory = os.path.join(self._cache, identity, generation)
        self.namespace = "_tv_py_" + generation
        self.source = source
        self.site_key = site_key
        entry = os.path.basename(urlsplit(source).path).removesuffix(".py")
        self.entry = entry if entry.isidentifier() else "spider"
        self._read = read
        self._lock = threading.RLock()
        self._closed = False
        self._modules = {}
        self._files = {}
        self._references = {}
        self._declared = set()
        self._downloads = {}
        self._loading = {}
        self._waiting = {}
        os.makedirs(self.directory, exist_ok=True)
        self._package("")

    def _check_open(self):
        if self._closed:
            raise RuntimeError("Python spider runtime is closed")

    def _name(self, name):
        reference = str(name).removeprefix("./").removesuffix(".py")
        if not reference:
            raise ValueError("invalid Python dependency name")
        name = reference.replace("/", ".")
        with self._lock:
            self._check_open()
            if any(not part.isidentifier() for part in reference.split("/")):
                name = "_file_" + hashlib.sha256(reference.encode()).hexdigest()
                self._references[name] = reference + ".py"
        return name

    def _package(self, name):
        with self._lock:
            self._check_open()
            if name in self._modules:
                return self._modules[name]
            full_name = self.namespace + ("." + name if name else "")
            module = types.ModuleType(full_name)
            module.__package__ = full_name
            module.__path__ = [os.path.join(self.directory, *name.split(".")) if name else self.directory]
            module.__spec__ = ModuleSpec(full_name, loader=None, is_package=True)
            self._register(name, module)
            return module

    def _register(self, name, module):
        self._modules[name] = module
        sys.modules[module.__name__] = module
        if name:
            parent, _, child = name.rpartition(".")
            setattr(self._modules[parent], child, module)

    def _remove(self, name, module):
        if self._modules.get(name) is not module:
            return
        self._modules.pop(name)
        if sys.modules.get(module.__name__) is module:
            sys.modules.pop(module.__name__)
        parent, _, child = name.rpartition(".")
        parent_module = self._modules.get(parent)
        if getattr(parent_module, child, None) is module:
            delattr(parent_module, child)

    def _resolve(self, reference):
        if urlsplit(self.source).scheme in ("http", "https", "file"):
            return urljoin(self.source, reference)
        return os.path.join(os.path.dirname(os.path.abspath(self.source)), reference)

    def _download(self, name, source=None):
        with self._lock:
            self._check_open()
            if name in self._files:
                return self._files[name]
            pending = self._downloads.get(name)
            if pending is None:
                pending = self._downloads[name] = _Load()
                owner = True
            else:
                owner = False
            reference = self._references.get(name, name.replace(".", "/") + ".py")
        if not owner:
            return self._wait(pending)
        try:
            payload = self._read(source if source is not None else self._resolve(reference))
            return self._save_download(name, payload, pending)
        except BaseException as error:
            with self._lock:
                self._downloads.pop(name, None)
                pending.error = error
                pending.done.set()
            raise

    def _save_download(self, name, payload, pending):
        path = os.path.join(self.directory, *name.split(".")) + ".py"
        with self._lock:
            self._check_open()
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as stream:
                stream.write(payload)
            pending.value = self._files[name] = (path, payload)
            self._downloads.pop(name)
            pending.done.set()
        return pending.value

    def _wait(self, pending, name=None):
        thread = threading.get_ident()
        with self._lock:
            self._check_open()
            if pending.done.is_set():
                if pending.error is not None:
                    raise pending.error
                return pending.value
            self._waiting[thread] = pending.owner
            owner = pending.owner
            while owner in self._waiting and owner != thread:
                owner = self._waiting[owner]
            if owner == thread:
                self._waiting.pop(thread)
                if name in self._modules:
                    return self._modules[name]
                raise ImportError("Circular Python source loading")
        try:
            pending.done.wait()
            with self._lock:
                self._check_open()
                if pending.error is not None:
                    raise pending.error
                return pending.value
        finally:
            with self._lock:
                self._waiting.pop(thread, None)

    def load_reference(self, reference):
        return self.load_module(self._name(reference))

    def _parents(self, name):
        parts = name.split(".")
        for index in range(1, len(parts)):
            self._load_parent(".".join(parts[:index]))

    def _load_parent(self, parent):
        with self._lock:
            self._check_open()
            initializer = parent + ".__init__"
            initialize = initializer in self._declared or initializer in self._files or parent in self._loading
        if initialize:
            self.load_module(parent)
        else:
            self._package(parent)

    def _module(self, name, path, package, module):
        full_name = self.namespace + "." + name
        if module is None:
            module = types.ModuleType(full_name)
        module.__file__ = path
        module.__package__ = full_name if package else full_name.rpartition(".")[0]
        module.__spec__ = ModuleSpec(full_name, loader=None, origin=path, is_package=package)
        if package:
            module.__path__ = [os.path.dirname(path)]
        module.__dict__["__builtins__"] = {**vars(builtins), "__import__": self._import}
        return module

    def _restore(self, name, module, snapshot):
        if module is None:
            return
        if snapshot is None or self._modules.get(name) is not module:
            self._remove(name, module)
            return
        module.__dict__.clear()
        module.__dict__.update(snapshot)
        for child_name, child in self._modules.items():
            parent, _, attribute = child_name.rpartition(".")
            if parent == name:
                setattr(module, attribute, child)

    def load_module(self, name):
        if name.endswith(".__init__"):
            name = name.removesuffix(".__init__")
        self._parents(name)
        with self._lock:
            self._check_open()
            initializer = name + ".__init__"
            known = self._declared | self._files.keys()
            package = initializer in known
            module = self._modules.get(name)
            pending = self._loading.get(name)
            if pending is not None:
                owner = False
            elif module is not None and (not package or module.__spec__.origin is not None):
                return module
            else:
                pending = self._loading[name] = _Load()
                owner = True
            if owner and not package and any(item.startswith(name + ".") for item in known):
                pending.value = self._package(name)
                self._loading.pop(name)
                pending.done.set()
                return pending.value
            snapshot = module.__dict__.copy() if owner and module is not None else None
        if not owner:
            return self._wait(pending, name)
        return self._execute_module(name, package, module, pending, snapshot)

    def _execute_module(self, name, package, module, pending, snapshot):
        try:
            path, payload = self._download(name + ".__init__" if package else name)
            module = self._module(name, path, package, module)
            with self._lock:
                self._check_open()
                self._register(name, module)
            exec(compile(payload, path, "exec"), module.__dict__)
            self._complete_module(name, module, pending)
            return module
        except BaseException as error:
            with self._lock:
                self._restore(name, module, snapshot)
                self._loading.pop(name, None)
                pending.error = error
                pending.done.set()
            raise

    def _complete_module(self, name, module, pending):
        with self._lock:
            self._check_open()
            pending.value = module
            self._loading.pop(name)
            pending.done.set()

    def _import(self, name, globals=None, locals=None, fromlist=(), level=0):
        with self._lock:
            self._check_open()
            known = any(item == name or item.startswith(name + ".") for item in self._declared | self._files.keys())
        if level:
            package = (globals or {}).get("__package__", "")
            prefix = package.split(".")
            if level > len(prefix) or prefix[0] != self.namespace:
                raise ImportError("relative import outside Python spider runtime")
            local_name = ".".join(prefix[1:len(prefix) - level + 1])
            local_name = ".".join(filter(None, (local_name, name)))
            module = self.load_module(local_name) if local_name else self._package("")
        elif known:
            module = self.load_module(name)
        else:
            try:
                return builtins.__import__(name, globals, locals, fromlist, level)
            except ModuleNotFoundError as error:
                if error.name != name.split(".")[0]:
                    raise
                try:
                    self._download(name)
                except SourceNotFoundError:
                    raise error from None
                module = self.load_module(name)
        if fromlist:
            self._import_children(module, fromlist)
            return module
        with self._lock:
            self._check_open()
            return self._modules[name.split(".")[0]] if not level else module

    def _import_children(self, module, fromlist):
        for item in fromlist:
            self._import_child(module, item)

    def _import_child(self, module, item):
        if item != "*" and hasattr(module, "__path__"):
            child = ".".join(filter(None, (module.__name__.removeprefix(self.namespace).lstrip("."), item)))
            with self._lock:
                self._check_open()
                loading = self._modules.get(child) if child in self._loading else None
            if not hasattr(module, item) or loading is not None and getattr(module, item) is loading:
                self.load_module(child)

    def spider(self, name=None):
        name = self.entry if name is None else self._name(name)
        module = self.load_module(name)
        cls = module.Spider
        cls._tv_runtime = self
        cls.siteKey = self.site_key
        return self.bind(cls())

    def bind(self, spider):
        with self._lock:
            self._check_open()
            spider._tv_runtime = self
            spider.siteKey = self.site_key
        return spider

    def create(self):
        try:
            self._download(self.entry, self.source)
            return self.spider()
        except BaseException:
            self.close()
            raise

    def prepare(self, spider):
        names = [self._name(name) for name in spider.getDependence() or []]
        with self._lock:
            self._check_open()
            self._declared.update(names)
        for name in names:
            self._download(name)

    def close(self):
        with self._lock:
            if self._closed:
                return
            directory = os.path.realpath(self.directory)
            if os.path.commonpath((directory, self._cache)) != self._cache or directory == self._cache:
                raise ValueError("Python runtime directory is outside its cache")
            self._closed = True
            for name in list(sys.modules):
                if name == self.namespace or name.startswith(self.namespace + "."):
                    sys.modules.pop(name)
            self._modules.clear()
            self._files.clear()
            self._references.clear()
            self._declared.clear()
            for pending in (*self._downloads.values(), *self._loading.values()):
                pending.done.set()
            self._downloads.clear()
            self._loading.clear()
        try:
            shutil.rmtree(directory)
        except FileNotFoundError:
            pass
