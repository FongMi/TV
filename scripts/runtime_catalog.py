import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
from urllib.parse import urlsplit
from zipfile import BadZipFile, ZIP_STORED, ZipFile


ABIS = ("arm64-v8a", "armeabi-v7a")
MODULES = {
    "python": ("catvod-python-plugin-v2", "libchaquopy_java.so"),
    "nodejs": ("catvod-nodejs-plugin-v2", "libnode.so"),
    "quickjs": ("fongmi-quickjs-ng-0.17-v11", "libquickjs-wrapper.so"),
    "thunder": ("catvod-thunder-plugin-v1", "libxl_thunder_sdk.so"),
    "bdj": ("libbluray-1.4.1-openjdk8-zero-v1", None),
    "mpv": ("mpv-jni-ffmpeg-shared-v1", "libplayer.so"),
}
LIBRARIES = {
    "python": ("libcrypto_python.so", "libssl_python.so", "libsqlite3_python.so", "libcrypto_chaquopy.so", "libssl_chaquopy.so", "libsqlite3_chaquopy.so", "libpython3.10.so", "libpython3.so", "libchaquopy_java.so"),
    "nodejs": ("libnode.so", "libnodebridge.so"),
    "quickjs": ("libquickjs-wrapper.so",),
    "thunder": ("libxl_stat.so", "libxl_thunder_sdk.so"),
    "mpv": ("libmpv.so", "libplayer.so"),
}
PYTHON_ASSETS = ("app.imy", "build.json", "bootstrap.imy", "stdlib-common.imy", "stdlib-{abi}.imy", "requirements-common.imy", "requirements-{abi}.imy", "cacert.pem")
JAVA_ENTRIES = {"python": "com.fongmi.chaquo.RuntimeEntry", "nodejs": "com.fongmi.nodejs.RuntimeEntry", "quickjs": "com.fongmi.quickjs.RuntimeEntry", "thunder": "com.xunlei.downloadlib.RuntimeEntry"}
BDJ_ARCHIVE = "assets/bdj/runtime.zip"
BDJ_LIBRARIES = ("libjava.so", "libverify.so", "libzip.so", "libnet.so", "libnio.so", "server/libjvm.so")
DEFAULT_URL = "https://github.com/FongMi/Release/releases/download/module"


def digest(stream):
    value = hashlib.sha256()
    for block in iter(lambda: stream.read(1024 * 1024), b""):
        value.update(block)
    return value.hexdigest()


def read_version(file, metadata):
    code, name = metadata.get("versionCode"), metadata.get("versionName")
    if type(code) is not int or code <= 0 or not isinstance(name, str) or not name:
        raise ValueError(f"{file.name}: invalid runtime version")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+-]*", name) or ".." in name or name.endswith((".", ".lock")):
        raise ValueError(f"{file.name}: invalid runtime release version")
    return code, name


def read_catalog(file):
    catalog = json.loads(file.read_text(encoding="utf-8"))
    if not isinstance(catalog, dict) or type(catalog.get("schema")) is not int or catalog["schema"] != 1:
        raise ValueError(f"{file.name}: invalid catalog schema")
    modules = catalog.get("modules")
    if not isinstance(modules, list) or len(modules) != len(MODULES):
        raise ValueError(f"{file.name}: incomplete runtime catalog")
    ids = set()
    for module in modules:
        verify_catalog_module(file, module, ids)
    return catalog


def verify_catalog_module(file, module, ids):
    if not isinstance(module, dict):
        raise ValueError(f"{file.name}: invalid catalog module")
    identity = module.get("id")
    if not isinstance(identity, str) or identity not in MODULES or identity in ids:
        raise ValueError(f"{file.name}: unknown or duplicate catalog module")
    ids.add(identity)
    if module.get("packageName") != "com.fongmi.runtime." + identity or module.get("compatibilityId") != MODULES[identity][0]:
        raise ValueError(f"{file.name}: incompatible catalog module")
    _, name = read_version(file, module)
    artifacts = module.get("artifacts")
    if not isinstance(artifacts, list) or len(artifacts) != len(ABIS):
        raise ValueError(f"{file.name}: incomplete catalog ABI pair")
    abis = set()
    for artifact in artifacts:
        verify_catalog_artifact(file, artifact, identity, name, abis)


def verify_catalog_artifact(file, artifact, module, version, abis):
    if not isinstance(artifact, dict):
        raise ValueError(f"{file.name}: invalid catalog artifact")
    abi = artifact.get("abi")
    if not isinstance(abi, str) or abi not in ABIS or abi in abis:
        raise ValueError(f"{file.name}: unknown or duplicate catalog ABI")
    abis.add(abi)
    if any(type(artifact.get(key)) is not int or artifact[key] <= 0 for key in ("size", "minSdk")):
        raise ValueError(f"{file.name}: invalid catalog artifact size or minSdk")
    sha = artifact.get("sha256")
    if not isinstance(sha, str) or not re.fullmatch("[0-9a-f]{64}", sha):
        raise ValueError(f"{file.name}: invalid catalog artifact digest")
    address = artifact.get("url")
    if not isinstance(address, str) or any(character.isspace() or ord(character) < 32 or ord(character) == 127 for character in address):
        raise ValueError(f"{file.name}: invalid catalog artifact URL")
    url = urlsplit(address)
    if (url.scheme not in ("https", "http") or not url.netloc or not url.hostname
            or url.query or url.fragment
            or url.path.rsplit("/", 1)[-1] != f"runtime-{module}-{version}-{abi}.apk"):
        raise ValueError(f"{file.name}: invalid catalog artifact URL")
    url.port


def input_modules(directory):
    names = {f"runtime-{module}-{abi.replace('-', '_')}.apk": module for module in MODULES for abi in ABIS}
    found = {file.name for file in directory.glob("*.apk")}
    unknown = found - names.keys()
    if unknown:
        raise ValueError(f"Unknown finalized APK: {sorted(unknown)[0]}")
    present = {names[name] for name in found}
    selected = [module for module in MODULES if module in present]
    if not selected:
        raise ValueError("No finalized runtime APK pairs")
    for module in selected:
        for abi in ABIS:
            source = directory / f"runtime-{module}-{abi.replace('-', '_')}.apk"
            if source.name not in found:
                raise ValueError(f"Missing finalized APK: {source}")
    return selected


def read_payloads(file, metadata, module, abi):
    path = BDJ_ARCHIVE if module == "bdj" else f"lib/{abi}/{MODULES[module][1]}"
    primary = {"path": path, "size": metadata.get("size"), "sha256": metadata.get("sha256")}
    if metadata.get("archive" if module == "bdj" else "library") != path or module == "bdj" and "library" in metadata:
        raise ValueError(f"{file.name}: incompatible runtime {'archive' if module == 'bdj' else 'library'}")
    payloads = metadata.get("files", [])
    if not isinstance(payloads, list) or not 0 < len(payloads) <= 256 or primary not in payloads:
        raise ValueError(f"{file.name}: invalid runtime payload list")
    libraries = {f"lib/{abi}/{name}" for name in LIBRARIES.get(module, ())}
    if module == "bdj" and payloads != [primary]:
        raise ValueError(f"{file.name}: invalid BD-J archive payload list")
    paths = read_payload_paths(file, payloads, module, libraries)
    verify_required_payloads(file, module, abi, libraries, paths)
    return payloads


def read_payload_paths(file, payloads, module, libraries):
    paths = set()
    for payload in payloads:
        add_payload_path(file, payload, paths, module, libraries)
    return paths


def add_payload_path(file, payload, paths, module, libraries):
    if not isinstance(payload, dict):
        raise ValueError(f"{file.name}: invalid runtime payload")
    path, size, sha = payload.get("path"), payload.get("size"), payload.get("sha256")
    if not isinstance(path, str) or path in paths or type(size) is not int or not 0 < size <= 128 * 1024 * 1024 or not isinstance(sha, str) or not re.fullmatch("[0-9a-f]{64}", sha):
        raise ValueError(f"{file.name}: invalid runtime payload")
    paths.add(path)
    verify_payload_path(file, path, module, libraries)


def verify_payload_path(file, path, module, libraries):
    if module == "bdj":
        if path != BDJ_ARCHIVE:
            raise ValueError(f"{file.name}: invalid BD-J archive path")
    elif path.startswith("lib/"):
        if path not in libraries:
            raise ValueError(f"{file.name}: invalid runtime library path")
    else:
        prefix = {"python": "assets/chaquopy/", "quickjs": "assets/js/", "nodejs": "assets/nodejs/"}.get(module)
        if prefix is None or not path.startswith(prefix) or ".." in path or "\\" in path or "//" in path or path.endswith("/"):
            raise ValueError(f"{file.name}: invalid runtime asset path")


def verify_required_payloads(file, module, abi, libraries, paths):
    if not libraries <= paths:
        raise ValueError(f"{file.name}: runtime library is missing")
    if module == "python" and any(f"assets/chaquopy/{name.format(abi=abi)}" not in paths for name in PYTHON_ASSETS):
        raise ValueError(f"{file.name}: Python asset is missing")
    if module == "nodejs" and "assets/nodejs/bootstrap.js" not in paths:
        raise ValueError(f"{file.name}: Node.js bootstrap is missing")
    if module == "quickjs" and not {"assets/js/lib/net.js", "assets/js/lib/spider.js"} <= paths:
        raise ValueError(f"{file.name}: QuickJS helpers are missing")


def verify_elf(file, stream, abi):
    header = stream.read(20)
    elf_class, machine = (2, 183) if abi == "arm64-v8a" else (1, 40)
    if len(header) != 20 or header[:4] != b"\x7fELF" or header[4] != elf_class or header[5] != 1 or int.from_bytes(header[18:20], "little") != machine:
        raise ValueError(f"{file.name}: runtime ELF ABI mismatch")


def verify_bdj_archive(file, apk, abi):
    if apk.getinfo(BDJ_ARCHIVE).compress_type != ZIP_STORED or any(name.startswith("lib/") or name.endswith(".dex") for name in apk.namelist()):
        raise ValueError(f"{file.name}: BD-J must be a stored data APK")
    architecture = "aarch64" if abi == "arm64-v8a" else "arm"
    libraries = {f"j2re-image/lib/{architecture}/{name}" for name in BDJ_LIBRARIES}
    with apk.open(BDJ_ARCHIVE) as stream, ZipFile(stream) as archive:
        verify_bdj_contents(file, archive, abi, libraries)


def verify_bdj_contents(file, archive, abi, libraries):
    entries = archive.infolist()
    paths = set()
    total = 0
    if not 0 < len(entries) <= 4096:
        raise ValueError(f"{file.name}: invalid BD-J archive entries")
    for entry in entries:
        verify_bdj_path(file, entry, paths)
        total += entry.file_size
        if not 0 <= entry.file_size <= 128 * 1024 * 1024 or total > 256 * 1024 * 1024:
            raise ValueError(f"{file.name}: invalid BD-J archive size")
        if entry.orig_filename.endswith(".so"):
            with archive.open(entry) as library:
                verify_elf(file, library, abi)
    if not libraries <= paths or "j2re-image/lib/rt.jar" not in paths:
        raise ValueError(f"{file.name}: BD-J runtime is incomplete")
    if archive.testzip() is not None:
        raise ValueError(f"{file.name}: BD-J archive CRC mismatch")
    verify_bdj_classes(file, archive)


def verify_bdj_path(file, entry, paths):
    path = entry.orig_filename
    if (path in paths or not path.startswith("j2re-image/") or "\\" in path or "\x00" in path
            or "//" in path or any(part in (".", "..") for part in path.split("/"))
            or stat.S_ISLNK(entry.external_attr >> 16)):
        raise ValueError(f"{file.name}: invalid BD-J archive path")
    paths.add(path)


def verify_bdj_classes(file, archive):
    with archive.open("j2re-image/lib/rt.jar") as stream, ZipFile(stream) as classes:
        entries = classes.infolist()
        if not 0 < len(entries) <= 32768 or sum(entry.file_size for entry in entries) > 256 * 1024 * 1024:
            raise ValueError(f"{file.name}: invalid BD-J rt.jar size")
        if not {"java/lang/Object.class", "java/net/URL.class", "sun/misc/Unsafe.class"} <= {entry.filename for entry in entries} or classes.testzip() is not None:
            raise ValueError(f"{file.name}: BD-J rt.jar is incomplete")


def verify_payload(file, apk, payload, abi):
    path, size, sha = payload["path"], payload["size"], payload["sha256"]
    if apk.getinfo(path).file_size != size:
        raise ValueError(f"{file.name}: runtime payload size mismatch")
    with apk.open(path) as stream:
        if path.endswith(".so"):
            verify_elf(file, stream, abi)
            stream.seek(0)
        if digest(stream) != sha:
            raise ValueError(f"{file.name}: runtime payload digest mismatch")


def verify_java_runtime(file, apk, metadata, module):
    if metadata.get("entryClass") != JAVA_ENTRIES[module] or type(metadata.get("javaApi")) is not int or metadata["javaApi"] != 1:
        raise ValueError(f"{file.name}: incompatible Java runtime API")
    names = apk.namelist()
    dex = [path for path in names if re.fullmatch(r"classes(?:[2-9]|1[0-6])?\.dex", path)]
    if len(names) != len(set(names)) or "classes.dex" not in dex or not 1 <= len(dex) <= 16 or any(path.endswith(".dex") and path not in dex for path in names):
        raise ValueError(f"{file.name}: invalid Java runtime DEX set")
    for path in dex:
        if not 112 <= apk.getinfo(path).file_size <= 64 * 1024 * 1024 or not re.fullmatch(rb"dex\n0[0-9]{2}\x00", apk.read(path)[:8]):
            raise ValueError(f"{file.name}: invalid Java runtime DEX")


def verify_mpv_runtime(file, apk, metadata, module, abi):
    hosts = metadata.get("hostLibraries")
    if not isinstance(hosts, dict) or set(hosts) != {"libc++_shared.so"} or any(not isinstance(sha, str) or not re.fullmatch("[0-9a-f]{64}", sha) for sha in hosts.values()):
        raise ValueError(f"{file.name}: invalid mpv host libraries")
    names = apk.namelist()
    libraries = {name for name in names if name.endswith(".so") or name.startswith("lib/") and not name.endswith("/")}
    if len(names) != len(set(names)) or libraries != {f"lib/{abi}/{name}" for name in LIBRARIES[module]} or any(name.endswith(".dex") for name in names):
        raise ValueError(f"{file.name}: mpv must contain only its native library pair")


def read_apk(file, module, abi, aapt2):
    package = "com.fongmi.runtime." + module
    profile, _ = MODULES[module]
    with ZipFile(file) as apk:
        metadata = read_runtime_metadata(file, apk, module, abi)
        code, name = read_version(file, metadata)
        verify_apk_payloads(file, apk, metadata, module, abi)
    min_sdk = read_manifest(file, module, abi, code, name, aapt2)
    with file.open("rb") as stream:
        sha = digest(stream)
    return {"id": module, "packageName": package, "compatibilityId": profile, "versionCode": code, "versionName": name,
            "artifact": {"abi": abi, "minSdk": min_sdk, "size": file.stat().st_size, "sha256": sha}}


def read_runtime_metadata(file, apk, module, abi):
    entry = apk.getinfo("assets/runtime.json")
    if not 0 < entry.file_size <= 64 * 1024:
        raise ValueError(f"{file.name}: invalid runtime metadata size")
    metadata = json.loads(apk.read(entry))
    if (not isinstance(metadata, dict) or metadata.get("module") != module
            or metadata.get("packageName") != "com.fongmi.runtime." + module
            or metadata.get("compatibilityId") != MODULES[module][0] or metadata.get("abi") != abi):
        raise ValueError(f"{file.name}: incompatible runtime metadata")
    return metadata


def verify_apk_payloads(file, apk, metadata, module, abi):
    if module in JAVA_ENTRIES:
        verify_java_runtime(file, apk, metadata, module)
    for payload in read_payloads(file, metadata, module, abi):
        if payload["path"].startswith("lib/") and apk.getinfo(payload["path"]).compress_type != ZIP_STORED:
            raise ValueError(f"{file.name}: {module} libraries must be stored uncompressed")
        verify_payload(file, apk, payload, abi)
    if module == "bdj":
        verify_bdj_archive(file, apk, abi)
    if module == "mpv":
        verify_mpv_runtime(file, apk, metadata, module, abi)


def read_manifest(file, module, abi, code, name, aapt2):
    result = subprocess.run([str(aapt2), "dump", "badging", str(file)], capture_output=True, text=True, encoding="utf-8", check=True)
    lines = result.stdout.splitlines()
    package_line = next((line for line in lines if line.startswith("package:")), "")
    fields = dict(re.findall(r"(\w+)='([^']*)'", package_line))
    if fields.get("name") != "com.fongmi.runtime." + module or fields.get("versionCode") != str(code) or fields.get("versionName") != name:
        raise ValueError(f"{file.name}: manifest package or version mismatch")
    sdk = next((line for line in lines if line.startswith(("minSdkVersion:", "sdkVersion:"))), "")
    match = re.fullmatch(r"(?:minSdkVersion|sdkVersion):'([0-9]+)'", sdk)
    if match is None or int(match[1]) <= 0:
        raise ValueError(f"{file.name}: invalid manifest minSdk")
    native = next((line for line in lines if line.startswith("native-code:")), "")
    if re.findall(r"'([^']*)'", native) != ([] if module == "bdj" else [abi]):
        raise ValueError(f"{file.name}: manifest native ABI mismatch")
    return int(match[1])


def check_existing(file, artifact):
    if not file.exists():
        return
    if file.is_file() and file.stat().st_size == artifact["size"]:
        with file.open("rb") as stream:
            if digest(stream) == artifact["sha256"]:
                return
    raise ValueError(f"Refusing to overwrite different APK: {file}")


def copy_apk(source, target, artifact):
    try:
        output = target.open("xb")
    except FileExistsError:
        check_existing(target, artifact)
        return
    try:
        with output, source.open("rb") as stream:
            shutil.copyfileobj(stream, output)
        check_existing(target, artifact)
    except BaseException:
        target.unlink()
        raise


def read_module_pair(directory, output, module, aapt2, url_base):
    copies = []
    current = None
    for abi in ABIS:
        source = directory / f"runtime-{module}-{abi.replace('-', '_')}.apk"
        if not source.is_file():
            raise ValueError(f"Missing finalized APK: {source}")
        metadata = read_apk(source, module, abi, aapt2)
        artifact = metadata.pop("artifact")
        if current is None:
            current = {**metadata, "artifacts": []}
        elif metadata != {key: value for key, value in current.items() if key != "artifacts"}:
            raise ValueError(f"{module}: versions differ between ABIs")
        target = output / f"runtime-{module}-{metadata['versionName']}-{abi}.apk"
        artifact["url"] = f"{url_base}/{target.name}"
        current["artifacts"].append(artifact)
        copies.append((source, target, artifact))
    return current, copies


def merge_published_module(module, current, old):
    if current["versionCode"] < old["versionCode"]:
        raise ValueError(f"{module}: refusing runtime version downgrade")
    if current["versionCode"] == old["versionCode"]:
        artifacts = {artifact["abi"]: artifact for artifact in old["artifacts"]}
        keys = ("abi", "minSdk", "size", "sha256")
        if current["versionName"] != old["versionName"] or any(any(artifact[key] != artifacts[artifact["abi"]][key] for key in keys) for artifact in current["artifacts"]):
            raise ValueError(f"{module}: same runtime version has different metadata or APK bytes")
        current = old
    elif current["versionName"] == old["versionName"]:
        raise ValueError(f"{module}: refusing to replace a published runtime version name")
    return current


def write_catalog(output, catalog):
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", newline="\n", dir=output, delete=False) as stream:
            temporary = Path(stream.name)
            json.dump(catalog, stream, indent=2, ensure_ascii=False)
            stream.write("\n")
        os.replace(temporary, output / "catalog.json")
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def publish(directory, output, aapt2="aapt2", url_base=DEFAULT_URL, base_catalog=None):
    url_base = url_base.rstrip("/")
    url = urlsplit(url_base)
    if url.scheme not in ("https", "http") or not url.netloc or url.query or url.fragment:
        raise ValueError("URL base must be an HTTP(S) directory without query or fragment")
    catalog = read_catalog(base_catalog) if base_catalog is not None else {"schema": 1, "modules": []}
    previous = {module["id"]: module for module in catalog["modules"]}
    selected = input_modules(directory) if base_catalog is not None else MODULES
    modules, copies = {}, []
    for module in selected:
        current, pending = read_module_pair(directory, output, module, aapt2, url_base)
        copies.extend(pending)
        if module in previous:
            current = merge_published_module(module, current, previous[module])
        modules[module] = current
    if base_catalog is None:
        catalog["modules"] = list(modules.values())
    else:
        catalog["modules"] = [modules.get(module["id"], module) for module in catalog["modules"]]
    for _, target, artifact in copies:
        check_existing(target, artifact)
    output.mkdir(parents=True, exist_ok=True)
    for source, target, artifact in copies:
        copy_apk(source, target, artifact)
    write_catalog(output, catalog)
    return catalog


def main():
    parser = argparse.ArgumentParser(description="Create catalog.json and versioned APKs from finalized runtime modules.")
    parser.add_argument("directory", type=Path, help="Twelve finalized runtime APKs, or complete changed module pairs with --base-catalog")
    parser.add_argument("--output", type=Path, required=True, help="Catalog and versioned APK directory")
    parser.add_argument("--aapt2", default="aapt2", help="Android SDK aapt2 executable")
    parser.add_argument("--url-base", default=DEFAULT_URL, help="Release asset directory URL")
    parser.add_argument("--base-catalog", type=Path, help="Existing full catalog whose unchanged module entries are preserved")
    args = parser.parse_args()
    try:
        publish(args.directory, args.output, args.aapt2, args.url_base, args.base_catalog)
    except (OSError, ValueError, KeyError, BadZipFile, subprocess.CalledProcessError) as error:
        parser.exit(1, f"error: {error}\n")
    print(f"Created {args.output / 'catalog.json'} and versioned runtime APKs")


if __name__ == "__main__":
    main()
