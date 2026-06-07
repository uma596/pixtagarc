package com.example.pixtagarc.exception;

/**
 * PDF出力に失敗した場合にスローされる例外。
 *
 * <p>PDFBoxによるPDF生成処理でエラーが発生した場合にスローされる。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class PdfExportException extends RuntimeException {

    /**
     * メッセージを指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     */
    public PdfExportException(String message) {
        super(message);
    }

    /**
     * メッセージと原因を指定するコンストラクタ。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public PdfExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
