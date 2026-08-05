# 🖤 ScreenMask
## ✨ 项目简介

**ScreenMask** 是一个跨平台「屏幕遮挡」工具，基于 **Kotlin Multiplatform + Compose Multiplatform** 构建，
通过系统悬浮窗权限叠加一层全屏黑屏，点击悬浮球即可一键开启，并支持多种手势退出与保活能力，
同一套 UI 与业务逻辑运行在 Android 与 iOS 上。

> 一套代码，多端运行 —— Kotlin Multiplatform 与 Compose 的跨端实践项目。

### 核心特性

- 🔘 **悬浮球一键黑屏**：可拖动的悬浮球常驻屏幕，点击即开启全屏黑屏，再点即退出
- ✋ **三种退出手势**：4 指及以上触摸 / 从底部上滑 / 画一个圆圈（O），全部可单独开关
- 📴 **息屏即退**：黑屏期间按电源键熄屏、或锁屏后解锁桌面，立即取消黑屏，防止“遮过头”
- 🛡️ **保活**：可选前台服务 + 常驻通知 / 唤醒锁，避免长时间黑屏后服务被杀
- 🌐 **国际化 (i18n)**：内置「跟随系统 / 中文 / English」资源
- 🌗 **深浅色主题**：支持亮色 / 暗色一键切换（樱花粉配色）
- 📱 **双端同 UI**：Android、iOS 共享全部界面与业务逻辑

---

## 🗂️ 项目结构

```
ScreenMask/
├── androidApp/      # Android 入口（MainActivity + Manifest）
├── iosApp/          # iOS 入口（SwiftUI 桥接，内嵌 Kotlin 框架）
└── shared/          # 跨平台共享代码（核心）
    └── src/
        ├── commonMain/
        │   ├── kotlin/zhiqiu/app/mk/
        │   │   ├── App.kt              # 根入口与主题
        │   │   ├── blackmask/          # 黑屏控制器抽象（expect）
        │   │   │   ├── BlackScreenController.kt
        │   │   │   └── BlackScreenStatus.kt
        │   │   ├── settings/           # 持久化（KeyValueStore + CarSettings）
        │   │   │   ├── KeyValueStore.kt
        │   │   │   └── CarSettings.kt
        │   │   └── ui/                 # Compose 界面与主题
        │   │       ├── home/           # 首页（开启/关闭黑屏）
        │   │       ├── settings/       # 设置页（退出方式 / 保活）
        │   │       ├── BlackScreenComponents.kt / BlackScreenViewModel.kt
        │   │       └── theme/          # 主题与配色
        │   │           └── AppTheme.kt
        │   └── composeResources/       # 多语言字符串资源（values / values-zh）
        ├── androidMain/                # Android 原生实现
        │   └── kotlin/zhiqiu/app/mk/
        │       ├── blackmask/          # 悬浮球 / 黑屏前台服务、手势识别、保活
        │       │   ├── BlackScreenService.kt
        │       │   ├── FloatingBallService.kt / FloatingBallView.kt
        │       │   ├── CircleGestureDetector.kt
        │       │   └── KeepAlive.kt
        │       └── settings/           # KeyValueStore → SharedPreferences
        └── iosMain/                    # iOS 实现（KeyValueStore → NSUserDefaults）
```

---

## 🔧 关键实现

### 架构：一套代码，双端共享
业务逻辑与界面全部位于 `shared` 模块的 `commonMain`，平台相关能力通过 Kotlin 的 `expect/actual` 下沉到各端：
- **黑屏能力**：`blackmask/BlackScreenController.kt` 声明期望，`androidMain` 用系统悬浮窗 + 前台服务实现，`iosMain` 为 no-op（iOS 平台能力受限，仅共享设置与 UI）。
- **持久化存储**：`settings/KeyValueStore.kt` 声明 `expect fun createKeyValueStore()`，各端分别落地（见下）。

### 黑屏机制（Android）
- 基于 `TYPE_APPLICATION_OVERLAY` 全屏覆盖层，叠加 `FLAG_KEEP_SCREEN_ON` 保证黑屏期间不熄屏。
- 悬浮球为可拖动的悬浮窗，点击触发 `BlackScreenService` 启停。
- 退出方式（可配置）：
  - **4 指及以上**同时触摸立即退出；
  - **底部上滑**（自下而上超过阈值距离）退出；
  - **画圈（O）**：单指画圈由 `CircleGestureDetector` 判定（路径长度、采样点、角度扫过的综合阈值）；
  - **息屏 / 解锁**：监听 `ACTION_SCREEN_OFF` 与 `ACTION_USER_PRESENT`，熄屏或解锁即退出。

### 保活（KeepAlive）
`KeepAlive` 提供统一的前台服务与唤醒锁帮助方法：
- **前台服务 + 常驻通知**：`startForeground(FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`，通知可点击直接退出黑屏；
- **唤醒锁**：`PARTIAL_WAKE_LOCK` 防止系统休眠后服务被冻结。

### 持久化
自研极简键值存储抽象 `KeyValueStore`（零第三方依赖），双端分别落地：

| 平台 | 实现 |
|------|------|
| Android | `SharedPreferences` |
| iOS | `NSUserDefaults` |

用于保存悬浮球开关、三种退出方式开关、前台服务 / 唤醒锁保活开关。

---

<div align="center">
Made with ❤️ and Kotlin Multiplatform
</div>
