package com.example.pixtagarc.exception;

/**
 * VLCがインストールされていない場合にスローされる例外。
 *
 * <p>VLCJがVLCライブラリを検出できなかった場合にスローされる。
 * この例外がスローされた場合、動画再生機能は無効化される。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class VlcNotInstalledException extends RuntimeException {

    /**
     * デフォルトメッセージのコンストラクタ。
     */
    public VlcNotInstalledException() {
        super("VLCがインストールされていないか、ライブラリが見つかりません。"
                + "動画再生機能を使用するにはVLCをインストールしてください。");
    }

    /**
     * メッセージを指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     */
    public VlcNotInstalledException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public VlcNotInstalledException(String message, Throwable cause) {
        super(message, cause);
    }
}
