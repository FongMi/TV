import argparse
import hashlib
import json
import os
import tempfile
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ABIS = {"arm64-v8a", "armeabi-v7a"}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "TV-Python-Runtime"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def verified_bytes(path, url, expected):
    if path.is_file():
        data = path.read_bytes()
        if digest(data) == expected:
            return data
    data = fetch(url)
    if digest(data) != expected:
        raise ValueError(f"SHA-256 mismatch: {url}")
    return data


def asset_name(name):
    if not isinstance(name, str) or not name or name in {".", ".."} or any(character in name for character in "/\\:"):
        raise ValueError(f"Invalid Python asset filename: {name}")
    return name


def read_manifest(data, lock, kind):
    manifest = json.loads(data)
    producer = manifest["producer"]
    if any(producer[key] != lock[key] for key in ["repository", "commit", "tag"]):
        raise ValueError(f"Python producer mismatch: {kind}")
    if set(manifest["abis"]) != ABIS:
        raise ValueError(f"Incomplete Python ABIs: {kind}")
    return manifest


def collect_assets(lock, directory, base):
    assets = {}
    manifests = {}
    for kind in ["wheels", "native-bundles"]:
        source = lock["manifests"][kind]
        name = asset_name(source["file"])
        data = verified_bytes(directory / kind / "manifest.json", base + name, source["sha256"])
        manifest = read_manifest(data, lock, kind)
        manifests[kind] = data
        for abi, report in manifest["abis"].items():
            if report["abi"] != abi:
                raise ValueError(f"Python ABI mismatch: {kind}")
            entries = report["wheels"] if kind == "wheels" else [report]
            if not entries:
                raise ValueError(f"Missing Python assets: {kind}/{abi}")
            for entry in entries:
                name = asset_name(entry["file"])
                key = (kind, name)
                if key in assets:
                    raise ValueError(f"Duplicate Python asset: {name}")
                assets[key] = entry["sha256"]
    return assets, manifests


def stage_assets(assets, directory, staging, base):
    for (kind, name), expected in assets.items():
        destination = directory / kind / name
        if destination.is_file() and digest(destination.read_bytes()) == expected:
            continue
        data = verified_bytes(destination, base + name, expected)
        staged = staging / kind / name
        staged.parent.mkdir(parents=True, exist_ok=True)
        staged.write_bytes(data)


def install_assets(kind, data, directory, staging):
    folder = directory / kind
    folder.mkdir(parents=True, exist_ok=True)
    staged = staging / kind
    if staged.is_dir():
        for source in staged.iterdir():
            os.replace(source, folder / source.name)
    metadata = staging / (kind + "-manifest.json")
    metadata.write_bytes(data)
    os.replace(metadata, folder / "manifest.json")


def sync(lock_file, directory, source=None):
    lock = json.loads(lock_file.read_text(encoding="utf-8"))
    repository = lock["repository"]
    if repository.count("/") != 1 or any(value in repository for value in ["..", "\\", ":", "?", "#"]):
        raise ValueError("Invalid Python producer repository")
    base = source.resolve().as_uri() + "/" if source is not None else f"https://github.com/{repository}/releases/download/{urllib.parse.quote(lock['tag'], safe='')}/"
    directory.mkdir(parents=True, exist_ok=True)
    assets, manifests = collect_assets(lock, directory, base)
    with tempfile.TemporaryDirectory(prefix="python-sync-", dir=directory) as temporary:
        staging = Path(temporary)
        if staging.resolve().parent != directory.resolve():
            raise ValueError("Invalid Python staging directory")
        stage_assets(assets, directory, staging, base)
        for kind, data in manifests.items():
            install_assets(kind, data, directory, staging)
    return {"repository": repository, "tag": lock["tag"], "commit": lock["commit"], "verifiedAssets": len(assets)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, help="Directory containing the CI release artifact")
    args = parser.parse_args()
    print(json.dumps(sync(ROOT / "chaquo/runtime/native.lock.json", ROOT / "chaquo/runtime", args.source), indent=2))
