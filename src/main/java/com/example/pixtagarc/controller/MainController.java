package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Author;
import com.example.pixtagarc.domain.SavedSearch;
import com.example.pixtagarc.domain.Tag;
import com.example.pixtagarc.dto.*;
import com.example.pixtagarc.repository.*;
import com.example.pixtagarc.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * メイン画面のコントローラークラス。
 *
 * <p>メイン画面のUI操作を処理する。
 * サムネイル/リスト表示の切替、検索実行、ビューア起動、
 * インポート・PDF出力ダイアログの表示などを担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class MainController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // ===== FXML注入フィールド =====

    /** インポートボタン。 */
    @FXML private Button importButton;

    /** PDF出力ボタン。 */
    @FXML private Button pdfExportButton;

    /** サムネイル表示切替ToggleButton。 */
    @FXML private ToggleButton thumbnailViewButton;

    /** リスト表示切替ToggleButton。 */
    @FXML private ToggleButton listViewButton;

    /** サムネイルサイズComboBox。 */
    @FXML private ComboBox<String> thumbnailSizeCombo;

    /** 保存済み検索管理ボタン。 */
    @FXML private Button savedSearchManageButton;

    /** キーワード入力フィールド。 */
    @FXML private TextField keywordField;

    /** タグフィルターMenuButton。 */
    @FXML private MenuButton tagFilterButton;

    /** キーワードクリアボタン。 */
    @FXML private Button clearKeywordButton;

    /** メインSplitPane。 */
    @FXML private SplitPane mainSplitPane;

    /** 作者フィルターComboBox。 */
    @FXML private ComboBox<String> authorFilterCombo;

    /** 非表示除外ToggleButton。 */
    @FXML private ToggleButton excludeHiddenToggle;

    /** Star フィルター ComboBox。 */
    @FXML private ComboBox<String> starFilterCombo;

    /** 検索ボタン。 */
    @FXML private Button searchButton;

    /** 検索保存ボタン。 */
    @FXML private Button saveSearchButton;

    /** 保存済み検索ComboBox。 */
    @FXML private ComboBox<String> savedSearchCombo;

    /** タグツリービュー。 */
    @FXML private TreeView<String> tagTreeView;

    /** コンテンツ切替StackPane。 */
    @FXML private StackPane contentPane;

    /** サムネイルScrollPane。 */
    @FXML private ScrollPane thumbnailScrollPane;

    /** サムネイルFlowPane。 */
    @FXML private FlowPane thumbnailPane;

    /** 画像TableView。 */
    @FXML private TableView<ImageSummary> imageTableView;

    /** ファイル名列。 */
    @FXML private TableColumn<ImageSummary, String> colFileName;

    /** 作者列。 */
    @FXML private TableColumn<ImageSummary, String> colAuthor;

    /** Star列。 */
    @FXML private TableColumn<ImageSummary, String> colStar;

    /** タグ列。 */
    @FXML private TableColumn<ImageSummary, String> colTags;

    /** 作成日時列。 */
    @FXML private TableColumn<ImageSummary, String> colCreatedAt;

    /** ファイルサイズ列。 */
    @FXML private TableColumn<ImageSummary, String> colFileSize;

    /** 解像度列。 */
    @FXML private TableColumn<ImageSummary, String> colResolution;

    /** 総件数ラベル。 */
    @FXML private Label totalCountLabel;

    /** ページ情報ラベル。 */
    @FXML private Label pageInfoLabel;

    /** 前ページボタン。 */
    @FXML private Button prevPageButton;

    /** 次ページボタン。 */
    @FXML private Button nextPageButton;

    /** ページサイズComboBox。 */
    @FXML private ComboBox<String> pageSizeCombo;

    /** ステータスメッセージラベル。 */
    @FXML private Label statusMessageLabel;

    // ===== サービス =====

    /** 検索サービス。 */
    private SearchService searchService;

    /** サムネイルサービス。 */
    private ThumbnailService thumbnailService;

    /** タグサービス。 */
    private TagService tagService;

    /** 作者サービス。 */
    private AuthorService authorService;

    /** 保存済み検索サービス。 */
    private SavedSearchService savedSearchService;

    /** 検索条件履歴サービス。 */
    private SearchHistoryService searchHistoryService;

    /** 画像サービス。 */
    private ImageService imageService;

    /** タグ使用履歴。 */
    private TagHistory tagHistory;

    // ===== 状態 =====

    /** メイン画面の表示状態。 */
    private MainViewState viewState = new MainViewState();

    /** 現在の検索結果。 */
    private SearchResult currentSearchResult;

    /** 現在の検索条件。 */
    private SearchCondition currentCondition = new SearchCondition();

    /** 保存済み検索のIDリスト（ComboBoxのインデックスと対応）。 */
    private List<Long> savedSearchIds = new ArrayList<>();

    /** 現在のページ番号（0始まり）。 */
    private int currentPage = 0;

    /**
     * サムネイル生成専用のバックグラウンドスレッドプール。
     * JavaFXスレッドをブロックしないようにIOと画像変換をオフロードする。
     */
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r, "thumbnail-loader");
                t.setDaemon(true); // アプリ終了時に強制終了
                return t;
            });

    /**
     * 現在のサムネイル表示セッションID。
     * 検索結果が変わったとき古いタスクの結果を破棄するために使用する。
     */
    private final AtomicLong thumbnailSessionId = new AtomicLong(0);

    /**
     * コントローラーを初期化する。
     *
     * <p>サービスを初期化し、UIコンポーネントを設定する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("MainControllerを初期化します");
        initializeServices();
        initializeUI();
        loadInitialData();
        log.info("MainControllerの初期化が完了しました");
    }

    /**
     * サービスを初期化する。
     */
    private void initializeServices() {
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ImageRepository imageRepository = new ImageRepository(dbConfig);
        TagRepository tagRepository = new TagRepository(dbConfig);
        AuthorRepository authorRepository = new AuthorRepository(dbConfig);
        ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);
        SavedSearchRepository savedSearchRepository = new SavedSearchRepository(dbConfig);

        searchService = new SearchService(dbConfig);
        thumbnailService = new ThumbnailService();
        tagService = new TagService(tagRepository, imageTagRepository);
        authorService = new AuthorService(authorRepository);
        savedSearchService = new SavedSearchService(savedSearchRepository);
        imageService = new ImageService(imageRepository);

        // タグ使用履歴の読み込み
        tagHistory = new TagHistory();
        tagHistory.loadFrom(AppConfig.getInstance().getState(AppConfig.KEY_TAG_HISTORY, ""));

        // 検索条件履歴
        searchHistoryService = new SearchHistoryService();
    }

    /**
     * UIコンポーネントを初期化する。
     */
    private void initializeUI() {
        // サムネイルサイズComboBoxの初期値
        thumbnailSizeCombo.setItems(FXCollections.observableArrayList("小", "中", "大"));
        thumbnailSizeCombo.setValue("中");

        // ページサイズComboBoxの初期値
        pageSizeCombo.setItems(FXCollections.observableArrayList("200", "500", "1000"));
        pageSizeCombo.setValue("200");

        // Star フィルター ComboBox の初期値
        starFilterCombo.setItems(FXCollections.observableArrayList(
                "すべて", "★1以上", "★2以上", "★3以上", "★4以上", "★5のみ"));
        starFilterCombo.setValue("すべて");

        // TableViewの列設定
        colFileName.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFileName()));
        colAuthor.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getAuthorName() != null ? data.getValue().getAuthorName() : ""));
        colStar.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        "★".repeat(data.getValue().getStar())));
        colTags.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.join(", ", data.getValue().getTagNames())));
        colCreatedAt.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt()));
        colFileSize.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFileSizeString()));
        colResolution.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getResolutionString()));

        // TableViewのダブルクリックでビューアを開く
        imageTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ImageSummary selected = imageTableView.getSelectionModel().getSelectedItem();
                if (selected != null && currentSearchResult != null) {
                    openViewerWithFullResults(selected);
                }
            }
        });

        // タグツリーのルートノード設定
        TreeItem<String> root = new TreeItem<>("タグ");
        root.setExpanded(true);
        tagTreeView.setRoot(root);
        tagTreeView.setShowRoot(false);

        // キーワード×クリアボタンの表示制御
        keywordField.textProperty().addListener((obs, oldVal, newVal) ->
                clearKeywordButton.setVisible(newVal != null && !newVal.isEmpty()));

        // コンテキストメニューの設定
        setupContextMenu();

        // タグツリーのコンテキストメニュー（Star設定）
        setupTagTreeContextMenu();
    }

    /**
     * 検索結果のコンテキストメニューを設定する。
     *
     * <p>サムネイルセルおよびTableViewの右クリックで操作メニューを表示する。
     * メニュー項目はビューア画面と同等の操作を提供する。
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem openViewer = new MenuItem("ビューアで開く");
        openViewer.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null && currentSearchResult != null) {
                openViewerWithFullResults(selected);
            }
        });

        MenuItem openWork = new MenuItem("作品を表示");
        openWork.setOnAction(e -> onOpenWorkFromMain());

        MenuItem editTags = new MenuItem("タグを編集...");
        editTags.setOnAction(e -> onEditTagsFromMain());

        MenuItem toggleHidden = new MenuItem("非表示にする");
        toggleHidden.setOnAction(e -> onToggleHiddenFromMain());

        // Star 設定サブメニュー
        Menu starMenu = new Menu("Star を設定");
        String[] starLabels = {"☆ なし", "★ 1", "★★ 2", "★★★ 3", "★★★★ 4", "★★★★★ 5"};
        for (int i = 0; i <= 5; i++) {
            final int starValue = i;
            MenuItem starItem = new MenuItem(starLabels[i]);
            starItem.setOnAction(e -> {
                ImageSummary selected = getSelectedImage();
                if (selected != null) {
                    try {
                        imageService.updateStar(selected.getId(), starValue);
                        selected.setStar(starValue);
                        statusMessageLabel.setText("Star評価を更新しました: " + selected.getFileName());
                    } catch (Exception ex) {
                        log.error("Star評価の更新に失敗しました", ex);
                    }
                }
            });
            starMenu.getItems().add(starItem);
        }

        MenuItem copyPath = new MenuItem("ファイルパスをコピー");
        copyPath.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getFilePath());
                Clipboard.getSystemClipboard().setContent(content);
                statusMessageLabel.setText("パスをコピーしました: " + selected.getFileName());
            }
        });

        MenuItem openInExplorer = new MenuItem("エクスプローラーで表示");
        openInExplorer.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null) {
                try {
                    File file = new File(selected.getFilePath());
                    if (file.exists()) {
                        Desktop.getDesktop().open(file.getParentFile());
                    }
                } catch (Exception ex) {
                    log.error("エクスプローラーの起動に失敗しました", ex);
                }
            }
        });

        MenuItem deleteImage = new MenuItem("画像をDBから削除...");
        deleteImage.setOnAction(e -> onDeleteImageFromMain());

        // 作品単位操作
        MenuItem applyTagsToWork = new MenuItem("タグを作品全体に反映");
        applyTagsToWork.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null && selected.getWorkId() != null) {
                try {
                    DatabaseConfig dbConfig = DatabaseConfig.getInstance();
                    ImageTagRepository itr = new ImageTagRepository(dbConfig);
                    int count = imageService.applyTagsToWork(selected.getId(), itr);
                    statusMessageLabel.setText(count + "件の画像にタグを反映しました");
                } catch (Exception ex) {
                    log.error("タグの作品反映に失敗しました", ex);
                }
            }
        });

        MenuItem applyStarToWork = new MenuItem("Starを作品全体に反映");
        applyStarToWork.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null && selected.getWorkId() != null) {
                try {
                    int count = imageService.applyStarToWork(selected.getId());
                    statusMessageLabel.setText(count + "件の画像にStar ★" + selected.getStar() + " を反映しました");
                } catch (Exception ex) {
                    log.error("Starの作品反映に失敗しました", ex);
                }
            }
        });

        MenuItem toggleHiddenWork = new MenuItem("作品全体を非表示にする");
        toggleHiddenWork.setOnAction(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null && selected.getWorkId() != null) {
                try {
                    boolean newState = !selected.isHidden();
                    int count = imageService.toggleHiddenForWork(selected.getWorkId(), newState);
                    statusMessageLabel.setText(count + "件の画像を" + (newState ? "非表示に" : "非表示解除") + "しました");
                    if (excludeHiddenToggle.isSelected()) onSearch();
                } catch (Exception ex) {
                    log.error("作品全体の非表示切替に失敗しました", ex);
                }
            }
        });

        // タグを追加サブメニュー（動的に構築）
        Menu addTagMenu = new Menu("タグを追加");

        contextMenu.getItems().addAll(
                openViewer,
                openWork,
                new SeparatorMenuItem(),
                addTagMenu,
                editTags,
                toggleHidden,
                toggleHiddenWork,
                starMenu,
                new SeparatorMenuItem(),
                applyTagsToWork,
                applyStarToWork,
                new SeparatorMenuItem(),
                copyPath,
                openInExplorer,
                new SeparatorMenuItem(),
                deleteImage
        );

        // TableViewにコンテキストメニューを設定
        imageTableView.setContextMenu(contextMenu);

        // コンテキストメニュー表示時に動的にラベルを更新
        contextMenu.setOnShowing(e -> {
            ImageSummary selected = getSelectedImage();
            if (selected != null) {
                toggleHidden.setText(selected.isHidden() ? "非表示を解除" : "非表示にする");
                toggleHiddenWork.setText(selected.isHidden() ? "作品全体の非表示を解除" : "作品全体を非表示にする");
                openWork.setDisable(selected.getWorkId() == null);
                toggleHiddenWork.setDisable(selected.getWorkId() == null);
                applyTagsToWork.setDisable(selected.getWorkId() == null);
                applyStarToWork.setDisable(selected.getWorkId() == null);
                // タグ追加サブメニューを動的に再構築
                rebuildAddTagMenu(addTagMenu, selected);
            } else {
                openWork.setDisable(true);
                toggleHiddenWork.setDisable(true);
                applyTagsToWork.setDisable(true);
                applyStarToWork.setDisable(true);
            }
        });

        // サムネイル用のコンテキストメニューを保持（createThumbnailCellで使用）
        this.thumbnailContextMenu = contextMenu;
    }

    /**
     * タグツリーのコンテキストメニュー（Star設定）を構築する。
     */
    private void setupTagTreeContextMenu() {
        ContextMenu tagContextMenu = new ContextMenu();

        Menu starMenu = new Menu("Star を設定");
        String[] labels = {"☆ なし", "★ 1", "★★ 2", "★★★ 3", "★★★★ 4", "★★★★★ 5"};
        for (int i = 0; i <= 5; i++) {
            final int starValue = i;
            MenuItem item = new MenuItem(labels[i]);
            item.setOnAction(e -> {
                TreeItem<String> selected = tagTreeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getParent() != null) {
                    String tagName = selected.getValue().replaceFirst("^★+ ", "");
                    tagService.findByName(tagName).ifPresent(tag -> {
                        tagService.updateStar(tag.getId(), starValue);
                        loadTagTree();
                        loadTagFilter();
                        statusMessageLabel.setText("タグ「" + tagName + "」のStarを★" + starValue + "に設定しました");
                    });
                }
            });
            starMenu.getItems().add(item);
        }

        tagContextMenu.getItems().add(starMenu);
        tagTreeView.setContextMenu(tagContextMenu);
    }

    /**
     * 「タグを追加」サブメニューを動的に再構築する (#19)。
     *
     * @param menu   再構築対象のMenuオブジェクト
     * @param target 対象画像サマリー
     */
    private void rebuildAddTagMenu(Menu menu, ImageSummary target) {
        menu.getItems().clear();
        List<String> currentTagNames = target.getTagNames() != null ? target.getTagNames() : List.of();

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
        java.util.Set<String> starTagNames = starTags.stream()
                .map(Tag::getName).collect(java.util.stream.Collectors.toSet());
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
        other.setOnAction(e -> onEditTagsFromMain());
        menu.getItems().add(other);
    }

    /**
     * 画像にタグを追加/削除する（コンテキストメニューから）。
     *
     * @param target     対象画像サマリー
     * @param tagName    タグ名
     * @param currentlyHas 現在付与済みかどうか
     */
    private void toggleTagOnImage(ImageSummary target, String tagName, boolean currentlyHas) {
        try {
            Tag tag = tagService.createOrGet(tagName);
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);

            if (currentlyHas) {
                tagService.removeTagFromImage(target.getId(), tag.getId());
                statusMessageLabel.setText("タグ「" + tagName + "」を削除しました");
            } else {
                tagService.addTagToImage(target.getId(), tag.getId());
                tagHistory.add(tagName);
                AppConfig.getInstance().setState(AppConfig.KEY_TAG_HISTORY, tagHistory.toCsv());
                statusMessageLabel.setText("タグ「" + tagName + "」を追加しました");
            }
            // FTS5更新
            ImageRepository imageRepository = new ImageRepository(dbConfig);
            com.example.pixtagarc.domain.Image image = imageRepository.findById(target.getId()).orElse(null);
            if (image != null) {
                String tagsText = imageTagRepository.getTagsTextForImage(target.getId());
                imageRepository.insertFts(target.getId(), image.getFileName(), tagsText, "");
            }
            // タグリスト更新
            loadTagTree();
            loadTagFilter();
        } catch (Exception ex) {
            log.error("タグ操作に失敗しました: {}", tagName, ex);
        }
    }

    /** サムネイルセルに右クリックメニューを提供するためのコンテキストメニュー参照。 */
    private ContextMenu thumbnailContextMenu;

    /** コンテキストメニュー操作時に対象となる画像（サムネイルで右クリックされた画像）。 */
    private ImageSummary contextMenuTarget;

    /**
     * 現在選択中（または右クリックされた）画像を返す。
     *
     * <p>サムネイル表示時は {@code contextMenuTarget}、リスト表示時はTableViewの選択行を返す。
     *
     * @return 選択中の画像サマリー（選択なしの場合は {@code null}）
     */
    private ImageSummary getSelectedImage() {
        if (viewState.getDisplayMode() == MainViewState.DisplayMode.THUMBNAIL) {
            return contextMenuTarget;
        } else {
            return imageTableView.getSelectionModel().getSelectedItem();
        }
    }

    /**
     * メイン画面から作品を表示する。
     *
     * <p>選択中の画像が所属する作品の全ページを取得し、新しいビューアウィンドウで開く。
     */
    private void onOpenWorkFromMain() {
        ImageSummary selected = getSelectedImage();
        if (selected == null || selected.getWorkId() == null) return;

        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            ImageRepository imageRepository = new ImageRepository(dbConfig);
            List<com.example.pixtagarc.domain.Image> workImages =
                    imageRepository.findByWorkId(selected.getWorkId());

            List<ImageSummary> summaryList = new ArrayList<>();
            int currentPageIndex = 0;
            for (int i = 0; i < workImages.size(); i++) {
                com.example.pixtagarc.domain.Image img = workImages.get(i);
                ImageSummary s = new ImageSummary();
                s.setId(img.getId());
                s.setFilePath(img.getFilePath());
                s.setFileName(img.getFileName());
                s.setFileSize(img.getFileSize());
                s.setWidth(img.getWidth());
                s.setHeight(img.getHeight());
                s.setMediaType(img.getMediaType());
                s.setHidden(img.isHidden());
                s.setCreatedAt(img.getCreatedAt());
                s.setStar(img.getStar());
                s.setWorkId(img.getWorkId());
                s.setPageNumber(img.getPageNumber());
                s.setTagNames(new ArrayList<>());
                summaryList.add(s);
                if (img.getId().equals(selected.getId())) {
                    currentPageIndex = i;
                }
            }

            if (!summaryList.isEmpty()) {
                ViewerController.openNewWindow(summaryList, currentPageIndex);
                log.info("作品を表示しました: workId={}, pages={}", selected.getWorkId(), summaryList.size());
            }
        } catch (Exception ex) {
            log.error("作品の表示に失敗しました", ex);
            showError("作品表示に失敗しました", ex.getMessage());
        }
    }

    /**
     * メイン画面からタグを編集する。
     *
     * <p>選択中の画像のタグを編集するダイアログを開く。
     */
    private void onEditTagsFromMain() {
        ImageSummary selected = getSelectedImage();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tag-edit.fxml"));
            Parent root = loader.load();
            TagEditController controller = loader.getController();
            controller.setImageId(selected.getId());
            Stage stage = new Stage();
            stage.setTitle("タグ編集 - " + selected.getFileName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // タグ編集後に再検索して結果を反映
            onSearch();
            loadTagTree();
            loadTagFilter();
        } catch (Exception ex) {
            log.error("タグ編集ダイアログの表示に失敗しました", ex);
            showError("タグ編集に失敗しました", ex.getMessage());
        }
    }

    /**
     * メイン画面から非表示フラグを切り替える。
     */
    private void onToggleHiddenFromMain() {
        ImageSummary selected = getSelectedImage();
        if (selected == null) return;

        try {
            boolean newState = !selected.isHidden();
            imageService.updateHidden(selected.getId(), newState);
            selected.setHidden(newState);
            statusMessageLabel.setText(
                    (newState ? "非表示にしました: " : "非表示を解除しました: ") + selected.getFileName());
            // 非表示除外フィルターが有効な場合は再検索
            if (excludeHiddenToggle.isSelected()) {
                onSearch();
            }
        } catch (Exception ex) {
            log.error("非表示フラグの切り替えに失敗しました", ex);
            showError("非表示の切り替えに失敗しました", ex.getMessage());
        }
    }

    /**
     * メイン画面から画像をDBから削除する（確認ダイアログ付き）。
     */
    private void onDeleteImageFromMain() {
        ImageSummary selected = getSelectedImage();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("画像の削除");
        confirm.setHeaderText("画像をDBから削除しますか？");
        confirm.setContentText("「" + selected.getFileName() + "」をDBから削除します。\n"
                + "ファイル自体は削除されません。");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseConfig dbConfig = DatabaseConfig.getInstance();
                    ImageRepository imageRepository = new ImageRepository(dbConfig);
                    imageRepository.deleteById(selected.getId());
                    statusMessageLabel.setText("画像を削除しました: " + selected.getFileName());
                    onSearch(); // 再検索して結果を更新
                } catch (Exception ex) {
                    log.error("画像の削除に失敗しました", ex);
                    showError("画像の削除に失敗しました", ex.getMessage());
                }
            }
        });
    }

    /**
     * ビューアを全件検索結果で開く。
     *
     * <p>現在の検索条件で全件再検索（タグ名なし軽量版）し、
     * 指定画像の位置から表示を開始する。
     *
     * @param startItem 表示開始画像
     */
    private void openViewerWithFullResults(ImageSummary startItem) {
        try {
            List<ImageSummary> fullResults = searchService.searchForViewer(currentCondition);
            int index = 0;
            for (int i = 0; i < fullResults.size(); i++) {
                if (fullResults.get(i).getId().equals(startItem.getId())) {
                    index = i;
                    break;
                }
            }
            // タグ変更コールバック: メイン画面のタグリストを即時更新 (#28)
            Runnable onTagChanged = () -> Platform.runLater(() -> {
                loadTagTree();
                loadTagFilter();
            });
            ViewerController.openNewWindow(fullResults, index, onTagChanged);
        } catch (Exception ex) {
            log.error("ビューアの起動に失敗しました", ex);
            showError("ビューアの起動に失敗しました", ex.getMessage());
        }
    }

    /**
     * 初期データを読み込む。
     */
    private void loadInitialData() {
        loadTagTree();
        loadAuthorFilter();
        loadTagFilter();
        loadSavedSearches();
        // 状態復元（検索条件・ページ・表示モード）
        restoreState();
        // 復元した条件で検索実行
        currentCondition = buildSearchCondition();
        executeSearch(currentCondition);
    }

    /**
     * タグツリーを読み込む。
     */
    private void loadTagTree() {
        try {
            List<Tag> tags = tagService.findAll();
            TreeItem<String> root = tagTreeView.getRoot();
            root.getChildren().clear();
            for (Tag tag : tags) {
                String label = (tag.getStar() > 0 ? "★".repeat(tag.getStar()) + " " : "") + tag.getName();
                TreeItem<String> item = new TreeItem<>(label);
                root.getChildren().add(item);
            }
        } catch (Exception e) {
            log.error("タグツリーの読み込みに失敗しました", e);
        }
    }

    /**
     * 作者フィルターComboBoxを読み込む。
     */
    private void loadAuthorFilter() {
        try {
            List<Author> authors = authorService.findAll();
            List<String> items = new ArrayList<>();
            items.add("すべての作者");
            authors.forEach(a -> items.add(a.getName()));
            authorFilterCombo.setItems(FXCollections.observableArrayList(items));
            authorFilterCombo.setValue("すべての作者");
        } catch (Exception e) {
            log.error("作者フィルターの読み込みに失敗しました", e);
        }
    }

    /**
     * タグフィルターMenuButtonを読み込む。
     */
    private void loadTagFilter() {
        try {
            tagFilterButton.getItems().clear();
            List<Tag> tags = tagService.findAll();

            // 「すべて（解除）」項目
            MenuItem allItem = new MenuItem("すべて（解除）");
            allItem.setOnAction(e -> {
                tagFilterButton.getItems().stream()
                        .filter(mi -> mi instanceof CheckMenuItem)
                        .map(mi -> (CheckMenuItem) mi)
                        .forEach(ci -> ci.setSelected(false));
                updateTagFilterLabel();
                onSearch();
            });
            tagFilterButton.getItems().add(allItem);
            tagFilterButton.getItems().add(new SeparatorMenuItem());

            // 各タグ（Star降順）
            for (Tag tag : tags) {
                String label = (tag.getStar() > 0 ? "★".repeat(tag.getStar()) + " " : "") + tag.getName();
                CheckMenuItem item = new CheckMenuItem(label);
                item.setUserData(tag.getId());
                item.setOnAction(e -> {
                    updateTagFilterLabel();
                    onSearch();
                    // メニューを閉じさせない: 再表示
                    Platform.runLater(() -> tagFilterButton.show());
                });
                tagFilterButton.getItems().add(item);
            }
        } catch (Exception e) {
            log.error("タグフィルターの読み込みに失敗しました", e);
        }
    }

    /**
     * タグフィルターMenuButtonのラベルを選択状態に応じて更新する。
     */
    private void updateTagFilterLabel() {
        long count = tagFilterButton.getItems().stream()
                .filter(mi -> mi instanceof CheckMenuItem && mi.getUserData() != null)
                .map(mi -> (CheckMenuItem) mi)
                .filter(CheckMenuItem::isSelected)
                .count();
        tagFilterButton.setText(count == 0 ? "すべてのタグ" : count + "タグ選択中");
    }

    /**
     * 選択中のタグIDリストを返す。
     *
     * @return 選択中のタグIDリスト（未選択の場合は空リスト）
     */
    private List<Long> getSelectedTagIds() {
        return tagFilterButton.getItems().stream()
                .filter(mi -> mi instanceof CheckMenuItem && mi.getUserData() != null)
                .map(mi -> (CheckMenuItem) mi)
                .filter(CheckMenuItem::isSelected)
                .map(ci -> (Long) ci.getUserData())
                .collect(Collectors.toList());
    }

    /**
     * 保存済み検索ComboBoxを読み込む。
     */
    private void loadSavedSearches() {
        updateSavedSearchCombo();
    }

    /**
     * 保存済み検索ComboBoxを再構築する（最近の検索 + 保存済み検索）。
     */
    private void updateSavedSearchCombo() {
        try {
            List<String> items = new ArrayList<>();

            // 最近の検索（先頭）
            List<SearchCondition> recent = searchHistoryService.getRecent();
            if (!recent.isEmpty()) {
                items.add("── 最近の検索 ──");
                for (SearchCondition c : recent) {
                    items.add("📋 " + searchHistoryService.buildSummary(c));
                }
            }

            // 保存済み検索
            List<SavedSearch> savedSearches = savedSearchService.findAll();
            savedSearchIds.clear();
            if (!savedSearches.isEmpty()) {
                items.add("── 保存済み ──");
                for (SavedSearch ss : savedSearches) {
                    items.add("💾 " + ss.getName());
                    savedSearchIds.add(ss.getId());
                }
            }

            savedSearchCombo.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            log.error("保存済み検索の読み込みに失敗しました", e);
        }
    }

    /**
     * 検索を実行する。
     */
    @FXML
    private void onSearch() {
        log.debug("検索を実行します");
        currentPage = 0;
        currentCondition = buildSearchCondition();
        searchHistoryService.add(currentCondition);
        executeSearch(currentCondition);
        updateSavedSearchCombo();
        saveCurrentState();
    }

    /**
     * 現在のUI状態から検索条件を構築する。
     *
     * @return 検索条件
     */
    private SearchCondition buildSearchCondition() {
        SearchCondition condition = new SearchCondition();
        condition.setKeyword(keywordField.getText());
        condition.setExcludeHidden(excludeHiddenToggle.isSelected());
        condition.setMinStar(resolveMinStar());
        condition.setSortColumn(viewState.getSortColumn());
        condition.setSortOrder(viewState.getSortOrder());
        condition.setPage(currentPage);
        condition.setPageSize(getPageSize());

        // タグフィルター: 選択中のタグIDリスト（複数選択OR検索）
        List<Long> tagIds = getSelectedTagIds();
        if (!tagIds.isEmpty()) {
            condition.setTagIds(tagIds);
        }

        // 作者フィルター: 選択中の作者名からIDを解決
        String selectedAuthor = authorFilterCombo.getValue();
        if (selectedAuthor != null && !"すべての作者".equals(selectedAuthor)) {
            authorService.findByName(selectedAuthor).ifPresent(author ->
                    condition.setAuthorIds(List.of(author.getId())));
        }

        return condition;
    }

    /**
     * Star フィルター ComboBox の選択値から minStar 値を返す。
     *
     * @return minStar 値（0〜5）
     */
    private int resolveMinStar() {
        String selected = starFilterCombo.getValue();
        if (selected == null || "すべて".equals(selected)) return 0;
        if ("★1以上".equals(selected)) return 1;
        if ("★2以上".equals(selected)) return 2;
        if ("★3以上".equals(selected)) return 3;
        if ("★4以上".equals(selected)) return 4;
        if ("★5のみ".equals(selected)) return 5;
        return 0;
    }

    /**
     * 検索を実行してUIを更新する。
     *
     * @param condition 検索条件
     */
    private void executeSearch(SearchCondition condition) {
        try {
            currentSearchResult = searchService.search(condition);
            updateUI(currentSearchResult);
        } catch (Exception e) {
            log.error("検索に失敗しました", e);
            statusMessageLabel.setText("検索に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 検索結果でUIを更新する。
     *
     * @param result 検索結果
     */
    private void updateUI(SearchResult result) {
        totalCountLabel.setText("全 " + String.format("%,d", result.getTotalCount()) + " 件");
        updatePageNavigation();

        if (viewState.getDisplayMode() == MainViewState.DisplayMode.THUMBNAIL) {
            updateThumbnailView(result.getItems());
        } else {
            updateListView(result.getItems());
        }
    }

    /**
     * サムネイルビューを更新する。
     *
     * <p>セッションIDをインクリメントして古い非同期タスクの結果を無効化し、
     * セルを先にプレースホルダーとして配置してからバックグラウンドで画像を読み込む。
     *
     * @param items 表示する画像サマリーリスト
     */
    private void updateThumbnailView(List<ImageSummary> items) {
        thumbnailPane.getChildren().clear();
        // セッションIDを更新して、前の検索結果の読み込みタスクを無効化する
        long sessionId = thumbnailSessionId.incrementAndGet();
        int sizePixels = resolveThumbnailSize();

        for (ImageSummary item : items) {
            VBox cell = createThumbnailCell(item, sizePixels, sessionId);
            thumbnailPane.getChildren().add(cell);
        }
    }

    /**
     * サムネイルセルを作成する。
     *
     * <p>セルを即座にUIに追加し、サムネイルの生成・読み込みはバックグラウンドで行う。
     * sessionId が変わっていた場合（新しい検索が実行された場合）は結果を破棄する。
     *
     * @param item       画像サマリー
     * @param sizePixels サムネイルサイズ
     * @param sessionId  現在のセッションID（古い結果の破棄に使用）
     * @return サムネイルセルのVBox
     */
    private VBox createThumbnailCell(ImageSummary item, int sizePixels, long sessionId) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(sizePixels);
        imageView.setFitHeight(sizePixels);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(item.getFileName());
        nameLabel.getStyleClass().add("thumbnail-label");
        nameLabel.setMaxWidth(sizePixels);

        VBox cell = new VBox(4, imageView, nameLabel);
        cell.getStyleClass().add("thumbnail-cell");
        cell.setPrefWidth(sizePixels + 8);

        // ダブルクリックでビューアを開く
        cell.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && currentSearchResult != null) {
                openViewerWithFullResults(item);
            }
        });

        // 右クリックでコンテキストメニューを表示
        cell.setOnContextMenuRequested(event -> {
            contextMenuTarget = item;
            if (thumbnailContextMenu != null) {
                thumbnailContextMenu.show(cell, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });

        // 画像ファイルのみバックグラウンドでサムネイルを生成・読み込む
        if ("image".equals(item.getMediaType())) {
            thumbnailExecutor.submit(() -> {
                // セッションが変わっていたら（新しい検索が来ていたら）スキップ
                if (thumbnailSessionId.get() != sessionId) {
                    return;
                }
                try {
                    String thumbPath = thumbnailService.getThumbnailPath(
                            item.getFilePath(), sizePixels);
                    // セッションが有効のままであればUIスレッドで反映
                    if (thumbnailSessionId.get() == sessionId) {
                        Platform.runLater(() -> {
                            if (thumbnailSessionId.get() == sessionId) {
                                imageView.setImage(new Image("file:" + thumbPath, true));
                            }
                        });
                    }
                } catch (Exception e) {
                    log.debug("サムネイルの読み込みに失敗しました: {}", item.getFilePath(), e);
                }
            });
        }

        return cell;
    }

    /**
     * リストビューを更新する。
     *
     * @param items 表示する画像サマリーリスト
     */
    private void updateListView(List<ImageSummary> items) {
        imageTableView.setItems(FXCollections.observableArrayList(items));
    }

    /**
     * 現在のサムネイルサイズ設定からピクセル数を返す。
     *
     * @return サムネイルサイズ（ピクセル）
     */
    private int resolveThumbnailSize() {
        String size = thumbnailSizeCombo.getValue();
        if ("小".equals(size)) return AppConfig.THUMBNAIL_SIZE_SMALL;
        if ("大".equals(size)) return AppConfig.THUMBNAIL_SIZE_LARGE;
        return AppConfig.THUMBNAIL_SIZE_MEDIUM;
    }

    /**
     * サムネイル表示に切り替える。
     */
    @FXML
    private void onSwitchToThumbnail() {
        viewState.setDisplayMode(MainViewState.DisplayMode.THUMBNAIL);
        thumbnailScrollPane.setVisible(true);
        imageTableView.setVisible(false);
        listViewButton.setSelected(false);
        thumbnailViewButton.setSelected(true);
        if (currentSearchResult != null) {
            updateThumbnailView(currentSearchResult.getItems());
        }
        saveCurrentState();
    }

    /**
     * リスト表示に切り替える。
     */
    @FXML
    private void onSwitchToList() {
        viewState.setDisplayMode(MainViewState.DisplayMode.LIST);
        thumbnailScrollPane.setVisible(false);
        imageTableView.setVisible(true);
        thumbnailViewButton.setSelected(false);
        listViewButton.setSelected(true);
        if (currentSearchResult != null) {
            updateListView(currentSearchResult.getItems());
        }
        saveCurrentState();
    }

    /**
     * サムネイルサイズが変更された際の処理。
     */
    @FXML
    private void onThumbnailSizeChanged() {
        if (currentSearchResult != null
                && viewState.getDisplayMode() == MainViewState.DisplayMode.THUMBNAIL) {
            updateThumbnailView(currentSearchResult.getItems());
        }
    }

    /**
     * タグフィルターが変更された際の処理（MenuButton版では各CheckMenuItemが直接onSearchを呼ぶため未使用）。
     */
    @FXML
    private void onTagFilterChanged() {
        onSearch();
    }

    /**
     * キーワードフィールドの×ボタンが押された際の処理。
     */
    @FXML
    private void onClearKeyword() {
        keywordField.clear();
        onSearch();
    }

    /**
     * 検索条件クリアボタンが押された際の処理。
     * 全フィルターをデフォルト値にリセットして再検索する。
     */
    @FXML
    private void onClearSearch() {
        keywordField.clear();
        // タグフィルター: 全チェックOFF
        tagFilterButton.getItems().stream()
                .filter(mi -> mi instanceof CheckMenuItem)
                .map(mi -> (CheckMenuItem) mi)
                .forEach(ci -> ci.setSelected(false));
        updateTagFilterLabel();
        authorFilterCombo.setValue("すべての作者");
        starFilterCombo.setValue("すべて");
        excludeHiddenToggle.setSelected(true);
        onSearch();
        statusMessageLabel.setText("検索条件をクリアしました");
    }

    /**
     * 作者フィルターが変更された際の処理。
     */
    @FXML
    private void onAuthorFilterChanged() {
        onSearch();
    }

    /**
     * 非表示除外フラグが変更された際の処理。
     */
    @FXML
    private void onExcludeHiddenChanged() {
        onSearch();
    }

    /**
     * Star フィルターが変更された際の処理。
     */
    @FXML
    private void onStarFilterChanged() {
        onSearch();
    }

    /**
     * 前のページへ移動する。
     */
    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            currentCondition.setPage(currentPage);
            executeSearch(currentCondition);
            thumbnailScrollPane.setVvalue(0);
            saveCurrentState();
        }
    }

    /**
     * 次のページへ移動する。
     */
    @FXML
    private void onNextPage() {
        if (currentSearchResult != null) {
            int totalPages = (int) Math.ceil((double) currentSearchResult.getTotalCount() / getPageSize());
            if (currentPage < totalPages - 1) {
                currentPage++;
                currentCondition.setPage(currentPage);
                executeSearch(currentCondition);
                thumbnailScrollPane.setVvalue(0);
                saveCurrentState();
            }
        }
    }

    /**
     * ページサイズが変更された際の処理。
     */
    @FXML
    private void onPageSizeChanged() {
        currentPage = 0;
        onSearch();
    }

    /**
     * ページナビゲーションUIを更新する。
     */
    private void updatePageNavigation() {
        if (currentSearchResult == null) return;
        long totalCount = currentSearchResult.getTotalCount();
        int pageSize = getPageSize();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));

        pageInfoLabel.setText(String.format("ページ %d / %d", currentPage + 1, totalPages));
        prevPageButton.setDisable(currentPage <= 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    /**
     * 現在選択中のページサイズを返す。
     *
     * @return ページサイズ（200/500/1000）
     */
    private int getPageSize() {
        String selected = pageSizeCombo.getValue();
        if ("500".equals(selected)) return 500;
        if ("1000".equals(selected)) return 1000;
        return 200;
    }

    /**
     * 現在の検索条件・表示状態を即時保存する。
     *
     * <p>異常終了時でも状態が失われないよう、状態変更のたびに呼び出す。
     */
    private void saveCurrentState() {
        AppConfig config = AppConfig.getInstance();
        java.util.Map<String, String> states = new java.util.LinkedHashMap<>();
        states.put(AppConfig.KEY_STATE_KEYWORD, keywordField.getText());
        // タグフィルター: 選択中のIDをカンマ区切りで保存
        List<Long> tagIds = getSelectedTagIds();
        states.put(AppConfig.KEY_STATE_TAG_IDS,
                tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        states.put(AppConfig.KEY_STATE_AUTHOR_FILTER,
                authorFilterCombo.getValue() != null ? authorFilterCombo.getValue() : "すべての作者");
        states.put(AppConfig.KEY_STATE_STAR_FILTER,
                starFilterCombo.getValue() != null ? starFilterCombo.getValue() : "すべて");
        states.put(AppConfig.KEY_STATE_EXCLUDE_HIDDEN, String.valueOf(excludeHiddenToggle.isSelected()));
        states.put(AppConfig.KEY_STATE_CURRENT_PAGE, String.valueOf(currentPage));
        states.put(AppConfig.KEY_STATE_PAGE_SIZE,
                pageSizeCombo.getValue() != null ? pageSizeCombo.getValue() : "200");
        states.put(AppConfig.KEY_STATE_DISPLAY_MODE, viewState.getDisplayMode().name());
        states.put(AppConfig.KEY_STATE_THUMBNAIL_SIZE,
                thumbnailSizeCombo.getValue() != null ? thumbnailSizeCombo.getValue() : "中");
        // SplitPane デバイダー位置
        if (mainSplitPane.getDividers().size() > 0) {
            states.put("state.split_divider",
                    String.valueOf(mainSplitPane.getDividerPositions()[0]));
        }
        config.setStates(states);
    }

    /**
     * 保存された状態をUIに復元する。
     *
     * <p>起動時に {@code initialize()} から呼び出す。
     */
    private void restoreState() {
        AppConfig config = AppConfig.getInstance();
        try {
            String keyword = config.getState(AppConfig.KEY_STATE_KEYWORD, "");
            keywordField.setText(keyword);

            // タグフィルター復元: 保存されたIDリストでCheckMenuItemを選択
            String tagIdsStr = config.getState(AppConfig.KEY_STATE_TAG_IDS, "");
            if (!tagIdsStr.isEmpty()) {
                List<Long> savedTagIds = java.util.Arrays.stream(tagIdsStr.split(","))
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                tagFilterButton.getItems().stream()
                        .filter(mi -> mi instanceof CheckMenuItem && mi.getUserData() != null)
                        .map(mi -> (CheckMenuItem) mi)
                        .forEach(ci -> ci.setSelected(savedTagIds.contains((Long) ci.getUserData())));
                updateTagFilterLabel();
            }

            String authorFilter = config.getState(AppConfig.KEY_STATE_AUTHOR_FILTER, "すべての作者");
            if (authorFilterCombo.getItems().contains(authorFilter)) {
                authorFilterCombo.setValue(authorFilter);
            }

            String starFilter = config.getState(AppConfig.KEY_STATE_STAR_FILTER, "すべて");
            starFilterCombo.setValue(starFilter);

            boolean excludeHidden = Boolean.parseBoolean(
                    config.getState(AppConfig.KEY_STATE_EXCLUDE_HIDDEN, "true"));
            excludeHiddenToggle.setSelected(excludeHidden);

            currentPage = Integer.parseInt(
                    config.getState(AppConfig.KEY_STATE_CURRENT_PAGE, "0"));

            String pageSize = config.getState(AppConfig.KEY_STATE_PAGE_SIZE, "200");
            pageSizeCombo.setValue(pageSize);

            String displayMode = config.getState(AppConfig.KEY_STATE_DISPLAY_MODE, "THUMBNAIL");
            if ("LIST".equals(displayMode)) {
                viewState.setDisplayMode(MainViewState.DisplayMode.LIST);
                thumbnailScrollPane.setVisible(false);
                imageTableView.setVisible(true);
                listViewButton.setSelected(true);
                thumbnailViewButton.setSelected(false);
            }

            String thumbSize = config.getState(AppConfig.KEY_STATE_THUMBNAIL_SIZE, "中");
            thumbnailSizeCombo.setValue(thumbSize);

            // SplitPane デバイダー位置復元
            String dividerStr = config.getState("state.split_divider", "0.15");
            try {
                double divider = Double.parseDouble(dividerStr);
                mainSplitPane.setDividerPositions(divider);
            } catch (NumberFormatException ignored) {
            }

            log.info("アプリ状態を復元しました: keyword={}, page={}", keyword, currentPage);
        } catch (Exception e) {
            log.warn("状態の復元に失敗しました（デフォルト値を使用します）", e);
        }
    }

    /**
     * 検索条件を保存する。
     */
    @FXML
    private void onSaveSearch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("検索条件の保存");
        dialog.setHeaderText("保存名を入力してください");
        dialog.setContentText("名前:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    savedSearchService.save(name, currentCondition);
                    loadSavedSearches();
                    statusMessageLabel.setText("検索条件を保存しました: " + name);
                } catch (Exception e) {
                    log.error("検索条件の保存に失敗しました", e);
                    showError("保存に失敗しました", e.getMessage());
                }
            }
        });
    }

    /**
     * 保存済み検索を読み込んで適用する。
     */
    @FXML
    private void onLoadSavedSearch() {
        String selected = savedSearchCombo.getValue();
        if (selected == null) return;

        // セパレーター行は無視
        if (selected.startsWith("──")) return;

        if (selected.startsWith("📋 ")) {
            // 最近の検索を復元
            List<SearchCondition> recent = searchHistoryService.getRecent();
            // ComboBox内の📋アイテムのインデックスを計算
            int recentIndex = 0;
            for (int i = 0; i < savedSearchCombo.getItems().size(); i++) {
                String item = savedSearchCombo.getItems().get(i);
                if (item.equals(selected)) {
                    // "── 最近の検索 ──" の後のインデックスを計算
                    recentIndex = i - 1; // ヘッダー行を引く
                    break;
                }
            }
            if (recentIndex >= 0 && recentIndex < recent.size()) {
                applySearchCondition(recent.get(recentIndex));
            }
        } else if (selected.startsWith("💾 ")) {
            // 保存済み検索を復元
            String name = selected.substring(2).trim();
            for (int i = 0; i < savedSearchIds.size(); i++) {
                try {
                    SearchCondition condition = savedSearchService.toSearchCondition(savedSearchIds.get(i));
                    currentCondition = condition;
                    keywordField.setText(condition.getKeyword() != null ? condition.getKeyword() : "");
                    excludeHiddenToggle.setSelected(condition.isExcludeHidden());
                    currentPage = 0;
                    executeSearch(condition);
                    break;
                } catch (Exception e) {
                    log.error("保存済み検索の読み込みに失敗しました", e);
                }
            }
        }
    }

    /**
     * 検索条件をUIに反映して再検索する。
     *
     * @param condition 適用する検索条件
     */
    private void applySearchCondition(SearchCondition condition) {
        keywordField.setText(condition.getKeyword() != null ? condition.getKeyword() : "");
        excludeHiddenToggle.setSelected(condition.isExcludeHidden());
        // Star フィルター復元
        if (condition.getMinStar() > 0) {
            starFilterCombo.setValue("★" + condition.getMinStar() + "以上");
        } else {
            starFilterCombo.setValue("すべて");
        }
        currentCondition = condition;
        currentPage = 0;
        executeSearch(condition);
        saveCurrentState();
    }

    /**
     * タグツリーがクリックされた際の処理。
     */
    @FXML
    private void onTagTreeClicked() {
        TreeItem<String> selected = tagTreeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            // Star前置を除去してタグ名を抽出
            String label = selected.getValue();
            String tagName = label.replaceFirst("^★+ ", "");
            keywordField.setText(tagName);
            onSearch();
        }
    }

    /**
     * インポートダイアログを開く。
     */
    @FXML
    private void onImport() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/import.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("フォルダのインポート");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // インポート後に再検索
            onSearch();
            loadTagTree();
            loadAuthorFilter();
            loadTagFilter();
        } catch (Exception e) {
            log.error("インポートダイアログの表示に失敗しました", e);
            showError("インポートダイアログを開けませんでした", e.getMessage());
        }
    }

    /**
     * PDF出力ダイアログを開く。
     */
    @FXML
    private void onPdfExport() {
        if (currentSearchResult == null || currentSearchResult.getItems().isEmpty()) {
            showInfo("PDF出力", "出力対象の画像がありません。先に検索を実行してください。");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/pdf-export.fxml"));
            Parent root = loader.load();
            PdfExportController controller = loader.getController();
            // 現在の検索結果の画像IDを渡す
            List<Long> imageIds = currentSearchResult.getItems().stream()
                    .map(ImageSummary::getId)
                    .collect(java.util.stream.Collectors.toList());
            controller.setImageIds(imageIds);
            Stage stage = new Stage();
            stage.setTitle("PDF出力");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            log.error("PDF出力ダイアログの表示に失敗しました", e);
            showError("PDF出力ダイアログを開けませんでした", e.getMessage());
        }
    }

    /**
     * 保存済み検索管理ダイアログを開く。
     */
    @FXML
    private void onManageSavedSearches() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/saved-search.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("保存済み検索の管理");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadSavedSearches();
        } catch (Exception e) {
            log.error("保存済み検索管理ダイアログの表示に失敗しました", e);
            showError("ダイアログを開けませんでした", e.getMessage());
        }
    }

    /**
     * 設定ダイアログを開く。
     */
    @FXML
    private void onSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("設定");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            log.error("設定ダイアログの表示に失敗しました", e);
            showError("設定ダイアログを開けませんでした", e.getMessage());
        }
    }

    /**
     * エラーダイアログを表示する。
     *
     * @param title   タイトル
     * @param message メッセージ
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 情報ダイアログを表示する。
     *
     * @param title   タイトル
     * @param message メッセージ
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
