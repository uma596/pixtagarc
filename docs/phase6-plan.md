# フェーズ6: ビューアタグ追加＋フォルダ作品化 — 実装計画

## 概要

ビューアでの連続タグ付け効率化と、meta.jsonなしの一般フォルダを作品として取り込む機能を実装する。

## 対象機能

| 順 | # | 機能 | 工数 |
|----|---|------|------|
| 1 | 36 | ビューア画面：コンテキストメニューからタグ追加（最近使ったタグ履歴） | 小 |
| 2 | 20 | インポート：フォルダを1作品として取り込み | 中 |

---

## 1. ビューアコンテキストメニューからタグ追加（#36）

### 変更対象

| ファイル | 変更内容 |
|---------|---------|
| `controller/ViewerController.java` | `TagHistory` フィールド追加、`rebuildAddTagMenu()` / `toggleTagOnImage()` 追加、`setupContextMenu()` に Menu 追加 |

### 実装ポイント

#### 既存の `getContextMenuTarget()` を活用

フェーズ5で追加済みの `getContextMenuTarget()` が見開き時の左右判定を含めた操作対象を返す。そのまま利用する。

#### TagHistory の初期化

```java
// フィールド追加
private TagHistory tagHistory;

// initializeServices() 内で初期化
tagHistory = new TagHistory();
tagHistory.loadFrom(AppConfig.getInstance().getState(AppConfig.KEY_TAG_HISTORY, ""));
```

#### setupContextMenu() への追加

既存のメニュー項目リストの先頭に「タグを追加」サブメニューを挿入:

```java
Menu addTagMenu = new Menu("タグを追加");

// contextMenu.getItems().addAll() の先頭に追加
contextMenu.getItems().addAll(
        addTagMenu,         // ← 新規
        editTags,
        toggleHidden,
        starMenu,
        ...
);
```

#### コンテキストメニュー表示時の動的再構築

```java
// imageAreaPane.setOnContextMenuRequested 内に追加:
ImageSummary menuTarget = current; // 既に左右判定済み
rebuildAddTagMenu(addTagMenu, menuTarget);
```

#### rebuildAddTagMenu() の実装

MainController の同名メソッドとほぼ同一。以下の点が異なる:
- 操作対象は `getContextMenuTarget()` ではなく、引数で渡された `target` を使用
- FTS5更新は `ImageRepository` を直接使用（MainControllerと同じパターン）
- 「その他...」は既存の `onAddTag()` を呼び出し

#### toggleTagOnImage() の実装

```java
private void toggleTagOnImage(ImageSummary target, String tagName, boolean currentlyHas) {
    try {
        Tag tag = tagService.createOrGet(tagName);
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);

        if (currentlyHas) {
            tagService.removeTagFromImage(target.getId(), tag.getId());
        } else {
            tagService.addTagToImage(target.getId(), tag.getId());
            tagHistory.add(tagName);
            AppConfig.getInstance().setState(AppConfig.KEY_TAG_HISTORY, tagHistory.toCsv());
        }
        // FTS5更新
        ImageRepository imageRepo = new ImageRepository(dbConfig);
        com.example.pixtagarc.domain.Image image = imageRepo.findById(target.getId()).orElse(null);
        if (image != null) {
            String tagsText = imageTagRepository.getTagsTextForImage(target.getId());
            imageRepo.insertFts(target.getId(), image.getFileName(), tagsText, "");
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

### 注意事項

- `tagService.findAll()` はDBアクセスが発生するため、メニュー表示のたびに呼ぶのはパフォーマンス上問題ない（タグ数は通常100件未満）
- `TagHistory` のCSV保存は即時書き込み（他ウィンドウとの共有のため）
- 既存の「タグを編集...」メニュー項目は残す（フルダイアログへのアクセス手段）

---

## 2. フォルダを1作品として取り込み（#20）

### 変更対象

| ファイル | 変更内容 |
|---------|---------|
| `fxml/import.fxml` | 「フォルダごとに1作品としてまとめる」CheckBox 追加 |
| `controller/ImportController.java` | CheckBox のFXMLバインド、ImportServiceへのフラグ受け渡し |
| `service/ImportService.java` | `groupByFolder` フラグ追加、フォルダ単位の作品作成ロジック |

### 仕様

| 項目 | 内容 |
|------|------|
| 作品タイトル | フォルダ名（末尾のディレクトリ名） |
| ページ番号 | フォルダ内のファイルをファイル名昇順ソートし、1始まりで連番付与 |
| 作者 | 未設定（後から手動で設定） |
| `-meta.json` との併用 | フォルダ内に `-meta.json` が存在すればそちらを優先（既存ロジック） |
| サブフォルダ展開 | 「サブフォルダも含める」＋「フォルダごとに1作品」で各サブフォルダが独立作品 |
| ルート直下のファイル | 作品未所属（work_id = NULL）として従来通りインポート |
| 外部ID | `F` + 10桁0埋め連番（例: `F0000000001`）。JSON由来の数字ID（pixiv `idNum`）と体系が異なるため衝突しない |
| 外部ID採番 | `SELECT MAX(CAST(SUBSTR(external_id, 2) AS INTEGER)) FROM works WHERE external_id LIKE 'F%'` で現在の最大値を取得し +1 |

### UI変更

```xml
<!-- import.fxml に追加 -->
<CheckBox fx:id="groupByFolderCheckBox" text="フォルダごとに1作品としてまとめる"
          selected="false" style="-fx-text-fill: #bbbbbb;" />
```

配置位置: 「全データをクリアして再インポート」の上（オプションVBoxの3番目）。

### 排他制御

| オプション組み合わせ | 動作 |
|---------------------|------|
| フォルダ作品 ON + サブフォルダ ON | 各サブフォルダが独立した作品。ルート直下は作品未所属 |
| フォルダ作品 ON + サブフォルダ OFF | ルートフォルダ直下のファイルのみインポート。1つの作品として作成 |
| フォルダ作品 OFF（デフォルト） | 従来動作（meta.jsonがあれば作品化、なければ作品未所属） |

### ImportService の変更

#### コンストラクタにフラグ追加

```java
/** フォルダを作品としてまとめるかどうか。 */
private final boolean groupByFolder;

// コンストラクタに引数追加
public ImportService(..., boolean groupByFolder) {
    ...
    this.groupByFolder = groupByFolder;
}
```

#### フォルダ→作品マッピングのキャッシュ

```java
/**
 * フォルダパス → 作品エンティティのキャッシュ。
 * 同一フォルダ内の複数ファイルで作品レコードの二重作成を防ぐ。
 */
private final ConcurrentHashMap<String, Work> folderWorkCache = new ConcurrentHashMap<>();

/**
 * フォルダ作品の external_id 採番用カウンター。
 * インポート開始時にDBの最大値を取得して初期化する。
 */
private final AtomicLong folderIdSequence = new AtomicLong(0);
```

#### external_id の採番初期化

`call()` メソッドの冒頭（フォルダ作品化モード時のみ）:

```java
if (groupByFolder) {
    // 現在のフォルダ作品の最大連番を取得
    long maxId = workRepository.getMaxFolderExternalId(); // "F%" の最大値
    folderIdSequence.set(maxId);
}
```

WorkRepository に追加するメソッド:

```java
/**
 * フォルダ作品の external_id の最大連番を返す。
 * external_id が 'F' + 10桁数字の形式のレコードから最大値を取得する。
 *
 * @return 最大連番（存在しない場合は 0）
 */
public long getMaxFolderExternalId() {
    String sql = "SELECT MAX(CAST(SUBSTR(external_id, 2) AS INTEGER)) FROM works WHERE external_id LIKE 'F%'";
    // ... 実装
}
```

#### importFile() 内の分岐

既存の `WORK_FILE_PATTERN` によるメタデータ作品化の前に、`groupByFolder` チェックを追加:

```java
// フォルダ作品化モードの処理（meta.jsonより前に判定）
if (groupByFolder && workExternalId == null) {
    // meta.json由来の作品IDがない場合のみフォルダ作品化を適用
    Path parentDir = file.getParent();
    if (parentDir != null && !parentDir.equals(rootDirectory)) {
        // ルート直下ではなくサブフォルダ内のファイル → フォルダを1作品として扱う
        String folderPath = parentDir.toAbsolutePath().toString();
        String folderName = parentDir.getFileName().toString();

        Work folderWork = folderWorkCache.computeIfAbsent(folderPath, key -> {
            // 新規フォルダ作品を作成（F + 10桁連番）
            long nextId = folderIdSequence.incrementAndGet();
            String externalId = String.format("F%010d", nextId);

            Work newWork = new Work();
            newWork.setTitle(folderName);
            newWork.setExternalId(externalId);
            newWork.setCreatedAt(LocalDateTime.now().format(DATETIME_FORMATTER));
            newWork.setUpdatedAt(newWork.getCreatedAt());
            synchronized (workRepository) {
                workRepository.save(newWork);
            }
            log.info("フォルダ作品を作成しました: title={}, externalId={}", folderName, externalId);
            return newWork;
        });

        workId = folderWork.getId();
        // ページ番号はフォルダ内でのファイル名ソート順で決定（後述）
    }
}
```

#### ページ番号の割り当て

フォルダ作品化時のページ番号は、同一フォルダ内のファイル名昇順での位置で決定する。ただし並列インポートでは順番が保証できないため、**インポート完了後にページ番号を一括更新**する方式を採用:

```java
/**
 * フォルダ作品化時のページ番号を一括設定する。
 *
 * <p>インポート完了後に各作品のページ番号をファイル名昇順で再付番する。
 */
private void assignFolderPageNumbers() {
    for (Map.Entry<String, Work> entry : folderWorkCache.entrySet()) {
        Long workId = entry.getValue().getId();
        List<Image> workImages = imageRepository.findByWorkId(workId);
        // ファイル名昇順ソート
        workImages.sort(Comparator.comparing(Image::getFileName));
        for (int i = 0; i < workImages.size(); i++) {
            imageRepository.updatePageNumber(workImages.get(i).getId(), i + 1);
        }
        // 総ページ数を更新
        workRepository.updateTotalPages(workId, workImages.size());
    }
}
```

`call()` メソッドの末尾（インポートループ完了後）で呼び出す:

```java
// フォルダ作品化のページ番号を割り当て
if (groupByFolder && !folderWorkCache.isEmpty()) {
    updateMessage("ページ番号を割り当て中...");
    assignFolderPageNumbers();
}
```

### Repository 追加メソッド

| クラス | メソッド | 内容 |
|--------|---------|------|
| `WorkRepository` | `getMaxFolderExternalId()` | `SELECT MAX(CAST(SUBSTR(external_id, 2) AS INTEGER)) FROM works WHERE external_id LIKE 'F%'` |
| `WorkRepository` | `updateTotalPages(Long id, int totalPages)` | UPDATE works SET total_pages = ? WHERE id = ? |
| `ImageRepository` | `updatePageNumber(Long id, Integer pageNumber)` | UPDATE images SET page_number = ? WHERE id = ? |

### 処理フロー

```
インポート開始（groupByFolder = true）
    │
    ▼
ファイルをスキャン（既存処理）
    │
    ▼
各ファイルに対して importFile() 実行:
    │
    ├── meta.json 由来の作品IDがある → 従来通り meta.json の情報で作品化
    │
    └── meta.json 由来の作品IDがない + フォルダ作品モード:
            ├── ルート直下のファイル → 作品未所属（従来通り）
            └── サブフォルダ内のファイル → フォルダ名で作品作成/再利用、work_id を設定
    │
    ▼
全ファイルのインポート完了
    │
    ▼
assignFolderPageNumbers(): 各フォルダ作品のページ番号をファイル名昇順で一括設定
    │
    ▼
完了
```

### テスト用のフォルダ構造例

```
import-root/
├── 旅行写真2026/
│   ├── DSC_0001.jpg  → 作品「旅行写真2026」page 1
│   ├── DSC_0002.jpg  → 作品「旅行写真2026」page 2
│   └── DSC_0003.jpg  → 作品「旅行写真2026」page 3
├── スキャン画像/
│   ├── scan001.png   → 作品「スキャン画像」page 1
│   └── scan002.png   → 作品「スキャン画像」page 2
├── 143716458_p0000-タイトル.jpg     → meta.json 作品化（従来動作を優先）
├── 143716458_p0000-タイトル-meta.json
└── 単独画像.jpg                      → 作品未所属（ルート直下）
```

### ImportController の変更

```java
// FXML フィールド追加
@FXML private CheckBox groupByFolderCheckBox;

// onStartImport() での ImportService 生成時にフラグを渡す
ImportService importService = new ImportService(
        imageRepository, thumbnailService,
        workRepository, imageTagRepository,
        tagService, authorService,
        authorRepository, dbConfig,
        Paths.get(folderPathField.getText()),
        recursiveCheckBox.isSelected(),
        skipExistingCheckBox.isSelected(),
        clearAllCheckBox.isSelected(),
        groupByFolderCheckBox.isSelected()  // ← 追加
);
```

---

## 実装順序の根拠

```
#36（ビューアタグ追加）← 小工数、独立。既存パターンの流用
    ↓
#20（フォルダ作品化）← 中工数。ImportServiceの拡張が中心
```

#36 は ViewerController の変更のみで他の機能に影響しない。
#20 は ImportService の拡張だが、既存のインポートフローを壊さない（`groupByFolder = false` がデフォルト）。

---

## 完了条件

- [ ] #36: ビューアのコンテキストメニューに「タグを追加」サブメニューが表示され、Star付きタグ・最近使用タグからワンクリックで付与/削除できること
- [ ] #36: 見開き表示時にマウス位置側の画像に対してタグ操作が行われること
- [ ] #36: タグ操作後に情報バーが即時更新されること
- [ ] #20: インポートダイアログに「フォルダごとに1作品としてまとめる」チェックボックスが表示されること
- [ ] #20: チェックON + サブフォルダONでインポートすると、各サブフォルダが独立した作品として登録されること
- [ ] #20: 作品のページ番号がファイル名昇順で正しく付番されること
- [ ] #20: `-meta.json` が存在するファイルは従来通りメタデータ由来の作品化が優先されること
- [ ] #20: ルート直下のファイルは作品未所属としてインポートされること
- [ ] ビルド成功（`gradlew build`）
- [ ] EXE作成成功（`gradlew jpackageWinExe`）
