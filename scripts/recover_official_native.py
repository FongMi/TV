"""Extract and verify the 5.6.6 native files for compatibility work.

This tool writes an inspection artifact. It deliberately does not modify app/ or
claim that the public Media3 AARs can call the recovered native libraries.
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
    "libffmpegDoviJNI.so",
    "libmedia3ass.so",
    "libisoJNI.so",
    "libcmg_decrypt.so",
    "libmedia3effect.so",
)
BDJ_FILES = ("libbluray-j2se-1.4.1.jar", "libbluray-awt-j2se-1.4.1.jar")
EXPECTED_ELF = {"arm64-v8a": (2, 183), "armeabi-v7a": (1, 40)}
REQUIRED_MEDIA3_CLASSES = (
    "libraries/exoplayer/src/main/java/androidx/media3/exoplayer/iso/IsoNavigationSession.java",
    "libraries/exoplayer_libass/src/main/java/androidx/media3/exoplayer/libass/LibassConfiguration.java",
    "libraries/exoplayer_libass/src/main/java/androidx/media3/exoplayer/libass/LibassSubtitleController.java",
    "libraries/decoder_ffmpeg/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegDolbyVisionP5Native.java",
    "libraries/exoplayer_hls/src/main/java/androidx/media3/exoplayer/hls/CmgNativeRuntime.java",
    "libraries/extractor/src/main/java/androidx/media3/extractor/iso/udf/NativeUdfFileSystem.java",
)


def file_sha256(path: Path) -> str:
    sha = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()


def inspect_media3_root(root: Path) -> list[str]:
    if not (root / "gradlew").is_file() or not (root / "libraries").is_dir():
        raise ValueError(f"not a Media3 checkout: {root}")
    return [name for name in REQUIRED_MEDIA3_CLASSES if not (root / name).is_file()]


def extract_apk(apk: Path, abi: str, expected_sha256: str, output_dir: Path) -> list[dict]:
    if abi not in EXPECTED_ELF:
        raise ValueError(f"unsupported ABI: {abi}")
    if file_sha256(apk) != expected_sha256.lower():
        raise ValueError(f"APK SHA256 mismatch: {apk}")

    wanted = {f"lib/{abi}/{name}": f"jniLibs/{abi}/{name}" for name in NATIVE_FILES}
    wanted.update({f"assets/bdj/{name}": f"assets/bdj/{name}" for name in BDJ_FILES})
    with zipfile.ZipFile(apk) as archive:
        # Check all entries before writing anything. This also checks each ZIP CRC.
        payloads = {}
        for source, target in wanted.items():
            try:
                payloads[target] = archive.read(source)
            except KeyError as error:
                raise ValueError(f"missing official file: {source}") from error
        for name in NATIVE_FILES:
            data = payloads[f"jniLibs/{abi}/{name}"]
            elf_class, machine = EXPECTED_ELF[abi]
            if (len(data) < 20 or data[:4] != b"\x7fELF" or
                    data[4] != elf_class or data[5] != 1 or
                    struct.unpack_from("<H", data, 16)[0] != 3 or
                    struct.unpack_from("<H", data, 18)[0] != machine):
                raise ValueError(f"{name} is not a shared ELF for {abi}")

    records = []
    for target, data in payloads.items():
        destination = output_dir / target
        if destination.exists() and destination.read_bytes() != data:
            raise ValueError(f"official APKs disagree about {target}")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        records.append({"path": target, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
    return records


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--arm64", type=Path, help="Official 5.6.6 mobile-arm64_v8a.apk")
    parser.add_argument("--armeabi", type=Path, help="Official 5.6.6 mobile-armeabi_v7a.apk")
    parser.add_argument("--output", type=Path, required=True, help="Separate inspection output folder")
    parser.add_argument("--media3-root", type=Path, help="Pinned public FongMi/media source folder")
    options = parser.parse_args()
    if not options.arm64 and not options.armeabi:
        parser.error("provide --arm64 or --armeabi")
    results = {}
    for abi, apk in (("arm64-v8a", options.arm64), ("armeabi-v7a", options.armeabi)):
        if apk:
            results[abi] = {
                "apk_sha256": APK_SHA256[abi],
                "files": extract_apk(apk, abi, APK_SHA256[abi], options.output),
            }
    report = {"version": "5.6.6", "official_files": results, "ready_to_integrate": False}
    if options.media3_root:
        report["missing_java_sources"] = inspect_media3_root(options.media3_root)
    options.output.mkdir(parents=True, exist_ok=True)
    (options.output / "inspection.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Verified {len(results)} ABI(s); extracted to {options.output}")
    if report.get("missing_java_sources"):
        print(f"Missing {len(report['missing_java_sources'])} matching Media3 Java sources; native files alone cannot restore playback.")


if __name__ == "__main__":
    main()
