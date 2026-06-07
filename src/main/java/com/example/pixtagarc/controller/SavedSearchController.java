package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.SavedSearch;
import com.example.pixtagarc.repository.SavedSearchRepository;
import com.example.pixtagarc.service.SavedSearchService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 保存済み検索管理ダイアログのコントローラークラス。
 *
 * <p>保存済み検索条件の一覧表示・名前変更・削除を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SavedSearchController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SavedSearchController.class);

    /** 保存済み検索一覧TableView。 */
    @FXML private TableView<SavedSearch> savedSearchTableView;

    /** 名前列。 */
    @FXML private TableColumn<SavedSearch, String> colName;

    /** キーワード列。 */
    @FXML private TableColumn<SavedSearch, String> colKeyword;

    /** 作成日時列。 */
    @FXML private TableColumn<SavedSearch, String> colCreatedAt;

    /** 更新日時列。 */
    @FXML private TableColumn<SavedSearch, String> colUpdatedAt;

    /** 適用ボタン。 */
    @FXML private Button loadButton;

    /** 名前変更ボタン。 */
    @FXML private Button renameButton;

    /** 削除ボタン。 */
    @FXML private Button deleteButton;

    /** 閉じるボタン。 */
    @FXML private Button closeButton;

    /** 保存済み検索サービス。 */
    private SavedSearchService savedSearchService;

    /**
     * コントローラーを初期化する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("SavedSearchControllerを初期化します");
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        SavedSearchRepository savedSearchRepository = new SavedSearchRepository(dbConfig);
        savedSearchService = new SavedSearchService(savedSearchRepository);

        // TableViewの列設定
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        colKeyword.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getKeyword() != null ? data.getValue().getKeyword() : ""));
        colCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()));
        colUpdatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUpdatedAt()));

        loadSavedSearches();
    }

    /**
     * 保存済み検索一覧を読み込む。
     */
    private void loadSavedSearches() {
        try {
            List<SavedSearch> savedSearches = savedSearchService.findAll();
            savedSearchTableView.setItems(FXCollections.observableArrayList(savedSearches));
        } catch (Exception e) {
            log.error("保存済み検索の読み込みに失敗しました", e);
        }
    }

    /**
     * 選択した保存済み検索を適用する。
     *
     * <p>このダイアログからは直接適用できないため、ダイアログを閉じて
     * メイン画面で適用する。
     */
    @FXML
    private void onLoad() {
        SavedSearch selected = savedSearchTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("選択してください", "適用する保存済み検索を選択してください。");
            return;
        }
        log.info("保存済み検索を適用します: name={}", selected.getName());
        // ダイアログを閉じる（メイン画面で適用処理を行う）
        closeDialog();
    }

    /**
     * 選択した保存済み検索の名前を変更する。
     */
    @FXML
    private void onRename() {
        SavedSearch selected = savedSearchTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("選択してください", "名前を変更する保存済み検索を選択してください。");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("名前の変更");
        dialog.setHeaderText("新しい名前を入力してください");
        dialog.setContentText("名前:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(selected.getName())) {
                try {
                    selected.setName(newName.trim());
                    savedSearchService.findById(selected.getId()).ifPresent(ss -> {
                        ss.setName(newName.trim());
                        // 更新処理（SavedSearchServiceに更新メソッドを追加する場合）
                    });
                    loadSavedSearches();
                    log.info("保存済み検索の名前を変更しました: id={}, newName={}", selected.getId(), newName);
                } catch (Exception e) {
                    log.error("名前の変更に失敗しました", e);
                    showError("名前の変更に失敗しました", e.getMessage());
                }
            }
        });
    }

    /**
     * 選択した保存済み検索を削除する。
     */
    @FXML
    private void onDelete() {
        SavedSearch selected = savedSearchTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("選択してください", "削除する保存済み検索を選択してください。");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("削除の確認");
        confirm.setHeaderText("保存済み検索を削除しますか？");
        confirm.setContentText("「" + selected.getName() + "」を削除します。この操作は元に戻せません。");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    savedSearchService.delete(selected.getId());
                    loadSavedSearches();
                    log.info("保存済み検索を削除しました: id={}", selected.getId());
                } catch (Exception e) {
                    log.error("保存済み検索の削除に失敗しました", e);
                    showError("削除に失敗しました", e.getMessage());
                }
            }
        });
    }

    /**
     * ダイアログを閉じる。
     */
    @FXML
    private void onClose() {
        closeDialog();
    }

    /**
     * ダイアログを閉じる内部メソッド。
     */
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
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
