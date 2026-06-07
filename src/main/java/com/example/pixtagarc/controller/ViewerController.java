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
import javafx.stage.Stage;
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

    /** 非表示ToggleButton。 */
    @FXML private ToggleButton hiddenToggle;

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
                    switch (event.getCode()) {
                        case LEFT, UP -> onPrev();
                        case RIGHT, DOWN -> onNext();
                        default -> { /* 他のキーは無視 */ }
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

        MenuItem editTags = new MenuItem("タグを編集...");
        editTags.setOnAction(e -> onAddTag());

        MenuItem toggleHidden = new MenuItem("非表示にする");
        toggleHidden.setOnAction(e -> onToggleHidden());

        MenuItem copyPath = new MenuItem("ファイルパスをコピー");
        copyPath.setOnAction(e -> {
            ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
            if (current != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(current.getFilePath());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        MenuItem openInExplorer = new MenuItem("エクスプローラーで表示");
        openInExplorer.setOnAction(e -> {
            ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
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
                ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
                if (current != null) {
                    try {
                        imageService.updateStar(current.getId(), starValue);
                        current.setStar(starValue);
                        log.info("Star評価を更新しました: id={}, star={}", current.getId(), starValue);
                    } catch (Exception ex) {
                        log.error("Star評価の更新に失敗しました", ex);
                    }
                }
            });
            starMenu.getItems().add(starItem);
        }

        contextMenu.getItems().addAll(
                editTags,
                toggleHidden,
                starMenu,
                new SeparatorMenuItem(),
                openWork,
                new SeparatorMenuItem(),
                copyPath,
                openInExplorer,
                new SeparatorMenuItem(),
                deleteImage
        );

        imageAreaPane.setOnContextMenuRequested(event -> {
            ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
            if (current != null) {
                // 非表示フラグの状態でラベルを動的変更
                toggleHidden.setText(current.isHidden() ? "非表示を解除" : "非表示にする");
                // 作品未所属の場合はグレーアウト
                openWork.setDisable(current.getWorkId() == null);
            }
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
        ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
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
        ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
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

        // 情報バーの更新
        updateInfoBar(current);
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
     * @param image 表示する動画サマリー
     */
    private void showVideo(ImageSummary image) {
        if (videoPlayerService == null) {
            log.warn("VLCが利用できないため動画を再生できません: {}", image.getFilePath());
            return;
        }
        singleImageView.setVisible(false);
        videoPane.setVisible(true);
        videoPlayerService.play(image.getFilePath());
    }

    /**
     * ImageViewに画像を読み込む。
     *
     * @param imageView 読み込み先ImageView
     * @param filePath  画像ファイルパス
     */
    private void loadImage(ImageView imageView, String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                imageView.setImage(new Image("file:" + filePath, true));
            }
        } catch (Exception e) {
            log.error("画像の読み込みに失敗しました: {}", filePath, e);
        }
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
     * @param image 表示中の画像サマリー
     */
    private void updateInfoBar(ImageSummary image) {
        filePathLabel.setText(image.getFilePath());
        resolutionLabel.setText(image.getResolutionString());
        hiddenToggle.setSelected(image.isHidden());

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
        ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
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
        ImageSummary current = viewerState != null ? viewerState.getCurrentImage() : null;
        if (current == null) return;
        try {
            boolean newHidden = hiddenToggle.isSelected();
            imageService.updateHidden(current.getId(), newHidden);
            current.setHidden(newHidden);
            log.info("非表示フラグを更新しました: id={}, isHidden={}", current.getId(), newHidden);
        } catch (Exception e) {
            log.error("非表示フラグの更新に失敗しました", e);
            // 失敗した場合はトグルを元に戻す
            hiddenToggle.setSelected(!hiddenToggle.isSelected());
        }
    }

    /**
     * ウィンドウを閉じる。
     */
    @FXML
    private void onClose() {
        if (videoPlayerService != null) {
            videoPlayerService.release();
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    ViewerController.class.getResource("/fxml/viewer.fxml"));
            Parent root = loader.load();
            ViewerController controller = loader.getController();

            ViewerState state = new ViewerState(imageList, initialIndex);
            controller.setViewerState(state);

            Stage stage = new Stage();
            String title = imageList.isEmpty() ? "ビューア"
                    : imageList.get(initialIndex).getFileName();
            stage.setTitle(title);
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.show();
            log.info("ビューアウィンドウを開きました: index={}, total={}", initialIndex, imageList.size());
        } catch (Exception e) {
            LoggerFactory.getLogger(ViewerController.class)
                    .error("ビューアウィンドウの表示に失敗しました", e);
        }
    }
}
