package com.example.pixtagarc.exception;

/**
 * サムネイル生成に失敗した場合にスローされる例外。
 *
 * <p>Thumbnailatorによるサムネイル生成処理でエラーが発生した場合にスローされる。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ThumbnailGenerationException extends RuntimeException {

    /**
     * メッセージを指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     */
    public ThumbnailGenerationException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public ThumbnailGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * ファイルパスと原因を指定するファクトリメソッド。
     *
     * @param filePath サムネイル生成に失敗したファイルパス
     * @param cause    原因となった例外
     * @return {@link ThumbnailGenerationException} インスタンス
     */
    public static ThumbnailGenerationException ofFile(String filePath, Throwable cause) {
        return new ThumbnailGenerationException(
                "サムネイル生成に失敗しました: " + filePath, cause);
    }
}
