# Sky: Children of the Light - Badge Manager

Badge Manager is an Android application designed to help users manage and quickly launch "Sky: Children of the Light Star Badges". It supports NFC reading functionality, allowing users to quickly populate links by tapping NFC tags, and provides a floating ball feature for quick access.

## ✨ Key Features

*   **Badge Management (CRUD)**:
    *   Create, Read, Update, and Delete badges.
    *   Badge properties include: Title, Remark, Link (URL), Tags, and Channel Type.
*   **NFC Integration**:
    *   Supports NFC reading (NDEF format).
    *   Tap an NFC tag on the home page or details page to automatically fill the "Link" input field with the tag's content.
*   **Floating Window Service**:
    *   Provides a global floating handle (Starlight effect).
    *   Allows users to quickly access badge functions over other applications (requires overlay permission).
*   **Data Persistence**:
    *   Uses **Room Database** for local data storage, ensuring data is not lost upon restart.
*   **Backup and Restore**:
    *   Support exporting all badge data as JSON files or restoring from local files.
*   **Data Synchronization**:
    *   Support instant data synchronization between devices on the same local network (LAN) using a 6-digit code.
*   **Internationalization**:
    *   Supports multiple languages: Simplified Chinese, Traditional Chinese (Taiwan), and English.
*   **Modern UI**:
    *   Built entirely with **Jetpack Compose**, following Material Design 3 specifications.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material3)
*   **Architecture**: MVVM (Model-View-ViewModel pattern with Repository)
*   **Local Storage**: Android Jetpack Room (SQLite)
*   **Hardware Interaction**: Android NFC API
*   **Background Service**: Android Service (Floating Window)
*   **Communication**: Socket & UDP Broadcast (for LAN Sync)
*   **Build Tool**: Gradle (Kotlin DSL), Version Catalogs (libs.versions.toml)
*   **Version Control**: Git
*   **CI/CD**: GitHub Actions

## 💻 Build & Run

### Prerequisites
*   Android Studio Ladybug or newer.
*   JDK 11 or higher.
*   An Android device with NFC support (for testing NFC functionality; emulators cannot fully simulate NFC touches).

### Build Steps

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/TatooiNoyo/BadgeManager.git
    ```
2.  **Open Project**:
    Launch Android Studio, select "Open", and navigate to the cloned directory.
3.  **Sync Gradle**:
    Wait for Android Studio to download dependencies and sync the project.
4.  **Run Application**:
    Connect an Android device and click the "Run" button.

### Building Release Version

To build a release version APK file, you can use the following command:

```bash
./gradlew assembleRelease
```

The built APK file will be located in the `app/build/outputs/apk/release/` directory, with the filename format `BadgeManager_{version}_{version_code}_release.apk`.

## 📝 User Guide

1.  **Add Badge**: Enter title and remark on the home page, select a channel. Manually enter a link, or tap an NFC tag with the back of the phone to automatically fill in the link, then click the "Add" button.
2.  **Edit/Delete**: Click on a list item to enter the details page. Click "Save Update" after modifying content, or click "Delete" to remove the badge.
3.  **NFC Input**: On any screen where you input a link, tapping an NFC tag will overwrite the current link input field content.
4.  **Use Badge**: Click the floating button to open the badge menu, then click to use. Supports devices without NFC functionality.
5.  **Management list**: Click on the list item to enter the detail page for editing. Long press the list item and drag to adjust the order of the badges.
6.  **Data synchronization**: Switch to the "Backup and Restore" TAB, and you can:
    *   Export/Import JSON backup files.
    *   Use LAN synchronization:
        *   Click on the "Nearby Share" tab
        *   Select "I am Sender" to generate a 6-digit share code, or provide the code to the receiver
        *   The receiver selects "I am Receiver" and enters the share code
        *   Data will be securely transmitted over the local network without internet connection
### Permissions

When running the app for the first time, the following permissions need to be granted:
1.  **Overlay Permission**: Used to display the global floating ball. The app will automatically detect and redirect to the settings page to request this upon startup.
2.  **NFC Permission**: Handled automatically by the system.


## 🔄 Continuous Integration and Deployment

This project uses GitHub Actions for continuous integration and deployment:

* **Build Checks**: Automatically run build checks each time code is pushed to the repository.
* **Automatic Releases**: When pushing commits with version tags (e.g., `v1.2.0`), automatically build the release version and create a GitHub Release.

## 🤝 Contribution

Issues and Pull Requests are welcome to improve this project!

## 📄 License

[MIT License](LICENSE)