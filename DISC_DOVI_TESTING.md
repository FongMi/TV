# 光盘菜单与 Dolby Vision P5 设备验证

本分支从校验过哈希的官方 APK 提取含 `discnav` 命令的 `libmpv.so`，与公开
Media3 的播放器 Java 接口和 FFmpeg 预编译库做动态符号核对后打包。
官方 APK 的 `libisoJNI.so`、BD-J 运行时及独立的 `libffmpegDoviJNI.so` 尚未移植。
FFmpeg/libplacebo 的 P5 RPU 映射实现已包含在公开 Media3 源码和此前的
源码构建 APK 中，本次增加路径检查和设备日志。

## 准备

1. 安装本分支生成的对应 ARM APK，先使用无加密、可合法测试的 DVD-Video ISO
   和带 HDMV 菜单的 Blu-ray ISO。记录文件来源、设备型号、Android 版本。
2. 在播放器设置中选 **MPV** 引擎，打开手机或电视可读取的 ISO。
   光盘菜单只有当 MPV 的 `disc-menu-active` 属性可用时才显示。
3. 用菜单按钮、电视遥控器的方向/确认/返回键以及手机触控分别操作。
   检查高亮、进入影片、回到菜单和音轨切换，并记下不支持的镜像。
4. 使用确知为 Dolby Vision Profile 5 且带 RPU 的测试文件，切换至 **EXO**
   引擎，检查实际画面颜色、字幕及拖动进度后的播放。分别在有/无原生 DV
   输出的显示设备上测试；不要只依据 APK 大小判断映射是否生效。

## 收集证据

在播放前连接 ADB，并用以下命令记录解码器选择和 FFmpeg 原生错误：

```bash
adb logcat -c
adb logcat -v time > disc-dovi-logcat.txt
```

停止录制后，提供出现问题的时间点、屏幕录制或照片、相应的 ISO/视频媒体
信息以及日志。应用会把 DV 格式、RPU 配置标志和所选视频解码器写入
`DolbyVisionPlayback` 标签；FFmpeg 的原生日志可用于判断 RPU/GLES 映射
是否进入工作路径。构建和单元测试无法替代上述 ARM 设备播放验证。

MPV 菜单路径依赖镜像格式和设备可读取的 URI；BD-J 菜单及官方专用 JNI
在此版本仍属后续开发范围。
