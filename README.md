# 光遇徽章管理器（Badge Manager）

[![GitHub stars](https://img.shields.io/github/stars/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/stargazers)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/releases)
[![GitHub all releases](https://img.shields.io/github/downloads/tatooinoyo/BadgeManager/total)](https://github.com/tatooinoyo/BadgeManager/releases)

[English Version](README_EN.md) | [日本語版](README_JA.md)

Badge Manager 是一个Android 应用程序，帮助玩家管理和快速启动“光遇 Star 徽章”。使用它, 能在游戏中快速使用Star 徽章。

## ✨ 主要功能

- **徽章管理**：录入、编辑和删除徽章，保存标题、备注、链接、游戏渠道与自定义标签；支持按标签筛选和拖拽排序。
- **快速录入**：读取 NFC 徽章或粘贴链接后自动识别已收录的徽章信息；连续录入模式可在填入链接后自动保存并清空表单。没有 NFC 的设备也可手动录入。
- **NFC 读写**：读取 NDEF 格式的文本或 URI，并可在徽章详情页将链接写入支持 NDEF 且可写的 NFC 标签。
- **游戏内快捷使用**：授予悬浮窗权限后，在横屏游戏中双击屏幕右侧边缘打开徽章菜单，可按标签筛选并启动对应渠道的《光·遇》。
- **备份与迁移**：将全部数据导出为 JSON 文件并覆盖还原；也可在同一局域网内使用 6 位分享码将数据同步到另一台设备。
- **选择性分享**：选择部分徽章后复制“标题 + 链接”文本，或生成带 6 位验证码的 `.badgeenc` 加密文件；导入加密文件时只新增数据，不覆盖现有徽章。
- **多语言与更新**：支持简体中文、繁体中文和英语，并可在应用内检查新版本。

## 📝 使用指南

### 安装与权限

1. 从 [Releases](https://github.com/TatooiNoyo/BadgeManager/releases) 下载最新 APK 并安装。应用要求 Android 10（API 29）或更高版本。
2. 首次启动时授予悬浮窗权限，以便在游戏中显示快捷菜单。NFC 为可选功能，不支持 NFC 的设备仍可粘贴或手动输入链接。
3. 如需切换界面语言，打开右上角菜单，进入“设置”选择语言。

### 录入和管理徽章

1. 在“录入”页选择游戏渠道，填写标题、备注、链接和标签。也可将 NFC 徽章贴近手机背面自动填入链接，然后点击“添加徽章”。
2. 批量录入时开启“连续录入”：读取 NFC、粘贴或填入链接后，应用会自动添加徽章并清空表单。
3. 在徽章列表中点击卡片可编辑或删除徽章，也可将当前链接写入 NFC 标签；拖动卡片右侧的排序手柄可调整顺序。
4. 使用列表上方的标签筛选徽章；在不同标签之间也可左右滑动切换。

### 在游戏中使用徽章

1. 确保目标徽章选择了与已安装游戏一致的渠道。
2. 横屏进入游戏后，双击屏幕右侧边缘打开悬浮菜单。
3. 选择标签并点击徽章，即可向对应渠道的《光·遇》发送徽章链接。悬浮菜单仅在横屏时显示。

### 备份、分享与同网同步

- **完整备份**：在“备份”页导出 JSON 文件；从 JSON 还原会覆盖当前全部徽章，请先保存现有备份。
- **分享部分徽章**：在“分享”页选择徽章，然后复制文本，或生成 `.badgeenc` 加密文件并将文件与 6 位验证码发给接收方。接收方使用本应用打开文件并输入验证码即可新增徽章。
- **同网同步**：两台设备连接同一 Wi-Fi，在“同网”页由发送方生成 6 位分享码，接收方输入分享码并确认导入。同步成功后，接收方的现有数据会被覆盖。

如果同网设备无法互相发现，请检查路由器是否启用了客户端隔离、禁用了 UDP 广播，或设备是否正在使用会拦截局域网流量的 VPN。

## 💻 开发流程

### 环境要求

- Android Studio（支持 Android Gradle Plugin 8.10.0）
- JDK 17
- Android SDK 35
- Android 10 或更高版本的设备或模拟器；测试 NFC 读写需要支持 NFC 的实体设备

### 本地开发

```bash
git clone https://github.com/TatooiNoyo/BadgeManager.git
cd BadgeManager
./gradlew assembleDebug
```

Debug APK 生成在 `app/build/outputs/apk/debug/app-debug.apk`。也可用 Android Studio 打开项目，等待 Gradle 同步完成后运行 `app` 配置。

提交改动前运行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

发布版本由 `v*.*.*` 标签触发 GitHub Actions 构建。Release 构建需要项目签名文件以及 `KEYSTORE_PASSWORD`、`KEY_PASSWORD` 环境变量，产物位于 `app/build/outputs/apk/release/`。

## 🤝 贡献

欢迎通过 [Issues](https://github.com/TatooiNoyo/BadgeManager/issues) 报告问题或提出建议，也欢迎提交 Pull Request：

1. Fork 仓库，并从 `main` 创建独立分支。
2. 完成修改，补充与改动相关的测试，并确保本地构建通过。
3. 提交信息建议遵循 Conventional Commits，例如 `feat: add ...`、`fix: resolve ...`、`docs: update ...`。
4. 发起 Pull Request，说明改动内容、原因和验证方式；涉及界面调整时请附上截图或录屏。

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。
