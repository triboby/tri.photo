<p align="center">
  <img src="docs/icon.png" width="128" height="128" alt="tri.photo 图标">
</p>

<h1 align="center">tri.photo</h1>

<p align="center">
  把家里群晖 Synology Photos 的照片放到电视上：电视空闲时是相框（屏保），打开时是幻灯片。<br>
  面向 Android TV（目标设备 Sony BRAVIA / Android 10）。桌面名「回忆」。<br>
  <sub>A photo-frame screensaver and slideshow for Android TV that reads a Synology Photos share link.</sub>
</p>

## 功能（第一版）

- **分享链接接入**：在 Synology Photos 里把相册设为公开分享，把链接（如 `http://192.168.50.94:5000/mo/sharing/xxxx`）填进来即可，不需要账号。保存前会先试读一次相册。
- **屏保**：系统屏幕保护程序里选「回忆」。黑底播放照片，按任意键退出。先放电视上缓存的照片，再在后台刷新相册，群晖休眠或断网时也能放。
- **全屏播放**：首页按「播放」或选一张照片开始。左 / 右键前后切换，OK 显示文件名和相机。
- **竖图双拼**：竖图优先和同一天的另一张竖图并排；单独的竖图用模糊的自身铺底。
- **Ken Burns**：横图缓慢缩放平移，可在设置里关掉。
- **视频**：用 Media3 播放，优先取群晖转码好的版本；屏保里默认静音。
- **日期和「N 年前」**：照片下方显示拍摄日期、地点（有的话）和距今几年，右上角时钟。
- **缓存**：缩略图（约 1920 像素）缓存在电视上，最多 800 MB；相册列表也缓存，打开即显示。

## 设为屏保

在电视「设置 → 设备偏好设置 → 屏幕保护程序」里选「回忆」。如果列表里没有：

```bash
adb shell settings put secure screensaver_components com.tritv.photo/.dream.PhotoDreamService
```

## 构建

需要 JDK 17+ 和 Android SDK（compileSdk 35，minSdk 23）。

```bash
./gradlew assembleRelease      # 输出 app/build/outputs/apk/release/app-release.apk（装到电视用这个）
./gradlew testDebugUnitTest    # 单元测试
```

每次推送都会由 GitHub Actions 构建，APK 在 Actions 运行页面的 Artifacts 里下载。release 包和 tri.iptv 用同一个调试签名，可以覆盖安装。

## 设计

设计方案（调研、App 形态、视觉规范、里程碑）见项目里的设计文档。图标和电视横幅由 `design/logo/render.py` 生成。

## 第三方资源

拉丁字母和数字使用 [Inter](https://github.com/rsms/inter) 字体（SIL Open Font License 1.1，见 [`app/FONT-LICENSE-OFL.txt`](app/FONT-LICENSE-OFL.txt)）。中文使用系统字体。
