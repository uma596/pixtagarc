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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

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

    /** タグフィルターComboBox。 */
    @FXML private ComboBox<String> tagFilterCombo;

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

    /** 表示範囲ラベル。 */
    @FXML private Label displayRangeLabel;

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

    // ===== 状態 =====

    /** メイン画面の表示状態。 */
    private MainViewState viewState = new MainViewState();

    /** 現在の検索結果。 */
    private SearchResult currentSearchResult;

    /** 現在の検索条件。 */
    private SearchCondition currentCondition = new SearchCondition();

    /** 保存済み検索のIDリスト（ComboBoxのインデックスと対応）。 */
    private List<Long> savedSearchIds = new ArrayList<>();

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
    }

    /**
     * UIコンポーネントを初期化する。
     */
    private void initializeUI() {
        // サムネイルサイズComboBoxの初期値
        thumbnailSizeCombo.setItems(FXCollections.observableArrayList("小", "中", "大"));
        thumbnailSizeCombo.setValue("中");

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
                    int index = currentSearchResult.getItems().indexOf(selected);
                    ViewerController.openNewWindow(currentSearchResult.getItems(), index);
                }
            }
        });

        // タグツリーのルートノード設定
        TreeItem<String> root = new TreeItem<>("タグ");
        root.setExpanded(true);
        tagTreeView.setRoot(root);
        tagTreeView.setShowRoot(false);
    }

    /**
     * 初期データを読み込む。
     */
    private void loadInitialData() {
        loadTagTree();
        loadAuthorFilter();
        loadTagFilter();
        loadSavedSearches();
        // 初期検索（全件）
        onSearch();
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
                TreeItem<String> item = new TreeItem<>(tag.getName());
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
     * タグフィルターComboBoxを読み込む。
     */
    private void loadTagFilter() {
        try {
            List<Tag> tags = tagService.findAll();
            List<String> items = new ArrayList<>();
            items.add("すべてのタグ");
            tags.forEach(t -> items.add(t.getName()));
            tagFilterCombo.setItems(FXCollections.observableArrayList(items));
            tagFilterCombo.setValue("すべてのタグ");
        } catch (Exception e) {
            log.error("タグフィルターの読み込みに失敗しました", e);
        }
    }

    /**
     * 保存済み検索ComboBoxを読み込む。
     */
    private void loadSavedSearches() {
        try {
            List<SavedSearch> savedSearches = savedSearchService.findAll();
            List<String> names = new ArrayList<>();
            savedSearchIds.clear();
            for (SavedSearch ss : savedSearches) {
                names.add(ss.getName());
                savedSearchIds.add(ss.getId());
            }
            savedSearchCombo.setItems(FXCollections.observableArrayList(names));
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
        currentCondition = buildSearchCondition();
        executeSearch(currentCondition);
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
        // AppConfigから検索上限件数を取得（設定変更がリアルタイムに反映される）
        condition.setPageSize(AppConfig.getInstance().getSearchLimit());
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
        totalCountLabel.setText("件数: " + String.format("%,d", result.getTotalCount()) + " 件");
        displayRangeLabel.setText("表示: " + result.getItems().size() + " 件");

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
                int index = currentSearchResult.getItems().indexOf(item);
                ViewerController.openNewWindow(currentSearchResult.getItems(), index);
            }
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
     * タグフィルターが変更された際の処理。
     */
    @FXML
    private void onTagFilterChanged() {
        onSearch();
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
        int index = savedSearchCombo.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < savedSearchIds.size()) {
            try {
                SearchCondition condition = savedSearchService.toSearchCondition(savedSearchIds.get(index));
                currentCondition = condition;
                keywordField.setText(condition.getKeyword() != null ? condition.getKeyword() : "");
                excludeHiddenToggle.setSelected(condition.isExcludeHidden());
                executeSearch(condition);
            } catch (Exception e) {
                log.error("保存済み検索の読み込みに失敗しました", e);
                showError("読み込みに失敗しました", e.getMessage());
            }
        }
    }

    /**
     * タグツリーがクリックされた際の処理。
     */
    @FXML
    private void onTagTreeClicked() {
        TreeItem<String> selected = tagTreeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            keywordField.setText(selected.getValue());
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
