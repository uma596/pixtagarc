# pixtagarc

数百万件規模の画像・動画ファイルをタグで管理し、検索・閲覧・PDF出力を行うデスクトップアプリケーション。

## スクリーンショット

<!-- TODO: スクリーンショットを追加 -->

## 主な機能

- **タグ管理** — 画像にタグを付与し、タグによる高速検索が可能
- **作品管理** — 漫画・同人誌などの連番画像を作品単位で管理
- **サムネイル一覧 / リスト表示** — 表示切替可能。仮想スクロールで大量データにも対応
- **ビューア** — 単ページ・見開き表示、右綴じ/左綴じ切替、複数ウィンドウ対応
- **検索** — FTS5 によるキーワード全文検索、タグ・作者・日付・Star評価による絞り込み
- **検索条件の保存** — よく使う検索条件に名前を付けて保存・呼び出し
- **Star評価** — 0〜5 の評価を付けてお気に入り管理
- **PDF出力** — 検索結果の画像をまとめて PDF に出力
- **動画再生** — WebM/VP9 ファイルを VLCJ で再生（VLC インストール必須）
- **JSONメタデータインポート** — 外部ツールで生成した `-meta.json` からタグ・作者・作品情報を一括取り込み

## 技術スタック

| 分類 | ライブラリ / ツール | バージョン |
|------|-------------------|-----------|
| 言語 | Java | 21 |
| UI フレームワーク | JavaFX | 21 |
| 動画再生 | VLCJ | 4.8.3 |
| データベース | SQLite (xerial/sqlite-jdbc) | 3.46.x |
| PDF 出力 | Apache PDFBox | 3.0.3 |
| 画像処理 | Thumbnailator | 0.4.20 |
| 追加画像フォーマット | TwelveMonkeys ImageIO | 3.11.0 |
| ロギング | SLF4J + Logback | 2.0.x / 1.5.x |
| テスト | JUnit 5 + Mockito | 5.11.x / 5.12.x |
| ビルドツール | Gradle | 8.8 |

## 前提条件

| ツール | バージョン | 備考 |
|--------|-----------|------|
| Java JDK | 21 | Microsoft Build of OpenJDK 21 推奨 |
| Gradle | 8.8 | `gradlew.bat` / `gradlew` 経由で自動ダウンロード |
| VLC | 3.x | 動画再生機能を使う場合のみ必要 |

## ビルド

```bash
# コンパイルのみ
./gradlew compileJava

# テスト込みフルビルド
./gradlew build

# Windows 実行ファイル（JRE同梱 app-image）作成
./gradlew jpackageWinExe

# macOS .dmg 作成（macOS 上で実行）
./gradlew jpackageMac

# Fat JAR 作成（JRE 同梱なし）
./gradlew fatJar
```

### クリーンビルド

問題が起きた場合:

```bash
./gradlew clean jpackageWinExe
```

## 起動方法

### EXE（Windows）

```
build\dist\pixtagarc\pixtagarc.exe
```

### Fat JAR（開発・動作確認用）

```bash
java -jar build/libs/pixtagarc-1.0.0-all.jar
```

## アプリデータの保存先

| データ | パス |
|--------|------|
| SQLite DB | `~/.pixtagarc/pixtagarc.db` |
| サムネイルキャッシュ | `~/.pixtagarc/thumbnails/` |
| ログファイル | `~/.pixtagarc/logs/app.log` |

保存先は設定または起動引数 `--db-dir <path>` で変更可能。

## プロジェクト構成

```
com.example.imagemanager
├── MainApp.java              # エントリーポイント（JavaFX Application）
├── controller/               # プレゼンテーション層（JavaFX Controller）
├── service/                  # ビジネスロジック層
├── repository/               # データアクセス層（SQLite JDBC）
├── domain/                   # ドメインモデル（Entity）
├── dto/                      # データ転送オブジェクト
├── config/                   # 設定クラス
└── exception/                # カスタム例外
```

## アーキテクチャ

レイヤードアーキテクチャを採用。各レイヤーは上位から下位への一方向依存のみ許可。

```
Presentation Layer  →  Service Layer  →  Repository Layer  →  Domain Layer
(JavaFX Controller)    (ビジネスロジック)  (SQLite アクセス)    (Entity / VO)
```

## 開発環境

本プロジェクトは [Kiro](https://kiro.dev/) を利用して開発しています。Steering ファイルで Java コーディングポリシーや Git 運用ルールをエージェントに適用し、コード品質を維持しています。

## ドキュメント

プロジェクトのアーキテクチャ設計書・追加機能検討ドキュメントは `docs/` フォルダに格納しています（Git管理外）。

## ライセンス

[MIT License](LICENSE)

### サードパーティライセンスについて

本プロジェクトが依存するライブラリのライセンスは以下の通りです。

| ライブラリ | ライセンス |
|---|---|
| JavaFX (OpenJFX) | GPL v2 + Classpath Exception |
| sqlite-jdbc | Apache License 2.0 |
| Apache PDFBox | Apache License 2.0 |
| Thumbnailator | MIT License |
| TwelveMonkeys ImageIO | BSD 3-Clause |
| SLF4J | MIT License |
| Logback | EPL 1.0 / LGPL 2.1 |

#### VLCJ（オプション依存）

動画再生機能は [VLCJ](https://github.com/caprica/vlcj)（GPL v3）を使用しますが、
**本プロジェクトの配布物には VLCJ を含みません**。
VLCJ は `compileOnly` 依存として宣言されており、実行時にクラスパスに存在する場合のみ動画再生が有効化されます。
動画再生機能を使用するには、ユーザー自身で VLC メディアプレイヤーをインストールしてください。
