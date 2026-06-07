package com.example.pixtagarc.exception;

/**
 * 画像が見つからない場合にスローされる例外。
 *
 * <p>指定されたIDやパスに対応する画像がデータベースに存在しない場合にスローされる。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageNotFoundException extends RuntimeException {

    /**
     * メッセージを指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     */
    public ImageNotFoundException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public ImageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 画像IDを指定するファクトリメソッド。
     *
     * @param id 見つからなかった画像ID
     * @return {@link ImageNotFoundException} インスタンス
     */
    public static ImageNotFoundException ofId(Long id) {
        return new ImageNotFoundException("画像が見つかりません: id=" + id);
    }

    /**
     * ファイルパスを指定するファクトリメソッド。
     *
     * @param filePath 見つからなかったファイルパス
     * @return {@link ImageNotFoundException} インスタンス
     */
    public static ImageNotFoundException ofPath(String filePath) {
        return new ImageNotFoundException("画像が見つかりません: path=" + filePath);
    }
}
