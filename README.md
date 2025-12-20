# 光遇徽章管理器 (Badge Manager)

[English Version](README_EN.md)

Badge Manager 是一个 Android 应用程序，帮助用户管理和快速启动“光遇star徽章”。它支持 NFC 读取功能，通过触碰
NFC 标签快速填充链接，并提供悬浮球功能以便快速访问。

## ✨ 主要功能

* **徽章管理 (CRUD)**:
    * 创建、读取、更新和删除徽章。
    * 徽章属性包括：标题、备注、链接 (URL)、标签 以及渠道类型。
* **NFC 集成**:
    * 支持 NFC 读取 (NDEF 格式)。
    * 在主页或详情页触碰 NFC 标签，可自动将标签内容填充至“链接”输入框。
* **悬浮窗服务**:
    * 提供一个全局悬浮球 (Floating Action Button)。
    * 允许用户在其他应用之上快速访问徽章功能（需开启悬浮窗权限）。
* **数据持久化**:
    * 使用 **Room Database** 进行本地数据存储，确保数据重启不丢失。
* **备份与还原**:
    * 支持将所有徽章数据导出为 JSON 文件，或从文件恢复。
* **数据同步**:
    * 支持在同网环境下, 将数据同步给其他设备。
* **国际化支持**:
    * 支持多种语言界面：简体中文、繁体中文（台湾）、英文。
* **现代化 UI**:
    * 完全使用 **Jetpack Compose** 构建，遵循 Material Design 3 规范。

## 🛠️ 技术栈

* **语言**: Kotlin
* **UI 框架**: Jetpack Compose (Material3)
* **架构**: MVVM (Model-View-ViewModel pattern with Repository)
* **本地存储**: Android Jetpack Room (SQLite)
* **硬件交互**: Android NFC API
* **后台服务**: Android Service (Floating Window)
* **网络通信**: Socket & UDP 广播（用于局域网同步）
* **构建工具**: Gradle (Kotlin DSL), Version Catalogs (libs.versions.toml)
* **版本控制**: Git
* **CI/CD**: GitHub Actions

## 💻 开发构建 (Build & Run)

### 前置要求

* Android Studio Ladybug 或更新版本。
* JDK 11 或更高版本。
* 一台支持 NFC 的 Android 设备（用于测试 NFC 功能，模拟器无法完全模拟 NFC 触碰）。

### 编译步骤

1. **克隆仓库**:
   ```bash
   git clone https://github.com/TatooiNoyo/BadgeManager.git
   ```
2. **打开项目**:
   启动 Android Studio，选择 "Open"，然后导航到克隆的目录。
3. **同步 Gradle**:
   等待 Android Studio 下载依赖并同步项目。
4. **运行应用**:
   连接 Android 设备并点击 "Run" 按钮。

### 构建发布版本

要构建发布版本的 APK 文件，可以使用以下命令：

```bash
./gradlew assembleRelease
```

构建好的 APK 文件将位于 `app/build/outputs/apk/release/` 目录下，文件名格式为 `BadgeManager_{版本号}_{版本代码}_release.apk`。

## 📝 使用指南

1. **添加徽章**: 在主页输入标题、备注，选择渠道。手动输入链接，或直接用手机背面触碰 NFC
   标签自动填入链接，点击“添加”按钮。
2. **编辑/删除**: 点击列表项进入详情页，修改内容后点击“保存更新”，或点击“删除”移除徽章。
3. **NFC 录入**: 在任何输入链接的界面，触碰 NFC 标签即可覆盖当前链接输入框内容。
4. **徽章使用**: 点击悬浮按钮打开徽章菜单， 点击即可使用。支持无NFC功能设备使用。
5. **管理列表**: 点击列表项进入详情页编辑；**长按列表项并拖动**可调整徽章顺序。
6. **数据同步**: 切换至“备份还原”标签页，您可以：
    * 导出/导入 JSON 备份文件。
    * 使用局域网同步功能：
        * 点击“同网互传”标签页
        * 选择“我是发送方”生成6位分享码，或将分享码提供给接收方
        * 接收方选择“我是接收方”并输入分享码
        * 数据将通过局域网安全传输，无需互联网连接

### 权限说明

首次运行应用时，需要授予以下权限：

1. **悬浮窗权限**: 用于显示全局悬浮球。应用启动时会自动检测并跳转至设置页面请求。
2. **NFC 权限**: 系统会自动处理。

## 🌍 国际化

本应用支持多种语言界面：

* 简体中文 (默认)
* 繁体中文 (台湾)
* 英语

您可以在应用内的设置界面中切换语言。

## 🤝 贡献

欢迎提交 Issue 或 Pull Request 来改进这个项目！

## 📄 许可证

[MIT License](LICENSE)