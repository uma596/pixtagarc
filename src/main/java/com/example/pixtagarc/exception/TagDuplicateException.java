package com.example.pixtagarc.exception;

/**
 * タグ名が重複している場合にスローされる例外。
 *
 * <p>既に存在するタグ名で新規作成しようとした場合にスローされる。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class TagDuplicateException extends RuntimeException {

    /**
     * メッセージを指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     */
    public TagDuplicateException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public TagDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * タグ名を指定するファクトリメソッド。
     *
     * @param tagName 重複したタグ名
     * @return {@link TagDuplicateException} インスタンス
     */
    public static TagDuplicateException ofName(String tagName) {
        return new TagDuplicateException("タグ名が既に存在します: " + tagName);
    }
}
