package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * PDF出力リクエストDTO。
 *
 * <p>PDF出力に必要なパラメータをまとめたデータ転送オブジェクト。
 * 対象画像IDリスト・用紙サイズ・1ページあたりの枚数・出力先パスを保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class PdfExportRequest {

    /** 出力対象の画像IDリスト。 */
    private List<Long> imageIds = new ArrayList<>();

    /** 用紙サイズ（例: {@code "A4"}, {@code "A3"}, {@code "Letter"}）。 */
    private String paperSize = "A4";

    /** 1ページあたりの画像枚数。 */
    private int imagesPerPage = 1;

    /** 出力先ファイルパス。 */
    private String outputPath;

    /**
     * デフォルトコンストラクタ。
     */
    public PdfExportRequest() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param imageIds      対象画像IDリスト
     * @param paperSize     用紙サイズ
     * @param imagesPerPage 1ページあたりの枚数
     * @param outputPath    出力先パス
     */
    public PdfExportRequest(List<Long> imageIds, String paperSize,
                            int imagesPerPage, String outputPath) {
        this.imageIds = imageIds != null ? imageIds : new ArrayList<>();
        this.paperSize = paperSize;
        this.imagesPerPage = imagesPerPage;
        this.outputPath = outputPath;
    }

    /**
     * 対象画像IDリストを返す。
     *
     * @return 画像IDリスト
     */
    public List<Long> getImageIds() {
        return imageIds;
    }

    /**
     * 対象画像IDリストを設定する。
     *
     * @param imageIds 画像IDリスト
     */
    public void setImageIds(List<Long> imageIds) {
        this.imageIds = imageIds != null ? imageIds : new ArrayList<>();
    }

    /**
     * 用紙サイズを返す。
     *
     * @return 用紙サイズ
     */
    public String getPaperSize() {
        return paperSize;
    }

    /**
     * 用紙サイズを設定する。
     *
     * @param paperSize 用紙サイズ
     */
    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    /**
     * 1ページあたりの画像枚数を返す。
     *
     * @return 1ページあたりの枚数
     */
    public int getImagesPerPage() {
        return imagesPerPage;
    }

    /**
     * 1ページあたりの画像枚数を設定する。
     *
     * @param imagesPerPage 1ページあたりの枚数
     */
    public void setImagesPerPage(int imagesPerPage) {
        this.imagesPerPage = imagesPerPage;
    }

    /**
     * 出力先ファイルパスを返す。
     *
     * @return 出力先パス
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * 出力先ファイルパスを設定する。
     *
     * @param outputPath 出力先パス
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String toString() {
        return "PdfExportRequest{imageIds.size=" + imageIds.size()
                + ", paperSize='" + paperSize + "', imagesPerPage=" + imagesPerPage
                + ", outputPath='" + outputPath + "'}";
    }
}
