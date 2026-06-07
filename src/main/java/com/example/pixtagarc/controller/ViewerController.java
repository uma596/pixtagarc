package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.config.VlcConfig;
import com.example.pixtagarc.domain.Tag;
import com.example.pixtagarc.dto.ImageSummary;
import com.example.pixtagarc.dto.ViewerState;
import com.example.pixtagarc.repository.AuthorRepository;
import com.example.pixtagarc.repository.ImageRepository;
import com.example.pixtagarc.repository.ImageTagRepository;
import com.example.pixtagarc.repository.TagRepository;
import com.example.pixtagarc.service.ImageService;
import com.example.pixtagarc.service.TagService;
import com.example.pixtagarc.service.VideoPlayerService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ビューア画面のコントローラークラス。
 *
 * <p>画像・動画の表示を管理する。
 * 単ページ/見開き表示の切替、ページ送り、オフセット調整、
 * タグ編集、作者変更、非表示フラグの切替などを担当する。
 * 複数のビューアウィンドウを同時に開くことができ、
 * 各ウィンドウは独立した {@link ViewerState} を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ViewerController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ViewerController.class);

    // ===== FXML注入フィールド =====

    /** 前ページボタン。 */
    @FXML private Button prevButton;

    /** 次ページボタン。 */
    @FXML private Button nextButton;

    /** ファイル名ラベル。 */
    @FXML private Label fileNameLabel;

    /** ページインデックスラベル。 */
    @FXML private Label pageIndexLabel;

    /** 単ページToggleButton。 */
    @FXML private ToggleButton singlePageToggle;

    /** 見開きToggleButton。 */
    @FXML private ToggleButton spreadPageToggle;

    /** 右綴じToggleButton。 */
    @FXML private ToggleButton rightToLeftToggle;

    /** 左綴じToggleButton。 */
    @FXML private ToggleButton leftToRightToggle;

    /** オフセット減少ボタン。 */
    @FXML private Button offsetMinusButton;

    /** オフセットラベル。 */
    @FXML private Label offsetLabel;

    /** オフセット増加ボタン。 */
    @FXML private Button offsetPlusButton;

    /** 閉じるボタン。 */
    @FXML private Button closeButton;

    /** 画像表示エリアStackPane。 */
    @FXML private StackPane imageAreaPane;

    /** 単ページ表示ImageView。 */
    @FXML private ImageView singleImageView;

    /** 見開き表示HBox。 */
    @FXML private HBox spreadPane;

    /** 見開き左ImageView。 */
    @FXML private ImageView leftImageView;

    /** 見開き右ImageView。 */
    @FXML private ImageView rightImageView;

    /** 動画再生Pane。 */
    @FXML private Pane videoPane;

    /** タグチップHBox。 */
    @FXML private HBox tagChipsPane;

    /** タグ追加ボタン。 */
    @FXML private Button addTagButton;

    /** Star 表示・設定 HBox。 */
    @FXML private HBox starPane;

    /** 作者ComboBox。 */
    @FXML private ComboBox<String> authorCombo;

    /** ファイルパスラベル。 */
    @FXML private Label filePathLabel;

    /** 解像度ラベル。 */
    @FXML private Label resolutionLabel;

    /** トップ情報バー（表示/非表示切替対象）。 */
    @FXML private VBox topInfoBar;

    /** ボトム情報バー（表示/非表示切替対象）。 */
    @FXML private VBox bottomInfoBar;

    /** 読み込み中スピナー。 */
    @FXML private ProgressIndicator loadingSpinner;

    /** VLCJ無効時プレースホルダー。 */
    @FXML private VBox videoUnavailablePane;

    // ===== サービス =====

    /** 画像サービス。 */
    private ImageService imageService;

    /** タグサービス。 */
    private TagService tagService;

    /** 動画再生サービス（VLC未インストール時はnull）。 */
    private VideoPlayerService videoPlayerService;

    // ===== 状態 =====

    /** このビューアウィンドウの表示状態。 */
    private ViewerState viewerState;

    /** 先読みキャッシュ（LRU方式、最大21枚）。 */
    private final java.util.LinkedHashMap<Integer, Image> prefetchCache =
            new java.util.LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Image> eldest) {
                    return size() > 21;
                }
            };

    /** 先読み範囲（±N ページ）。 */
    private static final int PREFETCH_RANGE = 10;

    /** ページ送りデバウンスタイマー。 */
    private final PauseTransition pageDebounce = new PauseTransition(Duration.millis(50));

    /** デバウンス中の送信先インデックス。 */
    private int pendingIndex = -1;

    /** 情報バーの表示フラグ。 */
    private boolean infoBarVisible = false;

    /** タグ変更時にメイン画面に通知するコールバック。 */
    private Runnable onTagChangedCallback;

    /** タグ使用履歴（メイン画面と共有）。 */
    private com.example.pixtagarc.service.TagHistory tagHistory;

    /** このビューアのセッション情報（状態復元用）。 */
    private com.example.pixtagarc.dto.ViewerSession currentSession;

    /**
     * コントローラーを初期化する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("ViewerControllerを初期化します");
        initializeServices();
        setupImageFitBindings();
        setupKeyboardShortcuts();
        setupScrollNavigation();
        setupClickNavigation();
        setupContextMenu();
        setupInfoBarToggle();
        setupMouseTracking();
        setupSwipeNavigation();
    }

    /**
     * サービスを初期化する。
     */
    private void initializeServices() {
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ImageRepository imageRepository = new ImageRepository(dbConfig);
        TagRepository tagRepository = new TagRepository(dbConfig);
        ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);
        AuthorRepository authorRepository = new AuthorRepository(dbConfig);

        imageService = new ImageService(imageRepository);
        tagService = new TagService(tagRepository, imageTagRepository);

        // タグ使用履歴の初期化
        tagHistory = new com.example.pixtagarc.service.TagHistory();
        tagHistory.loadFrom(com.example.pixtagarc.config.AppConfig.getInstance()
                .getState(com.example.pixtagarc.config.AppConfig.KEY_TAG_HISTORY, ""));

        // VLCが利用可能な場合のみVideoPlayerServiceを初期化
        if (VlcConfig.isVlcAvailable()) {
            try {
                videoPlayerService = new VideoPlayerService();
            } catch (Exception e) {
                log.warn("VideoPlayerServiceの初期化に失敗しました。動画再生は無効化されます。", e);
            }
        }
    }

    /**
     * ImageViewのサイズを表示エリアにバインドする。
     *
     * <p>ウィンドウリサイズ時にリアルタイムで画像サイズが追従する。
     * アスペクト比は preserveRatio=true で維持される。
     */
    private void setupImageFitBindings() {
        // 単ページ: 表示エリア全体にフィット（マージン20px）
        singleImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().subtract(20));
        singleImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));

        // 見開き: 左右それぞれ表示エリアの半分にフィット
        leftImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().divide(2).subtract(20));
        leftImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));
        rightImageView.fitWidthProperty().bind(imageAreaPane.widthProperty().divide(2).subtract(20));
        rightImageView.fitHeightProperty().bind(imageAreaPane.heightProperty().subtract(20));
    }

    /**
     * キーボードショートカットを設定する。
     */
    private void setupKeyboardShortcuts() {
        // ウィンドウ表示後にシーンが設定されてからキーイベントを登録する
        imageAreaPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.isControlDown()) {
                        // Ctrl+Arrow: 作品ジャンプ (#16)
                        switch (event.getCode()) {
                            case RIGHT -> { nextWork(); event.consume(); }
                            case LEFT -> { prevWork(); event.consume(); }
                            default -> {}
                        }
                    } else {
                        switch (event.getCode()) {
                            case LEFT, UP -> onPrev();
                            case RIGHT, DOWN -> onNext();
                            case I -> toggleInfoBar();
                            default -> { /* 他のキーは無視 */ }
                        }
                    }
                });
            }
        });
    }

    /**
     * マウススクロールによるページ送りを設定する。
     *
     * <p>下スクロールで次ページ、上スクロールで前ページに移動する。
     * 動画再生中はスクロールによるページ送りを無効化する。
     */
    private void setupScrollNavigation() {
        imageAreaPane.setOnScroll(event -> {
            if (viewerState == null) return;
            // 動画再生中はスクロールでページ送りしない
            ImageSummary current = viewerState.getCurrentImage();
            if (current != null && "video".equals(current.getMediaType())) {
                return;
            }
            if (event.getDeltaY() < 0) {
                onNext();
            } else if (event.getDeltaY() > 0) {
                onPrev();
            }
            event.consume();
        });
    }

    /**
     * クリックによるページ送りを設定する。
     *
     * <p>画像表示領域の左半分/右半分をクリックすることでページ送り/戻りを行う。
     * 綴じ方向に応じてクリック位置と送り方向の対応が変わる。
     * 中央10%はデッドゾーンとして誤操作を防止する。
     */
    private void setupClickNavigation() {
        imageAreaPane.setOnMouseClicked(event -> {
            if (viewerState == null) return;
            // 左クリックのみ（右クリックはコンテキストメニュー用）
            if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            // ダブルクリックは無視
            if (event.getClickCount() != 1) return;

            double areaWidth = imageAreaPane.getWidth();
            double clickX = event.getX();
            double relativeX = clickX / areaWidth;

            // 中央10%はデッドゾーン（誤操作防止）
            if (relativeX > 0.45 && relativeX < 0.55) return;

            boolean clickedLeft = relativeX <= 0.45;

            // 綴じ方向に応じてページ送り方向を決定
            if (viewerState.getBindingDirection() == ViewerState.BindingDirection.RIGHT_TO_LEFT) {
                // 右綴じ: 左クリック=次ページ、右エリアクリック=前ページ
                if (clickedLeft) onNext(); else onPrev();
            } else {
                // 左綴じ: 左クリック=前ページ、右エリアクリック=次ページ
                if (clickedLeft) onPrev(); else onNext();
            }
        });
    }

    /**
     * コンテキストメニューを設定する。
     *
     * <p>画像表示エリアの右クリックで操作メニューを表示する。
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // タグを追加サブメニュー (#36)
        Menu addTagMenu = new Menu("タグを追加");

        MenuItem editTags = new MenuItem("タグを編集...");
        editTags.setOnAction(e -> onAddTag());

        MenuItem toggleHidden = new MenuItem("非表示にする");
        toggleHidden.setOnAction(e -> onToggleHidden());

        MenuItem copyPath = new MenuItem("ファイルパスをコピー");
        copyPath.setOnAction(e -> {
            ImageSummary current = getContextMenuTarget();
            if (current != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(current.getFilePath());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        MenuItem openInExplorer = new MenuItem("エクスプローラーで表示");
        openInExplorer.setOnAction(e -> {
            ImageSummary current = getContextMenuTarget();
            if (current != null) {
                try {
                    File file = new File(current.getFilePath());
                    if (file.exists()) {
                        // OSのファイルマネージャで親ディレクトリを開く
                        Desktop.getDesktop().open(file.getParentFile());
                    }
                } catch (Exception ex) {
                    log.error("エクスプローラーの起動に失敗しました", ex);
                }
            }
        });

        MenuItem deleteImage = new MenuItem("画像をDBから削除...");
        deleteImage.setOnAction(e -> onDeleteImage());

        // 「作品を表示」メニュー（work_id != NULL の場合のみ有効）
        MenuItem openWork = new MenuItem("作品を表示");
        openWork.setOnAction(e -> onOpenWork());

        // Star 設定サブメニュー
        Menu starMenu = new Menu("Star を設定");
        String[] starLabels = {"☆ なし", "★ 1", "★★ 2", "★★★ 3", "★★★★ 4", "★★★★★ 5"};
        for (int i = 0; i <= 5; i++) {
            final int starValue = i;
            MenuItem starItem = new MenuItem(starLabels[i]);
            starItem.setOnAction(e -> {
                ImageSummary current = getContextMenuTarget();
                if (current != null) {
                    try {
                        imageService.updateStar(current.getId(), starValue);
                        current.setStar(starValue);
                        updateInfoBar(current);
                        log.info("Star評価を更新しました: id={}, star={}", current.getId(), starValue);
                    } catch (Exception ex) {
                        log.error("Star評価の更新に失敗しました", ex);
                    }
                }
            });
            starMenu.getItems().add(starItem);
        }

        // 情報バー表示/非表示切替 (#34)
        CheckMenuItem showInfoBarItem = new CheckMenuItem("情報バーを表示");
        showInfoBarItem.setSelected(infoBarVisible);
        showInfoBarItem.setOnAction(e -> toggleInfoBar());

        // 見開き/単ページ切替
        CheckMenuItem spreadToggleItem = new CheckMenuItem("見開き表示");
        spreadToggleItem.setOnAction(e -> {
            if (viewerState == null) return;
            if (spreadToggleItem.isSelected()) {
                viewerState.setDisplayMode(ViewerState.DisplayMode.SPREAD);
                singlePageToggle.setSelected(false);
                spreadPageToggle.setSelected(true);
            } else {
                viewerState.setDisplayMode(ViewerState.DisplayMode.SINGLE);
                singlePageToggle.setSelected(true);
                spreadPageToggle.setSelected(false);
            }
            updateDisplay();
        });

        contextMenu.getItems().addAll(
                addTagMenu,
                editTags,
                toggleHidden,
                starMenu,
                new SeparatorMenuItem(),
                openWork,
                new SeparatorMenuItem(),
                copyPath,
                openInExplorer,
                new SeparatorMenuItem(),
                spreadToggleItem,
                showInfoBarItem,
                new SeparatorMenuItem(),
                deleteImage
        );

        imageAreaPane.setOnContextMenuRequested(event -> {
            // 見開き表示時はクリック位置から操作対象を判定
            ImageSummary current;
            if (viewerState != null && viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) {
                // imageAreaPane内での相対X座標を計算して左右を判定
                javafx.geometry.Bounds boundsInScreen = imageAreaPane.localToScreen(imageAreaPane.getBoundsInLocal());
                double localX = event.getScreenX() - boundsInScreen.getMinX();
                double midX = imageAreaPane.getWidth() / 2.0;
                InfoSide clickSide = (localX < midX) ? InfoSide.LEFT : InfoSide.RIGHT;
                activeSide = clickSide;
                current = getSpreadImageForSide(clickSide);
                log.debug("コンテキストメニュー左右判定: localX={}, midX={}, side={}, target={}",
                        localX, midX, clickSide,
                        current != null ? current.getFileName() : "null");
            } else {
                current = viewerState != null ? viewerState.getCurrentImage() : null;
            }
            if (current != null) {
                // 非表示フラグの状態でラベルを動的変更
                toggleHidden.setText(current.isHidden() ? "非表示を解除" : "非表示にする");
                // 作品未所属の場合はグレーアウト
                openWork.setDisable(current.getWorkId() == null);
                // タグ追加サブメニューを動的に再構築 (#36)
                rebuildViewerAddTagMenu(addTagMenu, current);
                // 情報バーも同期
                updateInfoBar(current);
            }
            // 情報バー・見開き状態を同期
            showInfoBarItem.setSelected(infoBarVisible);
            spreadToggleItem.setSelected(viewerState != null
                    && viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD);
            contextMenu.show(imageAreaPane, event.getScreenX(), event.getScreenY());
        });

        // 左クリック時はコンテキストメニューを閉じる
        imageAreaPane.setOnMousePressed(event -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }

    /**
     * 作品を新しいビューアウィンドウで表示する。
     *
     * <p>現在表示中の画像が所属する作品の全ページを page_number 昇順で取得し、
     * 新しいビューアウィンドウで開く。
     */
    private void onOpenWork() {
        ImageSummary current = getContextMenuTarget();
        if (current == null || current.getWorkId() == null) return;

        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            ImageRepository imageRepository = new ImageRepository(dbConfig);
            List<com.example.pixtagarc.domain.Image> workImages =
                    imageRepository.findByWorkId(current.getWorkId());

            // Image エンティティを ImageSummary に変換
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
                if (img.getId().equals(current.getId())) {
                    currentPageIndex = i;
                }
            }

            if (!summaryList.isEmpty()) {
                openNewWindow(summaryList, currentPageIndex);
                log.info("作品を表示しました: workId={}, pages={}", current.getWorkId(), summaryList.size());
            }
        } catch (Exception ex) {
            log.error("作品の表示に失敗しました", ex);
        }
    }

    /**
     * 画像をDBから削除する（確認ダイアログ付き）。
     *
     * <p>ファイル自体は削除せず、DBレコードのみ削除する。
     */
    private void onDeleteImage() {
        ImageSummary current = getContextMenuTarget();
        if (current == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("画像の削除");
        confirm.setHeaderText("画像をDBから削除しますか？");
        confirm.setContentText("「" + current.getFileName() + "」をDBから削除します。\n"
                + "ファイル自体は削除されません。");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseConfig dbConfig = DatabaseConfig.getInstance();
                    ImageRepository imageRepository = new ImageRepository(dbConfig);
                    imageRepository.deleteById(current.getId());
                    // リストから削除して次の画像に移動
                    viewerState.getImageList().remove(viewerState.getCurrentIndex());
                    if (viewerState.getCurrentIndex() >= viewerState.getImageList().size()) {
                        viewerState.setCurrentIndex(Math.max(0, viewerState.getImageList().size() - 1));
                    }
                    if (viewerState.getImageList().isEmpty()) {
                        onClose();
                    } else {
                        updateDisplay();
                    }
                    log.info("画像をDBから削除しました: id={}", current.getId());
                } catch (Exception ex) {
                    log.error("画像の削除に失敗しました", ex);
                }
            }
        });
    }

    /**
     * ビューア状態を設定して表示を更新する。
     *
     * @param state ビューア状態
     */
    public void setViewerState(ViewerState state) {
        this.viewerState = state;
        // 表示モードのトグルボタンを同期
        if (state != null) {
            boolean isSpread = (state.getDisplayMode() == ViewerState.DisplayMode.SPREAD);
            singlePageToggle.setSelected(!isSpread);
            spreadPageToggle.setSelected(isSpread);
        }
        updateDisplay();
    }

    /**
     * 現在のビューア状態に基づいて表示を更新する。
     */
    private void updateDisplay() {
        if (viewerState == null || viewerState.getImageList().isEmpty()) {
            return;
        }

        ImageSummary current = viewerState.getCurrentImage();
        if (current == null) return;

        // ツールバーの更新
        fileNameLabel.setText(current.getFileName());
        pageIndexLabel.setText(String.format("(%d / %d)",
                viewerState.getCurrentIndex() + 1, viewerState.getTotalCount()));
        offsetLabel.setText(String.valueOf(viewerState.getPageOffset()));

        // 前後ボタンの有効/無効
        prevButton.setDisable(!viewerState.hasPrevious());
        nextButton.setDisable(!viewerState.hasNext());

        // 表示モードに応じて表示を切り替え
        if (viewerState.getDisplayMode() == ViewerState.DisplayMode.SINGLE) {
            showSinglePage(current);
        } else {
            showSpreadPage();
        }

        // 情報バーの更新（見開き時はactiveSide側の画像情報を表示）
        if (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) {
            ImageSummary sideImage = getSpreadImageForSide(activeSide);
            updateInfoBar(sideImage != null ? sideImage : current);
        } else {
            updateInfoBar(current);
        }

        // 先読み更新
        prefetchPages(viewerState.getCurrentIndex());
    }

    /**
     * 単ページ表示を行う。
     *
     * @param image 表示する画像サマリー
     */
    private void showSinglePage(ImageSummary image) {
        singleImageView.setVisible(true);
        spreadPane.setVisible(false);
        videoPane.setVisible(false);
        videoUnavailablePane.setVisible(false);

        if ("video".equals(image.getMediaType())) {
            // 動画の場合はVLCJで再生
            showVideo(image);
        } else {
            // 画像の場合はImageViewで表示
            loadImage(singleImageView, image.getFilePath());
        }
    }

    /**
     * 見開き表示を行う。
     */
    private void showSpreadPage() {
        singleImageView.setVisible(false);
        spreadPane.setVisible(true);
        videoPane.setVisible(false);
        videoUnavailablePane.setVisible(false);

        int index = viewerState.getCurrentIndex();
        int offset = viewerState.getPageOffset();
        List<ImageSummary> imageList = viewerState.getImageList();

        // オフセットを考慮したページインデックスを計算
        int adjustedIndex = index + offset;

        // 右綴じ/左綴じに応じてページを配置
        if (viewerState.getBindingDirection() == ViewerState.BindingDirection.RIGHT_TO_LEFT) {
            // 右綴じ: 右ページが先、左ページが次
            ImageSummary rightImage = getImageAt(imageList, adjustedIndex);
            ImageSummary leftImage = getImageAt(imageList, adjustedIndex + 1);
            loadImageOrClear(rightImageView, rightImage);
            loadImageOrClear(leftImageView, leftImage);
        } else {
            // 左綴じ: 左ページが先、右ページが次
            ImageSummary leftImage = getImageAt(imageList, adjustedIndex);
            ImageSummary rightImage = getImageAt(imageList, adjustedIndex + 1);
            loadImageOrClear(leftImageView, leftImage);
            loadImageOrClear(rightImageView, rightImage);
        }
    }

    /**
     * 動画を表示する。
     *
     * <p>VLCJが利用可能な場合は動画を再生し、利用不可の場合は×アイコンを表示する。
     *
     * @param image 表示する動画サマリー
     */
    private void showVideo(ImageSummary image) {
        videoUnavailablePane.setVisible(false);
        if (videoPlayerService != null) {
            singleImageView.setVisible(false);
            videoPane.setVisible(true);
            videoPlayerService.play(image.getFilePath());
        } else {
            log.warn("VLCが利用できないため動画を再生できません: {}", image.getFilePath());
            singleImageView.setVisible(false);
            videoPane.setVisible(false);
            videoUnavailablePane.setVisible(true);
        }
    }

    /**
     * ImageViewに画像を読み込む（先読みキャッシュ経由、ダブルバッファリング）。
     *
     * <p>キャッシュにヒットすれば即座に切り替え、ミスの場合は前の画像を保持したまま
     * バックグラウンドで読み込み完了を待つ。
     *
     * @param imageView 読み込み先ImageView
     * @param filePath  画像ファイルパス
     */
    private void loadImage(ImageView imageView, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return;

            // キャッシュからファイルパスで検索
            Image cached = findCachedImage(filePath);

            if (cached != null && cached.getProgress() >= 1.0) {
                // キャッシュヒット → 即座に差し替え（ちらつきなし）
                loadingSpinner.setVisible(false);
                imageView.setImage(cached);
            } else {
                // キャッシュミス → 前の画像を保持したままロード
                double w = imageAreaPane.getWidth() > 0 ? imageAreaPane.getWidth() : 1920;
                double h = imageAreaPane.getHeight() > 0 ? imageAreaPane.getHeight() : 1080;
                Image loading = (cached != null) ? cached
                        : new Image("file:" + filePath, w, h, true, true, true);

                // 500ms後にまだロード中ならスピナー表示
                PauseTransition spinnerDelay = new PauseTransition(Duration.millis(500));
                spinnerDelay.setOnFinished(e -> {
                    if (loading.getProgress() < 1.0) {
                        loadingSpinner.setVisible(true);
                    }
                });
                spinnerDelay.play();

                loading.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0) {
                        Platform.runLater(() -> {
                            loadingSpinner.setVisible(false);
                            imageView.setImage(loading);
                        });
                    }
                });
                // エラー時
                loading.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        Platform.runLater(() -> loadingSpinner.setVisible(false));
                    }
                });
            }
        } catch (Exception e) {
            log.error("画像の読み込みに失敗しました: {}", filePath, e);
        }
    }

    /**
     * 先読みキャッシュからファイルパスに一致する画像を返す。
     *
     * @param filePath 検索するファイルパス
     * @return キャッシュ済み画像（見つからない場合は null）
     */
    private Image findCachedImage(String filePath) {
        if (viewerState == null) return null;
        List<ImageSummary> images = viewerState.getImageList();
        for (java.util.Map.Entry<Integer, Image> entry : prefetchCache.entrySet()) {
            int idx = entry.getKey();
            if (idx >= 0 && idx < images.size()
                    && images.get(idx).getFilePath().equals(filePath)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * ImageViewに画像を読み込む（nullの場合はクリア）。
     *
     * @param imageView 読み込み先ImageView
     * @param image     画像サマリー（nullの場合はクリア）
     */
    private void loadImageOrClear(ImageView imageView, ImageSummary image) {
        if (image != null && "image".equals(image.getMediaType())) {
            loadImage(imageView, image.getFilePath());
        } else {
            imageView.setImage(null);
        }
    }

    /**
     * リストの指定インデックスの要素を返す（範囲外の場合はnull）。
     *
     * @param list  リスト
     * @param index インデックス
     * @return 要素（範囲外の場合はnull）
     */
    private ImageSummary getImageAt(List<ImageSummary> list, int index) {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * 情報バーを更新する。
     *
     * <p>タグ名が未ロードの場合はDBから遅延取得してキャッシュする。
     * 見開き表示時のサイド切替でファイル名・作者も連動して更新する。
     *
     * @param image 表示中の画像サマリー
     */
    private void updateInfoBar(ImageSummary image) {
        // ファイル名・パス・解像度・非表示を更新
        fileNameLabel.setText(image.getFileName());
        filePathLabel.setText(image.getFilePath());
        resolutionLabel.setText(image.getResolutionString());

        // 作者表示を更新
        if (image.getAuthorName() != null && !image.getAuthorName().isEmpty()) {
            authorCombo.setValue(image.getAuthorName());
        } else {
            authorCombo.setValue(null);
            authorCombo.setPromptText("作者を選択...");
        }

        // タグ名が未ロードの場合はDBから遅延取得する
        if (image.getTagNames().isEmpty() && image.getId() != null) {
            try {
                List<Tag> tags = tagService.getTagsForImage(image.getId());
                image.setTagNames(tags.stream()
                        .map(Tag::getName)
                        .collect(java.util.stream.Collectors.toList()));
            } catch (Exception e) {
                log.warn("タグの遅延取得に失敗しました: imageId={}", image.getId(), e);
            }
        }

        // タグチップを更新
        tagChipsPane.getChildren().clear();
        for (String tagName : image.getTagNames()) {
            Label chip = new Label(tagName);
            chip.getStyleClass().add("tag-chip");
            tagChipsPane.getChildren().add(chip);
        }

        // Star UIを更新（クリック可能な★アイコン）
        updateStarPane(image);
    }

    /**
     * Star表示UIを更新する。
     *
     * <p>5つの★ラベルを配置し、クリックでStar値をDB保存する。
     * 現在のStar値以下は★（黄色）、それ以上は☆（灰色）で表示する。
     *
     * @param image 表示中の画像サマリー
     */
    private void updateStarPane(ImageSummary image) {
        starPane.getChildren().clear();
        int currentStar = image.getStar();
        for (int i = 1; i <= 5; i++) {
            final int starValue = i;
            Label starLabel = new Label(i <= currentStar ? "★" : "☆");
            starLabel.setStyle(i <= currentStar
                    ? "-fx-text-fill: #ffc107; -fx-font-size: 16px; -fx-cursor: hand;"
                    : "-fx-text-fill: #666666; -fx-font-size: 16px; -fx-cursor: hand;");
            starLabel.setOnMouseClicked(event -> {
                ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
                if (current == null) return;
                // 同じStarをクリック → リセット(0)
                int newStar = (current.getStar() == starValue) ? 0 : starValue;
                try {
                    imageService.updateStar(current.getId(), newStar);
                    current.setStar(newStar);
                    updateStarPane(current);
                    log.debug("Star評価を更新しました: id={}, star={}", current.getId(), newStar);
                } catch (Exception ex) {
                    log.error("Star評価の更新に失敗しました", ex);
                }
            });
            starPane.getChildren().add(starLabel);
        }
    }

    /**
     * 前のページに移動する。
     *
     * <p>見開き表示時は2ページ分戻る。
     */
    @FXML
    private void onPrev() {
        if (viewerState != null && viewerState.hasPrevious()) {
            int step = (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) ? 2 : 1;
            int newIndex = Math.max(0, viewerState.getCurrentIndex() - step);
            viewerState.setCurrentIndex(newIndex);
            updateDisplay();
        }
    }

    /**
     * 次のページに移動する。
     *
     * <p>見開き表示時は2ページ分送る。
     */
    @FXML
    private void onNext() {
        if (viewerState != null && viewerState.hasNext()) {
            int step = (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) ? 2 : 1;
            int newIndex = Math.min(viewerState.getImageList().size() - 1,
                    viewerState.getCurrentIndex() + step);
            viewerState.setCurrentIndex(newIndex);
            updateDisplay();
        }
    }

    /**
     * 単ページ表示モードに切り替える。
     */
    @FXML
    private void onSinglePageMode() {
        if (viewerState != null) {
            viewerState.setDisplayMode(ViewerState.DisplayMode.SINGLE);
            spreadPageToggle.setSelected(false);
            singlePageToggle.setSelected(true);
            updateDisplay();
        }
    }

    /**
     * 見開き表示モードに切り替える。
     */
    @FXML
    private void onSpreadPageMode() {
        if (viewerState != null) {
            viewerState.setDisplayMode(ViewerState.DisplayMode.SPREAD);
            singlePageToggle.setSelected(false);
            spreadPageToggle.setSelected(true);
            updateDisplay();
        }
    }

    /**
     * 右綴じに切り替える。
     */
    @FXML
    private void onRightToLeft() {
        if (viewerState != null) {
            viewerState.setBindingDirection(ViewerState.BindingDirection.RIGHT_TO_LEFT);
            leftToRightToggle.setSelected(false);
            rightToLeftToggle.setSelected(true);
            updateDisplay();
        }
    }

    /**
     * 左綴じに切り替える。
     */
    @FXML
    private void onLeftToRight() {
        if (viewerState != null) {
            viewerState.setBindingDirection(ViewerState.BindingDirection.LEFT_TO_RIGHT);
            rightToLeftToggle.setSelected(false);
            leftToRightToggle.setSelected(true);
            updateDisplay();
        }
    }

    /**
     * オフセットを1減らす。
     */
    @FXML
    private void onOffsetMinus() {
        if (viewerState != null) {
            viewerState.setPageOffset(viewerState.getPageOffset() - 1);
            updateDisplay();
        }
    }

    /**
     * オフセットを1増やす。
     */
    @FXML
    private void onOffsetPlus() {
        if (viewerState != null) {
            viewerState.setPageOffset(viewerState.getPageOffset() + 1);
            updateDisplay();
        }
    }

    /**
     * タグ追加ダイアログを開く。
     */
    @FXML
    private void onAddTag() {
        ImageSummary current = getContextMenuTarget();
        if (current == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tag-edit.fxml"));
            Parent root = loader.load();
            TagEditController controller = loader.getController();
            controller.setImageId(current.getId());
            Stage stage = new Stage();
            stage.setTitle("タグの編集");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // タグ更新後に情報バーを更新
            List<Tag> tags = tagService.getTagsForImage(current.getId());
            current.setTagNames(tags.stream().map(Tag::getName).collect(java.util.stream.Collectors.toList()));
            updateInfoBar(current);
            // メイン画面に通知 (#28)
            notifyTagChanged();
        } catch (Exception e) {
            log.error("タグ編集ダイアログの表示に失敗しました", e);
        }
    }

    /**
     * 作者が変更された際の処理。
     */
    @FXML
    private void onAuthorChanged() {
        // 作者変更処理（実装省略 - 実際にはauthorComboの選択値をDBに保存）
        log.debug("作者が変更されました");
    }

    /**
     * 非表示フラグを切り替える。
     */
    @FXML
    private void onToggleHidden() {
        ImageSummary current = getContextMenuTarget();
        if (current == null) return;
        try {
            boolean newHidden = !current.isHidden();
            imageService.updateHidden(current.getId(), newHidden);
            current.setHidden(newHidden);
            log.info("非表示フラグを更新しました: id={}, isHidden={}", current.getId(), newHidden);
        } catch (Exception e) {
            log.error("非表示フラグの更新に失敗しました", e);
        }
    }

    /**
     * ウィンドウを閉じる。
     */
    @FXML
    private void onClose() {
        onCloseInternal();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 新しいビューアウィンドウを開くスタティックファクトリメソッド。
     *
     * <p>ダブルクリックのたびに新しいビューアウィンドウを開く。
     * 各ウィンドウは独立した {@link ViewerState} を保持する。
     *
     * @param imageList    表示する画像リスト（検索結果のスナップショット）
     * @param initialIndex 初期表示インデックス
     */
    public static void openNewWindow(List<ImageSummary> imageList, int initialIndex) {
        openNewWindow(imageList, initialIndex, null);
    }

    /**
     * 新しいビューアウィンドウを開く（タグ変更コールバック付き）。
     *
     * @param imageList        表示する画像リスト
     * @param initialIndex     初期表示インデックス
     * @param onTagChanged     タグ変更時にメイン画面に通知するコールバック
     */
    public static void openNewWindow(List<ImageSummary> imageList, int initialIndex, Runnable onTagChanged) {
        openNewWindow(imageList, initialIndex, onTagChanged, null);
    }

    /**
     * 新しいビューアウィンドウを開く（セッション指定付き）。
     *
     * @param imageList        表示する画像リスト
     * @param initialIndex     初期表示インデックス
     * @param onTagChanged     タグ変更時コールバック
     * @param session          復元用セッション（新規の場合はnull → 自動生成）
     */
    public static void openNewWindow(List<ImageSummary> imageList, int initialIndex,
                                     Runnable onTagChanged, com.example.pixtagarc.dto.ViewerSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ViewerController.class.getResource("/fxml/viewer.fxml"));
            Parent root = loader.load();
            ViewerController controller = loader.getController();

            ViewerState state = new ViewerState(imageList, initialIndex);

            // 見開き/単ページ自動判別 (#32)
            ViewerState.DisplayMode autoMode = controller.detectPageMode(imageList, initialIndex);
            state.setDisplayMode(autoMode);

            controller.onTagChangedCallback = onTagChanged;
            controller.setViewerState(state);

            // 情報バーのデフォルト非表示を適用 (#34)
            String infoBarSetting = com.example.pixtagarc.config.AppConfig.getInstance()
                    .getState("viewer.info_bar_visible", "false");
            controller.infoBarVisible = Boolean.parseBoolean(infoBarSetting);
            controller.applyInfoBarVisibility();

            // セッション登録 (#26)
            if (session == null) {
                session = new com.example.pixtagarc.dto.ViewerSession();
                session.setMode("search");
            }
            final com.example.pixtagarc.dto.ViewerSession finalSession = session;
            controller.currentSession = finalSession;
            com.example.pixtagarc.service.ViewerSessionManager sessionMgr =
                    com.example.pixtagarc.service.ViewerSessionManager.getInstance();
            sessionMgr.add(finalSession);

            Stage stage = new Stage();
            String title = imageList.isEmpty() ? "ビューア"
                    : imageList.get(initialIndex).getFileName();
            stage.setTitle(title);
            stage.setMinWidth(600);
            stage.setMinHeight(400);

            double w = session.getWindowWidth() > 0 ? session.getWindowWidth() : 1000;
            double h = session.getWindowHeight() > 0 ? session.getWindowHeight() : 700;
            stage.setScene(new javafx.scene.Scene(root, w, h));
            if (session.getWindowX() > 0 || session.getWindowY() > 0) {
                stage.setX(session.getWindowX());
                stage.setY(session.getWindowY());
            }
            stage.show();

            // ウィンドウ位置/サイズ変更時にセッションを即時更新 (#26)
            stage.xProperty().addListener((obs, o, n) -> { finalSession.setWindowX(n.doubleValue()); sessionMgr.save(); });
            stage.yProperty().addListener((obs, o, n) -> { finalSession.setWindowY(n.doubleValue()); sessionMgr.save(); });
            stage.widthProperty().addListener((obs, o, n) -> { finalSession.setWindowWidth(n.doubleValue()); sessionMgr.save(); });
            stage.heightProperty().addListener((obs, o, n) -> { finalSession.setWindowHeight(n.doubleValue()); sessionMgr.save(); });

            // ウィンドウ閉じる時（タスクバー等）はリソース解放のみ。セッションは残す（次回復元用）
            stage.setOnCloseRequest(event -> {
                if (controller.videoPlayerService != null) {
                    controller.videoPlayerService.release();
                }
            });

            // 先読み開始
            controller.prefetchPages(initialIndex);

            log.info("ビューアウィンドウを開きました: index={}, total={}, mode={}",
                    initialIndex, imageList.size(), autoMode);
        } catch (Exception e) {
            LoggerFactory.getLogger(ViewerController.class)
                    .error("ビューアウィンドウの表示に失敗しました", e);
        }
    }

    /**
     * ビューアを閉じる内部処理。
     *
     * <p>ビューアを明示的に「閉じる」ボタンで閉じた場合のみセッションを削除する。
     * タスクバー等からの強制終了時はセッションが残り、次回起動時に復元される。
     */
    private void onCloseInternal() {
        if (currentSession != null) {
            com.example.pixtagarc.service.ViewerSessionManager.getInstance().remove(currentSession);
            currentSession = null;
        }
        if (videoPlayerService != null) {
            videoPlayerService.release();
        }
    }

    /**
     * 見開き/単ページを画像サイズから自動判別する (#32)。
     *
     * <p>指定インデックス付近の5ページのアスペクト比で多数決。縦長が過半数なら見開き表示。
     *
     * @param images 画像リスト
     * @param startIndex 判定開始インデックス
     * @return 判定された表示モード
     */
    private ViewerState.DisplayMode detectPageMode(List<ImageSummary> images, int startIndex) {
        int portrait = 0, landscape = 0;
        int sample = Math.min(5, images.size());
        for (int i = 0; i < sample; i++) {
            int idx = startIndex + i;
            if (idx >= images.size()) idx = i; // 末尾を超えたら先頭から
            ImageSummary img = images.get(idx);
            if (img.getWidth() != null && img.getHeight() != null) {
                if (img.getHeight() > img.getWidth()) portrait++;
                else landscape++;
            }
        }
        return (portrait > landscape) ? ViewerState.DisplayMode.SPREAD : ViewerState.DisplayMode.SINGLE;
    }

    /**
     * 先読みを実行する（現在ページ ±PREFETCH_RANGE）。
     */
    private void prefetchPages(int currentIndex) {
        if (viewerState == null) return;
        List<ImageSummary> images = viewerState.getImageList();
        double w = imageAreaPane.getWidth() > 0 ? imageAreaPane.getWidth() : 1920;
        double h = imageAreaPane.getHeight() > 0 ? imageAreaPane.getHeight() : 1080;

        for (int offset = 0; offset <= PREFETCH_RANGE; offset++) {
            int fwd = currentIndex + offset;
            int bwd = currentIndex - offset;
            if (fwd >= 0 && fwd < images.size() && !prefetchCache.containsKey(fwd)
                    && "image".equals(images.get(fwd).getMediaType())) {
                prefetchCache.put(fwd, new Image("file:" + images.get(fwd).getFilePath(), w, h, true, true, true));
            }
            if (offset > 0 && bwd >= 0 && bwd < images.size() && !prefetchCache.containsKey(bwd)
                    && "image".equals(images.get(bwd).getMediaType())) {
                prefetchCache.put(bwd, new Image("file:" + images.get(bwd).getFilePath(), w, h, true, true, true));
            }
        }
    }

    /**
     * 情報バーの表示/非表示切替を初期設定する (#34)。
    /**
     * 情報バーの表示/非表示切替を初期設定する (#34)。
     * I キーのハンドリングは setupKeyboardShortcuts() に統合済み。
     */
    private void setupInfoBarToggle() {
        // 何もしない（I キーは setupKeyboardShortcuts() で処理）
    }

    /**
     * 情報バーの表示/非表示を切り替える。
     */
    private void toggleInfoBar() {
        infoBarVisible = !infoBarVisible;
        applyInfoBarVisibility();
        com.example.pixtagarc.config.AppConfig.getInstance()
                .setState("viewer.info_bar_visible", String.valueOf(infoBarVisible));
    }

    /**
     * 情報バーの表示状態を適用する。
     */
    private void applyInfoBarVisibility() {
        topInfoBar.setVisible(infoBarVisible);
        topInfoBar.setManaged(infoBarVisible);
        bottomInfoBar.setVisible(infoBarVisible);
        bottomInfoBar.setManaged(infoBarVisible);
    }

    /**
     * 見開き時のマウスオーバー側を追跡する (#33)。
     */
    private enum InfoSide { LEFT, RIGHT }
    private InfoSide activeSide = InfoSide.LEFT;

    private void setupMouseTracking() {
        imageAreaPane.setOnMouseMoved(event -> {
            if (viewerState == null) return;
            if (viewerState.getDisplayMode() != ViewerState.DisplayMode.SPREAD) return;

            double midX = imageAreaPane.getWidth() / 2.0;
            InfoSide newSide = (event.getX() < midX) ? InfoSide.LEFT : InfoSide.RIGHT;

            if (newSide != activeSide) {
                activeSide = newSide;
                ImageSummary target = getSpreadImageForSide(activeSide);
                if (target != null) {
                    updateInfoBar(target);
                }
            }
        });
    }

    /**
     * 見開き表示時に指定側の画像を返す。
     */
    private ImageSummary getSpreadImageForSide(InfoSide side) {
        if (viewerState == null) return null;
        int index = viewerState.getCurrentIndex();
        int offset = viewerState.getPageOffset();
        int adjustedIndex = index + offset;
        List<ImageSummary> list = viewerState.getImageList();

        if (viewerState.getBindingDirection() == ViewerState.BindingDirection.RIGHT_TO_LEFT) {
            return (side == InfoSide.RIGHT) ? getImageAt(list, adjustedIndex)
                                            : getImageAt(list, adjustedIndex + 1);
        } else {
            return (side == InfoSide.LEFT) ? getImageAt(list, adjustedIndex)
                                           : getImageAt(list, adjustedIndex + 1);
        }
    }

    /**
     * コンテキストメニューの操作対象画像を返す。
     *
     * <p>見開き表示時は {@code activeSide}（直近の右クリック位置）に基づいて
     * 左右どちらの画像を操作対象とするか決定する。
     * 単ページ表示時は現在表示中の画像を返す。
     *
     * @return 操作対象の画像サマリー。取得できない場合は {@code null}。
     */
    private ImageSummary getContextMenuTarget() {
        if (viewerState == null) return null;
        if (viewerState.getDisplayMode() == ViewerState.DisplayMode.SPREAD) {
            ImageSummary target = getSpreadImageForSide(activeSide);
            return target != null ? target : viewerState.getCurrentImage();
        }
        return viewerState.getCurrentImage();
    }

    /**
     * マウススワイプで次/前の作品にジャンプする (#16)。
     */
    private double swipeStartX;
    private double swipeStartY;

    private void setupSwipeNavigation() {
        imageAreaPane.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            swipeStartX = event.getScreenX();
            swipeStartY = event.getScreenY();
        });

        imageAreaPane.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            double deltaX = event.getScreenX() - swipeStartX;
            double deltaY = event.getScreenY() - swipeStartY;

            if (Math.abs(deltaX) > 100 && Math.abs(deltaX) > Math.abs(deltaY) * 2) {
                if (deltaX > 0) {
                    nextWork();
                } else {
                    prevWork();
                }
            }
        });
    }

    /**
     * 次の作品の先頭ページにジャンプする。
     */
    private void nextWork() {
        if (viewerState == null) return;
        int nextIndex = findNextWorkIndex(viewerState.getCurrentIndex());
        if (nextIndex >= 0) {
            viewerState.setCurrentIndex(nextIndex);
            prefetchCache.clear();
            updateDisplay();
        }
    }

    /**
     * 前の作品の先頭ページにジャンプする。
     */
    private void prevWork() {
        if (viewerState == null) return;
        int prevIndex = findPrevWorkIndex(viewerState.getCurrentIndex());
        if (prevIndex >= 0) {
            viewerState.setCurrentIndex(prevIndex);
            prefetchCache.clear();
            updateDisplay();
        }
    }

    /**
     * 検索結果リスト内で次の作品の先頭インデックスを返す。
     */
    private int findNextWorkIndex(int currentIndex) {
        List<ImageSummary> images = viewerState.getImageList();
        Long currentWorkId = images.get(currentIndex).getWorkId();

        int i = currentIndex + 1;
        while (i < images.size()) {
            Long workId = images.get(i).getWorkId();
            if (!java.util.Objects.equals(workId, currentWorkId)) {
                return i;
            }
            if (currentWorkId == null) return i;
            i++;
        }
        return -1;
    }

    /**
     * 検索結果リスト内で前の作品の先頭インデックスを返す。
     */
    private int findPrevWorkIndex(int currentIndex) {
        List<ImageSummary> images = viewerState.getImageList();
        Long currentWorkId = images.get(currentIndex).getWorkId();

        int groupStart = currentIndex;
        if (currentWorkId != null) {
            while (groupStart > 0 && java.util.Objects.equals(
                    images.get(groupStart - 1).getWorkId(), currentWorkId)) {
                groupStart--;
            }
        }

        if (groupStart == 0) return -1;
        int prevLast = groupStart - 1;
        Long prevWorkId = images.get(prevLast).getWorkId();

        int prevStart = prevLast;
        if (prevWorkId != null) {
            while (prevStart > 0 && java.util.Objects.equals(
                    images.get(prevStart - 1).getWorkId(), prevWorkId)) {
                prevStart--;
            }
        }
        return prevStart;
    }

    /**
     * タグ変更をメイン画面に通知する。
     */
    private void notifyTagChanged() {
        if (onTagChangedCallback != null) {
            onTagChangedCallback.run();
        }
    }

    /**
     * ビューアのコンテキストメニュー「タグを追加」サブメニューを動的に再構築する (#36)。
     *
     * <p>Star付きタグ上位5件 + 最近使用したタグ10件 + 「その他...」で構成する。
     * 既に付与済みのタグには ✓ マークを付け、クリックで削除する。
     *
     * @param menu   再構築対象のMenuオブジェクト
     * @param target 操作対象の画像サマリー
     */
    private void rebuildViewerAddTagMenu(Menu menu, ImageSummary target) {
        menu.getItems().clear();
        if (target == null) return;

        // タグ名が未ロードの場合はDBから取得
        if (target.getTagNames().isEmpty() && target.getId() != null) {
            try {
                List<Tag> tags = tagService.getTagsForImage(target.getId());
                target.setTagNames(tags.stream()
                        .map(Tag::getName)
                        .collect(java.util.stream.Collectors.toList()));
            } catch (Exception e) {
                log.warn("タグの取得に失敗しました: imageId={}", target.getId(), e);
            }
        }
        List<String> currentTagNames = target.getTagNames();

        // 履歴を再読み込み（他画面で更新されている可能性）
        tagHistory.loadFrom(com.example.pixtagarc.config.AppConfig.getInstance()
                .getState(com.example.pixtagarc.config.AppConfig.KEY_TAG_HISTORY, ""));

        // 1. Star付きタグ上位5件
        List<Tag> starTags = tagService.findAll().stream()
                .filter(t -> t.getStar() > 0)
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        for (Tag tag : starTags) {
            boolean hasTag = currentTagNames.contains(tag.getName());
            MenuItem item = new MenuItem((hasTag ? "✓ " : "  ") + "★".repeat(tag.getStar()) + " " + tag.getName());
            item.setOnAction(e -> toggleTagOnImage(target, tag.getName(), hasTag));
            menu.getItems().add(item);
        }

        if (!starTags.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 2. 最近使用したタグ（Star付きと重複除外、最大10件）
        java.util.Set<String> starTagNames = starTags.stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.toSet());
        List<String> recentTags = tagHistory.getRecent(10).stream()
                .filter(name -> !starTagNames.contains(name))
                .collect(java.util.stream.Collectors.toList());

        for (String tagName : recentTags) {
            boolean hasTag = currentTagNames.contains(tagName);
            MenuItem item = new MenuItem((hasTag ? "✓ " : "  ") + tagName);
            item.setOnAction(e -> toggleTagOnImage(target, tagName, hasTag));
            menu.getItems().add(item);
        }

        if (!recentTags.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 3. その他...（タグ編集ダイアログ）
        MenuItem other = new MenuItem("その他...");
        other.setOnAction(e -> onAddTag());
        menu.getItems().add(other);
    }

    /**
     * 画像にタグを追加/削除する（ビューアコンテキストメニューから） (#36)。
     *
     * @param target       操作対象の画像サマリー
     * @param tagName      タグ名
     * @param currentlyHas 現在付与済みかどうか
     */
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
                com.example.pixtagarc.config.AppConfig.getInstance()
                        .setState(com.example.pixtagarc.config.AppConfig.KEY_TAG_HISTORY, tagHistory.toCsv());
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
            target.setTagNames(updatedTags.stream()
                    .map(Tag::getName)
                    .collect(java.util.stream.Collectors.toList()));
            updateInfoBar(target);
            // メイン画面に通知 (#28)
            notifyTagChanged();
        } catch (Exception ex) {
            log.error("タグ操作に失敗しました: {}", tagName, ex);
        }
    }
}
