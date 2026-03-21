# TKAndroid — WebView APP 构建说明

## 项目简介

将 `https://tk.kikirepository.cn/` 网站打包为 Android 原生 APP，基于 WebView 实现，支持 Cookie 持久化记忆（保持登录状态）。

## 项目结构

```
TKAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/cn/kikirepository/tk/
│   │   │   ├── MainActivity.java      # WebView 主界面
│   │   │   └── SplashActivity.java    # 启动页
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml  # 主界面布局（WebView + 进度条）
│   │   │   │   └── activity_splash.xml# 启动页布局
│   │   │   ├── values/
│   │   │   │   ├── strings.xml        # 字符串资源
│   │   │   │   ├── colors.xml         # 颜色资源
│   │   │   │   └── themes.xml         # 主题样式
│   │   │   └── xml/
│   │   │       ├── network_security_config.xml  # 网络安全配置
│   │   │       └── file_provider_paths.xml      # 文件共享配置
│   │   └── AndroidManifest.xml
│   ├── build.gradle                   # App 模块构建配置
│   └── proguard-rules.pro             # 混淆规则
├── build.gradle                       # 项目根构建配置
├── settings.gradle                    # 项目设置
├── gradle.properties                  # Gradle 属性
└── gradle/wrapper/
    └── gradle-wrapper.properties      # Gradle 版本配置
```

## 主要功能

| 功能 | 实现方式 |
|------|---------|
| WebView 加载网站 | `WebView.loadUrl()` |
| Cookie 持久化 | `CookieManager.flush()` 在 `onPause`/`onDestroy` 调用 |
| 第三方 Cookie | `setAcceptThirdPartyCookies(true)` |
| 返回键回退历史 | `webView.canGoBack()` / `webView.goBack()` |
| 文件上传 | `WebChromeClient.onShowFileChooser()` |
| 下拉刷新 | `SwipeRefreshLayout` |
| 进度条 | `onProgressChanged()` 回调 |
| 混合内容 | `MIXED_CONTENT_ALWAYS_ALLOW` |
| 兼容性 | minSdk 21 (Android 5.0+) |

## 构建步骤

### 方式一：通过 Android Studio（推荐）

1. 打开 Android Studio
2. 选择 **File → Open**，选择 `C:\Users\K\Documents\webAPP\TKAndroid` 目录
3. 等待 Gradle 同步完成
4. 选择 **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. APK 生成路径：`app/build/outputs/apk/debug/app-debug.apk`

### 方式二：通过命令行

> 需要先安装 [Android SDK](https://developer.android.com/studio#downloads) 并配置 `ANDROID_HOME` 环境变量

```powershell
cd C:\Users\K\Documents\webAPP\TKAndroid
.\gradlew assembleDebug
```

### 安装到设备

```powershell
# 确保已连接 Android 设备且开启 USB 调试
adb install app\build\outputs\apk\debug\app-debug.apk
```

## GitHub Actions 自动化打包

项目已配置 GitHub Actions，支持手动触发在线打包并自动将 APK 提交回仓库：

1. 将项目推送到 GitHub。
2. 在 GitHub 仓库页面点击 **Actions** 选项卡。
3. 选择左侧的 **Android Build** 工作流。
4. 点击右侧的 **Run workflow** 下拉菜单，点击绿色的 **Run workflow** 按钮。
5. 等待构建完成，成功后 `TK_Medicine_App.apk` 将自动出现在仓库根目录下。

## 应用图标

已预建一个简易的背景图标以保证构建成功。建议后期更换为正式图标：
- 背景文件：`app/src/main/res/drawable/ic_launcher_foreground.xml` (矢量)
- 适配性定义：`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
> 
> 如需添加自定义图标，请在 Android Studio 中右键 `res` 目录 → **New → Image Asset**。
> 需要生成以下目录的图标：
> - `mipmap-mdpi/ic_launcher.png`（48×48 px）
> - `mipmap-hdpi/ic_launcher.png`（72×72 px）
> - `mipmap-xhdpi/ic_launcher.png`（96×96 px）
> - `mipmap-xxhdpi/ic_launcher.png`（144×144 px）
> - `mipmap-xxxhdpi/ic_launcher.png`（192×192 px）
> - 以及对应的圆形图标 `ic_launcher_round.png`

## 兼容性说明

- **最低支持版本**：Android 5.0 (API 21)，覆盖约 99% 的 Android 设备
- **目标版本**：Android 14 (API 34)
- **硬件加速**：已启用，确保 WebView 渲染流畅
- **HTTP/HTTPS**：均支持（通过网络安全配置）
- **文件上传**：支持 Android 5.0+

## 自定义配置

如需修改目标网站，在 `MainActivity.java` 第 43 行改为：

```java
private static final String TARGET_URL = "https://你的网站地址/";
```

如需修改 APP 名称，编辑 `res/values/strings.xml`：

```xml
<string name="app_name">你的应用名</string>
```
