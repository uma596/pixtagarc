package com.example.pixtagarc.dto;

/**
 * メイン画面状態DTO。
 *
 * <p>メイン画面の表示状態を保持するデータ転送オブジェクト。
 * 表示モード・サムネイルサイズ・ソート設定を管理する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class MainViewState {

    /**
     * 表示モード列挙型。
     */
    public enum DisplayMode {
        /** サムネイルグリッド表示。 */
        THUMBNAIL,
        /** リスト（テーブル）表示。 */
        LIST
    }

    /**
     * サムネイルサイズ列挙型。
     */
    public enum ThumbnailSize {
        /** 小サイズ（80px）。 */
        SMALL(80),
        /** 中サイズ（150px）。 */
        MEDIUM(150),
        /** 大サイズ（250px）。 */
        LARGE(250);

        /** サムネイルのピクセルサイズ。 */
        private final int pixels;

        /**
         * コンストラクタ。
         *
         * @param pixels ピクセルサイズ
         */
        ThumbnailSize(int pixels) {
            this.pixels = pixels;
        }

        /**
         * ピクセルサイズを返す。
         *
         * @return ピクセルサイズ
         */
        public int getPixels() {
            return pixels;
        }
    }

    /** 現在の表示モード。デフォルトはサムネイル表示。 */
    private DisplayMode displayMode = DisplayMode.THUMBNAIL;

    /** サムネイルサイズ。デフォルトは中サイズ。 */
    private ThumbnailSize thumbnailSize = ThumbnailSize.MEDIUM;

    /** ソート列名。デフォルトは {@code "created_at"}。 */
    private String sortColumn = "created_at";

    /** ソート順。デフォルトは降順。 */
    private String sortOrder = "DESC";

    /**
     * デフォルトコンストラクタ。
     */
    public MainViewState() {
    }

    /**
     * 表示モードを返す。
     *
     * @return 表示モード
     */
    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * 表示モードを設定する。
     *
     * @param displayMode 表示モード
     */
    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * サムネイルサイズを返す。
     *
     * @return サムネイルサイズ
     */
    public ThumbnailSize getThumbnailSize() {
        return thumbnailSize;
    }

    /**
     * サムネイルサイズを設定する。
     *
     * @param thumbnailSize サムネイルサイズ
     */
    public void setThumbnailSize(ThumbnailSize thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    /**
     * ソート列名を返す。
     *
     * @return ソート列名
     */
    public String getSortColumn() {
        return sortColumn;
    }

    /**
     * ソート列名を設定する。
     *
     * @param sortColumn ソート列名
     */
    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    /**
     * ソート順を返す。
     *
     * @return ソート順（{@code "ASC"} または {@code "DESC"}）
     */
    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * ソート順を設定する。
     *
     * @param sortOrder ソート順
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
