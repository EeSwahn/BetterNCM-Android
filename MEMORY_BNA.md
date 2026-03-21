# 项目记忆：BNA

## 📌 项目概览
- **名称**: BNA
- **平台**: Android (最低 SDK 24, 目标 SDK 34)
- **技术栈**: Kotlin, Jetpack Compose, 协程 (Coroutines), Flow。
- **核心功能**: 集成网易云音乐的播放器，支持高精度歌词动画和卡拉OK式人声控制。

## 🏗️ 架构 (MVVM)
- **UI 层**: 基于 Jetpack Compose 的界面 (`ui/screen`)。
- **ViewModel 层**: 状态管理 (`viewmodel`)，使用 `StateFlow` 进行响应式更新。
- **数据层**: 仓库模式 (`data/repository`)，配合 Retrofit 服务 (`data/network`)。
- **播放引擎**: 封装了 Media3/ExoPlayer 的 `MusicPlayer` 单例。

## 🔑 关键组件与特性
- **歌词引擎** (`LyricsScreen.kt`):
    - 支持 **YRC** (逐词) 和 **LRC** 格式。
    - 具备逐词 **浮动动画** (Floating Animation)，支持调节动画强度。
    - 高频 UI 刷新 (约 32ms/次)，确保动画与音频毫秒级同步。
- **音频处理** (`SuiXinChangAudioProcessor.kt`):
    - 处理 **4 声道 PCM** 输入（伴奏 L/R + 人声 L/R）。
    - 支持实时 **人声音量控制**，实现随心唱/卡拉OK效果。
- **网络/API** (`NeteaseApiService.kt`):
    - 使用自定义请求头和 **EAPI** 协议获取逐词歌词。
    - 模拟 **安卓平板/PC** 身份，绕过扫码限制实现无忧登录。
- **后台服务** (`MusicService.kt`):
    - 使用 Android `Service` 支持后台播放与通知栏控制。

## 🔍 核心组件详解：LyricsScreen.kt (已完成解耦重构)
为了提升可维护性，原 1700 行的 `LyricsScreen.kt` 已被拆分为 `ui/screen/lyrics/` 目录下的多个模块。

### 1. 目录结构
- **`LyricsScreen.kt`**: 入口文件，负责高层级状态监听、背景模糊封面绘制及布局分发。
- **`lyrics/TabletLyricsLayout.kt`**: 专门处理平板/大屏的左右分布布局。
- **`lyrics/PhoneLyricsLayout.kt`**: 专门处理手机端的封面-歌词切换布局。
- **`lyrics/LyricsComponents.kt`**: 存放所有共享 UI 组件（如 `LyricsPanel`, `SuiXinChangButton` 等）。
- **`lyrics/LyricsUtils.kt`**: 存放工具函数（时间格式化、逐词进度计算）及 `rememberPreference` 辅助方法。

### 2. 核心逻辑保持
- **高频刷新**：保持 60fps 刷新，毫秒级轮询，确保动画同步。
- **逐词动画**：浮动 (TranslationY) 与 缩放 (Scale) 逻辑封装在 `LyricLineItem` 中。
- **视觉重心**：滚动对齐算法保持在 38% 的位置。
- **随心唱**：音量条交互及 Popup 窗口已解耦至 `SuiXinChangButton` 独立维护。

### 3. 重构说明
- 极大降低了单一文件的开发心智负担，支持手机与平板布局独立演进。
- 所有 UI 参数滑块通过 `LyricsUtils` 的 Preference 委托实现跨界面配置共享。

## 🛠️ 开发流程
- **一键运行**: `D:\CODE\Run_BNA.ps1` (无线 ADB 调试，自动构建并安装)。
- **日志**: 根目录存有大量调试日志 (如 `all.log`, `logcat.txt`)，用于解决复杂的动画同步与音频处理问题。

## 💡 给 AI 的重要提示
- **同步精度**: 项目极度重视歌词同步的丝滑感。修改 `MusicPlayer` 的进度更新频率会直接影响动画效果。
- **API 敏感性**: 网易云 API 对加密和 Header 极其敏感（尤其是 EAPI）。修改接口时必须确保客户端身份 (Android Tablet) 匹配。
- **布局优化**: 针对手机和平板/大屏设备均进行了适配。

## 🧠 歌词界面更新
- **逐字歌词的坑**: `LyricsComponents.kt` 里当前行和非当前行必须尽量保持同一套测量结构；一旦只给当前行额外 padding / 不同布局，高度切换就会导致居中歌词抽动或跳动。
- **透明度责任要单一**: 逐字模式下不要同时让父容器和单字都做明暗过渡；父级 `alpha` 突变会造成整行先变白一下，再回到逐字动画。
- **字距稳定性**: 逐字开关开启后，不能只在当前行切换成按字排版，否则居中瞬间会因为排版模型切换出现字距变化。
- **平板滑块默认值入口**: 默认值写在 `lyrics/TabletLyricsLayout.kt` 的 `rememberFloatPreference(...)` 初始值里；只影响新安装或清数据，不会覆盖已保存设置。
