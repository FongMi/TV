"""Check that the Exo libass dependency can run on both published ABIs."""

import io
import pathlib
import unittest
import zipfile


ROOT = pathlib.Path(__file__).resolve().parents[1]
AAR = ROOT / "app/libs/lib-exoplayer-libass-release.aar"


class LibassAarTest(unittest.TestCase):
    def test_aar_contains_java_bridge_and_both_android_native_libraries(self):
        self.assertTrue(AAR.is_file(), f"Missing Exo libass AAR: {AAR}")
        with zipfile.ZipFile(AAR) as aar:
            with zipfile.ZipFile(io.BytesIO(aar.read("classes.jar"))) as classes:
                self.assertIn(
                    "androidx/media3/exoplayer/libass/LibassPlaybackSession.class",
                    classes.namelist(),
                )

            for abi, elf_class in (("arm64-v8a", 2), ("armeabi-v7a", 1)):
                library = aar.read(f"jni/{abi}/libmedia3ass.so")
                with self.subTest(abi=abi):
                    self.assertTrue(library.startswith(b"\x7fELF"))
                    self.assertEqual(library[4], elf_class)
                    self.assertIn(b"JNI_OnLoad", library)


if __name__ == "__main__":
    unittest.main()
