# 官方 5.6.6 原生组件核验

把配套 ZIP 里的 `.github`、`scripts` 目录和本文件复制到你的 `fongmi` 仓库根目录，
在 GitHub Desktop 中提交，提交说明建议用 `Inspect official native [skip ci]`，
推送后到仓库 Actions 页面手动运行下面的工作流。`[skip ci]` 可避免此次仅
上传检查工具的提交额外触发原有四版本打包；手动运行不受影响。

本仓库现有的 `.github/workflows/main.yml` 使用 `FongMi/media` 的公开提交
`3c2cbe8ac742c2fe15eff52f03eeb3b1b648848d` 来构建播放器依赖。
官方 5.6.6 APK 比当前自建 APK 多五个原生库以及两个 BD-J 资源。

在 GitHub 仓库的 **Actions → Inspect official 5.6.6 native dependencies → Run workflow**
运行一次，会从官方 Release 下载手机 arm64 和 armeabi 两种 ABI 的 APK。
工具先校验官方发布的 SHA256 和库的 ELF 架构，再把多出来的文件写入
`official-5.6.6-native-inspection` 工作流产物，并生成 `inspection.json`。
同一产物还会包含六个关键调用类的 JADX 反编译参考源码与日志，以及 ELF
依赖清单；这些反编译文件只是分析资料，不是可直接编译的 Media3 源码。
这个工作流不会修改 `main.yml`、创建 GitHub Release 或替换现有 APK。

本机已有官方 APK 时也可直接核验 arm64：

```bash
python3 scripts/recover_official_native.py \
  --arm64 /path/to/mobile-arm64_v8a.apk \
  --output /path/to/inspection
```

## 兼容性结论

| 官方 APK 原生文件 | 官方 APK 里的调用类 | 公开 Media3 源码现状 |
| --- | --- | --- |
| `libmedia3ass.so` | `androidx.media3.exoplayer.libass.LibassNative` | 缺少与应用源码匹配的 `LibassConfiguration`、`LibassSubtitleController` 等接口 |
| `libisoJNI.so` | `androidx.media3.exoplayer.iso.IsoNavigationSession`、`androidx.media3.extractor.iso.udf.NativeUdfFileSystem` | 这些类未包含在公开分支；当前应用光盘菜单入口返回 `false` |
| `libffmpegDoviJNI.so` | `androidx.media3.decoder.ffmpeg.FfmpegDolbyVisionP5Native` | 公开分支缺少这个类；原生库另外依赖相同版本的 `libavcodec.so` 和 `libavutil.so` |
| `libcmg_decrypt.so` | `androidx.media3.exoplayer.hls.CmgNativeRuntime` | 公开分支缺少对应运行时类 |
| `libmedia3effect.so` | Media3 视频特效 JNI | 需确认 Media3 Java 模块版本 |
| `assets/bdj/*.jar` | BD-J 菜单运行时 | 需要匹配的 ISO 菜单 Java 类和调用链 |

**不要把检查产物直接复制进 `app/src/main/jniLibs` 或发布成正式 APK。**
只有原生文件、缺少其 Java 调用类时，不会自动恢复功能；其中 Dolby Vision
原生库还依赖官方版本的 FFmpeg 库。下一阶段需恢复并编译配套 Media3 源码，
逐项用 APK 检查和实际播放验证。通过构建只能证明 Java API 与 Gradle 依赖兼容，
不能替代设备上的字幕、ISO 菜单和 Dolby Vision 播放测试。
