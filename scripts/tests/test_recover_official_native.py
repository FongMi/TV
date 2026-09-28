import hashlib
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path

from scripts.recover_official_native import extract_apk, inspect_media3_root


NATIVE_FILES = (
    "libffmpegDoviJNI.so",
    "libmedia3ass.so",
    "libisoJNI.so",
    "libcmg_decrypt.so",
    "libmedia3effect.so",
)
BDJ_FILES = (
    "libbluray-j2se-1.4.1.jar",
    "libbluray-awt-j2se-1.4.1.jar",
)


def fake_elf(abi):
    result = bytearray(64)
    result[:4] = b"\x7fELF"
    result[4] = 2 if abi == "arm64-v8a" else 1
    result[5] = 1
    struct.pack_into("<H", result, 16, 3)
    struct.pack_into("<H", result, 18, 183 if abi == "arm64-v8a" else 40)
    return bytes(result)


def fake_apk(path, abi, native_abi=None):
    with zipfile.ZipFile(path, "w") as output:
        for filename in NATIVE_FILES:
            output.writestr(f"lib/{abi}/{filename}", fake_elf(native_abi or abi))
        for filename in BDJ_FILES:
            output.writestr(f"assets/bdj/{filename}", b"example jar")
        output.writestr("assets/unrelated.txt", b"do not copy")
    return hashlib.sha256(path.read_bytes()).hexdigest()


class RecoverOfficialNativeTest(unittest.TestCase):
    def test_rejects_modified_apk_before_writing_files(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            apk = root / "official.apk"
            fake_apk(apk, "arm64-v8a")
            output = root / "result"
            with self.assertRaisesRegex(ValueError, "SHA256"):
                extract_apk(apk, "arm64-v8a", "0" * 64, output)
            self.assertFalse(output.exists())

    def test_extracts_only_missing_files_and_records_hashes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            apk = root / "official.apk"
            checksum = fake_apk(apk, "arm64-v8a")
            output = root / "result"
            records = extract_apk(apk, "arm64-v8a", checksum, output)
            self.assertEqual(len(records), 7)
            self.assertFalse((output / "assets/unrelated.txt").exists())
            extracted = output / "jniLibs/arm64-v8a/libisoJNI.so"
            self.assertEqual(extracted.read_bytes(), fake_elf("arm64-v8a"))
            entry = next(item for item in records if item["path"] == "jniLibs/arm64-v8a/libisoJNI.so")
            self.assertEqual(entry["sha256"], hashlib.sha256(fake_elf("arm64-v8a")).hexdigest())
            self.assertTrue((output / "assets/bdj/libbluray-j2se-1.4.1.jar").is_file())

    def test_rejects_native_binary_for_wrong_abi(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            apk = root / "official.apk"
            checksum = fake_apk(apk, "armeabi-v7a", native_abi="arm64-v8a")
            with self.assertRaisesRegex(ValueError, "armeabi-v7a"):
                extract_apk(apk, "armeabi-v7a", checksum, root / "result")

    def test_rejects_nonexistent_media3_checkout_instead_of_reporting_missing_apis(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, "Media3 checkout"):
                inspect_media3_root(Path(temp) / "absent")


if __name__ == "__main__":
    unittest.main()
