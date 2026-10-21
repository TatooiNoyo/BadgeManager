# Sky 星を紡ぐ子どもたち バッジマネージャー

[![GitHub stars](https://img.shields.io/github/stars/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/stargazers)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/tatooinoyo/BadgeManager)](https://github.com/tatooinoyo/BadgeManager/releases)
[![GitHub all releases](https://img.shields.io/github/downloads/tatooinoyo/BadgeManager/total)](https://github.com/tatooinoyo/BadgeManager/releases)

[简体中文](README.md) | [English](README_EN.md)

Badge Manager は、『Sky 星を紡ぐ子どもたち』の STAR 機能付きバッジを管理し、ゲーム内ですばやく使うための Android アプリです。

## ✨ 主な機能

- **バッジ管理**：バッジの登録、編集、削除ができます。名前、備考、リンク、ゲームチャンネル、カスタムタグを保存し、タグで絞り込んだりドラッグで並べ替えたりできます。
- **すばやく登録**：NFC バッジを読み取るかリンクを貼り付けると、登録済みのバッジ情報を自動で認識します。「連続登録」を有効にすると、リンク入力後に自動で保存してフォームをクリアします。NFC 非対応端末では手入力できます。
- **NFC の読み書き**：NDEF 形式のテキストまたは URI を読み取れます。バッジ詳細画面から、書き込み可能な NDEF 対応 NFC タグへリンクを書き込めます。
- **ゲーム内ですばやく使用**：他のアプリの上に重ねて表示する権限を許可すると、横画面でゲームをプレイ中に画面右端をダブルタップしてメニューを開けます。タグで絞り込み、対応する Sky のチャンネルでバッジを起動できます。
- **バックアップと移行**：全データを JSON にエクスポートして復元できます。また、同じ LAN 内の別端末と6桁の共有コードで同期できます。
- **選択して共有**：選択したバッジを「名前＋リンク」のテキストとしてコピーするか、6桁の確認コードで保護した暗号化 `.badgeenc` ファイルを作成できます。暗号化ファイルのインポートでは既存データを置き換えずに追加します。
- **言語と更新**：日本語、簡体字中国語、繁体字中国語、英語に対応し、アプリ内で更新を確認できます。

## 📝 使い方

### インストールと権限

1. [Releases](https://github.com/TatooiNoyo/BadgeManager/releases) から最新の APK をダウンロードしてインストールします。Android 10（API 29）以降が必要です。
2. 初回起動時に、ゲーム内ショートカットメニューを表示するための「他のアプリの上に重ねて表示」権限を許可します。NFC は任意です。NFC 非対応端末でもリンクの貼り付けや手入力ができます。
3. 表示言語を変更するには、右上のメニューから「設定」を開いて言語を選択します。

### バッジの登録と管理

1. 「登録」画面でゲームチャンネルを選び、名前、備考、リンク、タグを入力します。NFC バッジをスマートフォンの背面にかざしてリンクを読み取り、「バッジを追加」をタップすることもできます。
2. 複数のバッジを登録する場合は「連続登録」を有効にします。NFC の読み取り、貼り付け、リンク入力後にバッジが自動で追加され、フォームがクリアされます。
3. バッジ一覧のカードをタップすると編集または削除できます。詳細画面から現在のリンクを NFC タグに書き込むこともできます。カード右側のハンドルをドラッグすると並べ替えられます。
4. 一覧上部のタグでバッジを絞り込めます。左右にスワイプしてタグを切り替えることもできます。

### ゲーム内でバッジを使う

1. バッジに、インストール済みゲームと同じチャンネルが設定されていることを確認します。
2. 横画面でゲームを起動し、画面右端をダブルタップしてフローティングメニューを開きます。
3. タグを選び、バッジをタップすると、対応する Sky のチャンネルへリンクを送ります。フローティングメニューは横画面でのみ表示されます。

### バックアップ、共有、LAN 同期

- **全データのバックアップ**：「バックアップ」画面から JSON をエクスポートします。JSON から復元すると現在のバッジがすべて置き換わるため、事前にバックアップしてください。
- **選択したバッジの共有**：「共有」画面でバッジを選び、テキストとしてコピーするか暗号化 `.badgeenc` ファイルを作成します。ファイルと6桁の確認コードを相手に送ってください。受信側は Badge Manager でファイルを開き、コードを入力するとバッジを追加できます。
- **LAN 同期**：両方の端末を同じ Wi-Fi に接続します。「近くの端末」画面で送信側が6桁の共有コードを発行し、受信側がコードを入力してインポートを確認します。同期が完了すると、受信側の既存データは上書きされます。

端末同士を検出できない場合は、ルーターでクライアント間通信が禁止されていないか、UDP ブロードキャストが無効になっていないか、VPN が LAN 通信を遮断していないか確認してください。

## 💻 開発

### 必要な環境

- Android Gradle Plugin 8.10.0 に対応する Android Studio
- JDK 17
- Android SDK 35
- Android 10 以降の端末またはエミュレーター。NFC の読み書きには NFC 対応の実機が必要です。

### ローカルでの開発

```bash
git clone https://github.com/TatooiNoyo/BadgeManager.git
cd BadgeManager
./gradlew assembleDebug
```

Debug APK は `app/build/outputs/apk/debug/app-debug.apk` に生成されます。Android Studio でプロジェクトを開き、Gradle の同期後に `app` 構成を実行することもできます。

変更を提出する前に、次のコマンドを実行してください。

```bash
./gradlew testDebugUnitTest assembleDebug
```

`v*.*.*` 形式のタグを push すると、GitHub Actions で Release ビルドが開始されます。Release ビルドには署名ファイルと `KEYSTORE_PASSWORD`、`KEY_PASSWORD` 環境変数が必要です。成果物は `app/build/outputs/apk/release/` に生成されます。

## 🤝 コントリビューション

[Issues](https://github.com/TatooiNoyo/BadgeManager/issues) での不具合報告や提案、Pull Request を歓迎します。

1. リポジトリを Fork し、`main` から作業用ブランチを作成します。
2. 変更に関連するテストを追加し、ローカルでビルドが通ることを確認します。
3. `feat: add ...`、`fix: resolve ...`、`docs: update ...` のような Conventional Commits 形式を推奨します。
4. 変更内容、理由、確認方法を説明して Pull Request を作成します。画面を変更した場合はスクリーンショットまたは録画を添付してください。

## 📄 ライセンス

本プロジェクトは [MIT License](LICENSE) のもとで公開されています。
