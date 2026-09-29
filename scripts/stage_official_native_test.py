"""Stage verified official 5.6.6 native payloads for compatibility testing.

This script intentionally stages only the official native files which are absent
from the public Media3 checkout. The source-built libmedia3ass.so is kept to
avoid replacing it with a JNI-incompatible binary.

Packaging these files proves they are present in the APK; it does not by itself
restore the missing Java call chains.
"""

import argparse
import hashlib
import json
import struct
import zipfile
from pathlib import Path

APK_SHA256 = {
    "arm64-v8a": "0906ebed0424c25df4886735efc40146044f9c78ee2b7e7a5d1c470da56c6173",
    "armeabi-v7a": "ff6bfa26006a50c6825b64d2f6a3107dfa99b8277509909bd10987b6ca09fcc5",
}

NATIVE_FILES = (
    "libisoJNI.so",
    "libffmpegDoviJNI.so",
    "libcmg_decrypt.so",
    "libmedia3effect.so",
)

BDJ_FILES = (
    "libbluray-j2se-1.4.1.jar",
    "libbluray-awt-j2se-1.4.1.jar",
)

EXPECTED_ELF = {
    "arm64-v8a": (2, 183),
    "armeabi-v7a": (1, 40),
}


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify_elf(data: bytes, abi: str, name: str) -> None:
    elf_class, machine = EXPECTED_ELF[abi]
    if (
        len(data) < 20
        or data[:4] != b"\x7fELF"
        or data[4] != elf_class
        or data[5] != 1
        or struct.unpack_from("<H", data, 16)[0] != 3
        or struct.unpack_from("<H", data, 18)[0] != machine
    ):
        raise ValueError(f"{name} is not a valid shared ELF for {abi}")


def extract_payload(apk: Path, abi: str, app_root: Path, bdj_reference: dict[str, bytes]) -> list[dict]:
    expected_hash = APK_SHA256[abi]
    actual_hash = sha256_file(apk)
    if actual_hash != expected_hash:
        raise ValueError(
            f"Official {abi} APK SHA256 mismatch: expected {expected_hash}, got {actual_hash}"
        )

    records: list[dict] = []
    with zipfile.ZipFile(apk) as archive:
        for name in NATIVE_FILES:
            source = f"lib/{abi}/{name}"
            data = archive.read(source)
            verify_elf(data, abi, name)
            target = app_root / "src" / "main" / "jniLibs" / abi / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            records.append(
                {
                    "path": target.as_posix(),
                    "bytes": len(data),
                    "sha256": sha256_bytes(data),
                }
            )

        for name in BDJ_FILES:
            source = f"assets/bdj/{name}"
            data = archive.read(source)
            previous = bdj_reference.get(name)
            if previous is not None and previous != data:
                raise ValueError(f"Official APKs disagree about BD-J asset {name}")
            bdj_reference[name] = data

    return records


def write_bdj_assets(app_root: Path, payloads: dict[str, bytes]) -> list[dict]:
    records: list[dict] = []
    for name, data in payloads.items():
        target = app_root / "src" / "main" / "assets" / "bdj" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        records.append(
            {
                "path": target.as_posix(),
                "bytes": len(data),
                "sha256": sha256_bytes(data),
            }
        )
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--arm64", type=Path, required=True)
    parser.add_argument("--armeabi", type=Path, required=True)
    parser.add_argument("--app-root", type=Path, default=Path("app"))
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    app_root = args.app_root.resolve()
    bdj_payloads: dict[str, bytes] = {}
    records: dict[str, list[dict]] = {}

    for abi, apk in (
        ("arm64-v8a", args.arm64),
        ("armeabi-v7a", args.armeabi),
    ):
        records[abi] = extract_payload(apk, abi, app_root, bdj_payloads)

    records["bdj"] = write_bdj_assets(app_root, bdj_payloads)
    total_bytes = sum(item["bytes"] for group in records.values() for item in group)

    report = {
        "official_version": "5.6.6",
        "staged_native_files": list(NATIVE_FILES),
        "staged_bdj_files": list(BDJ_FILES),
        "kept_source_built_libmedia3ass": True,
        "total_staged_bytes": total_bytes,
        "records": records,
        "warning": (
            "Payload presence is a packaging milestone only. Missing Java/Media3 call chains "
            "still need to be restored and tested on ARM hardware."
        ),
    }

    report_path = args.report or (app_root.parent / "official-native-stage.json")
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"Staged {len(NATIVE_FILES)} native libraries for each ABI and {len(BDJ_FILES)} BD-J assets")
    print(f"Total staged bytes: {total_bytes}")
    print(f"Report: {report_path}")


if __name__ == "__main__":
    main()
