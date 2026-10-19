# Sky: Children of the Light - Badge Manager

[![GitHub stars](https://img.shields.io/github/stars/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/stargazers)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/releases)
[![GitHub all releases](https://img.shields.io/github/downloads/tatooinoyo/BadgeManager/total)](https://github.com/tatooinoyo/BadgeManager/releases)

[中文版本](README.md)

Badge Manager is an Android application that helps players manage and quickly launch “Sky: Children of the Light STAR badges.” With it, players can quickly use STAR badges in the game.

## ✨ Key Features

- **Badge management**: Add, edit, and delete badges while storing their title, notes, link, game channel, and custom tags. Badges can be filtered by tag and reordered by dragging.
- **Fast entry**: Scan an NFC badge or paste a link to automatically identify saved badge information. Continuous Entry mode automatically saves the badge and clears the form after a link is filled in. Devices without NFC can enter badges manually.
- **NFC reading and writing**: Read text or URI records in NDEF format and write a badge link to a writable, NDEF-compatible NFC tag from the badge details screen.
- **Quick in-game access**: After granting overlay permission, double-tap the right edge of the screen while playing in landscape mode to open the badge menu, filter badges by tag, and launch the matching Sky game channel.
- **Backup and migration**: Export all data to a JSON file and restore it by replacing existing data. Data can also be synchronized to another device on the same local network with a 6-digit share code.
- **Selective sharing**: Select badges and copy their “title + link” as text, or create an encrypted `.badgeenc` file protected by a 6-digit verification code. Importing an encrypted file adds badges without replacing existing data.
- **Languages and updates**: Supports Simplified Chinese, Traditional Chinese, and English, with in-app update checks.

## 📝 User Guide

### Installation and permissions

1. Download and install the latest APK from [Releases](https://github.com/TatooiNoyo/BadgeManager/releases). Android 10 (API 29) or later is required.
2. Grant overlay permission on first launch to enable the in-game shortcut menu. NFC is optional; devices without NFC can still paste or manually enter links.
3. To change the interface language, open the menu in the upper-right corner, go to “Settings,” and select a language.

### Adding and managing badges

1. On the “Entry” tab, select a game channel and enter the title, notes, link, and tags. You can also hold an NFC badge near the back of the phone to fill in the link, then tap “Add Badge.”
2. Enable “Continuous Entry” when adding several badges. After an NFC scan, paste, or link entry, the app automatically adds the badge and clears the form.
3. Tap a card in the badge list to edit or delete it, or to write its current link to an NFC tag. Drag the reorder handle on the right side of a card to change its position.
4. Use the tags above the list to filter badges. You can also swipe left or right to switch between tags.

### Using badges in the game

1. Make sure the badge uses the same game channel as the installed game.
2. Enter the game in landscape mode, then double-tap the right edge of the screen to open the overlay menu.
3. Select a tag and tap a badge to send its link to the matching Sky game channel. The overlay menu is only visible in landscape mode.

### Backup, sharing, and LAN synchronization

- **Full backup**: Export a JSON file from the “Backup” tab. Restoring from JSON replaces all current badges, so save a backup of the existing data first.
- **Share selected badges**: Select badges on the “Share” tab, then copy them as text or create an encrypted `.badgeenc` file. Send both the file and its 6-digit verification code to the recipient. The recipient can open the file with Badge Manager and enter the code to add the badges.
- **LAN synchronization**: Connect both devices to the same Wi-Fi network. On the “Nearby” tab, the sender generates a 6-digit share code, and the receiver enters the code and confirms the import. A successful synchronization replaces the receiver's existing data.

If the devices cannot discover each other, check whether the router has client isolation enabled or UDP broadcasts disabled, and whether a VPN is blocking local network traffic.

## 💻 Development Workflow

### Requirements

- Android Studio with support for Android Gradle Plugin 8.10.0
- JDK 17
- Android SDK 35
- A device or emulator running Android 10 or later; NFC reading and writing require a physical NFC-capable device

### Local development

```bash
git clone https://github.com/TatooiNoyo/BadgeManager.git
cd BadgeManager
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. You can also open the project in Android Studio, wait for Gradle synchronization to finish, and run the `app` configuration.

Before submitting changes, run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Pushing a `v*.*.*` tag triggers the GitHub Actions release build. A release build requires the project signing file and the `KEYSTORE_PASSWORD` and `KEY_PASSWORD` environment variables. Build artifacts are written to `app/build/outputs/apk/release/`.

## 🤝 Contributing

Bug reports and suggestions are welcome in [Issues](https://github.com/TatooiNoyo/BadgeManager/issues), and Pull Requests are welcome:

1. Fork the repository and create a dedicated branch from `main`.
2. Make your changes, add relevant tests, and ensure the project builds locally.
3. Conventional Commits are recommended, for example: `feat: add ...`, `fix: resolve ...`, or `docs: update ...`.
4. Open a Pull Request explaining what changed, why it changed, and how it was verified. Include screenshots or a screen recording for interface changes.

## 📄 License

This project is licensed under the [MIT License](LICENSE).
