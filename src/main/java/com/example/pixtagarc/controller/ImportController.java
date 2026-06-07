package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.repository.AuthorRepository;
import com.example.pixtagarc.repository.ImageRepository;
import com.example.pixtagarc.repository.ImageTagRepository;
import com.example.pixtagarc.repository.TagRepository;
import com.example.pixtagarc.repository.WorkRepository;
import com.example.pixtagarc.service.AuthorService;
import com.example.pixtagarc.service.ImportService;
import com.example.pixtagarc.service.TagService;
import com.example.pixtagarc.service.ThumbnailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * インポートダイアログのコントローラークラス。
 *
 * <p>フォルダを選択してファイルをスキャン・DB登録するダイアログを制御する。
 * {@link ImportService} の {@link javafx.concurrent.Task} をProgressBarにバインドして
 * 進捗を表示する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImportController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    /** フォルダパス表示フィールド。 */
    @FXML private TextField folderPathField;

    /** フォルダ選択ボタン。 */
    @FXML private Button selectFolderButton;

    /** サブフォルダ含めるチェックボックス。 */
    @FXML private CheckBox recursiveCheckBox;

    /** 既存スキップチェックボックス。 */
    @FXML private CheckBox skipExistingCheckBox;

    /** 全データクリアチェックボックス。 */
    @FXML private CheckBox clearAllCheckBox;

    /** フォルダごとに1作品としてまとめるチェックボックス。 */
    @FXML private CheckBox groupByFolderCheckBox;

    /** 進捗ラベル。 */
    @FXML private Label progressLabel;

    /** 進捗バー。 */
    @FXML private ProgressBar progressBar;

    /** 詳細ラベル。 */
    @FXML private Label detailLabel;

    /** インポート開始ボタン。 */
    @FXML private Button startButton;

    /** キャンセルボタン。 */
    @FXML private Button cancelButton;

    /** 現在実行中のインポートタスク。 */
    private ImportService currentTask;

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
        log.debug("ImportControllerを初期化します");
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "import-thread");
            t.setDaemon(true);
            return t;
        });

        // 全クリアON時は既存スキップを無効化
        clearAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                skipExistingCheckBox.setSelected(false);
                skipExistingCheckBox.setDisable(true);
            } else {
                skipExistingCheckBox.setDisable(false);
            }
        });
    }

    /**
     * フォルダ選択ダイアログを開く。
     */
    @FXML
    private void onSelectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("インポートするフォルダを選択");
        Stage stage = (Stage) selectFolderButton.getScene().getWindow();
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            folderPathField.setText(selected.getAbsolutePath());
            log.debug("フォルダを選択しました: {}", selected.getAbsolutePath());
        }
    }

    /**
     * インポートを開始する。
     */
    @FXML
    private void onStartImport() {
        String folderPath = folderPathField.getText();
        if (folderPath == null || folderPath.trim().isEmpty()) {
            showError("フォルダが選択されていません", "インポートするフォルダを選択してください。");
            return;
        }

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            showError("フォルダが見つかりません", "指定されたフォルダが存在しません: " + folderPath);
            return;
        }

        // UIを無効化
        startButton.setDisable(true);
        selectFolderButton.setDisable(true);
        recursiveCheckBox.setDisable(true);
        skipExistingCheckBox.setDisable(true);
        clearAllCheckBox.setDisable(true);
        groupByFolderCheckBox.setDisable(true);
        cancelButton.setText("キャンセル");

        // インポートタスクを作成
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        ImageRepository imageRepository = new ImageRepository(dbConfig);
        ThumbnailService thumbnailService = new ThumbnailService();
        WorkRepository workRepository = new WorkRepository(dbConfig);
        ImageTagRepository imageTagRepository = new ImageTagRepository(dbConfig);
        TagRepository tagRepository = new TagRepository(dbConfig);
        AuthorRepository authorRepository = new AuthorRepository(dbConfig);
        TagService tagService = new TagService(tagRepository, imageTagRepository);
        AuthorService authorService = new AuthorService(authorRepository);

        boolean clearAll = clearAllCheckBox.isSelected();

        // 全クリア時は確認ダイアログを表示
        if (clearAll) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("全データクリア");
            confirm.setHeaderText("⚠ 全データをクリアします");
            confirm.setContentText("画像メタデータ・タグ紐付け・作品情報・サムネイルキャッシュが全て削除されます。\n"
                    + "タグマスタ（タグ名の定義）は保持されます。\n\n"
                    + "この操作は元に戻せません。続行しますか？");
            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                return;
            }
        }

        currentTask = new ImportService(
                imageRepository,
                thumbnailService,
                workRepository,
                imageTagRepository,
                tagService,
                authorService,
                authorRepository,
                dbConfig,
                Paths.get(folderPath),
                recursiveCheckBox.isSelected(),
                skipExistingCheckBox.isSelected(),
                clearAll,
                groupByFolderCheckBox.isSelected()
        );

        // ProgressBarとラベルをタスクにバインド
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        // タスク完了時の処理
        currentTask.setOnSucceeded(event -> Platform.runLater(this::onImportCompleted));
        currentTask.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = currentTask.getException();
            log.error("インポートに失敗しました", ex);
            onImportFailed(ex);
        }));
        currentTask.setOnCancelled(event -> Platform.runLater(this::onImportCancelled));

        // バックグラウンドで実行
        executor.submit(currentTask);
        log.info("インポートを開始しました: folder={}", folderPath);
    }

    /**
     * インポートをキャンセルする。
     */
    @FXML
    private void onCancel() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
            log.info("インポートのキャンセルを要求しました");
        } else {
            // タスクが実行中でない場合はダイアログを閉じる
            closeDialog();
        }
    }

    /**
     * インポート完了時の処理。
     */
    private void onImportCompleted() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressBar.setProgress(1.0);
        progressLabel.setText("インポートが完了しました");
        startButton.setDisable(false);
        selectFolderButton.setDisable(false);
        recursiveCheckBox.setDisable(false);
        skipExistingCheckBox.setDisable(false);
        clearAllCheckBox.setDisable(false);
        groupByFolderCheckBox.setDisable(false);
        cancelButton.setText("閉じる");
        log.info("インポートが完了しました");
    }

    /**
     * インポート失敗時の処理。
     *
     * @param ex 発生した例外
     */
    private void onImportFailed(Throwable ex) {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressLabel.setText("インポートに失敗しました");
        detailLabel.setText(ex != null ? ex.getMessage() : "不明なエラー");
        startButton.setDisable(false);
        selectFolderButton.setDisable(false);
        recursiveCheckBox.setDisable(false);
        skipExistingCheckBox.setDisable(false);
        clearAllCheckBox.setDisable(false);
        groupByFolderCheckBox.setDisable(false);
        cancelButton.setText("閉じる");
    }

    /**
     * インポートキャンセル時の処理。
     */
    private void onImportCancelled() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressLabel.setText("インポートがキャンセルされました");
        startButton.setDisable(false);
        selectFolderButton.setDisable(false);
        recursiveCheckBox.setDisable(false);
        skipExistingCheckBox.setDisable(false);
        clearAllCheckBox.setDisable(false);
        groupByFolderCheckBox.setDisable(false);
        cancelButton.setText("閉じる");
        log.info("インポートがキャンセルされました");
    }

    /**
     * ダイアログを閉じる。
     */
    private void closeDialog() {
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
}
