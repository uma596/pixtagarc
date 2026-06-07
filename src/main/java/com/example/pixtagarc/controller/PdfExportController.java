package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.dto.PdfExportRequest;
import com.example.pixtagarc.repository.ImageRepository;
import com.example.pixtagarc.service.PdfExportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PDF出力ダイアログのコントローラークラス。
 *
 * <p>検索結果の画像をPDFに出力する設定ダイアログを制御する。
 * 用紙サイズ・1ページあたりの枚数・出力先パスの設定と、
 * PDF出力の実行を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class PdfExportController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(PdfExportController.class);

    /** 対象画像数ラベル。 */
    @FXML private Label imageCountLabel;

    /** 用紙サイズComboBox。 */
    @FXML private ComboBox<String> paperSizeCombo;

    /** 1ページあたりの枚数Spinner。 */
    @FXML private Spinner<Integer> imagesPerPageSpinner;

    /** フィットモードComboBox。 */
    @FXML private ComboBox<String> fitModeCombo;

    /** 出力先パスフィールド。 */
    @FXML private TextField outputPathField;

    /** 参照ボタン。 */
    @FXML private Button browseButton;

    /** 出力進捗ラベル。 */
    @FXML private Label exportProgressLabel;

    /** 出力進捗バー。 */
    @FXML private ProgressBar exportProgressBar;

    /** 出力実行ボタン。 */
    @FXML private Button exportButton;

    /** キャンセルボタン。 */
    @FXML private Button cancelButton;

    /** PDF出力サービス。 */
    private PdfExportService pdfExportService;

    /** 出力対象の画像IDリスト。 */
    private List<Long> imageIds;

    /** タスク実行用ExecutorService。 */
    private ExecutorService executor;

    /**
     * コントローラーを初期化する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("PdfExportControllerを初期化します");
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ImageRepository imageRepository = new ImageRepository(dbConfig);
        pdfExportService = new PdfExportService(imageRepository);

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pdf-export-thread");
            t.setDaemon(true);
            return t;
        });

        // 初期値設定
        paperSizeCombo.setItems(FXCollections.observableArrayList("A4", "A3", "B4", "B5", "Letter"));
        paperSizeCombo.setValue("A4");
        fitModeCombo.setItems(FXCollections.observableArrayList("ページに合わせる", "原寸大", "幅に合わせる"));
        fitModeCombo.setValue("ページに合わせる");

        // Spinnerの設定
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 1);
        imagesPerPageSpinner.setValueFactory(valueFactory);
    }

    /**
     * 出力対象の画像IDリストを設定する。
     *
     * @param imageIds 画像IDリスト
     */
    public void setImageIds(List<Long> imageIds) {
        this.imageIds = imageIds;
        imageCountLabel.setText(imageIds.size() + " 件");
    }

    /**
     * 出力先ファイル選択ダイアログを開く。
     */
    @FXML
    private void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("PDF出力先を選択");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDFファイル", "*.pdf"));
        chooser.setInitialFileName("export.pdf");
        Stage stage = (Stage) browseButton.getScene().getWindow();
        File selected = chooser.showSaveDialog(stage);
        if (selected != null) {
            outputPathField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * PDF出力を実行する。
     */
    @FXML
    private void onExport() {
        String outputPath = outputPathField.getText();
        if (outputPath == null || outputPath.trim().isEmpty()) {
            showError("出力先が指定されていません", "PDF出力先ファイルパスを指定してください。");
            return;
        }
        if (imageIds == null || imageIds.isEmpty()) {
            showError("出力対象がありません", "出力対象の画像がありません。");
            return;
        }

        PdfExportRequest request = new PdfExportRequest(
                imageIds,
                paperSizeCombo.getValue(),
                imagesPerPageSpinner.getValue(),
                outputPath.trim()
        );

        // UIを無効化
        exportButton.setDisable(true);
        browseButton.setDisable(true);
        exportProgressBar.setVisible(true);
        exportProgressBar.setProgress(-1); // 不定進捗
        exportProgressLabel.setText("PDF出力中...");

        // バックグラウンドで実行
        executor.submit(() -> {
            try {
                pdfExportService.export(request);
                Platform.runLater(() -> onExportCompleted(outputPath));
            } catch (Exception e) {
                log.error("PDF出力に失敗しました", e);
                Platform.runLater(() -> onExportFailed(e));
            }
        });
    }

    /**
     * PDF出力完了時の処理。
     *
     * @param outputPath 出力先パス
     */
    private void onExportCompleted(String outputPath) {
        exportProgressBar.setProgress(1.0);
        exportProgressLabel.setText("PDF出力が完了しました");
        exportButton.setDisable(false);
        browseButton.setDisable(false);
        log.info("PDF出力が完了しました: {}", outputPath);
        showInfo("PDF出力完了", "PDFを出力しました:\n" + outputPath);
    }

    /**
     * PDF出力失敗時の処理。
     *
     * @param ex 発生した例外
     */
    private void onExportFailed(Exception ex) {
        exportProgressBar.setVisible(false);
        exportProgressLabel.setText("PDF出力に失敗しました");
        exportButton.setDisable(false);
        browseButton.setDisable(false);
        showError("PDF出力に失敗しました", ex.getMessage());
    }

    /**
     * ダイアログを閉じる。
     */
    @FXML
    private void onCancel() {
        if (executor != null) {
            executor.shutdown();
        }
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
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
