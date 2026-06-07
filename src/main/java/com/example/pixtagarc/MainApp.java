package com.example.pixtagarc;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.config.VlcConfig;
import com.example.pixtagarc.exception.VlcNotInstalledException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * pixtagarc アプリケーションのエントリーポイント。
 *
 * <p>JavaFX {@link Application} を継承したメインクラス。
 * アプリケーション起動時に以下の初期化を行う:
 * <ol>
 *   <li>データベース初期化（{@link DatabaseConfig}）</li>
 *   <li>VLC検出・初期化（{@link VlcConfig}）</li>
 *   <li>メイン画面（main.fxml）のロードと表示</li>
 * </ol>
 *
 * <p>VLCが見つからない場合はダイアログを表示して動画再生機能を無効化し、
 * アプリケーションは通常通り起動する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class MainApp extends Application {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    /** アプリケーションタイトル。 */
    private static final String APP_TITLE = "pixtagarc";

    /** メイン画面のFXMLパス。 */
    private static final String MAIN_FXML_PATH = "/fxml/main.fxml";

    /** メイン画面のCSSパス。 */
    private static final String APP_CSS_PATH = "/css/app.css";

    /** デフォルトウィンドウ幅。 */
    private static final double DEFAULT_WIDTH = 1280;

    /** デフォルトウィンドウ高さ。 */
    private static final double DEFAULT_HEIGHT = 800;

    /**
     * JavaFXアプリケーションのエントリーポイント。
     *
     * <p>データベースとVLCを初期化し、メイン画面を表示する。
     *
     * @param primaryStage プライマリステージ
     */
    @Override
    public void start(Stage primaryStage) {
        log.info("pixtagarcを起動します");

        // データベースを初期化
        try {
            DatabaseConfig.getInstance();
            log.info("データベースの初期化が完了しました");
        } catch (Exception e) {
            log.error("データベースの初期化に失敗しました", e);
            showFatalError("データベースエラー",
                    "データベースの初期化に失敗しました。\nアプリケーションを終了します。\n\n" + e.getMessage());
            return;
        }

        // VLCを初期化（失敗時はダイアログ表示して動画再生機能を無効化）
        initializeVlc();

        // メイン画面をロード
        try {
            Parent root = loadMainFxml();
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // ウィンドウを閉じる際に Platform.exit() を呼び出す（stop()に処理を委譲）
            primaryStage.setOnCloseRequest(event -> {
                log.info("ウィンドウが閉じられました");
                javafx.application.Platform.exit();
            });

            primaryStage.show();
            log.info("メイン画面を表示しました");
        } catch (IOException e) {
            log.error("メイン画面のロードに失敗しました", e);
            showFatalError("起動エラー",
                    "メイン画面のロードに失敗しました。\nアプリケーションを終了します。\n\n" + e.getMessage());
        }
    }

    /**
     * VLCを初期化する。
     *
     * <p>VLCが見つからない場合はダイアログを表示して動画再生機能を無効化する。
     * アプリケーション自体は通常通り起動する。
     */
    private void initializeVlc() {
        try {
            VlcConfig.initialize();
            log.info("VLCの初期化が完了しました");
        } catch (VlcNotInstalledException e) {
            log.warn("VLCが見つかりませんでした。動画再生機能は無効化されます。", e);
            showVlcNotInstalledDialog();
        } catch (Exception e) {
            log.error("VLCの初期化中にエラーが発生しました", e);
            showVlcNotInstalledDialog();
        }
    }

    /**
     * VLC未インストールダイアログを表示する。
     */
    private void showVlcNotInstalledDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("VLCが見つかりません");
        alert.setHeaderText("動画再生機能が無効化されます");
        alert.setContentText(
                "VLCメディアプレイヤーがインストールされていないか、\n"
                + "ライブラリが見つかりませんでした。\n\n"
                + "動画ファイル（WebM等）の再生には VLC が必要です。\n"
                + "https://www.videolan.org/vlc/ からダウンロードできます。\n\n"
                + "画像の表示・管理機能は通常通り使用できます。"
        );
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * メイン画面のFXMLをロードする。
     *
     * @return ロードされたルートノード
     * @throws IOException FXMLのロードに失敗した場合
     */
    private Parent loadMainFxml() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML_PATH));
        return loader.load();
    }

    /**
     * 致命的エラーダイアログを表示してアプリケーションを終了する。
     *
     * @param title   ダイアログタイトル
     * @param message エラーメッセージ
     */
    private void showFatalError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        javafx.application.Platform.exit();
    }

    /**
     * アプリケーションのメインメソッド。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        log.info("pixtagarc 起動: args={}", (Object) args);

        // 未捕捉例外（OOM含む）でプロセスが残留しないようにシャットダウンフックを登録
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LoggerFactory.getLogger(MainApp.class)
                    .error("未捕捉例外が発生しました: thread={}", thread.getName(), throwable);
            // JavaFXスレッド外からの例外でもプロセスを終了させる
            javafx.application.Platform.exit();
            System.exit(1);
        });

        // OOM等でJVMがシャットダウンされる際にDBを確実にクローズする
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggerFactory.getLogger(MainApp.class).info("シャットダウンフックを実行します");
            try {
                DatabaseConfig.getInstance().close();
            } catch (Exception e) {
                // シャットダウン中のエラーは無視
            }
        }, "shutdown-hook"));

        launch(args);
    }

    /**
     * JavaFXアプリケーションのライフサイクル終了メソッド。
     *
     * <p>Platform.exit() 呼び出し後にJavaFXランタイムによって呼び出される。
     * バックグラウンドスレッドが残留しないよう System.exit() で確実に終了する。
     *
     * @throws Exception 終了処理に失敗した場合
     */
    @Override
    public void stop() throws Exception {
        log.info("アプリケーションを停止します");
        DatabaseConfig.getInstance().close();
        // daemon=false のスレッドが残っている場合に備えて強制終了
        System.exit(0);
    }
}
