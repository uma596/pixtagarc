# ImageManager — 追加機能検討

> **注意: DB互換性ポリシー**
> テーブル構造の変更を伴う機能追加について、当面の間は過去バージョンとの互換性を考慮しない。
> スキーマ変更時は `schema.sql` を直接変更し、マイグレーション処理（ALTER TABLE）は実装しない。
> 既存DBがある場合は DB ファイルを削除して再インポートで対応する。

## 目次

| # | 機能 | 状態 |
|---|------|------|
| 1 | ビューア画面：マウススクロールでページ送り/戻り | ✅ 実装済み |
| 2 | ビューア画面：画面サイズに合わせて画像をフィット表示 | ✅ 実装済み |
| 3 | ビューア画面：コンテキストメニュー | ✅ 実装済み |
| 4 | 画像インポート：JSONファイルによるタグ情報一括読み込み | ✅ 実装済み |
| 5 | テーブル構成：作品IDとページ番号 | ✅ 実装済み |
| 6 | テーブル構成：Star評価 | ✅ 実装済み |
| 7 | インポート機能：全データクリア＆再インポート | ✅ 実装済み |
| 8 | DBファイルの保存先：設定で変更可能 | 未実装 |
| 9 | ビューア画面：クリックでページ送り/戻り | ✅ 実装済み |
| 10 | メイン画面：コンテキストメニュー | ✅ 実装済み |
| 11 | コンテキストメニュー：タグとStarを同一作品に反映 | ✅ 実装済み |
| 12 | メイン画面：タグフィルター複数選択（OR検索） | ✅ 実装済み |
| 13 | メイン画面：検索条件クリアボタン | ✅ 実装済み |
| 14 | タグのStar評価（タグリストをStar順にソート） | ✅ 実装済み |
| 15 | 非表示機能：作品全体をまとめて非表示 | ✅ 実装済み |
| 16 | ビューア画面：次の作品へ/前の作品へ（マウススワイプ操作） | ✅ 実装済み |
| 17 | ビューア画面：画像読み込み中プログレスアイコン表示 | ✅ 実装済み |
| 18 | ビューア画面：VLCJ無効時の動画×アイコン表示 | ✅ 実装済み |
| 19 | コンテキストメニュー：最近つけたタグ履歴から選択 | ✅ 実装済み |
| 20 | インポート：フォルダを1作品として取り込み | ✅ 実装済み |
| 21 | インポート：PDFファイルの取り込み | 未実装 |
| 22 | インポート：ZIPファイル内の画像をビューア表示 | 未実装 |
| 23 | 検索テキストボックス：入力欄内×クリアボタン | ✅ 実装済み |
| 24 | 検索結果：ページング表示（上限なし） | ✅ 実装済み |
| 25 | 状態復元：メイン画面の検索条件とページを保持・復元 | ✅ 実装済み |
| 26 | 状態復元：ビューア画面の状態を保持・復元 | ✅ 実装済み |
| 27 | 保存済み検索：最近の検索条件履歴（5件）を先頭表示 | ✅ 実装済み |
| 28 | タグ追加時のメイン画面即時反映 | ✅ 実装済み |
| 29 | メイン画面：タグリストと検索リストの幅をドラッグ調整可能 | ✅ 実装済み |
| 30 | UI：ウィンドウ縮小時のレスポンシブ対応（ツールチップ・オーバーフロー） | ✅ 実装済み |
| 31 | ビューア画面：ページ切り替え時のちらつき防止（先読み＋フェード） | ✅ 実装済み |
| 32 | ビューア画面：見開き/単ページの画像サイズ自動判別 | ✅ 実装済み |
| 33 | ビューア画面：見開き時のマウスオーバー側情報表示 | ✅ 実装済み |
| 34 | ビューア画面：メニュー表示/非表示切り替え（デフォルト非表示） | ✅ 実装済み |
| 35 | ビューア画面：ページ送りボタンのラベルを矢印のみに変更 | ✅ 実装済み |
| 36 | ビューア画面：コンテキストメニューからタグ追加（最近使ったタグ履歴） | ✅ 実装済み |

---

## 1. ビューア画面：マウススクロールでページ送り/戻り

### 概要

ビューア画面で画像を表示中に、マウスホイールの上下スクロールでページ送り（次/前）を行う。

### 仕様

| 項目 | 内容 |
|------|------|
| スクロール↓（下方向） | 次のページへ移動 |
| スクロール↑（上方向） | 前のページへ移動 |
| 見開きモード時 | 2ページ分送る/戻る |
| 動画再生中 | マウスホイールはページ送りに使用しない（動画のシークに予約） |
| スクロール感度 | 1ノッチ = 1ページ（連続高速スクロールは最終位置のみ反映） |

### 実装方針

- `ViewerController` の `imageAreaPane` に `setOnScroll` イベントを登録
- `ScrollEvent.getDeltaY()` の正負でページ送り方向を判定
- 連続スクロール時のチャタリング防止として、前回のスクロールから50ms未満の連続入力は無視する

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `ViewerController.java` | `setupKeyboardShortcuts()` にスクロールイベント追加 |

### コードイメージ

```java
imageAreaPane.setOnScroll(event -> {
    if (viewerState == null) return;
    // 動画再生中はスクロールでページ送りしない
    if (viewerState.getCurrentImage() != null
            && "video".equals(viewerState.getCurrentImage().getMediaType())) {
        return;
    }
    if (event.getDeltaY() < 0) {
        onNext(); // 下スクロール → 次ページ
    } else if (event.getDeltaY() > 0) {
        onPrev(); // 上スクロール → 前ページ
    }
    event.consume();
});
```

---

## 2. ビューア画面：画面サイズに合わせて画像をフィット表示

### 概要

ビューアの画像表示エリアのサイズに追従して、画像をアスペクト比を維持したまま最大化表示する。ウィンドウリサイズ時にもリアルタイムで追従する。

### 仕様

| 項目 | 内容 |
|------|------|
| フィット方式 | ウィンドウの表示エリア（幅・高さ）に収まる最大サイズ。アスペクト比を維持 |
| 拡大制限 | 元画像の実寸を超える拡大はしない（ピクセル等倍以下） |
| 見開きモード | 左右各パネルのサイズに合わせて個別にフィット |
| リサイズ追従 | `imageAreaPane` のサイズプロパティにバインドしてリアルタイム追従 |

### 実装方針

- `ImageView.fitWidthProperty()` と `fitHeightProperty()` を `imageAreaPane` のサイズにバインド
- `preserveRatio = true` は既に設定済み
- 現在は固定値 (`fitWidth=800`, `fitHeight=600`) で設定されているので、これをバインディングに変更

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `ViewerController.java` | `initialize()` で `ImageView` を `StackPane` のサイズにバインド |
| `viewer.fxml` | `ImageView` の `fitWidth` / `fitHeight` 固定値を削除 |

### コードイメージ

```java
// 単ページ
singleImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().subtract(20));
singleImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));

// 見開き（左右それぞれ半分）
leftImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().divide(2).subtract(20));
leftImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));
rightImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().divide(2).subtract(20));
rightImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));
```

---

## 3. ビューア画面：コンテキストメニュー

### 概要

ビューア画面の画像表示エリアを右クリックすると、現在表示中の画像に対する操作メニューを表示する。

### メニュー項目

| メニュー項目 | 動作 |
|-------------|------|
| タグを編集... | タグ編集ダイアログを開く（既存の `onAddTag()` と同等） |
| 作者を変更... | 作者変更ダイアログを開く |
| 非表示にする / 非表示を解除 | `is_hidden` フラグを切替 |
| ファイルパスをコピー | クリップボードにファイルパスを設定 |
| Finder で表示 / エクスプローラーで表示 | OS のファイルマネージャで該当ファイルを選択状態で開く |
| 画像を削除... | 確認ダイアログ付きで DB から削除（ファイル自体は削除しない） |

### 実装方針

- `ContextMenu` を作成して `imageAreaPane` に `setOnContextMenuRequested` で紐づけ
- 見開き表示時はクリック位置（左右どちらか）で対象画像を判定
- メニュー項目は動的に生成（非表示フラグの状態でラベルを切替）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `ViewerController.java` | `ContextMenu` 作成・表示ロジック追加 |

### コードイメージ

```java
private void setupContextMenu() {
    ContextMenu contextMenu = new ContextMenu();

    MenuItem editTags = new MenuItem("タグを編集...");
    editTags.setOnAction(e -> onAddTag());

    MenuItem toggleHidden = new MenuItem("非表示にする");
    toggleHidden.setOnAction(e -> onToggleHidden());

    MenuItem copyPath = new MenuItem("ファイルパスをコピー");
    copyPath.setOnAction(e -> {
        ImageSummary current = viewerState.getCurrentImage();
        if (current != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(current.getFilePath());
            clipboard.setContent(content);
        }
    });

    MenuItem openInFinder = new MenuItem("エクスプローラーで表示");
    openInFinder.setOnAction(e -> {
        ImageSummary current = viewerState.getCurrentImage();
        if (current != null) {
            // Windows: explorer /select, macOS: open -R
            hostServices.showDocument(new File(current.getFilePath()).getParent());
        }
    });

    MenuItem deleteImage = new MenuItem("画像を削除...");
    deleteImage.setOnAction(e -> onDeleteImage());

    contextMenu.getItems().addAll(editTags, toggleHidden,
            new SeparatorMenuItem(), copyPath, openInFinder,
            new SeparatorMenuItem(), deleteImage);

    imageAreaPane.setOnContextMenuRequested(event -> {
        // 非表示フラグの状態でラベルを動的変更
        ImageSummary current = viewerState.getCurrentImage();
        if (current != null) {
            toggleHidden.setText(current.isHidden() ? "非表示を解除" : "非表示にする");
        }
        contextMenu.show(imageAreaPane, event.getScreenX(), event.getScreenY());
    });
}
```

---

## 4. 画像インポート：JSONファイルによるタグ情報一括読み込み

### 概要

画像ファイルと同名の `-meta.json` ファイルを読み込み、タグ・作者・作品情報を画像に自動付与する。外部ツールで生成したメタデータをインポート時に一括取り込みする。

### JSON フォーマット

画像ファイルと同名の `-meta.json` ファイルから読み込む。

ファイル名規則:
```
{作品ID}_p{ページ番号4桁0埋め}-{作品タイトル}-meta.json
```

例:
```
12345678_p0000-作品タイトル-meta.json
```

> `-meta.json` ファイルは作品ごとに `p0000` の1ファイルのみ存在する。この1ファイルの内容を作品全体（同一作品IDの全ページ）に反映する。

対応する画像ファイル名:
```
{作品ID}_p{ページ番号4桁0埋め}-{作品タイトル}.{拡張子}
```

例:
```
12345678_p0000-作品タイトル.jpg
12345678_p0001-作品タイトル.jpg
12345678_p0002-作品タイトル.jpg
...
```

### 処理ルール

| ルール | 内容 |
|--------|------|
| JSON不在 | `-meta.json` がなければタグ・作者なしでインポート（従来通り） |
| JSON適用範囲 | `p0000` の `-meta.json` 1ファイルの内容を、同一作品ID（ファイル名のプレフィックスが一致する全画像）に反映 |
| タグの追加 | 既存タグがあれば再利用、なければ新規作成 |
| 作者の追加 | 既存作者がいれば再利用、いなければ新規作成 |
| エラー時 | JSONのパースに失敗した場合は警告ログを出力してタグ・作者なしでインポートを継続 |

### 実装方針

- `ImportService` に JSON 読み込みロジックを追加
- 画像ファイルのインポート時に、同一ディレクトリ内の `{作品ID}_p0000-{タイトル}-meta.json` を探して読み込む
- 同一作品IDの画像には同じ JSON の内容（タグ・作者・作品情報）を適用する
- JSONパースには **Gson 2.11.x** を使用（軽量・Apache License 2.0）
- `importFile()` 内でタグ・作者・作品情報を付与

### 依存追加

```groovy
// Gson（JSONパーサー）
implementation 'com.google.code.gson:gson:2.11.0'
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `build.gradle` | Gson 依存追加 |
| `ImportService.java` | JSON読み込み・タグ/作者/作品付与ロジック追加 |
| `dto/ImportMetadata.java`（新規） | JSONマッピング用DTO |

---

## 実装優先度

| 機能 | 工数 | 優先度 | 理由 |
|------|------|--------|------|
| マウススクロールでページ送り | 小 | 高 | UX改善。数行の追加で完了 |
| 画像フィット表示 | 小 | 高 | UX改善。バインディング変更のみ |
| コンテキストメニュー | 中 | 中 | 操作の利便性向上。既存メソッドの呼び出しが中心 |
| JSONメタデータ読み込み | 中〜大 | 中 | 大量ファイルの一括タグ付けに有効。Gson追加＋読み込みロジック |


---

## 5. テーブル構成：作品IDとページ番号

### 概要

画像テーブルに「作品ID」と「ページ番号」を追加する。複数の画像ファイルをひとまとまりの作品として管理し、作品単位での表示・ソート・検索を可能にする。

### 用途

- 漫画・同人誌：1作品 = 複数ページ（ファイル）
- 写真集：1セット = 複数カット
- 連番画像：順序を明示的に管理

### テーブル変更

#### 新規テーブル: `works`（作品テーブル）

```sql
CREATE TABLE IF NOT EXISTS works (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,          -- 作品タイトル
    author_id   INTEGER REFERENCES authors(id) ON DELETE SET NULL,
    total_pages INTEGER,                   -- 総ページ数（任意、表示用）
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_works_title ON works(title);
CREATE INDEX IF NOT EXISTS idx_works_author_id ON works(author_id);
```

#### `images` テーブルへのカラム追加

```sql
ALTER TABLE images ADD COLUMN work_id     INTEGER REFERENCES works(id) ON DELETE SET NULL;
ALTER TABLE images ADD COLUMN page_number INTEGER;

CREATE INDEX IF NOT EXISTS idx_images_work_id ON images(work_id);
CREATE INDEX IF NOT EXISTS idx_images_work_page ON images(work_id, page_number);
```

#### 変更後の `images` テーブル全体像

```sql
CREATE TABLE images (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path   TEXT    NOT NULL UNIQUE,   -- ファイルパス（一意キー）
    file_name   TEXT    NOT NULL,
    file_size   INTEGER,
    width       INTEGER,
    height      INTEGER,
    media_type  TEXT    NOT NULL,
    author_id   INTEGER REFERENCES authors(id) ON DELETE SET NULL,
    work_id     INTEGER REFERENCES works(id)   ON DELETE SET NULL, -- 作品ID（NULL = 未所属）
    page_number INTEGER,                   -- 作品内ページ番号（1始まり、NULL = 未設定）
    is_hidden   INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL,
    imported_at TEXT    NOT NULL
);
```

### ER図（関連）

```
works (1) ──── (N) images
  │                  │
  └── author_id ─── authors
                     │
images.author_id ───┘
```

- 1つの作品に複数画像が所属する（1:N）
- 画像は作品に所属しなくてもよい（`work_id = NULL`）
- 作品にも作者を設定できる（画像個別の作者と別に管理可能）

### 仕様

| 項目 | 内容 |
|------|------|
| `work_id` | 所属する作品のID。NULL の場合は単独の画像として扱う |
| `page_number` | 作品内の表示順序。1始まり。NULL の場合は未設定（ファイル名順で代用） |
| ページ番号の一意性 | 同一 `work_id` 内で `page_number` の重複を許可しない（UNIQUE制約を付与するか、アプリ側で制御） |
| ソート | 作品内ではページ番号昇順でソート。未設定の場合はファイル名昇順 |

### ビューアとの連携

| 場面 | 動作 |
|------|------|
| ダブルクリック（従来通り） | 検索結果の全ファイルをビューアに渡し、クリックした画像から表示開始 |
| コンテキストメニュー「作品を表示」 | その画像が所属する作品の全ページのみをビューアに渡し、該当ページから表示開始 |
| 作品未所属の場合 | コンテキストメニューの「作品を表示」はグレーアウト |
| 見開き表示 | 作品モードで開いた場合、ページ番号に基づいてページ組みを行う |
| ページオフセット | 作品単位で適用（作品ごとにオフセットを記憶可能にする将来拡張余地あり） |

**操作フロー図**

```
[メイン画面：検索結果一覧]
    │
    ├── ダブルクリック
    │       → 検索結果の全ファイルをビューアに渡す（従来動作）
    │       → クリックしたファイルの位置から表示開始
    │
    └── 右クリック → コンテキストメニュー
            │
            ├── 「作品を表示」（work_id != NULL の場合のみ有効）
            │       → 同一 work_id の全画像を page_number 昇順で取得
            │       → 取得した画像リストのみでビューアを開く
            │       → クリックした画像の page_number から表示開始
            │
            └── 「タグを編集...」「非表示にする」等（他のメニュー項目）
```

**メリット**

- ダブルクリック = 検索結果を横断して連続閲覧（従来の使い方を維持）
- 作品表示 = ノイズなしで1作品を通読（見開き・ページ番号が正確に機能する）

### 検索との連携

| 場面 | 動作 |
|------|------|
| 作品タイトルで検索 | `works.title` を FTS5 に追加、またはキーワード検索で `works` テーブルをJOIN |
| 検索結果のソート | 同一作品に属する画像はページ番号順でまとめて表示する |
| 作品一覧表示 | 将来的に「作品モード」ビューを追加（サムネイルに表紙のみ表示、クリックでページ一覧） |

**検索結果のソート仕様**

| 条件 | ソート順 |
|------|---------|
| 第1キー | ユーザー指定のソート列（作成日時・ファイル名等） |
| 第2キー | `work_id`（同一作品をまとめる。NULL は最後にグループ化） |
| 第3キー | `page_number` 昇順（作品内のページ順） |

**動作例**

ソート列 = `created_at DESC` の場合：

```
検索結果（日時降順）:
  [作品A] page 1    ← 作品Aの created_at が最新
  [作品A] page 2       同一作品はページ順でまとまる
  [作品A] page 3
  [単独画像X]        ← 作品未所属は通常ソート
  [作品B] page 1    ← 作品Bは作品Aより古い
  [作品B] page 2
  [単独画像Y]
```

**実装方針（SQL）**

```sql
-- 作品に所属する画像は作品の代表日時（最古 or 最新）でグループ化
-- その中でページ番号順にソート
SELECT i.*
FROM images i
LEFT JOIN works w ON i.work_id = w.id
WHERE ...
ORDER BY
    -- 第1キー: ユーザー指定（作品所属の場合は作品の代表値を使用）
    COALESCE(w.created_at, i.created_at) DESC,
    -- 第2キー: 同一作品をまとめる
    i.work_id,
    -- 第3キー: 作品内ページ順
    i.page_number ASC,
    -- 第4キー: 作品未所属・ページ番号未設定のフォールバック
    i.file_name ASC
```

> 作品の「代表日時」は `works.created_at` を使用する。これにより作品全体が1つの塊としてソート位置が決まる。

### JSONメタデータとの連携

画像ファイルと同名の `-meta.json` ファイルから読み込む（セクション4のフォーマット仕様に準拠）。

ファイル名規則:
```
{作品ID}_p{ページ番号4桁0埋め}-{作品タイトル}-meta.json
```

例:
```
12345678_p0000-作品タイトル-meta.json
```

> `-meta.json` は `p0000` の1ファイルのみ。内容は同一作品IDの全ページに反映する。

対応する画像ファイル名:
```
{作品ID}_p{ページ番号4桁0埋め}-{作品タイトル}.{拡張子}
```

例:
```
12345678_p0000-作品タイトル.jpg
12345678_p0001-作品タイトル.jpg
12345678_p0002-作品タイトル.jpg
...
```

#### JSON 構造

```json
{
  "idNum": 12345678,
  "id": "12345678_p0",
  "title": "作品タイトル",
  "pageCount": 10,
  "index": 0,
  "tags": ["タグ1", "タグ2", "タグ3"],
  "user": "作者名",
  "userId": "87654321",
  "ext": "jpg",
  "date": "2026-04-18T15:07:00+00:00",
  ...
}
```

#### フィールドマッピング

| JSON フィールド | ImageManager のフィールド | 備考 |
|----------------|------------------------|------|
| `idNum` | `works.id`（外部キーとして利用）または作品検索キー | 作品IDとして `works` テーブルに登録 |
| `title` | `works.title` | 作品タイトル |
| `pageCount` | `works.total_pages` | 作品の総ページ数 |
| `index` | `images.page_number` | 0始まり → 1始まりに変換して保存 |
| `tags` | `tags` テーブル + `image_tags` | タグリスト。既存タグは再利用 |
| `userId` | `authors` テーブルの外部識別子 | 作者の外部ID |
| `user` | `authors.name` | 作者名 |
| `date` | `images.created_at` | ISO8601 パース |

#### ページ番号の取り扱い

| 条件 | 処理 |
|------|------|
| `index` フィールドが存在 | `index + 1` を `page_number` とする（0始まり→1始まり変換） |
| `index` が存在しない場合 | ファイル名の `_p{NNNN}` 部分をパースして `NNNN + 1` を使用 |
| ページ番号ファイルの欠落 | 欠番があっても存在するファイルのみ登録。ページ番号は JSON/ファイル名の値をそのまま使用（連番に詰めない） |

例: `pageCount=10` だが `_p0003` と `_p0007` のファイルが欠落している場合、8ファイルのみ登録し `page_number` は 1,2,3,5,6,7,9,10 となる（4,8 は欠番）。

#### 作品の重複判定

| 条件 | 処理 |
|------|------|
| 同一 `idNum` の作品が既にDB上に存在 | 既存の `works` レコードを再利用 |
| 同一 `idNum` が存在しない | `works` テーブルに新規登録 |

`works` テーブルに外部ID用カラムを追加:

```sql
ALTER TABLE works ADD COLUMN external_id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_works_external_id ON works(external_id);
```

`external_id` に `"{idNum}"` 形式で保存し、重複検出に使用する。

#### インポート処理フロー

```
ディレクトリをスキャン
    │
    ▼
画像ファイルを発見
    │
    ▼
同名の -meta.json が存在するか？
    │ Yes                            │ No
    ▼                                ▼
JSONをパース                    通常インポート（メタデータなし）
    │
    ▼
idNum から作品を検索 or 作成
    │
    ▼
画像を登録（work_id, page_number を設定）
    │
    ▼
tags を全てタグテーブルに登録 & image_tags に紐づけ
    │
    ▼
user/userId から作者を検索 or 作成 → images.author_id に設定
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `schema.sql` | `works` テーブル追加、`images` にカラム追加 |
| `domain/Work.java`（新規） | 作品エンティティ |
| `domain/Image.java` | `workId`, `pageNumber` フィールド追加 |
| `repository/WorkRepository.java`（新規） | 作品の CRUD |
| `repository/ImageRepository.java` | `findByWorkId()`, `updateWork()` メソッド追加 |
| `service/WorkService.java`（新規） | 作品管理ビジネスロジック |
| `dto/SearchCondition.java` | `workId` による絞り込み追加 |
| `service/ImportService.java` | JSONからの作品情報読み込み対応 |
| `controller/ViewerController.java` | 作品内ページ送り対応 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 大 | 高 | 漫画・連番画像の管理はアプリのコア機能。ビューアの見開き表示と直結する |


---

## 6. テーブル構成：Star評価

### 概要

画像ファイルごとに 0〜5 の Star 評価を付与し、検索条件で「指定した Star 数以上」の画像を絞り込めるようにする。

### テーブル変更

#### `images` テーブルへのカラム追加

```sql
ALTER TABLE images ADD COLUMN star INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_images_star ON images(star);
```

#### 変更後の `images` テーブル（該当カラムのみ抜粋）

```sql
CREATE TABLE images (
    ...
    star        INTEGER NOT NULL DEFAULT 0,  -- Star評価（0〜5、0=未評価）
    is_hidden   INTEGER NOT NULL DEFAULT 0,
    ...
);
```

### 仕様

| 項目 | 内容 |
|------|------|
| 値の範囲 | 0〜5（整数） |
| 0 の意味 | 未評価（Star なし） |
| 1〜5 の意味 | ★1 〜 ★5 |
| デフォルト | 0（インポート時は未評価） |
| 設定方法 | ビューア画面の情報バー、またはコンテキストメニューから設定 |
| 複数選択時 | メイン画面で複数画像を選択して一括 Star 設定可能 |

### 検索条件への追加

#### `SearchCondition` への追加フィールド

```java
/** Star評価の最小値フィルター。0の場合はフィルターなし。 */
private int minStar = 0;
```

#### 検索クエリへの追加

```sql
-- minStar > 0 の場合のみ追加
WHERE i.star >= ?
```

#### UI（メイン画面の検索バー）

検索バーに Star フィルター用の ComboBox を追加：

```
[☆ Star▼]  → 選択肢: すべて / ★1以上 / ★2以上 / ★3以上 / ★4以上 / ★5のみ
```

| 選択値 | `minStar` | SQL条件 |
|--------|-----------|---------|
| すべて | 0 | 条件なし |
| ★1以上 | 1 | `star >= 1` |
| ★2以上 | 2 | `star >= 2` |
| ★3以上 | 3 | `star >= 3` |
| ★4以上 | 4 | `star >= 4` |
| ★5のみ | 5 | `star >= 5` |

### ビューア画面での Star 設定

情報バーに Star 表示・設定 UI を追加：

```
┌──────────────────────────────────────────────────────────┐
│  タグ: [風景] [夕日] [+ タグ追加]                          │
│  作者: [作者名▼]  Star: [★★★☆☆]  パス: /Users/.../...     │
└──────────────────────────────────────────────────────────┘
```

- Star 部分はクリック可能な5つの ★ アイコン
- クリックした位置の Star 数を即座に DB に保存
- 同じ Star をクリックすると 0（リセット）

### コンテキストメニュー

ビューア・メイン画面のコンテキストメニューに Star 設定サブメニューを追加：

```
右クリック → Star を設定 →  ☆ なし
                            ★ 1
                            ★★ 2
                            ★★★ 3
                            ★★★★ 4
                            ★★★★★ 5
```

### リスト表示への反映

メイン画面のリスト表示（TableView）に Star 列を追加：

| ファイル名 | 作者 | Star | タグ | 作成日時 | サイズ |
|-----------|------|------|------|---------|--------|
| image001.jpg | 山田 | ★★★ | [風景] | 2024-01-01 | 3.2MB |

- Star 列はクリックでソート可能
- Star 列のセルは ★ アイコンで視覚的に表示

### 保存済み検索への反映

`saved_searches` テーブルに `min_star` カラムを追加：

```sql
ALTER TABLE saved_searches ADD COLUMN min_star INTEGER NOT NULL DEFAULT 0;
```

検索条件の保存・復元時に `minStar` を含める。

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `schema.sql` | `images` に `star` カラム追加、`saved_searches` に `min_star` 追加 |
| `domain/Image.java` | `star` フィールド追加 |
| `dto/SearchCondition.java` | `minStar` フィールド追加 |
| `dto/ImageSummary.java` | `star` フィールド追加 |
| `repository/ImageRepository.java` | `updateStar()` メソッド追加 |
| `service/SearchService.java` | `WHERE i.star >= ?` 条件追加 |
| `service/ImageService.java` | `updateStar()` メソッド追加 |
| `controller/MainController.java` | Star フィルター ComboBox 追加、リスト列追加 |
| `controller/ViewerController.java` | 情報バーに Star 設定 UI 追加 |
| `main.fxml` | 検索バーに Star ComboBox 追加、TableView に Star 列追加 |
| `viewer.fxml` | 情報バーに Star UI 追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | お気に入り管理の基本機能。検索・ソートと組み合わせることで利便性が大きく向上する |


---

## 7. インポート機能：全データクリア＆再インポート

### 概要

インポートダイアログに「全データをクリアして再インポート」オプションを追加する。既存のDB上の画像・タグ紐付け・作品・FTS5インデックスをすべて削除した上で、選択したフォルダから新規インポートを行う。

### ユースケース

- メタデータJSONの構造を修正した後に全件やり直したい
- ファイル構成を大幅に変更した後にDBを同期し直したい
- 開発・テスト中にDBをリセットしたい

### UI

インポートダイアログに CheckBox を追加：

```
┌─────────────────────────────────────────────────────┐
│  フォルダ: [____________________________] [参照...]  │
│                                                     │
│  ☑ サブフォルダも含める                              │
│  ☑ 既にインポート済みのファイルをスキップ             │
│  ☐ 全データをクリアして再インポート ⚠                 │
│                                                     │
│  [インポート開始]  [キャンセル]                       │
└─────────────────────────────────────────────────────┘
```

### 仕様

| 項目 | 内容 |
|------|------|
| チェック状態 | デフォルト OFF（通常は差分インポート） |
| 「既存スキップ」との排他 | 「全クリア」ON の場合、「既存スキップ」は自動的に無効化（グレーアウト） |
| 確認ダイアログ | ON でインポート開始時、確認ダイアログを表示してから実行 |
| クリア対象 | `images`、`image_tags`、`works`、`image_fts`、`authors`（※タグマスタは残す/消すを選択可能） |
| サムネイルキャッシュ | クリア時にサムネイルキャッシュディレクトリも削除する |

### 確認ダイアログ

```
⚠ 全データをクリアします

以下のデータがすべて削除されます:
• 画像メタデータ（XX,XXX 件）
• タグ紐付け
• 作品情報
• サムネイルキャッシュ

タグマスタ（タグ名の定義）は保持されます。

この操作は元に戻せません。続行しますか？

[キャンセル]  [クリアして再インポート]
```

### クリア処理の実行順序

```sql
-- 1. FTS5 インデックスをクリア
DELETE FROM image_fts;

-- 2. 画像-タグ紐付けを削除
DELETE FROM image_tags;

-- 3. 画像を全削除（CASCADE で image_tags も消えるが念のため先に削除）
DELETE FROM images;

-- 4. 作品を全削除
DELETE FROM works;

-- 5. 作者を全削除（タグマスタは残す）
DELETE FROM authors;

-- 6. SQLite の VACUUM で空き領域を解放
VACUUM;
```

### サムネイルキャッシュのクリア

```java
// ~/.imagemanager/thumbnails/ 配下を全削除
FileUtils.deleteDirectory(AppConfig.getThumbnailCacheDirectory());
Files.createDirectories(AppConfig.getThumbnailCacheDirectory());
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `import.fxml` | 「全データをクリアして再インポート」CheckBox 追加 |
| `ImportController.java` | CheckBox のバインド、確認ダイアログ表示、排他制御 |
| `ImportService.java` | コンストラクタに `clearAll` フラグ追加、`call()` 冒頭でクリア処理実行 |
| `repository/ImageRepository.java` | `deleteAll()` メソッド追加 |
| `repository/WorkRepository.java` | `deleteAll()` メソッド追加 |
| `repository/AuthorRepository.java` | `deleteAll()` メソッド追加 |
| `repository/ImageTagRepository.java` | `deleteAll()` メソッド追加 |
| `config/DatabaseConfig.java` | `vacuum()` メソッド追加 |
| `service/ThumbnailService.java` | `clearAllFiles()` メソッド追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 開発中は頻繁に使う。本番でもメタデータ修正後のやり直しに必須 |


---

## 8. DBファイルの保存先：設定で変更可能

### 概要

DBファイル・サムネイルキャッシュの保存先を設定で変更可能にする。デフォルトは従来通り `~/.imagemanager/` だが、外付けドライブや任意のパスに変更できる。

### 方式

**設定ファイル + 起動引数の組み合わせ**

| 優先度 | 指定方法 | 用途 |
|--------|---------|------|
| 高 | 起動引数 `--db-dir "D:/ImageLibrary"` | ショートカットごとにDB切替 |
| 中 | `settings.properties` の `db.dir` キー | 恒久的な保存先変更 |
| 低 | デフォルト（`~/.imagemanager/`） | 未設定時のフォールバック |

### 仕様

| 項目 | 内容 |
|------|------|
| 設定キー | `db.dir`（ディレクトリパス） |
| 起動引数 | `--db-dir <path>` |
| 対象ファイル | `imagemanager.db`、`thumbnails/`、`logs/`（settings.properties 自体は `~/.imagemanager/` に残す） |
| ディレクトリ未存在時 | 自動作成する |
| 無効なパス | 書き込み不可の場合はエラーダイアログを表示してデフォルトにフォールバック |
| 設定変更時 | 既存DBの移動はしない（新しいパスに空のDBが作成される。移動はユーザーが手動で行う） |

### 設定画面のUI追加

設定ダイアログに「データ保存先」セクションを追加：

```
┌─────────────────────────────────────────────────────┐
│  設定                                                │
├─────────────────────────────────────────────────────┤
│  検索設定                                            │
│    検索上限件数: [5000 ▲▼] 件                        │
│                                                     │
│  データ保存先                                        │
│    現在のパス: C:\Users\yyy\.imagemanager             │
│    [フォルダ: ____________________________] [参照...]│
│    ※ 変更後は再起動が必要です                        │
│                                                     │
│  [閉じる]                                            │
└─────────────────────────────────────────────────────┘
```

### settings.properties 例

```properties
# DBディレクトリ（空欄 or 未設定 = ~/.imagemanager/）
db.dir=

# カスタム例
db.dir=D:/ImageLibrary/.imagemanager
```

### 起動引数の例

```cmd
ImageManager.exe --db-dir "D:\ImageLibrary\.imagemanager"
```

### 処理フロー

```
アプリ起動
    │
    ▼
起動引数 --db-dir が指定されているか？
    │ Yes → そのパスを使用
    │ No
    ▼
settings.properties の db.dir が設定されているか？
    │ Yes → そのパスを使用
    │ No
    ▼
デフォルト ~/.imagemanager/ を使用
    │
    ▼
パスの書き込み権限を確認
    │ OK → そのまま起動
    │ NG → エラーダイアログ → デフォルトにフォールバック
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `config/AppConfig.java` | `db.dir` の読み込み、`getAppDataDirectory()` をインスタンスメソッド化 |
| `MainApp.java` | 起動引数 `--db-dir` のパース |
| `controller/SettingsController.java` | データ保存先変更UI追加 |
| `fxml/settings.fxml` | フォルダ選択UI追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 中 | ポータブル運用や複数DB管理に有効。ただし大半のユーザーはデフォルトで十分 |


---

## 9. ビューア画面：クリックでページ送り/戻り

### 概要

ビューア画面の画像表示領域をクリックした位置（左半分/右半分）に応じてページ送り/戻りを行う。漫画ビューアの標準的な操作方法。

### 仕様

| 項目 | 内容 |
|------|------|
| 右半分クリック | 綴じ方向に応じて次または前のページへ移動 |
| 左半分クリック | 綴じ方向に応じて前または次のページへ移動 |
| 右綴じ（右→左読み）の場合 | 左クリック = 次ページ、右クリック = 前ページ |
| 左綴じ（左→右読み）の場合 | 右クリック = 次ページ、左クリック = 前ページ |
| 中央のデッドゾーン | 画面幅の中央10%はクリック無効（誤操作防止） |
| 右クリック（コンテキストメニュー） | ページ送りではなくコンテキストメニューを表示（既存動作を維持） |

### 綴じ方向との関係

**右綴じ（日本語漫画の標準）:**

```
┌────────────────────────────────────────┐
│          │  デッド  │                  │
│  次ページ │  ゾーン  │  前ページ         │
│  ←       │  (無効)  │            →     │
│          │          │                  │
└────────────────────────────────────────┘
  左半分      中央10%     右半分
```

**左綴じ（西洋コミックの標準）:**

```
┌────────────────────────────────────────┐
│          │  デッド  │                  │
│  前ページ │  ゾーン  │  次ページ         │
│  ←       │  (無効)  │            →     │
│          │          │                  │
└────────────────────────────────────────┘
  左半分      中央10%     右半分
```

### 実装方針

- `imageAreaPane` に `setOnMouseClicked` イベントを登録
- `MouseEvent.getButton() == PRIMARY`（左クリック）のみ反応
- クリック位置の X 座標が表示エリア幅の何%かで判定
- 中央10%（45%〜55%）はデッドゾーンとして無視
- コンテキストメニュー（右クリック）とは干渉しない

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `ViewerController.java` | `setupClickNavigation()` メソッド追加、`initialize()` から呼び出し |

### コードイメージ

```java
private void setupClickNavigation() {
    imageAreaPane.setOnMouseClicked(event -> {
        if (viewerState == null) return;
        // 左クリックのみ（右クリックはコンテキストメニュー用）
        if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
        // ダブルクリックは無視（将来の拡張余地）
        if (event.getClickCount() != 1) return;

        double areaWidth = imageAreaPane.getWidth();
        double clickX = event.getX();
        double relativeX = clickX / areaWidth;

        // 中央10%はデッドゾーン（誤操作防止）
        if (relativeX > 0.45 && relativeX < 0.55) return;

        boolean clickedLeft = relativeX <= 0.45;

        // 綴じ方向に応じてページ送り方向を決定
        if (viewerState.getBindingDirection() == ViewerState.BindingDirection.RIGHT_TO_LEFT) {
            // 右綴じ: 左クリック=次、右クリック=前
            if (clickedLeft) onNext(); else onPrev();
        } else {
            // 左綴じ: 左クリック=前、右クリック=次
            if (clickedLeft) onPrev(); else onNext();
        }
    });
}
```

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 漫画ビューアとしての基本操作。数行の追加で完了 |


---

## 10. メイン画面：コンテキストメニュー

### 概要

メイン画面の検索結果一覧（サムネイル表示・リスト表示の両方）で右クリックした際に、選択した画像に対する操作メニューを表示する。

### メニュー項目

| メニュー項目 | 動作 |
|-------------|------|
| ビューアで開く | クリックした画像から検索結果全体をビューアで表示 |
| 作品を表示 | 所属する作品の全ページのみでビューアを開く（`work_id = NULL` の場合はグレーアウト） |
| タグを編集... | タグ編集ダイアログを開く |
| 非表示にする / 非表示を解除 | `is_hidden` フラグを切替 |
| Star を設定 | サブメニューで ☆なし / ★1〜★5 を選択 |
| ファイルパスをコピー | クリップボードにファイルパスを設定 |
| エクスプローラーで表示 | OS のファイルマネージャで親フォルダを開く |
| 画像をDBから削除... | 確認ダイアログ付きで DB から削除（ファイル自体は削除しない） |

### 実装方針

- サムネイル表示: 各サムネイルセル（VBox）に `setOnContextMenuRequested` を設定
- リスト表示: `TableView` に `setContextMenu()` を設定
- コンテキストメニューは1つのインスタンスを共有し、右クリック時に対象画像を `contextMenuTarget` に保持
- メニュー表示時に動的にラベルを更新（非表示フラグ、作品の有無）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/MainController.java` | `setupContextMenu()` メソッド追加、`createThumbnailCell()` に右クリックハンドラ追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | メイン画面での操作性向上。ビューアを開かずにタグ・Star・非表示の設定が可能になる |


---

## 11. コンテキストメニュー：タグとStarを同一作品に反映

### 概要

メイン画面・ビューア画面のコンテキストメニューに「タグを作品全体に反映」「Starを作品全体に反映」メニューを追加する。選択中の画像に付与されているタグやStar評価を、同一 `work_id` に属する全画像に一括で反映する。

### ユースケース

- 作品の1ページ目にだけタグを付けた後、作品全体にまとめて適用したい
- ビューアで読了後に特定ページでStar評価を付け、それを作品全体に反映したい
- 個別ページへのタグ付けは維持しつつ、作品レベルの共通タグを追加したい

### メニュー項目

コンテキストメニュー（メイン画面・ビューア画面の両方）に追加：

```
右クリック →
    ...
    ─────────────────
    タグを作品全体に反映
    Star を作品全体に反映
    ─────────────────
    ...
```

| メニュー項目 | 動作 | 無効条件 |
|-------------|------|---------|
| タグを作品全体に反映 | 選択画像のタグを同一 `work_id` の全画像に追加（既存タグは保持、重複はスキップ） | `work_id = NULL` の場合グレーアウト |
| Star を作品全体に反映 | 選択画像の Star 値を同一 `work_id` の全画像に上書き | `work_id = NULL` の場合グレーアウト |

### 仕様

#### 「タグを作品全体に反映」

| 項目 | 内容 |
|------|------|
| 反映元 | 右クリックした画像（= 選択中の画像）のタグ一覧 |
| 反映先 | 同一 `work_id` に属する全画像（反映元自身を含む） |
| 反映方式 | **追加マージ**（反映先の既存タグは削除しない。新しいタグのみ追加） |
| 重複制御 | `INSERT OR IGNORE`（既に紐付いているタグは再追加しない） |
| FTS5更新 | 反映先の各画像について FTS5 インデックスを再構築する |
| 完了通知 | ステータスバーに「X件の画像にタグを反映しました」と表示 |

#### 「Starを作品全体に反映」

| 項目 | 内容 |
|------|------|
| 反映元 | 右クリックした画像の Star 値（0〜5） |
| 反映先 | 同一 `work_id` に属する全画像 |
| 反映方式 | **上書き**（反映先の Star 値を反映元の値で一律更新） |
| 完了通知 | ステータスバーに「X件の画像にStar ★N を反映しました」と表示 |

### 処理フロー

#### タグ反映

```
右クリック → 「タグを作品全体に反映」
    │
    ▼
選択画像の work_id を取得
    │
    ▼
同一 work_id の全画像IDを取得（ImageRepository.findByWorkId）
    │
    ▼
選択画像のタグ一覧を取得（ImageTagRepository.findTagsByImageId）
    │
    ▼
各画像に対してタグを追加（INSERT OR IGNORE で重複スキップ）
    │
    ▼
各画像の FTS5 インデックスを再構築
    │
    ▼
ステータスバーに完了メッセージ表示
```

#### Star反映

```
右クリック → 「Star を作品全体に反映」
    │
    ▼
選択画像の work_id と star を取得
    │
    ▼
同一 work_id の全画像IDを取得
    │
    ▼
各画像の star を UPDATE
    │
    ▼
ステータスバーに完了メッセージ表示
```

### 実装方針

- `ImageService`（または新規 `WorkService`）に `applyTagsToWork(Long imageId)` と `applyStarToWork(Long imageId)` メソッドを追加
- 既存の `ImageTagRepository.addTag()` は `INSERT OR IGNORE` なので重複追加は自動的にスキップされる
- FTS5 の再構築は `ImageRepository.insertFts()` を各画像に対して呼び出す
- メニュー項目の無効化は `work_id == null` で判定（既存の「作品を表示」と同じロジック）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `service/ImageService.java` | `applyTagsToWork(Long imageId)`, `applyStarToWork(Long imageId)` メソッド追加 |
| `controller/MainController.java` | コンテキストメニューに2項目追加、アクションハンドラ追加 |
| `controller/ViewerController.java` | コンテキストメニューに2項目追加、アクションハンドラ追加 |

### SQL（参考）

```sql
-- タグ反映: 選択画像のタグを同一作品の全画像に追加
INSERT OR IGNORE INTO image_tags (image_id, tag_id)
SELECT i.id, it.tag_id
FROM images i
CROSS JOIN image_tags it
WHERE i.work_id = ?        -- 対象作品のwork_id
  AND it.image_id = ?      -- 反映元画像のID
  AND i.id != ?;           -- 反映元自身は除外（既に持っている）

-- Star反映: 同一作品の全画像のStarを更新
UPDATE images SET star = ? WHERE work_id = ?;
```

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 作品単位での管理が楽になる。既存のリポジトリメソッドの組み合わせで実装可能 |


---

## 12. メイン画面：タグフィルター複数選択（OR検索）

### 概要

現在のタグフィルターは単一選択の ComboBox だが、これを複数選択可能に変更する。選択された複数タグのいずれかを持つ画像を表示する（OR条件）。

### 現状の問題

- 現在: ComboBox で1つのタグしか選択できない
- 要望: 「タグA OR タグB OR タグC」のように複数タグのいずれかを持つ画像を一括検索したい

### 仕様

| 項目 | 内容 |
|------|------|
| UI部品 | CheckComboBox（カスタム実装）またはチェック付きドロップダウン |
| 検索条件 | 選択されたタグのいずれかを持つ画像を返す（OR条件） |
| 未選択時 | 全タグ（フィルターなし）と同等 |
| 表示 | 選択中のタグ数をラベルに表示（例: 「3タグ選択中」） |
| リセット | 「すべて」をクリックで全チェックOFF（フィルター解除） |

### UI イメージ

```
┌─────────────────────────────┐
│  タグ: [3タグ選択中 ▼]       │
│       ┌───────────────────┐ │
│       │ ☐ すべて（解除）   │ │
│       │ ☑ R-18            │ │
│       │ ☑ おねショタ       │ │
│       │ ☐ 乳首責め         │ │
│       │ ☑ 手コキ           │ │
│       │ ☐ 足コキ           │ │
│       │ ...               │ │
│       └───────────────────┘ │
└─────────────────────────────┘
```

### 実装方針

JavaFX には標準の CheckComboBox がないため、以下のいずれかで実装する：

**方針A: MenuButton + CheckMenuItem（推奨・依存ゼロ）**

- `MenuButton` を使い、各タグを `CheckMenuItem` として追加
- チェック変更時に `SearchCondition.tagIds` を更新して即時検索
- MenuButton のテキストを選択状態に応じて動的に変更

**方針B: ControlsFX の CheckComboBox**

- ControlsFX ライブラリを依存追加すれば `CheckComboBox` が使える
- ただし依存追加が必要（ライセンス: BSD 3-Clause）

→ **方針A を採用**（外部依存なし・軽量）

### 既存コードの変更

| 変更前 | 変更後 |
|--------|--------|
| `ComboBox<String> tagFilterCombo` | `MenuButton tagFilterButton` |
| `main.fxml` の ComboBox | MenuButton に変更 |
| `loadTagFilter()` で setItems | `loadTagFilter()` で CheckMenuItem 追加 |
| `buildSearchCondition()` でタグ名→ID変換 | 選択中の CheckMenuItem からタグID リストを構築 |

### `SearchCondition` との連携

`SearchCondition.tagIds` は既に `List<Long>` で OR 検索に対応済み（SQLは `tag_id IN (?, ?, ...)` で構築される）。UI側の変更のみで機能する。

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | `ComboBox` → `MenuButton` に置き換え |
| `controller/MainController.java` | `tagFilterCombo` → `tagFilterButton` に変更、CheckMenuItem ロジック追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | タグベースの柔軟な検索はアプリのコア体験。OR条件で絞り込めないと大量タグ環境で不便 |


---

## 13. メイン画面：検索条件クリアボタン

### 概要

検索バーに「条件をクリア」ボタンを追加し、すべての検索条件（キーワード・タグ・作者・Star・非表示除外）を一括でデフォルト状態にリセットする。

### ユースケース

- 複雑な絞り込みを設定した後、手動で1つずつ戻すのが面倒
- 「全件表示に戻りたい」をワンクリックで実現

### 仕様

| 項目 | 内容 |
|------|------|
| ボタン配置 | 検索バーの右端（検索ボタンの隣） |
| ボタンラベル | 「✕ クリア」または「リセット」 |
| リセット対象 | キーワード、タグフィルター、作者フィルター、Starフィルター、非表示除外フラグ |
| リセット後の動作 | 自動的に再検索を実行して全件表示に戻る |

### リセット後の各フィールドの値

| フィールド | リセット後の値 |
|-----------|--------------|
| キーワード | 空文字 |
| タグフィルター | 全チェックOFF（= フィルターなし） |
| 作者フィルター | 「すべての作者」 |
| Star フィルター | 「すべて」 |
| 非表示除外 | ON（デフォルト: 非表示を除外する） |

### UI イメージ

```
┌─────────────────────────────────────────────────────────────────┐
│  [キーワード____] [タグ▼] [作者▼] [Star▼] [☑非表示除外]          │
│                                           [検索] [✕ クリア]     │
└─────────────────────────────────────────────────────────────────┘
```

### 実装方針

- `main.fxml` に Button を追加（`fx:id="clearSearchButton"`）
- `MainController` に `onClearSearch()` メソッドを追加
- 各フィールドをデフォルト値にリセットしてから `onSearch()` を呼び出す

### コードイメージ

```java
@FXML
private void onClearSearch() {
    keywordField.clear();
    // タグフィルター: 全チェックOFF
    resetTagFilter();
    authorFilterCombo.setValue("すべての作者");
    starFilterCombo.setValue("すべて");
    excludeHiddenToggle.setSelected(true);
    onSearch();
    statusMessageLabel.setText("検索条件をクリアしました");
}
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | 検索バーに「クリア」ボタン追加 |
| `controller/MainController.java` | `onClearSearch()` メソッド追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | ワンクリックで全条件リセットは基本的なUX。実装コストが極めて小さい |


---

## 14. タグのStar評価（タグリストをStar順にソート）

### 概要

タグテーブルに `star` カラム（0〜5）を追加し、タグ自体に重要度の評価を付けられるようにする。タグ一覧（タグフィルター、タグツリー、タグ編集ダイアログ）ではStar評価の高い順に並べることで、よく使う重要タグを上位に表示する。

### ユースケース

- タグ数が多い環境で、重要なタグ（ジャンル分類など）を上位に固定したい
- 頻繁に使うタグを目立つ位置に置いて操作性を向上させたい
- タグを「ジャンル」「属性」「補助」のように重要度で階層化したい

### テーブル変更

#### `tags` テーブルへのカラム追加

```sql
ALTER TABLE tags ADD COLUMN star INTEGER NOT NULL DEFAULT 0;
```

#### 変更後の `tags` テーブル

```sql
CREATE TABLE IF NOT EXISTS tags (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE,
    star INTEGER NOT NULL DEFAULT 0  -- Star評価（0〜5、0=未評価）
);
```

### 仕様

| 項目 | 内容 |
|------|------|
| 値の範囲 | 0〜5（整数） |
| 0 の意味 | 未評価（デフォルト） |
| デフォルト | 0（タグ新規作成時） |
| ソート規則 | Star降順 → タグ名昇順（同じStar内は名前順） |

### ソート規則の詳細

全てのタグリスト表示箇所で以下のソートを適用する：

```sql
SELECT * FROM tags ORDER BY star DESC, name ASC;
```

結果例：
```
★★★ おねショタ
★★★ R-18
★★  手コキ
★★  足コキ
★   DL販売
☆   ドMホイホイ
☆   乳首責め
☆   逆転無し
```

### Star設定UI

#### タグ編集ダイアログ

タグ一覧の各行に Star 設定コントロールを追加：

```
┌─────────────────────────────────────────────────┐
│  タグ編集 - image001.jpg                         │
├─────────────────────────────────────────────────┤
│  現在のタグ:                                     │
│    ★★★ おねショタ         [削除]                │
│    ★★  手コキ             [削除]                │
│    ☆   逆転無し           [削除]                │
│                                                 │
│  タグを追加: [______________] [追加]             │
│                                                 │
│  既存タグ:                                      │
│    ★★★ R-18        ★★ 足コキ                   │
│    ★★★ おねショタ   ★  DL販売                   │
│    ...                                          │
└─────────────────────────────────────────────────┘
```

#### タグ管理画面（将来拡張）

タグの Star を変更する専用UI（またはコンテキストメニュー）：

```
右クリック → Star を設定 →  ☆ なし / ★1 / ★2 / ★3 / ★4 / ★5
```

#### 簡易設定（初回実装スコープ）

タグツリーのコンテキストメニューに Star 設定を追加：

```
タグツリーで右クリック →
    Star を設定 →  ☆ なし / ★1 / ★2 / ★3 / ★4 / ★5
```

### 影響範囲（ソート変更箇所）

| 箇所 | 現在のソート | 変更後のソート |
|------|-------------|--------------|
| タグツリー（メイン画面左パネル） | `ORDER BY name` | `ORDER BY star DESC, name ASC` |
| タグフィルター（ComboBox / MenuButton） | `ORDER BY name` | `ORDER BY star DESC, name ASC` |
| タグ編集ダイアログ: 既存タグ一覧 | `ORDER BY name` | `ORDER BY star DESC, name ASC` |
| タグ編集ダイアログ: 画像のタグ表示 | `ORDER BY name` | `ORDER BY star DESC, name ASC` |
| FTS5 インデックス | 影響なし | 影響なし |

### Domain エンティティ変更

```java
public class Tag {
    private Long id;
    private String name;
    private int star = 0;  // 追加

    // コンストラクタ、getter/setter追加
}
```

### Repository 変更

```java
// TagRepository
public void updateStar(Long id, int star);

// findAll のソートを変更
public List<Tag> findAll() {
    String sql = "SELECT * FROM tags ORDER BY star DESC, name ASC";
    ...
}

// ImageTagRepository.findTagsByImageId のソートを変更
public List<Tag> findTagsByImageId(Long imageId) {
    String sql = """
        SELECT t.id, t.name, t.star
        FROM tags t
        INNER JOIN image_tags it ON t.id = it.tag_id
        WHERE it.image_id = ?
        ORDER BY t.star DESC, t.name ASC
        """;
    ...
}
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `schema.sql` | `tags` テーブルに `star` カラム追加 |
| `domain/Tag.java` | `star` フィールド追加 |
| `repository/TagRepository.java` | `updateStar()` 追加、`findAll()` ソート変更、`mapRow()` に star 追加 |
| `repository/ImageTagRepository.java` | `findTagsByImageId()` ソート変更、`mapRow()` に star 追加 |
| `config/DatabaseConfig.java` | マイグレーション（`ALTER TABLE tags ADD COLUMN star`） |
| `controller/MainController.java` | タグツリーの並び順変更、タグ Star 設定コンテキストメニュー追加 |
| `controller/TagEditController.java` | タグ一覧の並び順変更、Star 表示 |

### マイグレーション

既存DBとの互換性のため、`DatabaseConfig` の初期化時にカラムの存在チェックを行い、存在しなければ ALTER TABLE を実行する：

```java
// 既存DBにstarカラムがなければ追加
try {
    connection.createStatement().execute(
        "ALTER TABLE tags ADD COLUMN star INTEGER NOT NULL DEFAULT 0");
} catch (SQLException e) {
    // 既にカラムが存在する場合は無視（duplicate column name エラー）
}
```

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | タグ数が増えると一覧から探すのが困難になる。重要タグの上位固定で操作性が大幅向上 |


---

## 15. 非表示機能：作品全体をまとめて非表示

### 概要

コンテキストメニューに「作品全体を非表示にする」「作品全体の非表示を解除」メニューを追加し、同一 `work_id` に属する全画像の `is_hidden` フラグを一括で切り替える。

### ユースケース

- 作品単位で「見たくない作品」を一括非表示にしたい
- 1ページ目だけ非表示にしても他のページが検索結果に出てくるのを防ぎたい
- 非表示にした作品をまとめて復帰させたい

### メニュー項目

コンテキストメニュー（メイン画面・ビューア画面の両方）に追加：

```
右クリック →
    ...
    非表示にする              ← 既存（1画像のみ）
    作品全体を非表示にする     ← 新規
    ─────────────────
    ...
```

| メニュー項目 | 動作 | 無効条件 |
|-------------|------|---------|
| 作品全体を非表示にする / 作品全体の非表示を解除 | 同一 `work_id` の全画像の `is_hidden` を一括切替 | `work_id = NULL` の場合グレーアウト |

### 仕様

| 項目 | 内容 |
|------|------|
| 反映元 | 右クリックした画像の `work_id` |
| 反映先 | 同一 `work_id` に属する全画像 |
| 動作判定 | 反映元画像が非表示 → 作品全体の非表示を解除 / 反映元画像が表示中 → 作品全体を非表示にする |
| ラベル動的切替 | 反映元画像の `is_hidden` 状態に応じてメニューラベルを切り替える |
| 完了通知 | ステータスバーに「X件の画像を非表示にしました」「X件の画像の非表示を解除しました」と表示 |
| 検索結果反映 | 非表示除外フィルターが ON の場合、操作後に自動的に再検索する |

### SQL

```sql
-- 作品全体を非表示にする
UPDATE images SET is_hidden = 1 WHERE work_id = ?;

-- 作品全体の非表示を解除する
UPDATE images SET is_hidden = 0 WHERE work_id = ?;
```

### 処理フロー

```
右クリック → 「作品全体を非表示にする」
    │
    ▼
選択画像の work_id を取得
    │ work_id = NULL → メニュー無効（到達しない）
    ▼
UPDATE images SET is_hidden = 1 WHERE work_id = ?
    │
    ▼
更新件数をステータスバーに表示
    │
    ▼
非表示除外フィルターが ON の場合 → 再検索
```

### 実装方針

- `ImageRepository` に `updateHiddenByWorkId(Long workId, boolean isHidden)` メソッドを追加
- `ImageService` に `toggleHiddenForWork(Long workId, boolean isHidden)` メソッドを追加
- メイン画面・ビューア画面のコンテキストメニューに項目追加
- 既存の「非表示にする」（1画像）と並べて配置

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `repository/ImageRepository.java` | `updateHiddenByWorkId(Long workId, boolean isHidden)` メソッド追加 |
| `service/ImageService.java` | `toggleHiddenForWork(Long workId, boolean isHidden)` メソッド追加 |
| `controller/MainController.java` | コンテキストメニューに「作品全体を非表示にする」追加 |
| `controller/ViewerController.java` | コンテキストメニューに「作品全体を非表示にする」追加 |

### コードイメージ

```java
// ImageRepository
public int updateHiddenByWorkId(Long workId, boolean isHidden) {
    String sql = "UPDATE images SET is_hidden = ? WHERE work_id = ?";
    try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
        ps.setInt(1, isHidden ? 1 : 0);
        ps.setLong(2, workId);
        int count = ps.executeUpdate();
        log.debug("作品全体の非表示を更新しました: workId={}, isHidden={}, count={}", workId, isHidden, count);
        return count;
    } catch (SQLException e) {
        log.error("作品全体の非表示更新に失敗しました: workId={}", workId, e);
        throw new RuntimeException("作品全体の非表示更新に失敗しました", e);
    }
}
```

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 作品単位の管理はコア機能。1ページずつ非表示にする手間をなくす。既存メソッドのバリエーションで実装可能 |


---

## 16. ビューア画面：次の作品へ/前の作品へ（マウススワイプ操作）

### 概要

ビューア画面で作品の最後のページを超えてページ送りした場合、または水平方向のマウススワイプ操作で、検索結果内の次/前の作品へジャンプする。作品単位でのブラウジングを可能にする。

### ユースケース

- 検索結果に複数の作品が含まれている状態で、作品を順番に「読み切り→次の作品」とスムーズに移動したい
- 興味のない作品をスワイプで飛ばして次の作品に素早くアクセスしたい
- 1枚画像（作品未所属）が混在していても、作品単位のスキップが機能してほしい

### 操作方法

| 操作 | 動作 |
|------|------|
| 水平スワイプ（左→右） | 次の作品の先頭ページへジャンプ |
| 水平スワイプ（右→左） | 前の作品の先頭ページへジャンプ |
| 最終ページで「次へ」操作 | 次の作品の先頭ページへジャンプ（ページ送りの延長） |
| 先頭ページで「前へ」操作 | 前の作品の最終ページへジャンプ |
| キーボードショートカット | `Ctrl + →` = 次の作品、`Ctrl + ←` = 前の作品 |

### 「次の作品」の定義

ビューアに渡された画像リスト（検索結果）内で、現在の画像と異なる `work_id` を持つ次の画像グループの先頭。

```
検索結果リスト:
  [作品A] page 1  ← 現在ここ
  [作品A] page 2
  [作品A] page 3
  [作品B] page 1  ← 「次の作品」= ここへジャンプ
  [作品B] page 2
  [単独画像X]      ← 「次の作品」= ここ（work_id=NULL の画像は1つで1グループ扱い）
  [作品C] page 1  ← 「次の作品」= ここ
```

### 仕様

| 項目 | 内容 |
|------|------|
| スワイプ検出 | マウスドラッグの水平移動距離が閾値（100px）を超えた場合にスワイプと判定 |
| 垂直スワイプ | 無視（ページ送りはスクロール/クリックで行う） |
| スワイプ方向 | 綴じ方向に関係なく、左→右 = 次の作品、右→左 = 前の作品（直感的操作） |
| 作品未所属画像 | `work_id = NULL` の画像は1枚で独立した「作品」として扱う |
| 最後の作品 | 次の作品がない場合は何もしない（バウンス表現など将来拡張余地） |
| 最初の作品 | 前の作品がない場合は何もしない |
| ページ送りからの自動遷移 | 作品最終ページで「次へ」→ 次の作品先頭にジャンプ（現在は検索結果の次画像に行くが、作品モード時は作品単位で動作） |

### スワイプ検出の実装

```
mousePressed → ドラッグ開始位置（startX, startY）を記録
mouseDragged → 移動量を追跡
mouseReleased → 水平移動量を計算
    │
    ├── |deltaX| > 100px && |deltaX| > |deltaY| * 2（水平方向が支配的）
    │       │
    │       ├── deltaX > 0（左→右）→ nextWork()
    │       └── deltaX < 0（右→左）→ prevWork()
    │
    └── それ以外 → 通常のクリック操作として処理
```

### 作品ジャンプのロジック

```java
/**
 * 検索結果リスト内で次の作品の先頭インデックスを返す。
 */
private int findNextWorkIndex(int currentIndex) {
    List<ImageSummary> images = viewerState.getImages();
    Long currentWorkId = images.get(currentIndex).getWorkId();

    // まず現在の作品グループを抜ける
    int i = currentIndex + 1;
    while (i < images.size()) {
        Long workId = images.get(i).getWorkId();
        // work_idが変わった = 次の作品の先頭
        if (!Objects.equals(workId, currentWorkId)) {
            return i;
        }
        // currentWorkId == null の場合、各画像が独立なので次の画像 = 次の作品
        if (currentWorkId == null) {
            return i;
        }
        i++;
    }
    return -1; // 次の作品なし
}

/**
 * 検索結果リスト内で前の作品の先頭インデックスを返す。
 */
private int findPrevWorkIndex(int currentIndex) {
    List<ImageSummary> images = viewerState.getImages();
    Long currentWorkId = images.get(currentIndex).getWorkId();

    // まず現在の作品グループの先頭を探す
    int groupStart = currentIndex;
    if (currentWorkId != null) {
        while (groupStart > 0 && Objects.equals(images.get(groupStart - 1).getWorkId(), currentWorkId)) {
            groupStart--;
        }
    }

    // 1つ前の画像が前の作品の最終ページ
    if (groupStart == 0) return -1; // 前の作品なし
    int prevLast = groupStart - 1;
    Long prevWorkId = images.get(prevLast).getWorkId();

    // 前の作品の先頭を探す
    int prevStart = prevLast;
    if (prevWorkId != null) {
        while (prevStart > 0 && Objects.equals(images.get(prevStart - 1).getWorkId(), prevWorkId)) {
            prevStart--;
        }
    }
    return prevStart;
}
```

### キーボードショートカット

| キー | 動作 |
|------|------|
| `Ctrl + →` (Right) | 次の作品の先頭へ |
| `Ctrl + ←` (Left) | 前の作品の先頭へ |
| `Ctrl + Shift + →` | 次の作品の先頭へ（スワイプと同等） |
| `Ctrl + Shift + ←` | 前の作品の先頭へ |

### 視覚フィードバック（オプション・将来拡張）

- スワイプ中にビューア全体が横に少しスライドする（ページめくり感）
- ジャンプ時に作品タイトルをオーバーレイ表示（0.5秒で自動消去）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `setupSwipeNavigation()` メソッド追加、`nextWork()` / `prevWork()` メソッド追加 |
| `controller/ViewerController.java` | `setupKeyboardShortcuts()` に Ctrl+Arrow キー追加 |
| `dto/ViewerState.java` | （変更なし — 既存の画像リスト・currentIndex で対応可能） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 中 | 作品ブラウジングの利便性向上。ただし既存のページ送り + 「作品を表示」で代替可能ではある |


---

## 17. ビューア画面：画像読み込み中プログレスアイコン表示

### 概要

ビューア画面で画像を切り替えた際、ファイルの読み込み（デコード）が完了するまでの間、プログレスインジケーター（スピナー）を画像表示エリアの中央に表示する。大きな画像ファイルやネットワークドライブ上のファイルで読み込みに時間がかかる場合のフィードバックを提供する。

### ユースケース

- 高解像度画像（10MB超）の読み込みに1〜2秒かかるとき、画面が真っ白で応答なしに見えるのを防ぐ
- ネットワークドライブ / 外付けHDD上のファイルでレイテンシがある場合のUX改善
- 見開き表示で片方だけ先に表示される場合の視覚的な整合性

### 仕様

| 項目 | 内容 |
|------|------|
| 表示条件 | 画像切り替え開始時に表示、`Image.progressProperty()` が 1.0 になったら非表示 |
| インジケーター種類 | JavaFX `ProgressIndicator`（円形スピナー） |
| 配置 | 画像表示エリア（`imageAreaPane`）の中央 |
| サイズ | 64x64 px |
| 動画の場合 | 動画はVLCJで再生するためプログレスは非表示（動画には別の対応 → #18） |
| 見開き表示 | 左右それぞれに独立したインジケーターを配置 |

### 実装方針

JavaFX の `Image` クラスは非同期読み込みに対応しており、`backgroundLoading = true` を指定すると `progressProperty()` で読み込み進捗を取得できる。

```java
// 非同期読み込みを有効化
Image fxImage = new Image("file:" + filePath, true); // backgroundLoading = true

// プログレスインジケーターの表示制御
ProgressIndicator spinner = new ProgressIndicator();
spinner.setMaxSize(64, 64);
spinner.visibleProperty().bind(fxImage.progressProperty().lessThan(1.0));
```

### 処理フロー

```
ページ切り替え発生
    │
    ▼
プログレスインジケーターを表示
    │
    ▼
Image("file:...", true) で非同期読み込み開始
    │
    ▼
Image.progressProperty() を監視
    │ progress == 1.0
    ▼
インジケーターを非表示、ImageView に画像を設定
    │
    ▼
読み込みエラー発生時
    → インジケーターを非表示
    → エラーアイコン（またはプレースホルダー）を表示
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `ProgressIndicator` の追加、画像読み込み時のバインディング |
| `viewer.fxml` | `StackPane` 内に `ProgressIndicator` を配置（初期状態は非表示） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | ユーザー体験の基本。読み込み中のフィードバックがないとアプリがフリーズしたように見える |


---

## 18. ビューア画面：VLCJ無効時の動画×アイコン表示

### 概要

VLCJが利用不可（VLC未インストールまたはVLCJ初期化失敗）の環境で動画ファイル（webm, mp4等）を表示しようとした際、画像表示エリアに「再生不可」を示す×アイコンとメッセージを表示する。現在は何も表示されず空白になるため、ユーザーに状態が伝わらない。

### ユースケース

- VLC未インストール環境で動画ファイルがインポートされている場合の状態表示
- 「表示されない = バグ？」という誤解を防ぐ
- VLCのインストールを促すガイダンスを提供

### 仕様

| 項目 | 内容 |
|------|------|
| 表示条件 | `mediaType == "video"` かつ VLCJ が使用不可の場合 |
| 表示内容 | ×アイコン（またはビデオ禁止アイコン）+ メッセージ |
| メッセージ | 「動画を再生できません\nVLC Media Player をインストールしてください」 |
| クリック動作 | なし（静的な表示のみ） |
| VLCJ有効時 | 従来通り動画を再生（この機能は発動しない） |

### 表示イメージ

```
┌──────────────────────────────────────────────────┐
│                                                  │
│                                                  │
│                   🚫                             │
│              （×アイコン）                        │
│                                                  │
│         動画を再生できません                      │
│   VLC Media Player をインストールしてください      │
│                                                  │
│                                                  │
└──────────────────────────────────────────────────┘
```

### VLCJ有効判定

現在の `VideoPlayerService` で VLCJ の初期化結果を保持しているので、その状態を参照する：

```java
// VideoPlayerService で VLCJ が使用可能かどうか
public boolean isAvailable() {
    return player != null;
}
```

### 実装方針

- `ViewerController` の画像表示メソッドで `mediaType == "video"` の分岐を追加
- VLCJ 有効 → 従来通り動画再生
- VLCJ 無効 → `ImageView` の代わりにプレースホルダー（Label + アイコン）を表示
- プレースホルダーは `StackPane` 内に配置し、画像表示エリアと同じ位置に表示

### コードイメージ

```java
private void displayVideoUnavailable() {
    // 画像エリアをクリア
    singleImageView.setImage(null);

    // プレースホルダーを表示
    videoUnavailableLabel.setVisible(true);
}

private void hideVideoUnavailable() {
    videoUnavailableLabel.setVisible(false);
}
```

```fxml
<!-- viewer.fxml の imageAreaPane 内に配置 -->
<VBox fx:id="videoUnavailablePane" alignment="CENTER" spacing="8" visible="false">
    <Label text="🚫" style="-fx-font-size: 48px;"/>
    <Label text="動画を再生できません" style="-fx-font-size: 16px; -fx-text-fill: #888;"/>
    <Label text="VLC Media Player をインストールしてください" style="-fx-font-size: 12px; -fx-text-fill: #aaa;"/>
</VBox>
```

### サムネイル表示での対応

メイン画面のサムネイル一覧でも動画ファイルにはビデオアイコンのプレースホルダーを表示する（現在は画像サムネイルが生成できない → 空白）。

| 場面 | 表示 |
|------|------|
| 画像ファイル | サムネイル画像 |
| 動画ファイル（VLCJ有効） | ビデオアイコン + ファイル名 |
| 動画ファイル（VLCJ無効） | ×アイコン + ファイル名 |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | 動画表示時の VLCJ 有効判定・プレースホルダー表示ロジック追加 |
| `viewer.fxml` | `videoUnavailablePane`（VBox）を追加 |
| `controller/MainController.java` | サムネイル一覧での動画プレースホルダー表示（オプション） |
| `service/VideoPlayerService.java` | `isAvailable()` メソッド追加（既存の状態を公開） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | VLC未インストール環境での基本的なフィードバック。空白表示はバグに見える |


---

## 19. コンテキストメニュー：最近つけたタグ履歴から選択

### 概要

コンテキストメニューに「タグを追加」サブメニューを追加し、最近使用したタグの履歴リストから選択するだけで画像にタグを付与できるようにする。タグ編集ダイアログを開かずに、頻繁に使うタグをワンクリックで付けられる。

### ユースケース

- 大量の画像を連続で閲覧しながら「お気に入り」「あとで読む」などのタグを素早く付けたい
- タグ編集ダイアログを開く手間（右クリック→タグを編集→入力→追加→閉じる）を省略したい
- 同じタグを繰り返し付ける作業（連続タグ付け）の効率化

### メニュー構造

```
右クリック →
    ...
    タグを追加 →  ★★★ おねショタ
                  ★★  手コキ
                  ★   R-18
                  ─────────────
                  最近使用したタグ:
                  足コキ
                  DL販売
                  乳首責め
                  ─────────────
                  その他... (タグ編集ダイアログを開く)
    ...
```

### 仕様

| 項目 | 内容 |
|------|------|
| 上部セクション | Star評価の高いタグ上位N件（★1以上、最大5件） |
| 中部セクション | 最近使用したタグ（時系列降順、最大10件） |
| 下部 | 「その他...」でタグ編集ダイアログを開く |
| 既に付与済みのタグ | チェックマーク（✓）を付けて表示。クリックで削除 |
| 未付与のタグ | クリックで即座に付与 |
| 履歴の保持件数 | 最大20件（メモリ内リングバッファ、永続化は settings.properties） |
| 履歴の更新タイミング | タグを追加した時点で履歴の先頭に追加 |

### 「最近使用したタグ」の定義

- ユーザーが任意の画像にタグを**付与した**操作を時系列で記録
- 同一タグが複数回使われた場合は最新の使用時刻で1エントリに集約
- タグの**削除**操作は履歴に含めない
- インポート時の自動タグ付けは履歴に含めない（手動操作のみ）

### 表示ロジック

```
メニュー表示時:
    1. Star付きタグ（Star ≥ 1、Star DESC, name ASC）上位5件を表示
    2. セパレーター
    3. 最近使用したタグ 最大10件（Star付きタグと重複するものは除外）
    4. セパレーター
    5. 「その他...」（タグ編集ダイアログ）

各タグの表示:
    - 画像に既に付与済み → "✓ タグ名"（クリックで削除）
    - 未付与 → "  タグ名"（クリックで追加）
```

### 履歴管理クラス

```java
/**
 * 最近使用したタグの履歴を管理するクラス。
 * アプリ起動中はメモリに保持し、終了時に settings.properties に永続化する。
 */
public class TagHistory {

    /** 最大保持件数。 */
    private static final int MAX_SIZE = 20;

    /** 履歴リスト（新しいものが先頭）。 */
    private final LinkedList<String> history = new LinkedList<>();

    /**
     * タグ名を履歴に追加する（先頭に挿入、既存は削除して再挿入）。
     */
    public void add(String tagName) {
        history.remove(tagName);
        history.addFirst(tagName);
        if (history.size() > MAX_SIZE) {
            history.removeLast();
        }
    }

    /**
     * 最近使用したタグを最大N件返す。
     */
    public List<String> getRecent(int limit) {
        return history.stream().limit(limit).collect(Collectors.toList());
    }
}
```

### 永続化

`settings.properties` に CSV 形式で保存：

```properties
# 最近使用したタグ（カンマ区切り、新しい順）
tag.history=足コキ,DL販売,乳首責め,おねショタ,手コキ
```

### 操作フロー

```
右クリック → 「タグを追加」サブメニュー
    │
    ▼
メニュー表示時に現在の画像のタグ一覧を取得
    │
    ▼
Star付きタグ + 最近使用タグ を一覧表示（付与済みは ✓ マーク）
    │
    ▼
ユーザーがタグをクリック
    │
    ├── 未付与のタグ → image_tags に追加 → FTS5更新 → 履歴に追加
    │
    └── 付与済みのタグ（✓付き）→ image_tags から削除 → FTS5更新
    │
    ▼
ステータスバーに「タグ '○○' を追加しました」表示
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `service/TagHistory.java`（新規） | タグ履歴管理クラス |
| `config/AppConfig.java` | `tag.history` の読み書きメソッド追加 |
| `controller/MainController.java` | コンテキストメニューに「タグを追加」サブメニュー追加 |
| `controller/ViewerController.java` | コンテキストメニューに「タグを追加」サブメニュー追加 |
| `service/TagService.java` | タグ追加時に履歴を更新するフック |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | 連続タグ付け作業の効率が劇的に向上する。ダイアログを開く手間の削減はUXインパクト大 |


---

## 20. インポート：フォルダを1作品として取り込み

### 概要

指定したフォルダ内の画像ファイルを「1つの作品」として取り込む。フォルダ名を作品タイトルとし、フォルダ内のファイルをファイル名順でページ番号を自動付与する。`-meta.json` が存在しない一般的なフォルダ構成でも作品としてまとめてインポートできる。

### ユースケース

- 自分で整理したフォルダ（例: `2026-06-01 旅行写真/`）を1作品として登録したい
- pixivダウンローダー以外のソース（手動ダウンロード、スキャン画像）を作品化したい
- サブフォルダ1つ = 1作品 としてまとめてインポートしたい

### 操作方法

#### 方法A: インポートダイアログにオプション追加

```
┌─────────────────────────────────────────────────────┐
│  フォルダ: [____________________________] [参照...]  │
│                                                     │
│  ☑ サブフォルダも含める                              │
│  ☑ 既にインポート済みのファイルをスキップ             │
│  ☐ 全データをクリアして再インポート ⚠                 │
│  ☐ フォルダごとに1作品としてまとめる                  │  ← 新規
│                                                     │
│  [インポート開始]  [キャンセル]                       │
└─────────────────────────────────────────────────────┘
```

#### 方法B: コンテキストメニュー / 専用ダイアログ

メイン画面メニューバーに「フォルダを作品としてインポート...」を追加。

### 仕様

| 項目 | 内容 |
|------|------|
| 作品タイトル | フォルダ名（末尾のディレクトリ名） |
| ページ番号 | フォルダ内のファイルをファイル名昇順ソートし、1始まりで連番付与 |
| 作者 | 未設定（後から手動で設定） |
| `-meta.json` との併用 | フォルダ内に `-meta.json` が存在すればそちらを優先（既存ロジック） |
| サブフォルダ展開 | 「サブフォルダも含める」+ 「フォルダごとに1作品」の組み合わせで、各サブフォルダが独立した作品になる |
| 重複判定 | ファイルパスベースの既存チェック（既存と同じ） |

### 処理フロー（サブフォルダごとに1作品）

```
ルートフォルダ/
├── 作品A/
│   ├── 001.jpg  → 作品A page 1
│   ├── 002.jpg  → 作品A page 2
│   └── 003.jpg  → 作品A page 3
├── 作品B/
│   ├── page01.png  → 作品B page 1
│   └── page02.png  → 作品B page 2
└── 単独画像.jpg     → 作品未所属（ルート直下）
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `import.fxml` | 「フォルダごとに1作品としてまとめる」CheckBox 追加 |
| `controller/ImportController.java` | CheckBox バインド |
| `service/ImportService.java` | フォルダ単位の作品作成ロジック追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | `-meta.json` がないファイルでも作品として管理できるのは必須。汎用的な取り込み手段 |


---

## 21. インポート：PDFファイルの取り込み

### 概要

PDFファイルをインポート対象に追加する。DBには PDF ファイル自体を1レコードとして登録し、ビューア表示時に各ページをキャッシュに展開する遅延展開方式を採用する。

### 現状

- `AppConfig.SUPPORTED_IMAGE_EXTENSIONS` / `SUPPORTED_VIDEO_EXTENSIONS` にPDFは含まれていない
- `Apache PDFBox 3.0.3` は既に依存に含まれている（PDF出力用）
- PDFBox はページの画像レンダリング（`PDFRenderer`）にも対応している

### 設計方針: 遅延展開方式

**DBに登録するのはPDFファイル自体。ビューア表示時にキャッシュに画像展開する。**

| 項目 | 内容 |
|------|------|
| インポート時 | PDFファイルを `images` テーブルに1レコード登録 + `works` テーブルに作品登録 |
| ビューア表示時 | PDFBoxで要求されたページを画像レンダリングし、キャッシュディレクトリに保存 |
| サムネイル | 初回表示時に1ページ目をレンダリングしてサムネイル生成 |

### DB登録イメージ

```sql
-- images テーブル: PDFファイル自体を登録
INSERT INTO images (file_path, file_name, media_type, work_id, page_number)
VALUES ('D:/manga/同人誌A.pdf', '同人誌A.pdf', 'document', 1, NULL);

-- works テーブル: 作品として登録
INSERT INTO works (title, total_pages, source_type, source_path, external_id, ...)
VALUES ('同人誌A', 24, 'pdf', 'D:/manga/同人誌A.pdf', ...);
```

### テーブル変更

`works` テーブルに `source_type` と `source_path` カラムを追加:

```sql
ALTER TABLE works ADD COLUMN source_type TEXT;    -- 'images', 'pdf', 'zip'
ALTER TABLE works ADD COLUMN source_path TEXT;    -- 元ファイルの絶対パス（PDF/ZIP用）
```

| source_type | 意味 |
|-------------|------|
| `NULL` or `'images'` | 通常の画像ファイル群による作品（従来） |
| `'pdf'` | PDFファイルが元ソース |
| `'zip'` | ZIPファイルが元ソース |

### キャッシュディレクトリ

```
<アプリデータ>/db/page-cache/<source_pathのSHA256先頭16文字>/
    page_001.png
    page_002.png
    ...
    _meta.json   ← { "totalPages": 24, "dpi": 150, "cachedAt": "...", "sourceModified": "..." }
```

### 処理フロー

#### インポート時

```
PDFファイル発見
    │
    ▼
PDFBox で開いてページ数のみ取得（フルレンダリングはしない）
    │
    ▼
works テーブルに作品登録（title=ファイル名, total_pages=ページ数, source_type='pdf', source_path=パス）
    │
    ▼
images テーブルにPDFファイルを1レコード登録（media_type='document', work_id=作品ID）
    │
    ▼
1ページ目のみレンダリング → サムネイル生成（メイン画面表示用）
    │
    ▼
完了（高速、数秒で終わる）
```

#### ビューア表示時

```
ビューアで作品を開く
    │
    ▼
works.source_type == 'pdf' ?
    │ Yes
    ▼
キャッシュに要求ページの画像が存在するか？
    │ Yes → キャッシュから読み込み → ImageView に表示
    │ No  ↓
    ▼
PDFBox でページをレンダリング（150 DPI）→ キャッシュに PNG 保存
    │
    ▼
ImageView に表示
    │
    ▼
先読み: 現在ページ ±2 ページをバックグラウンドでレンダリング（プリフェッチ）
```

### 仕様

| 項目 | 内容 |
|------|------|
| 対象拡張子 | `.pdf` |
| DB登録 | PDFファイル自体を1レコード登録 |
| 作品化 | 1 PDF = 1作品（`works` テーブル、`source_type='pdf'`） |
| 作品タイトル | PDFファイル名（拡張子除く） |
| ページ数取得 | インポート時に PDFBox で取得（レンダリング不要） |
| 画像展開 | ビューア表示時に遅延レンダリング（150 DPI） |
| 先読み | 現在ページ ±2 ページをバックグラウンドでプリフェッチ |
| キャッシュ有効性 | 元PDFの更新日時と比較し、変更があれば再レンダリング |
| キャッシュクリア | 設定画面から手動クリア可能 |
| パスワード付きPDF | スキップして警告ログ出力 |

### ビューアでのページ取得インターフェース

```java
/**
 * 作品のページ画像を取得するプロバイダー。
 * source_type に応じて実装を切り替える。
 */
public interface PageImageProvider {
    /** 指定ページの画像パスを返す（キャッシュに展開済みならそのパス、なければ展開してから返す） */
    Path getPageImage(int pageNumber);
    /** 総ページ数を返す */
    int getTotalPages();
}

// PDF用実装
public class PdfPageProvider implements PageImageProvider { ... }
// ZIP用実装
public class ZipPageProvider implements PageImageProvider { ... }
// 通常画像用（従来: DB上のfile_pathをそのまま返す）
public class ImageFilePageProvider implements PageImageProvider { ... }
```

### メリット

- インポートが高速（ページ数の取得のみ、フルレンダリング不要）
- ディスク容量を常時消費しない（キャッシュは必要に応じて生成・削除可能）
- 元PDFを移動してもキャッシュがあればビューア表示可能（元ファイル消失時は警告）
- DB構造がシンプル（1ファイル = 1レコード）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `schema.sql` | `works` テーブルに `source_type`, `source_path` カラム追加 |
| `domain/Work.java` | `sourceType`, `sourcePath` フィールド追加 |
| `config/AppConfig.java` | `SUPPORTED_DOCUMENT_EXTENSIONS` に `pdf` 追加 |
| `service/ImportService.java` | PDF検出時の分岐（ページ数取得・作品登録のみ） |
| `service/PageImageProvider.java`（新規） | ページ画像取得インターフェース |
| `service/PdfPageProvider.java`（新規） | PDF遅延展開実装 |
| `controller/ViewerController.java` | `source_type` に応じた `PageImageProvider` の切り替え |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 中 | PDFBox は既に依存に含まれており技術的障壁は低い。漫画PDF・同人誌PDFの管理に有効 |


---

## 22. インポート：ZIPファイル内の画像をビューア表示

### 概要

ZIPファイルをインポートし、DBにはZIPファイル自体を登録する。ビューア表示時にZIP内の画像をキャッシュに展開して表示する。1 ZIP = 1作品として管理する。PDF取り込み（#21）と同じ遅延展開方式で統一する。

### 技術的実現可能性

**結論: 可能。** Java 標準ライブラリの `java.util.zip.ZipFile` で外部依存なしに実装可能。

### 設計方針: 遅延展開方式

**DBに登録するのはZIPファイル自体。ビューア表示時にキャッシュに画像展開する。**

| 項目 | 内容 |
|------|------|
| インポート時 | ZIPファイルを `images` テーブルに1レコード登録 + `works` テーブルに作品登録 |
| ビューア表示時 | ZIPから要求されたページの画像を展開し、キャッシュディレクトリに保存 |
| サムネイル | 初回表示時に1ページ目を展開してサムネイル生成 |

### DB登録イメージ

```sql
-- images テーブル: ZIPファイル自体を登録
INSERT INTO images (file_path, file_name, media_type, work_id, page_number)
VALUES ('D:/manga/作品A.zip', '作品A.zip', 'archive', 1, NULL);

-- works テーブル: 作品として登録
INSERT INTO works (title, total_pages, source_type, source_path, external_id, ...)
VALUES ('作品A', 30, 'zip', 'D:/manga/作品A.zip', ...);
```

### キャッシュディレクトリ

```
<アプリデータ>/db/page-cache/<source_pathのSHA256先頭16文字>/
    page_001.jpg
    page_002.jpg
    ...
    _meta.json   ← { "totalPages": 30, "cachedAt": "...", "sourceModified": "...", "entries": [...] }
```

### 処理フロー

#### インポート時

```
ZIPファイル発見
    │
    ▼
ZipFile で開いて画像エントリ数のみカウント（展開はしない）
    │
    ▼
works テーブルに作品登録（title=ZIP名, total_pages=画像数, source_type='zip', source_path=パス）
    │
    ▼
images テーブルにZIPファイルを1レコード登録（media_type='archive', work_id=作品ID）
    │
    ▼
_meta.json に画像エントリ一覧（ファイル名昇順）を保存
    │
    ▼
1ページ目のみ展開 → サムネイル生成（メイン画面表示用）
    │
    ▼
完了（高速）
```

#### ビューア表示時

```
ビューアで作品を開く
    │
    ▼
works.source_type == 'zip' ?
    │ Yes
    ▼
キャッシュに要求ページの画像が存在するか？
    │ Yes → キャッシュから読み込み → ImageView に表示
    │ No  ↓
    ▼
ZipFile から該当エントリを展開 → キャッシュに保存
    │
    ▼
ImageView に表示
    │
    ▼
先読み: 現在ページ ±2 ページをバックグラウンドで展開（プリフェッチ）
```

### 仕様

| 項目 | 内容 |
|------|------|
| 対象拡張子 | `.zip`, `.cbz`（将来: `.rar`, `.cbr`, `.7z`, `.cb7`） |
| DB登録 | ZIPファイル自体を1レコード登録 |
| 作品化 | 1 ZIP = 1作品（`works` テーブル、`source_type='zip'`） |
| 作品タイトル | ZIPファイル名（拡張子除く） |
| ページ数取得 | インポート時にZIPエントリをスキャン（展開不要） |
| ページ順 | ZIP内の画像ファイル名昇順 |
| 対象ファイル | ZIP内のサポート画像拡張子のみ（非画像・ディレクトリは無視） |
| 画像展開 | ビューア表示時に遅延展開 |
| 先読み | 現在ページ ±2 ページをバックグラウンドでプリフェッチ |
| キャッシュ有効性 | 元ZIPの更新日時と比較し、変更があれば再展開 |
| キャッシュクリア | 設定画面から手動クリア可能 |
| パスワード付きZIP | スキップして警告ログ出力（将来: zip4j で対応可能） |
| ファイル名エンコーディング | UTF-8 前提（日本語ファイル名対応） |

### PageImageProvider インターフェース（#21 と共通）

```java
// ZIP用実装
public class ZipPageProvider implements PageImageProvider {
    private final Path zipPath;
    private final Path cacheDir;
    private final List<String> imageEntries; // ファイル名昇順の画像エントリ名リスト

    @Override
    public Path getPageImage(int pageNumber) {
        Path cached = cacheDir.resolve(String.format("page_%03d%s", pageNumber, getExtension(pageNumber)));
        if (Files.exists(cached)) return cached;
        // ZIPから展開してキャッシュに保存
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(imageEntries.get(pageNumber - 1));
            try (InputStream is = zip.getInputStream(entry)) {
                Files.copy(is, cached, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return cached;
    }

    @Override
    public int getTotalPages() {
        return imageEntries.size();
    }
}
```

### メリット

- インポートが高速（エントリスキャンのみ、展開不要）
- ディスク容量を常時消費しない（キャッシュは必要に応じて生成・削除可能）
- ZIPファイルを圧縮状態のまま保持できる
- PDF取り込み（#21）と同じアーキテクチャで統一性が高い
- DB構造がシンプル（1ファイル = 1レコード）

### 注意点

- ZIP内のディレクトリ構造 → フラット化して画像のみ抽出（エントリ名のベースファイル名でソート）
- 巨大ZIP（数GB） → エントリ単位で逐次展開するためメモリ問題なし
- `.cbz` ファイル → 実態はZIPなので拡張子追加のみで対応可能

### 将来拡張

| フォーマット | ライブラリ | ライセンス |
|-------------|-----------|-----------|
| `.rar`, `.cbr` | junrar | LGPL |
| `.7z`, `.cb7` | Apache Commons Compress | Apache 2.0 |
| パスワード付きZIP | zip4j | Apache 2.0 |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `config/AppConfig.java` | `SUPPORTED_ARCHIVE_EXTENSIONS` に `zip`, `cbz` 追加 |
| `service/ImportService.java` | ZIP検出時の分岐（エントリスキャン・作品登録のみ） |
| `service/ZipPageProvider.java`（新規） | ZIP遅延展開実装（`PageImageProvider` インターフェース実装） |
| `controller/ViewerController.java` | `source_type` に応じた `PageImageProvider` の切り替え（#21 と共通） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 中 | Java標準ライブラリで実装可能。漫画ZIPの管理はコアユースケースの一つ。PDF取り込みと実装パターンが共通 |

### #21/#22 共通仕様: 検索結果での混在表示とシームレスなページング

#### 前提

検索結果には以下が混在する可能性がある:
- 通常の画像ファイル（`media_type = 'image'`）
- 動画ファイル（`media_type = 'video'`）
- PDFファイル（`media_type = 'document'`）
- ZIPファイル（`media_type = 'archive'`）

#### 要件

検索結果をビューアで開いた際、PDF/ZIP を含む一連の結果を**連続的にページングして閲覧**できること。ユーザーから見れば、通常画像もPDF内ページもZIP内画像も区別なくページ送りで連続して表示される。

#### ビューアでの動作

```
検索結果（例）:
  [画像A.jpg]           ← 通常画像として表示
  [画像B.png]           ← 通常画像として表示
  [漫画C.pdf]           ← PDF: ページ送りで内部の各ページを順に表示
    └ page 1 → page 2 → ... → page 24
  [画像D.jpg]           ← 通常画像として表示
  [作品E.zip]           ← ZIP: ページ送りで内部の各画像を順に表示
    └ page 1 → page 2 → ... → page 15
  [動画F.webm]          ← 動画として表示
```

ユーザーがビューアで「次へ」を押し続けると:
```
画像A → 画像B → 漫画C page1 → 漫画C page2 → ... → 漫画C page24 → 画像D → 作品E page1 → ... → 作品E page15 → 動画F
```

#### 展開待ちの処理（プログレス表示）

PDF/ZIPのページを表示する際にキャッシュが未生成の場合:

1. **プログレスインジケーターを表示**（#17 と同じスピナー）
2. **バックグラウンドでキャッシュ生成**（レンダリング or 展開）
3. **完了後に画像を表示**
4. **先読み（プリフェッチ）**: 現在表示ページの ±2 ページを同時にバックグラウンドで生成

```
ページ送り操作
    │
    ▼
次に表示すべきコンテンツの種類を判定
    │
    ├── media_type = 'image' → 従来通り画像ファイルを直接表示
    │
    ├── media_type = 'video' → VLCJで再生 or ×アイコン表示
    │
    ├── media_type = 'document' → PdfPageProvider でキャッシュ取得
    │       │ キャッシュあり → 即座に表示
    │       │ キャッシュなし → スピナー表示 → レンダリング → 表示
    │
    └── media_type = 'archive' → ZipPageProvider でキャッシュ取得
            │ キャッシュあり → 即座に表示
            │ キャッシュなし → スピナー表示 → 展開 → 表示
```

#### ビューアの内部モデル: フラットなページリスト

ビューアは検索結果を「フラットなページリスト」に展開して保持する。PDF/ZIP は内部ページ数分だけエントリが増える。

```java
/**
 * ビューアに表示するページの論理エントリ。
 * 通常画像は1エントリ、PDF/ZIPは内部ページ数分のエントリを生成する。
 */
public class ViewerPageEntry {
    /** 表示用のページ画像を提供するプロバイダー */
    private PageImageProvider provider;
    /** プロバイダー内でのページ番号（通常画像は常に1） */
    private int pageInProvider;
    /** 元の ImageSummary への参照（タグ・Star操作用） */
    private ImageSummary source;
}
```

ビューア初期化時:
```java
List<ViewerPageEntry> flatPages = new ArrayList<>();
for (ImageSummary item : searchResults) {
    switch (item.getMediaType()) {
        case "image":
            flatPages.add(new ViewerPageEntry(new ImageFileProvider(item), 1, item));
            break;
        case "document":
            PdfPageProvider pdf = new PdfPageProvider(item);
            for (int p = 1; p <= pdf.getTotalPages(); p++) {
                flatPages.add(new ViewerPageEntry(pdf, p, item));
            }
            break;
        case "archive":
            ZipPageProvider zip = new ZipPageProvider(item);
            for (int p = 1; p <= zip.getTotalPages(); p++) {
                flatPages.add(new ViewerPageEntry(zip, p, item));
            }
            break;
        case "video":
            flatPages.add(new ViewerPageEntry(new VideoProvider(item), 1, item));
            break;
    }
}
```

#### 検索結果一覧での表示

| メディアタイプ | サムネイル表示 | リスト表示 |
|---------------|--------------|-----------|
| `image` | サムネイル画像 | 通常表示 |
| `video` | ビデオアイコン | "動画" 表記 |
| `document` | 1ページ目のサムネイル | "PDF (24p)" 表記 |
| `archive` | 1ページ目のサムネイル | "ZIP (30p)" 表記 |

#### タグ・Star操作の対象

PDF/ZIPの内部ページを表示中にタグ・Starを操作した場合、操作は**元のPDF/ZIPファイルのDBレコード**に対して行われる（個別ページには適用しない）。

#### キャッシュ管理

| 項目 | 内容 |
|------|------|
| キャッシュ保持 | アプリ終了後もキャッシュは残す（次回起動時に高速表示） |
| キャッシュ削除 | 設定画面の「キャッシュクリア」ボタンで一括削除可能 |
| キャッシュサイズ上限 | 将来的にLRU方式で古いキャッシュを自動削除（初期実装では上限なし） |
| キャッシュ無効化 | 元ファイルの更新日時が変わっていたら再生成 |


---

## 23. 検索テキストボックス：入力欄内×クリアボタン

### 概要

キーワード入力テキストフィールドの右端に×ボタンを配置し、クリックでテキストを一括クリアする。ブラウザの検索バーと同じUXを提供する。

### 仕様

| 項目 | 内容 |
|------|------|
| ボタン表示条件 | テキストが1文字以上入力されている場合のみ表示 |
| ボタン非表示条件 | テキストが空の場合は非表示 |
| クリック時の動作 | テキストをクリア → 即座に再検索実行 |
| ボタンの外観 | 背景透明、「×」テキスト（またはアイコン）、ホバーでカーソル変更 |
| TextField との配置 | `StackPane` で重ね、ボタンは右寄せ配置 |
| TextFieldのpadding | 右側に×ボタン分の余白を確保（文字が×と重ならない） |

### UIイメージ

```
テキスト入力中:
┌────────────────────────────┐
│  おねショタ              × │
└────────────────────────────┘

テキスト空:
┌────────────────────────────┐
│                            │
└────────────────────────────┘
  （×ボタンは非表示）
```

### 実装方針

```java
// StackPaneでTextFieldと×ボタンを重ねる
StackPane searchBox = new StackPane();
Button clearBtn = new Button("×");
clearBtn.getStyleClass().add("clear-button");
clearBtn.setVisible(false);
StackPane.setAlignment(clearBtn, Pos.CENTER_RIGHT);
StackPane.setMargin(clearBtn, new Insets(0, 4, 0, 0));
searchBox.getChildren().addAll(keywordField, clearBtn);

// テキスト変更時に×ボタンの表示/非表示を切り替え
keywordField.textProperty().addListener((obs, oldVal, newVal) ->
    clearBtn.setVisible(newVal != null && !newVal.isEmpty()));

// ×ボタンクリック時にクリア＆再検索
clearBtn.setOnAction(e -> {
    keywordField.clear();
    onSearch();
});

// TextFieldの右paddingを確保
keywordField.setStyle("-fx-padding: 4 24 4 4;");
```

### CSS

```css
.clear-button {
    -fx-background-color: transparent;
    -fx-text-fill: #888;
    -fx-font-size: 14px;
    -fx-cursor: hand;
    -fx-padding: 2 6;
}
.clear-button:hover {
    -fx-text-fill: #333;
}
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | `TextField` を `StackPane` で囲み、`Button` を追加 |
| `controller/MainController.java` | ×ボタンの表示制御・クリアハンドラ追加 |
| `styles.css` | `.clear-button` スタイル追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 検索のやり直しが格段に楽になる。数行の追加で完了 |


---

## 24. 検索結果：ページング表示（上限なし）

### 概要

現在の検索上限件数制限を撤廃し、検索結果をページング表示する。1ページあたりの表示件数を切り替え可能にし、UIにページナビゲーションを追加する。

### 現状の問題

- 検索上限件数（デフォルト5000件）を超える結果は切り捨てられる
- 全件を一度にUIに描画するためサムネイル数千件で描画が重くなる
- 「全部で何件あるか」は分かるが、上限以降の結果にアクセスできない

### 仕様

| 項目 | 内容 |
|------|------|
| SQL LIMIT | 撤廃しない（1ページ分のみ取得するために引き続き使用） |
| ページサイズ選択肢 | 200件（デフォルト）/ 500件 / 1000件 |
| ページナビゲーション | 「← 前へ」「次へ →」ボタン + 現在ページ / 全ページ数表示 |
| 初期ページ | 1ページ目 |
| 検索条件変更時 | 1ページ目にリセット |
| ソート変更時 | 1ページ目にリセット |
| 総件数表示 | 従来通りステータスバーに表示（全ページ分の総件数） |
| 設定の保存 | 選択したページサイズは `settings.properties` に保存 |

### UIイメージ

```
┌─────────────────────────────────────────────────────────────────────┐
│  [キーワード___×] [タグ▼] [作者▼] [Star▼] [☑非表示除外]             │
│                                         [検索] [✕ クリア]           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [サムネイル一覧 ... 200件分]                                        │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  ← 前へ   ページ 3 / 25   次へ →   │  表示件数: [200▼]  │ 全4,832件 │
└─────────────────────────────────────────────────────────────────────┘
```

### ページサイズ選択UI

```
表示件数: [200 ▼]
           ┌──────┐
           │  200 │  ← デフォルト
           │  500 │
           │ 1000 │
           └──────┘
```

### 既存コードとの関係

`SearchCondition` は既に `page` / `pageSize` / `getOffset()` を持っている。SQL は `LIMIT ? OFFSET ?` で発行済み。変更が必要なのは主にUI側。

#### 現在のフロー
```
onSearch() → condition.pageSize = AppConfig.getSearchLimit() (5000)
           → condition.page = 0
           → executeSearch() → 全件（最大5000件）をUIに表示
```

#### 変更後のフロー
```
onSearch() → condition.pageSize = selectedPageSize (200/500/1000)
           → condition.page = currentPage (0始まり)
           → executeSearch() → 1ページ分のみUIに表示
           → ページナビゲーションを更新
```

### 処理フロー

```
検索実行
    │
    ▼
SELECT COUNT(*) で総件数取得（既存）
    │
    ▼
SELECT ... LIMIT pageSize OFFSET (page * pageSize) で1ページ分取得（既存）
    │
    ▼
UIに1ページ分のみ表示
    │
    ▼
ページナビゲーション更新:
    totalPages = ceil(totalCount / pageSize)
    「ページ {page+1} / {totalPages}」
    「← 前へ」: page > 0 で有効
    「次へ →」: page < totalPages - 1 で有効
```

### 設定の変更

| 変更前 | 変更後 |
|--------|--------|
| `AppConfig.KEY_SEARCH_LIMIT` = 検索上限件数 | 廃止（または後方互換のために残すが使用しない） |
| — | `AppConfig.KEY_PAGE_SIZE` = 1ページ表示件数（200/500/1000） |

```properties
# settings.properties
page.size=200
```

### ページ移動時のスクロール位置

- ページ移動後はスクロール位置を先頭にリセット
- サムネイル表示の `ScrollPane` の `vvalue` を 0 にセット

### ビューアとの連携

- ビューア起動時は**検索条件で全件再検索（LIMIT なし）**して全件リストを渡す
- メイン画面のページングはUI描画の最適化用であり、ビューアの閲覧範囲を制限しない
- ダブルクリックした画像は全件リスト内でインデックスを特定し、そこから表示開始
- ビューアに渡すリストは**タグ名なし**（軽量版）で構築し、表示時に1件ずつ遅延取得

#### 大量検索結果（10万件級）への対応

| 処理 | 10万件での影響 | 対策 |
|------|--------------|------|
| SQLiteクエリ（10万行SELECT） | 0.5〜2秒 | 許容範囲（非同期で実行） |
| `List<ImageSummary>` メモリ | 50〜100MB | 許容範囲（タグ名を除外して軽量化） |
| タグ名の個別取得（N+1問題） | 10万回SELECT → 致命的に遅い | **ビューア用リストではタグ名を取得しない** |
| ビューアの描画負荷 | 影響なし（常に1〜2枚表示） | — |

#### ビューア向け軽量検索メソッド

```java
/**
 * ビューア向けに全件検索する（タグ名取得なし・軽量版）。
 * メイン画面のページングとは独立して全結果にアクセスする。
 */
public List<ImageSummary> searchForViewer(SearchCondition condition) {
    // LIMIT なしで全件取得（タグ名は空リストのまま）
    // タグ名はビューア表示時に1件ずつ遅延取得する
}
```

#### 安全上限

| 項目 | 内容 |
|------|------|
| デフォルト上限 | 100,000件 |
| 超過時の動作 | 確認ダイアログ「検索結果が○○件あります。ビューアで全件を開きますか？」 |
| 設定 | `settings.properties` の `viewer.max_items`（0 = 無制限） |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | ページナビゲーションバー追加（前へ/次へボタン、ページ表示、ページサイズComboBox） |
| `controller/MainController.java` | ページ状態管理、前へ/次へハンドラ、ページサイズ変更ハンドラ |
| `config/AppConfig.java` | `KEY_PAGE_SIZE` 追加、`KEY_SEARCH_LIMIT` は deprecated 化 |
| `dto/MainViewState.java` | `currentPage` フィールド追加 |

### コードイメージ

```java
/** 現在のページ番号（0始まり）。 */
private int currentPage = 0;

/** ページサイズ ComboBox。 */
@FXML private ComboBox<String> pageSizeCombo;

/** ページ表示ラベル。 */
@FXML private Label pageInfoLabel;

/** 前へボタン。 */
@FXML private Button prevPageButton;

/** 次へボタン。 */
@FXML private Button nextPageButton;

@FXML
private void onPrevPage() {
    if (currentPage > 0) {
        currentPage--;
        executeCurrentSearch();
        thumbnailScrollPane.setVvalue(0); // スクロール先頭に戻す
    }
}

@FXML
private void onNextPage() {
    int totalPages = (int) Math.ceil((double) currentSearchResult.getTotalCount() / getPageSize());
    if (currentPage < totalPages - 1) {
        currentPage++;
        executeCurrentSearch();
        thumbnailScrollPane.setVvalue(0);
    }
}

private void updatePageNavigation() {
    long totalCount = currentSearchResult.getTotalCount();
    int pageSize = getPageSize();
    int totalPages = (int) Math.ceil((double) totalCount / pageSize);

    pageInfoLabel.setText(String.format("ページ %d / %d", currentPage + 1, totalPages));
    prevPageButton.setDisable(currentPage <= 0);
    nextPageButton.setDisable(currentPage >= totalPages - 1);
}

private int getPageSize() {
    String selected = pageSizeCombo.getValue();
    if ("500".equals(selected)) return 500;
    if ("1000".equals(selected)) return 1000;
    return 200; // デフォルト
}

@FXML
private void onPageSizeChanged() {
    currentPage = 0; // ページサイズ変更時は1ページ目に戻る
    onSearch();
}
```

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | 大量画像環境での基本的な操作性。上限5000件の制約を解消し、全件にアクセス可能にする |


---

## 25. 状態復元：メイン画面の検索条件とページを保持・復元

### 概要

アプリ終了時にメイン画面の検索条件（キーワード・タグフィルター・作者フィルター・Starフィルター・非表示除外・ページ番号・表示モード）を保存し、次回起動時に復元する。前回の作業の続きからシームレスに再開できる。

### 保存対象

| 項目 | 保存キー | 例 |
|------|---------|-----|
| キーワード | `state.keyword` | `おねショタ` |
| タグフィルター（選択中タグID） | `state.tag_ids` | `1,5,12` |
| 作者フィルター | `state.author_filter` | `すべての作者` or `顔印象零` |
| Starフィルター | `state.star_filter` | `★2以上` |
| 非表示除外 | `state.exclude_hidden` | `true` |
| 現在ページ | `state.current_page` | `2` |
| ページサイズ | `state.page_size` | `200` |
| 表示モード | `state.display_mode` | `THUMBNAIL` or `LIST` |
| サムネイルサイズ | `state.thumbnail_size` | `中` |
| ウィンドウサイズ | `state.window_width`, `state.window_height` | `1280`, `800` |
| ウィンドウ位置 | `state.window_x`, `state.window_y` | `100`, `50` |

### 保存先

`settings.properties` に追加（既存の設定ファイルに統合）。

```properties
# --- アプリ状態 ---
state.keyword=おねショタ
state.tag_ids=1,5,12
state.author_filter=すべての作者
state.star_filter=すべて
state.exclude_hidden=true
state.current_page=2
state.page_size=200
state.display_mode=THUMBNAIL
state.thumbnail_size=中
state.window_width=1280
state.window_height=800
state.window_x=100
state.window_y=50
```

### 処理フロー

#### 保存タイミング: 即時保存方式

異常終了（クラッシュ・強制終了・OOM）時でも状態が失われないよう、**状態変更のたびに即時保存**する。`Stage.setOnCloseRequest` に依存しない。

| トリガー | 保存内容 |
|---------|---------|
| 検索実行時（`onSearch()`） | キーワード、タグ、作者、Star、非表示除外、ページ=0にリセット |
| ページ切り替え時（`onPrevPage()` / `onNextPage()`） | 現在ページ番号 |
| ページサイズ変更時 | ページサイズ、ページ=0にリセット |
| 表示モード切り替え時 | THUMBNAIL / LIST |
| サムネイルサイズ変更時 | サムネイルサイズ |
| ウィンドウ移動/リサイズ時 | ウィンドウ位置・サイズ（デバウンス: 500ms） |

`settings.properties` への書き込みは 1ms 以下の軽量処理なので、頻繁に呼んでもパフォーマンスに影響しない。

```java
@FXML
private void onSearch() {
    currentPage = 0;
    currentCondition = buildSearchCondition();
    executeSearch(currentCondition);
    saveCurrentState(); // 即時保存
}

@FXML
private void onNextPage() {
    currentPage++;
    executeCurrentSearch();
    saveCurrentState(); // 即時保存
}

@FXML
private void onPrevPage() {
    currentPage--;
    executeCurrentSearch();
    saveCurrentState(); // 即時保存
}
```

#### アプリ起動時

```
MainController.initialize()
    │
    ▼
settings.properties から状態を読み込み
    │
    ▼
各UIコンポーネントに値を反映
    │
    ▼
復元した条件で検索実行
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `config/AppConfig.java` | 状態保存/復元メソッド追加 |
| `controller/MainController.java` | `saveState()` / `restoreState()` メソッド追加、`initialize()` で復元、終了時に保存 |
| `MainApp.java` | `Stage.setOnCloseRequest` で `saveState()` 呼び出し |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | 毎回同じ条件を設定し直す手間をなくす。properties の読み書きのみで完了 |


---

## 26. 状態復元：ビューア画面の状態を保持・復元

### 概要

アプリ終了時に開いているビューア画面の状態を保存し、次回起動時に復元する。各ビューアが「どの条件で開かれたか」を保持し、起動時にその条件で再検索して先頭ページから表示する。

### 設計方針

**ビューアごとの検索条件（または作品ID）を保存し、起動時は再検索して先頭から表示する。**

| ポイント | 内容 |
|---------|------|
| 保存するもの | ビューアごとの「開かれた条件」（作品ID or 検索条件） |
| 復元時の動作 | 条件で再検索 → 結果の先頭ページから表示開始 |
| ページ位置 | 復元しない（先頭から） — DBの最新状態を反映するため |
| 複数ビューア | 全ビューア分の条件をリストで保存、起動時に全部復元 |

### メリット

- 検索結果のスナップショットを保持する必要がない（条件だけ保持）
- 再検索なのでDBの最新状態（追加・削除・タグ変更）が常に反映される
- 作品モード・検索結果モードを統一的に扱える
- 「ページ位置がずれる」問題が発生しない（常に先頭開始）
- 複数ビューアも単純にリストで管理可能

### 保存データ構造

```json
// settings.properties に JSON 配列で保存
viewer.sessions=[
  {
    "mode": "work",
    "workId": 42,
    "pageMode": "SPREAD",
    "bindingDirection": "RIGHT_TO_LEFT",
    "windowX": 100, "windowY": 50, "windowWidth": 1200, "windowHeight": 900
  },
  {
    "mode": "search",
    "condition": {
      "keyword": "おねショタ",
      "tagIds": [1, 5],
      "authorIds": [],
      "minStar": 2,
      "excludeHidden": true
    },
    "pageMode": "SINGLE",
    "bindingDirection": "RIGHT_TO_LEFT",
    "windowX": 200, "windowY": 100, "windowWidth": 1000, "windowHeight": 800
  }
]
```

### 復元フロー

```
アプリ起動
    │
    ▼
settings.properties から viewer.sessions を読み込み
    │ 空配列 → ビューアは開かない
    │ 1件以上 ↓
    ▼
各セッションに対して:
    │
    ├── mode == "work"
    │       → workId で DB から画像リスト取得
    │       → 取得成功 → ビューアウィンドウを起動（先頭ページ）
    │       → 取得失敗（作品削除済み）→ スキップ、ログ警告
    │
    └── mode == "search"
            → condition で再検索
            → 結果 > 0 件 → ビューアウィンドウを起動（先頭ページ）
            → 結果 0 件 → スキップ
    │
    ▼
各ビューアのウィンドウサイズ・位置・表示モード・綴じ方向を復元
```

### ビューアの開閉とセッション管理

```
ビューア起動時:
    → ViewerSession オブジェクトを生成、開いた条件を保持

ビューア閉じる時:
    → そのビューアのセッションをリストから削除

アプリ終了時:
    → 現在開いているビューアのセッション一覧を settings.properties に保存
```

### 保存上限・タイミング

| 項目 | 内容 |
|------|------|
| 最大保存件数 | 上限なし（開いているビューア全件を保存） |
| 保存タイミング | **即時保存**（ビューアを開いた時・閉じた時に即座に書き込み。異常終了に耐える） |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `dto/ViewerSession.java`（新規） | ビューアセッション情報DTO（mode, workId, condition, 表示設定） |
| `config/AppConfig.java` | `viewer.sessions` の JSON 読み書き |
| `controller/ViewerController.java` | 起動時にセッション情報を受け取る、閉じる時にセッション削除通知 |
| `controller/MainController.java` | 起動時にセッション一覧から各ビューアを復元 |
| `MainApp.java` | 終了時に全ビューアのセッションを保存 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 中 | あると便利だが、#25（メイン画面復元）ほど必須ではない。条件ベースの再検索方式で実装はシンプル |


---

## 27. 保存済み検索：最近の検索条件履歴（5件）を先頭表示

### 概要

保存済み検索のプルダウン（ComboBox）の先頭に、最近実行した検索条件の履歴（最大5件）を自動的に表示する。明示的に「保存」しなくても、直近の検索条件に素早くアクセスできる。

### ユースケース

- 「さっきの検索条件に戻りたい」を即座に実現
- 保存するほどではない一時的な検索条件を再利用したい
- 検索→ビューア→検索に戻ったとき、直前の条件が失われない安心感

### UIイメージ

```
保存済み検索: [▼]
┌──────────────────────────────────┐
│  📋 最近の検索:                   │
│     おねショタ ★2以上             │  ← 最新
│     R-18 (作者: 顔印象零)         │
│     手コキ                        │
│     ★3以上 非表示含む             │
│     (条件なし)                    │
│  ─────────────────────────────── │
│  💾 保存済み:                     │
│     お気に入り作品                 │
│     未分類チェック用               │
│     高評価のみ                    │
└──────────────────────────────────┘
```

### 仕様

| 項目 | 内容 |
|------|------|
| 履歴件数 | 最大5件 |
| 記録タイミング | 検索実行時（`onSearch()` のたびに記録） |
| 重複排除 | 同一条件（キーワード+タグ+作者+Star+非表示の組み合わせ）は1エントリに集約（最新位置に移動） |
| 表示ラベル | 検索条件を要約した文字列（「キーワード Star Starフィルター (作者: 名前)」形式） |
| 選択時の動作 | 保存済み検索と同様、条件をUIに反映して再検索 |
| 永続化 | `settings.properties` に JSON 配列として保存 |
| 保存済み検索との区別 | セパレーターで「最近の検索」と「保存済み」を視覚的に分離 |

### 検索条件の要約ラベル生成

```java
private String buildSearchSummary(SearchCondition condition) {
    List<String> parts = new ArrayList<>();
    if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
        parts.add(condition.getKeyword());
    }
    if (condition.getMinStar() > 0) {
        parts.add("★" + condition.getMinStar() + "以上");
    }
    if (!condition.getTagIds().isEmpty()) {
        parts.add(condition.getTagIds().size() + "タグ");
    }
    if (!condition.isExcludeHidden()) {
        parts.add("非表示含む");
    }
    return parts.isEmpty() ? "(条件なし)" : String.join(" ", parts);
}
```

### 履歴の永続化

```properties
# settings.properties
search.history=[{"keyword":"おねショタ","minStar":2,"tagIds":[],"excludeHidden":true},{"keyword":"R-18","minStar":0,"tagIds":[],"authorIds":[3],"excludeHidden":true},...]
```

### ComboBox の構成

```java
// ComboBox のアイテム構成
List<String> items = new ArrayList<>();

// 最近の検索（先頭）
items.add("── 最近の検索 ──");  // セパレーター的な項目（選択不可）
for (SearchHistory h : recentSearches) {
    items.add("📋 " + h.getSummary());
}

// 保存済み検索
items.add("── 保存済み ──");  // セパレーター
for (SavedSearch ss : savedSearches) {
    items.add("💾 " + ss.getName());
}

savedSearchCombo.setItems(FXCollections.observableArrayList(items));
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `service/SearchHistoryService.java`（新規） | 検索履歴の管理（追加・取得・永続化） |
| `config/AppConfig.java` | `search.history` の読み書き |
| `controller/MainController.java` | `onSearch()` 時に履歴追加、ComboBox 構成変更 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | 「さっきの検索に戻りたい」は頻出操作。明示的な保存なしでアクセスできるのが重要 |


---

## 28. タグ追加時のメイン画面即時反映

### 概要

任意の経路で新規タグが追加された際に、メイン画面のタグツリーとタグフィルタープルダウンに即時反映する。特にビューア画面からタグを追加した場合にメイン画面が古いタグリストのままにならないようにする。

### 現状

| タグ追加の経路 | メイン画面への反映 | 状態 |
|---------------|-------------------|------|
| タグ編集ダイアログ（メイン画面から起動） | ✅ ダイアログ close 後に `loadTagTree()` + `loadTagFilter()` | 対応済み |
| インポート時（JSON自動タグ付け） | ✅ インポート完了後に再読み込み | 対応済み |
| ビューア画面からタグ追加 | ❌ メイン画面に反映されない | **未対応** |
| コンテキストメニューからの即時タグ追加（#19） | ❌ 未実装 | #19 実装時に対応 |

### 仕様

| 項目 | 内容 |
|------|------|
| 反映対象 | タグツリー（左パネル）、タグフィルター（MenuButton / ComboBox） |
| 反映タイミング | 新規タグがDBに追加された直後 |
| 反映方式 | `loadTagTree()` + `loadTagFilter()` の再実行（全件再取得） |

### 対応方針

#### ビューア→メイン画面の通知

ビューア画面でタグを追加/作成した際にメイン画面のタグリストを更新する。以下のいずれかの方式:

**方式A: コールバック方式（推奨）**

ビューア起動時に `MainController` への参照またはコールバックを渡し、タグ変更時に呼び出す。

```java
// MainController でビューア起動時にコールバックを設定
Runnable onTagChanged = () -> {
    Platform.runLater(() -> {
        loadTagTree();
        loadTagFilter();
    });
};
ViewerController.openNewWindow(images, index, onTagChanged);
```

```java
// ViewerController でタグ追加後にコールバック実行
private void onAddTagCompleted() {
    if (onTagChangedCallback != null) {
        onTagChangedCallback.run();
    }
}
```

**方式B: ウィンドウフォーカス方式**

メイン画面がフォーカスを得たとき（ビューアからメイン画面に戻ったとき）にタグリストを再読み込みする。

```java
// MainApp またはMainController でStageのフォーカスを監視
stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
    if (isFocused) {
        loadTagTree();
        loadTagFilter();
    }
});
```

方式Bはシンプルだが、タグ変更がなくても毎回再読み込みする無駄がある。方式Aは必要な場合のみ再読み込みで効率的。

**→ 方式A を推奨。** 将来の #19（コンテキストメニューからの即時タグ追加）でも同じコールバック機構を使い回せる。

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | タグ変更コールバック（`Runnable`）を受け取るフィールド追加、タグ操作後にコールバック実行 |
| `controller/MainController.java` | ビューア起動時にコールバックを渡す |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | ビューアでタグ追加→メイン画面に戻ったらリストにない、は混乱を招く。コールバック1つで解決 |


---

## 29. メイン画面：タグリストと検索リストの幅をドラッグ調整可能

### 概要

メイン画面の左側パネル（タグツリー）と中央コンテンツ（検索結果一覧）の幅を、境界線のドラッグで自由に調整可能にする。調整した幅は状態保持対象とし、次回起動時に復元する。

### 現状

- `BorderPane` の `left` に固定幅（`minWidth=180`, `maxWidth=250`, `prefWidth=200`）でタグツリーを配置
- ユーザーが幅を変更する手段がない

### 実装方式: SplitPane

JavaFX の `SplitPane` を使えば、標準機能でドラッグリサイズが実現できる。外部ライブラリ不要。

```xml
<!-- 変更前: BorderPane の left + center -->
<BorderPane>
    <left>
        <VBox>...</VBox>  <!-- タグツリー -->
    </left>
    <center>
        <StackPane>...</StackPane>  <!-- 検索結果 -->
    </center>
</BorderPane>

<!-- 変更後: SplitPane で左右を分割 -->
<BorderPane>
    <center>
        <SplitPane fx:id="mainSplitPane" dividerPositions="0.15">
            <!-- 左: タグツリー -->
            <VBox>...</VBox>
            <!-- 右: 検索結果 -->
            <StackPane>...</StackPane>
        </SplitPane>
    </center>
</BorderPane>
```

### 仕様

| 項目 | 内容 |
|------|------|
| ドラッグ境界 | タグツリーと検索結果の間（SplitPane のデバイダー） |
| 初期位置 | 0.15（左パネルが全体幅の15%） |
| 最小幅（左パネル） | 120px |
| 最大幅（左パネル） | 400px |
| 状態保持 | デバイダー位置を `settings.properties` に保存・復元 |
| 保存タイミング | 即時保存（ドラッグ終了時、デバウンス500ms） |

### 状態保持

```properties
# settings.properties
state.split_divider=0.15
```

```java
// 起動時に復元
double dividerPos = appConfig.getDouble("state.split_divider", 0.15);
mainSplitPane.setDividerPositions(dividerPos);

// ドラッグ変更時に即時保存（デバウンス付き）
mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
    debounce(() -> appConfig.set("state.split_divider", newVal.doubleValue()), 500);
});
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | `BorderPane` の `left` + `center` → `SplitPane` に変更 |
| `controller/MainController.java` | `mainSplitPane` フィールド追加、デバイダー位置の保存/復元 |
| `config/AppConfig.java` | `state.split_divider` の読み書き（#25 の保存対象に追加） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | SplitPane への置き換えのみ。タグ数が多い環境ではパネル幅の調整は必須 |


---

## 30. UI：ウィンドウ縮小時のレスポンシブ対応（ツールチップ・オーバーフロー）

### 概要

ウィンドウサイズを小さくした際に、ツールバーの入力項目やボタンが文字潰れ・切れを起こす問題に対応する。マウスオーバーでツールチップ表示、ボタンとStar UIはサイズ固定、収まらない項目はオーバーフローメニュー（>>）に格納する。

### 現状の問題

- ウィンドウ幅を狭めると ComboBox やボタンのラベルが `...` で省略される
- 何の項目か分からなくなる
- 重要なボタン（検索、インポート等）が見えなくなる

### 対応内容

#### 1. ツールチップ表示

全ての入力項目・ボタンにマウスオーバーでツールチップを表示する。

| UI要素 | ツールチップ内容 |
|--------|----------------|
| キーワードフィールド | 「キーワード検索（ファイル名・タグ・作者で全文検索）」 |
| タグフィルター | 「タグで絞り込み」+ 現在の選択状態 |
| 作者フィルター | 「作者で絞り込み」+ 現在の選択値 |
| Starフィルター | 「Star評価で絞り込み」 |
| 非表示除外トグル | 「非表示ファイルを検索結果から除外する」 |
| 検索ボタン | 「検索を実行 (Enter)」 |
| クリアボタン | 「検索条件をすべてクリア」 |
| インポートボタン | 「フォルダからファイルをインポート」 |
| PDF出力ボタン | 「検索結果をPDFに出力」 |
| 表示件数 | 「1ページあたりの表示件数」 |

```java
// 例
keywordField.setTooltip(new Tooltip("キーワード検索（ファイル名・タグ・作者で全文検索）"));
searchButton.setTooltip(new Tooltip("検索を実行 (Enter)"));
tagFilterButton.setTooltip(new Tooltip("タグで絞り込み"));
```

#### 2. ボタン・Star UIのサイズ固定

操作に必須のボタンとビューア画面のStar設定UIは `minWidth` を固定し、縮小されないようにする。

| UI要素 | 固定方式 |
|--------|---------|
| 検索ボタン | `minWidth="60"` |
| クリアボタン | `minWidth="60"` |
| インポートボタン | `minWidth="90"` |
| PDF出力ボタン | `minWidth="80"` |
| ページ前へ/次へ | `minWidth="50"` |
| Star UI（ビューア情報バー） | `minWidth="120"`、★5個分の幅を固定確保 |

```xml
<Button fx:id="searchButton" text="検索" minWidth="60" />
<Button fx:id="importButton" text="インポート" minWidth="90" />
```

#### 3. ToolBar オーバーフロー（>> メニュー）

JavaFX の `ToolBar` は標準でオーバーフロー機能を持っている。ウィンドウ幅が足りないと自動的に `>>` ボタンが表示され、収まらない項目がドロップダウンに格納される。

現在の検索バーが `HBox` で構成されている場合、`ToolBar` に置き換えることで自動的にオーバーフローが有効になる。

```xml
<!-- 変更前: HBox -->
<HBox spacing="8" alignment="CENTER_LEFT">
    <TextField fx:id="keywordField" ... />
    <ComboBox fx:id="starFilterCombo" ... />
    <Button fx:id="searchButton" ... />
    ...
</HBox>

<!-- 変更後: ToolBar（オーバーフロー対応） -->
<ToolBar fx:id="searchToolBar">
    <TextField fx:id="keywordField" ... />
    <ComboBox fx:id="starFilterCombo" ... />
    <Button fx:id="searchButton" ... />
    ...
</ToolBar>
```

### ToolBar オーバーフローの動作

```
ウィンドウ幅が十分:
┌────────────────────────────────────────────────────────────────┐
│  [キーワード___] [タグ▼] [作者▼] [Star▼] [☑非表示] [検索] [✕]  │
└────────────────────────────────────────────────────────────────┘

ウィンドウ幅が狭い:
┌──────────────────────────────────────────────┐
│  [キーワード___] [タグ▼] [作者▼] [検索] [>>]  │
└──────────────────────────────────────────────┘
                                          │
                                     クリック
                                          │
                                   ┌──────────────┐
                                   │ [Star▼]      │
                                   │ [☑非表示除外] │
                                   │ [✕ クリア]    │
                                   └──────────────┘
```

### オーバーフロー時の優先度（左から優先表示）

| 優先度 | 項目 | 理由 |
|--------|------|------|
| 1（最優先） | キーワードフィールド | 最も使用頻度が高い |
| 2 | タグフィルター | 主要フィルター |
| 3 | 作者フィルター | 主要フィルター |
| 4 | 検索ボタン | 必須操作（固定サイズ） |
| 5 | Starフィルター | 補助フィルター |
| 6 | 非表示除外トグル | 補助フィルター |
| 7 | クリアボタン | 補助操作 |

`ToolBar` のオーバーフローは子要素の追加順（左から右）で表示され、右側から順に >> に格納される。上記の優先度順に並べれば自然に動作する。

### ビューア画面の Star UI

ビューア情報バーの Star 設定 UI は固定幅で確保:

```
┌──────────────────────────────────────────────────┐
│  タグ: [...]   作者: [...]   Star: [★★★☆☆]      │
│                              ↑ minWidth=120 固定  │
└──────────────────────────────────────────────────┘
```

- Star の ★5個分（+ ラベル）は常に表示
- ウィンドウが狭い場合、タグ表示やファイルパス表示が省略される（Star は固定）

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `main.fxml` | 検索バーの `HBox` → `ToolBar` に変更、各ボタンに `minWidth` 設定 |
| `controller/MainController.java` | 全UI要素に `setTooltip()` 追加 |
| `viewer.fxml` | Star UI に `minWidth` 設定 |
| `styles.css` | ToolBar のオーバーフローボタンのスタイル調整 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | 小さいディスプレイやウィンドウ分割利用時に必須のUX改善。ToolBar 置き換えで大部分は自動対応 |


---

## 31. ビューア画面：ページ切り替え時のちらつき防止（先読み＋フェード）

### 概要

ビューアでページを切り替えた際に、画像が一瞬空白→表示される「ちらつき」を防止する。前後ページの先読み（プリフェッチ）と、表示切り替え時のバッファリングで滑らかな表示を実現する。

### 現状の問題

ページ切り替え時に以下が発生する:
1. `ImageView.setImage(null)` で前の画像がクリアされる → **空白が見える**
2. `new Image("file:...", true)` で非同期読み込み開始
3. 読み込み完了後に ImageView に設定 → 画像が表示される

ステップ1→3の間（数十ms〜数百ms）に空白フレームが描画され、ちらつきとして知覚される。

### 対策: 3つの組み合わせ

#### 対策1: 前後ページの先読み（プリフェッチ）

次に表示される可能性の高いページを事前にメモリにロードしておく。

```java
/** 先読みキャッシュ（キー: ページインデックス、値: ロード済みImage） */
private final Map<Integer, Image> prefetchCache = new LinkedHashMap<>(32, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
        return size() > MAX_PREFETCH_SIZE; // 最大21枚保持
    }
};

/** 先読み対象: 現在ページ ±10 */
private static final int PREFETCH_RANGE = 10;

private void prefetchPages(int currentIndex) {
    for (int i = currentIndex - PREFETCH_RANGE; i <= currentIndex + PREFETCH_RANGE; i++) {
        if (i >= 0 && i < images.size() && !prefetchCache.containsKey(i)) {
            String path = images.get(i).getFilePath();
            // バックグラウンドで非同期読み込み
            Image img = new Image("file:" + path, true);
            prefetchCache.put(i, img);
        }
    }
}
```

#### 対策2: ダブルバッファリング（前の画像を保持して切り替え）

新しい画像のロードが完了するまで、前の画像を表示し続ける。`setImage(null)` を呼ばない。

```java
private void displayPage(int index) {
    Image nextImage = prefetchCache.get(index);

    if (nextImage != null && nextImage.getProgress() >= 1.0) {
        // キャッシュにあり、ロード完了済み → 即座に切り替え（ちらつきなし）
        imageView.setImage(nextImage);
    } else {
        // 未ロードまたはロード中 → 前の画像を保持したまま待機
        Image loading = (nextImage != null) ? nextImage : new Image("file:" + path, true);
        loading.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                Platform.runLater(() -> imageView.setImage(loading));
            }
        });
        // ロードエラー時のフォールバック
        loading.errorProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> showErrorPlaceholder());
            }
        });
    }
}
```

**ポイント: `imageView.setImage(null)` を呼ばない。** 前の画像を表示したまま、新しい画像のロード完了を待って一気に差し替える。

#### 対策3: 最小表示ウェイト（オプション）

高速連続ページ送り時に各ページが一瞬だけ表示されてちらつくのを防ぐ。ページ送り入力を受けてから実際の表示更新まで短いディレイ（50〜100ms）を設け、連続入力を集約する。

```java
/** ページ送りのデバウンスタイマー。 */
private final PauseTransition pageDebounce = new PauseTransition(Duration.millis(50));

private void requestPageChange(int newIndex) {
    pendingIndex = newIndex;
    pageDebounce.setOnFinished(e -> displayPage(pendingIndex));
    pageDebounce.playFromStart(); // 50ms以内の連続入力は最後の1回だけ反映
}
```

これにより:
- マウススクロールの高速連続入力で中間ページが一瞬表示されるのを防ぐ
- 最終的に止まったページのみ表示 → ちらつき感がなくなる

### 組み合わせた動作フロー

```
ページ送り操作
    │
    ▼
デバウンス（50ms待機、連続入力を集約）
    │ 50ms以内に次の入力 → タイマーリセット
    │ 50ms経過 → 確定
    ▼
先読みキャッシュに画像があるか？
    │ Yes + ロード完了 → 即座に ImageView を差し替え（0ms、ちらつきなし）
    │ Yes + ロード中  → 前の画像を保持したまま完了を待つ → 完了時に差し替え
    │ No  → 新規読み込み開始 → 前の画像を保持 → 完了時に差し替え
    ▼
先読み更新: 新しい currentIndex ±2 をプリフェッチ
```

### 先読みのタイミング

| イベント | 先読み対象 |
|---------|-----------|
| ビューア起動時 | 初期ページ ±10（優先度: 現在→±1→±2→...→±10 の順で逐次ロード） |
| ページ切り替え確定後 | 新ページ ±10 |
| 作品切り替え時 | キャッシュクリア → 新作品の先頭 ±10 |

### メモリ管理

| 項目 | 内容 |
|------|------|
| キャッシュ保持枚数 | 最大21枚（±10、LRU方式で古いものから破棄） |
| メモリ消費 | 1枚あたり約8MB（表示サイズにスケーリング）× 21枚 = 約170MB |
| スケーリング読み込み | 表示エリアのサイズに合わせてデコード（元画像が高解像度でもメモリを抑制） |

```java
// スケーリング読み込み（メモリ節約 — 表示サイズでデコード）
double targetWidth = imageAreaPane.getWidth();
double targetHeight = imageAreaPane.getHeight();
Image img = new Image("file:" + path, targetWidth, targetHeight, true, true, true);
// 引数: url, requestedWidth, requestedHeight, preserveRatio, smooth, backgroundLoading
```

> **注意:** スケーリング読み込みにより、8000×6000の画像でも表示サイズ（例: 1920×1080）相当のメモリしか消費しない。±10でも約170MBで収まる。

### 設定項目（将来拡張）

| 設定 | デフォルト | 説明 |
|------|-----------|------|
| `viewer.prefetch_range` | 10 | 先読みページ数（±N） |
| `viewer.page_debounce_ms` | 50 | ページ送りデバウンス（ms） |
| `viewer.max_cache_pages` | 21 | 先読みキャッシュ最大枚数（PREFETCH_RANGE × 2 + 1） |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | 先読みキャッシュ追加、ダブルバッファリング表示ロジック、デバウンスタイマー |
| `dto/ViewerState.java` | `prefetchCache` フィールド追加（またはViewerControllerに直接保持） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 中 | 高 | ビューア使用時に毎回知覚される品質問題。先読み+バッファリングで根本解決 |


---

## 32. ビューア画面：見開き/単ページの画像サイズ自動判別

### 概要

ビューア起動時に画像のアスペクト比から見開き/単ページ表示を自動判別する。縦長画像が多い場合は見開き表示、横長画像が多い場合は単ページ表示をデフォルトにする。

### 判定ロジック

| 画像の形状 | 条件 | 適切な表示モード |
|-----------|------|----------------|
| 縦長 | `height > width` | 見開き表示（漫画の1ページ相当） |
| 横長 | `width >= height` | 単ページ表示（見開きスキャン済み or イラスト） |
| 正方形 | `width == height` | 単ページ表示 |

### 自動判別の方式

ビューア起動時に、画像リストの**先頭数ページ（最大5ページ）**のアスペクト比を確認し、多数決で決定する。

```java
private PageMode detectPageMode(List<ImageSummary> images) {
    int portraitCount = 0;  // 縦長
    int landscapeCount = 0; // 横長

    int sampleSize = Math.min(5, images.size());
    for (int i = 0; i < sampleSize; i++) {
        ImageSummary img = images.get(i);
        if (img.getWidth() != null && img.getHeight() != null) {
            if (img.getHeight() > img.getWidth()) {
                portraitCount++;
            } else {
                landscapeCount++;
            }
        }
    }

    // 縦長が過半数 → 見開き表示
    return (portraitCount > landscapeCount) ? PageMode.SPREAD : PageMode.SINGLE;
}
```

### 仕様

| 項目 | 内容 |
|------|------|
| 判定タイミング | ビューア起動時（画像リストを受け取った直後） |
| サンプル数 | 先頭5ページ（5ページ未満の場合は全ページ） |
| 判定基準 | 縦長が過半数 → 見開き、それ以外 → 単ページ |
| 解像度情報がない場合 | DBの `width`/`height` が NULL → 判定対象から除外 |
| 全て NULL の場合 | 単ページ表示（フォールバック） |
| ユーザーの手動切替 | 自動判別後でも手動で切替可能（手動設定が優先される） |
| 設定で無効化 | 「起動時に自動判別する」チェックボックスで ON/OFF 可能 |

### 手動切替との優先度

```
ビューア起動
    │
    ▼
自動判別が有効か？（設定確認）
    │ No → 前回の表示モードを使用（#26 の状態復元）
    │ Yes ↓
    ▼
先頭5ページのアスペクト比で判定
    │
    ▼
判定結果で表示モードを設定
    │
    ▼
ユーザーが手動で切替 → 以降はユーザー設定を優先（そのセッション内）
```

### 混在ケースの考慮

作品内に縦長と横長が混在する場合（表紙=横長、本文=縦長など）:

| ケース | 判定 | 補足 |
|--------|------|------|
| 表紙（横長）1枚 + 本文（縦長）多数 | 見開き | 先頭5枚中で縦長が過半数 |
| イラスト集（横長多数） | 単ページ | 横長が過半数 |
| 全て正方形 | 単ページ | フォールバック |

### 将来拡張: ページごとの動的切替

先頭の判定だけでなく、ページ送り中に現在ページのアスペクト比を見て動的に切り替える方式も将来的に検討可能:
- 縦長ページ → 見開きとして左右に配置
- 横長ページ → 単ページとして全幅表示

ただし表示が頻繁に切り替わるとUXが悪いため、初期実装では起動時判定のみとする。

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `detectPageMode()` メソッド追加、`initialize()` で呼び出し |
| `dto/ViewerState.java` | `autoDetectedMode` フィールド追加（手動切替との区別用） |
| `config/AppConfig.java` | `viewer.auto_detect_page_mode` 設定追加（デフォルト: true） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | DB上の width/height を参照するだけで判定可能。毎回手動切替する手間をなくす |


---

## 33. ビューア画面：見開き時のマウスオーバー側情報表示

### 概要

ビューア画面の見開き表示時に、情報バー（トップ/ボトム）に表示する内容を、マウスカーソルがある側（左/右）の画像情報に動的に切り替える。マウスオーバーしているページのファイル名・タグ・Star・ページ番号などを表示する。

### 現状の問題

- 見開き表示では左右に別々のページが表示されるが、情報バーはどちらか一方の情報しか表示していない
- ユーザーが「今見ているページ」の情報を確認したいとき、どちら側の情報か分からない

### 仕様

| 項目 | 内容 |
|------|------|
| 検知方式 | `imageAreaPane` の `setOnMouseMoved` でマウスX座標を監視 |
| 判定 | X座標がペイン幅の50%より左 → 左ページの情報表示、右 → 右ページの情報表示 |
| 更新タイミング | マウスが左右の境界を越えたときのみ更新（毎フレーム更新はしない） |
| 情報バーの表示内容 | ファイル名、ページ番号、Star、タグ、ファイルパス |
| マウスが画面外 | 最後にオーバーしていた側の情報を維持 |
| 単ページ表示時 | 無条件で現在ページの情報を表示（従来通り） |
| 視覚的フィードバック | 情報バーの左端にどちら側か示すインジケーター（例: 「◀ 左」「右 ▶」） |

### UIイメージ

```
見開き表示（マウスが左側にある場合）:

┌──────────────────────────────────────────────────────────────┐
│  ◀ 左ページ | page 3/24 | ★★★ | おねショタ, R-18             │  ← 情報バー（トップ）
├────────────────────────────┬─────────────────────────────────┤
│                            │                                 │
│      左ページ画像           │      右ページ画像                │
│     （マウスここ）          │                                 │
│                            │                                 │
├────────────────────────────┴─────────────────────────────────┤
│  143716458_p0002-タイトル.jpg  |  作者: 顔印象零               │  ← 情報バー（ボトム）
└──────────────────────────────────────────────────────────────┘

マウスを右側に移動すると:

┌──────────────────────────────────────────────────────────────┐
│  右ページ ▶ | page 4/24 | ★★★ | おねショタ, R-18             │  ← 右ページの情報に切替
├────────────────────────────┬─────────────────────────────────┤
│                            │                                 │
│      左ページ画像           │      右ページ画像                │
│                            │    （マウスここ）                 │
│                            │                                 │
├────────────────────────────┴─────────────────────────────────┤
│  143716458_p0003-タイトル.jpg  |  作者: 顔印象零               │  ← 右ページの情報に切替
└──────────────────────────────────────────────────────────────┘
```

### 実装方針

```java
/** 現在情報表示中の側（LEFT / RIGHT）。 */
private Side activeSide = Side.LEFT;

private void setupMouseTracking() {
    imageAreaPane.setOnMouseMoved(event -> {
        if (viewerState.getPageMode() != PageMode.SPREAD) return;

        double midX = imageAreaPane.getWidth() / 2.0;
        Side newSide = (event.getX() < midX) ? Side.LEFT : Side.RIGHT;

        if (newSide != activeSide) {
            activeSide = newSide;
            updateInfoBar(activeSide);
        }
    });
}

private void updateInfoBar(Side side) {
    ImageSummary target = (side == Side.LEFT)
            ? viewerState.getLeftImage()
            : viewerState.getRightImage();

    if (target == null) return;

    // トップ情報バー更新
    sideIndicatorLabel.setText(side == Side.LEFT ? "◀ 左ページ" : "右ページ ▶");
    pageNumberLabel.setText(String.format("page %d/%d", target.getPageNumber(), totalPages));
    starLabel.setText("★".repeat(target.getStar()));
    tagsLabel.setText(String.join(", ", target.getTagNames()));

    // ボトム情報バー更新
    fileNameLabel.setText(target.getFileName());
    authorLabel.setText(target.getAuthorName() != null ? target.getAuthorName() : "");
}
```

### コンテキストメニューとの連携

見開き表示時のコンテキストメニュー操作（タグ追加、Star設定等）の対象画像も、マウス位置に基づいて左右を判定する。

| 操作 | 対象 |
|------|------|
| 右クリック（左側）→ コンテキストメニュー | 左ページに対して操作 |
| 右クリック（右側）→ コンテキストメニュー | 右ページに対して操作 |

これは既存のコンテキストメニュー（#3）の実装とも整合する。

### 注意点

| ケース | 動作 |
|--------|------|
| 見開きの片方が空（最終ページが奇数） | 空側にマウスがある場合はもう一方の情報を維持 |
| 右綴じ/左綴じの考慮 | 表示位置（物理的な左右）で判定。綴じ方向でページ番号の割り当てが変わるが、情報バーは常にマウス位置の画像情報を表示 |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `setupMouseTracking()` メソッド追加、`updateInfoBar(Side)` メソッド追加 |
| `dto/ViewerState.java` | `getLeftImage()` / `getRightImage()` メソッド追加（見開き時の左右画像参照） |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 中 | マウス位置の判定と情報バー更新のみ。見開き表示の実用性が大きく向上する |


---

## 34. ビューア画面：メニュー表示/非表示切り替え（デフォルト非表示）

### 概要

ビューア画面のトップメニュー（情報バー）・ボトムメニュー（ステータスバー）をデフォルトで非表示にし、画像表示エリアを最大化する。表示/非表示はコンテキストメニューで切り替え可能とする。

### ユースケース

- 画像をできるだけ大きく表示したい（特にフルスクリーン時）
- メニューバーが常時見えているとビューアの没入感を損なう
- 情報を見たいときだけメニューを出せれば十分

### 仕様

| 項目 | 内容 |
|------|------|
| デフォルト状態 | メニュー非表示（画像表示エリアのみ） |
| 切替操作 | コンテキストメニュー「情報バーを表示」/ 「情報バーを非表示」 |
| 切替対象 | トップバー（ページ番号・タグ等）とボトムバー（ファイル名・パス等）を一括で切替 |
| 状態保持 | 表示/非表示の設定を `settings.properties` に即時保存、次回起動時に復元 |
| キーボードショートカット | `I` キーでトグル（Information の頭文字） |

### UIイメージ

#### メニュー非表示（デフォルト）

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│                                                              │
│                       画像表示エリア                           │
│                    （最大化表示）                              │
│                                                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

#### メニュー表示

```
┌──────────────────────────────────────────────────────────────┐
│  page 3/24 | ★★★ | おねショタ, R-18 | 作者: 顔印象零         │  ← トップバー
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                       画像表示エリア                           │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  143716458_p0002-タイトル.jpg  |  1076x1522  |  3.2MB        │  ← ボトムバー
└──────────────────────────────────────────────────────────────┘
```

### コンテキストメニュー項目

既存のコンテキストメニュー（#3）に追加:

```
右クリック →
    ...
    ─────────────────
    ☑ 情報バーを表示        ← チェック付きメニュー項目
    ─────────────────
    ...
```

| メニュー項目 | 動作 |
|-------------|------|
| 「情報バーを表示」（チェックON） | トップバー・ボトムバーを表示 |
| 「情報バーを表示」（チェックOFF） | トップバー・ボトムバーを非表示 |

### 実装方針

```java
/** 情報バーの表示フラグ。 */
private boolean infoBarVisible = false; // デフォルト非表示

/** 情報バーの表示/非表示を切り替える。 */
private void toggleInfoBar() {
    infoBarVisible = !infoBarVisible;
    topInfoBar.setVisible(infoBarVisible);
    topInfoBar.setManaged(infoBarVisible);  // レイアウトからも除外
    bottomInfoBar.setVisible(infoBarVisible);
    bottomInfoBar.setManaged(infoBarVisible);
    // 即時保存
    appConfig.set("viewer.info_bar_visible", infoBarVisible);
}
```

`setManaged(false)` を使うことで、非表示時にバーのスペースが残らず画像表示エリアが自動的に拡大される。

### キーボードショートカット

```java
scene.setOnKeyPressed(event -> {
    switch (event.getCode()) {
        case I -> toggleInfoBar();
        // 既存のショートカット...
    }
});
```

### コンテキストメニューの CheckMenuItem

```java
CheckMenuItem showInfoBar = new CheckMenuItem("情報バーを表示");
showInfoBar.setSelected(infoBarVisible);
showInfoBar.setOnAction(e -> toggleInfoBar());

// メニュー表示時に状態を同期
contextMenu.setOnShowing(e -> showInfoBar.setSelected(infoBarVisible));
```

### 状態保持

```properties
# settings.properties
viewer.info_bar_visible=false
```

ビューアセッション（#26）の保存対象にも追加する。

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `toggleInfoBar()` メソッド追加、`I` キーショートカット追加、コンテキストメニューに `CheckMenuItem` 追加 |
| `viewer.fxml` | トップバー・ボトムバーに `fx:id` 設定（`managed` 属性のバインド用） |
| `config/AppConfig.java` | `viewer.info_bar_visible` 設定追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | `setVisible` + `setManaged` の切替のみ。画像を最大限大きく見せるのはビューアの基本UX |


---

## 35. ビューア画面：ページ送りボタンのラベルを矢印のみに変更

### 概要

ビューア画面のページ送りボタン（前へ/次へ）の表示テキストを、現在の「← 前」「→ 次」から矢印記号のみ（「←」「→」）に変更する。

### 背景

「前へ」「次へ」というテキスト表記はどちらの方向にページが進むのか直感的に分かりにくい。見開き表示では綴じ方向（右綴じ/左綴じ）によって「次」の意味する方向が変わるため、テキストよりも矢印記号のほうが曖昧さが少ない。

### 仕様

| 項目 | 変更前 | 変更後 |
|------|--------|--------|
| 前ページボタン | `← 前` | `←` |
| 次ページボタン | `→ 次` | `→` |

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `src/main/resources/fxml/viewer.fxml` | `prevButton` の `text` を `←` に変更、`nextButton` の `text` を `→` に変更 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 極小 | 低 | FXML のテキスト属性を2箇所変更するのみ |


---

## 36. ビューア画面：コンテキストメニューからタグ追加（最近使ったタグ履歴）

### 概要

ビューア画面のコンテキストメニューに「タグを追加」サブメニューを追加し、メイン画面と同様に Star 付きタグ・最近使用したタグ履歴から選択するだけで画像にタグを付与/削除できるようにする。タグ編集ダイアログを開かずにワンクリックでタグ操作が可能になる。

### 現状

| 画面 | タグ追加方法 | 状態 |
|------|-------------|------|
| メイン画面 | コンテキストメニュー「タグを追加」サブメニュー（Star付き + 最近使用 + その他...） | ✅ 実装済み (#19) |
| ビューア画面 | コンテキストメニュー「タグを編集...」→ 別ダイアログが開く | 旧来の方法のみ |

### ユースケース

- ビューアで作品を閲覧しながら「お気に入り」「あとで読む」などのタグを素早く付けたい
- タグ編集ダイアログを開く手間（右クリック→タグを編集→入力→追加→閉じる）を省略したい
- メイン画面と同じ操作感でタグ付けしたい

### メニュー構造

メイン画面のコンテキストメニュー (#19) と同一の構造:

```
右クリック →
    タグを追加 →  ★★★ おねショタ
                  ★★  手コキ
                  ★   R-18
                  ─────────────
                  足コキ
                  DL販売
                  乳首責め
                  ─────────────
                  その他... (タグ編集ダイアログを開く)
    タグを編集...
    非表示にする
    ...
```

### 仕様

| 項目 | 内容 |
|------|------|
| 上部セクション | Star評価の高いタグ上位5件（★1以上、Star DESC, name ASC） |
| 中部セクション | 最近使用したタグ（時系列降順、最大10件、Star付きタグと重複除外） |
| 下部 | 「その他...」でタグ編集ダイアログを開く（既存の `onAddTag()` を呼び出し） |
| 既に付与済みのタグ | 先頭に「✓」マークを付けて表示。クリックでタグを削除 |
| 未付与のタグ | クリックで即座に付与 |
| 履歴の更新 | タグを付与した時点で `TagHistory` に追加し `settings.properties` に即時保存 |
| 見開き表示時 | マウスオーバー側（`activeSide`）の画像を操作対象とする |
| 情報バー更新 | タグ追加/削除後に `updateInfoBar()` を再実行して即時反映 |
| メイン画面通知 | タグ変更時に `onTagChangedCallback` を呼び出してメイン画面のタグツリーを更新 (#28) |

### 見開き表示時の操作対象

見開き表示中は #33（マウスオーバー側情報表示）の `activeSide` を利用して操作対象を決定する:

```java
// コンテキストメニュー表示時に操作対象を決定
ImageSummary target;
if (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) {
    target = getSpreadImageForSide(activeSide);
} else {
    target = viewerState.getCurrentImage();
}
```

### 実装方針

メイン画面の `rebuildAddTagMenu()` と `toggleTagOnImage()` のロジックを ViewerController にも同様に実装する。`TagHistory` インスタンスは `AppConfig` から読み込み、ビューア・メイン画面で共有する。

### コードイメージ

```java
// ViewerController に追加

/** タグ使用履歴（メイン画面と共有）。 */
private TagHistory tagHistory;

// initialize() 内で初期化
tagHistory = new TagHistory();
tagHistory.loadFrom(AppConfig.getInstance().getState(AppConfig.KEY_TAG_HISTORY, ""));

// setupContextMenu() 内に「タグを追加」サブメニューを追加
Menu addTagMenu = new Menu("タグを追加");
// contextMenu.getItems() の先頭付近に追加

// コンテキストメニュー表示時に動的再構築
contextMenu.setOnShowing(e -> {
    ImageSummary target = getContextMenuTarget();
    rebuildAddTagMenu(addTagMenu, target);
});

private ImageSummary getContextMenuTarget() {
    if (viewerState == null) return null;
    if (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) {
        return getSpreadImageForSide(activeSide);
    }
    return viewerState.getCurrentImage();
}

private void rebuildAddTagMenu(Menu menu, ImageSummary target) {
    menu.getItems().clear();
    if (target == null) return;

    // タグ名が未ロードの場合はDBから取得（#33の遅延取得と同じ）
    if (target.getTagNames().isEmpty() && target.getId() != null) {
        try {
            List<Tag> tags = tagService.getTagsForImage(target.getId());
            target.setTagNames(tags.stream().map(Tag::getName).collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("タグの取得に失敗しました: imageId={}", target.getId(), e);
        }
    }
    List<String> currentTagNames = target.getTagNames();

    // 履歴を再読み込み
    tagHistory.loadFrom(AppConfig.getInstance().getState(AppConfig.KEY_TAG_HISTORY, ""));

    // 1. Star付きタグ上位5件
    List<Tag> starTags = tagService.findAll().stream()
            .filter(t -> t.getStar() > 0)
            .limit(5)
            .collect(Collectors.toList());

    for (Tag tag : starTags) {
        boolean hasTag = currentTagNames.contains(tag.getName());
        MenuItem item = new MenuItem((hasTag ? "✓ " : "  ") + "★".repeat(tag.getStar()) + " " + tag.getName());
        item.setOnAction(e -> toggleTagOnImage(target, tag.getName(), hasTag));
        menu.getItems().add(item);
    }

    menu.getItems().add(new SeparatorMenuItem());

    // 2. 最近使用したタグ（Star付きと重複除外、最大10件）
    Set<String> starTagNames = starTags.stream().map(Tag::getName).collect(Collectors.toSet());
    List<String> recentTags = tagHistory.getRecent(10).stream()
            .filter(name -> !starTagNames.contains(name))
            .collect(Collectors.toList());

    for (String tagName : recentTags) {
        boolean hasTag = currentTagNames.contains(tagName);
        MenuItem item = new MenuItem((hasTag ? "✓ " : "  ") + tagName);
        item.setOnAction(e -> toggleTagOnImage(target, tagName, hasTag));
        menu.getItems().add(item);
    }

    menu.getItems().add(new SeparatorMenuItem());

    // 3. その他...（タグ編集ダイアログ）
    MenuItem other = new MenuItem("その他...");
    other.setOnAction(e -> onAddTag());
    menu.getItems().add(other);
}

private void toggleTagOnImage(ImageSummary target, String tagName, boolean currentlyHas) {
    try {
        Tag tag = tagService.createOrGet(tagName);
        if (currentlyHas) {
            tagService.removeTagFromImage(target.getId(), tag.getId());
        } else {
            tagService.addTagToImage(target.getId(), tag.getId());
            tagHistory.add(tagName);
            AppConfig.getInstance().setState(AppConfig.KEY_TAG_HISTORY, tagHistory.toCsv());
        }
        // タグリストを再取得して情報バーを更新
        List<Tag> updatedTags = tagService.getTagsForImage(target.getId());
        target.setTagNames(updatedTags.stream().map(Tag::getName).collect(Collectors.toList()));
        updateInfoBar(target);
        // メイン画面に通知 (#28)
        notifyTagChanged();
    } catch (Exception ex) {
        log.error("タグ操作に失敗しました: {}", tagName, ex);
    }
}
```

### 既存メニューとの統合

コンテキストメニューの項目順序:

```
タグを追加 → [サブメニュー: Star付き + 最近使用 + その他...]
タグを編集...      ← 既存（フルダイアログ）
非表示にする       ← 既存
Star を設定 →      ← 既存
───────────
作品を表示         ← 既存
───────────
ファイルパスをコピー ← 既存
エクスプローラーで表示 ← 既存
───────────
見開き表示          ← 既存
情報バーを表示      ← 既存
───────────
画像をDBから削除...  ← 既存
```

### 実装箇所

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `TagHistory` フィールド追加、`rebuildAddTagMenu()` メソッド追加、`toggleTagOnImage()` メソッド追加、`setupContextMenu()` に「タグを追加」Menu 追加 |

### 工数・優先度

| 工数 | 優先度 | 理由 |
|------|--------|------|
| 小 | 高 | メイン画面の実装パターン (#19) を流用するだけで完了。ビューアでの連続タグ付け作業が劇的に効率化される |
