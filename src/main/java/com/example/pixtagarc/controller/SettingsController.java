package com.example.pixtagarc.controller;

import com.example.pixtagarc.config.AppConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 設定ダイアログのコントローラークラス。
 *
 * <p>アプリケーション設定（検索上限件数など）の表示・変更を担当する。
 * 変更は即座に {@link AppConfig} に保存され、次回の検索から反映される。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SettingsController implements Initializable {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    /**
     * 検索上限件数Spinner。
     * 100〜100000件の範囲で100刻みで設定可能。
     */
    @FXML private Spinner<Integer> searchLimitSpinner;

    /** 検索上限件数の説明ラベル。 */
    @FXML private Label searchLimitDescLabel;

    /** 閉じるボタン。 */
    @FXML private Button closeButton;

    /**
     * コントローラーを初期化する。
     *
     * @param location  FXMLのURL
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("SettingsControllerを初期化します");
        int currentLimit = AppConfig.getInstance().getSearchLimit();

        // Spinnerの設定: 100〜100000件、100刻み
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        AppConfig.MIN_SEARCH_LIMIT,
                        AppConfig.MAX_SEARCH_LIMIT,
                        currentLimit,
                        100);
        searchLimitSpinner.setValueFactory(valueFactory);
        searchLimitSpinner.setEditable(true);

        // 値変更時にリアルタイムで保存
        searchLimitSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applySearchLimit(newVal);
            }
        });

        updateDescription(currentLimit);
    }

    /**
     * 検索上限件数を適用して保存する。
     *
     * @param limit 検索上限件数
     */
    private void applySearchLimit(int limit) {
        try {
            AppConfig.getInstance().setSearchLimit(limit);
            updateDescription(limit);
            log.info("検索上限件数を変更しました: {}", limit);
        } catch (IllegalArgumentException e) {
            log.warn("検索上限件数の設定値が不正です: {}", limit);
        }
    }

    /**
     * 説明ラベルを更新する。
     *
     * @param limit 現在の検索上限件数
     */
    private void updateDescription(int limit) {
        if (searchLimitDescLabel != null) {
            searchLimitDescLabel.setText(
                    String.format("検索結果の最大表示件数: %,d 件", limit));
        }
    }

    /**
     * ダイアログを閉じる。
     */
    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
