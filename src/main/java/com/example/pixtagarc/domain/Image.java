package com.example.pixtagarc.domain;

/**
 * 画像エンティティ。
 *
 * <p>データベースの {@code images} テーブルに対応するドメインオブジェクト。
 * 画像・動画ファイルのメタデータを保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class Image {

    /** 主キー。新規作成時は {@code null}。 */
    private Long id;

    /** ファイルの絶対パス。UNIQUE制約あり。 */
    private String filePath;

    /** ファイル名（パスのベース名）。 */
    private String fileName;

    /** ファイルサイズ（バイト）。 */
    private Long fileSize;

    /** 画像の幅（ピクセル）。動画の場合は動画の幅。 */
    private Integer width;

    /** 画像の高さ（ピクセル）。動画の場合は動画の高さ。 */
    private Integer height;

    /** メディアタイプ。{@code "image"} または {@code "video"}。 */
    private String mediaType;

    /** 作者ID。{@code authors} テーブルへの外部キー。NULL可。 */
    private Long authorId;

    /** 作品ID。{@code works} テーブルへの外部キー。NULL可。 */
    private Long workId;

    /** ページ番号（1始まり）。NULL可。 */
    private Integer pageNumber;

    /** Star評価（0〜5）。デフォルトは0。 */
    private int star = 0;

    /** 非表示フラグ。{@code true} の場合は非表示。 */
    private boolean hidden;

    /** ファイルの作成日時（ISO8601形式）。 */
    private String createdAt;

    /** DBへのインポート日時（ISO8601形式）。 */
    private String importedAt;

    /**
     * デフォルトコンストラクタ。
     */
    public Image() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param id         主キー
     * @param filePath   ファイルの絶対パス
     * @param fileName   ファイル名
     * @param fileSize   ファイルサイズ（バイト）
     * @param width      幅（ピクセル）
     * @param height     高さ（ピクセル）
     * @param mediaType  メディアタイプ
     * @param authorId   作者ID
     * @param workId     作品ID
     * @param pageNumber ページ番号
     * @param star       Star評価
     * @param hidden     非表示フラグ
     * @param createdAt  作成日時
     * @param importedAt インポート日時
     */
    public Image(Long id, String filePath, String fileName, Long fileSize,
                 Integer width, Integer height, String mediaType, Long authorId,
                 Long workId, Integer pageNumber, int star,
                 boolean hidden, String createdAt, String importedAt) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.mediaType = mediaType;
        this.authorId = authorId;
        this.workId = workId;
        this.pageNumber = pageNumber;
        this.star = star;
        this.hidden = hidden;
        this.createdAt = createdAt;
        this.importedAt = importedAt;
    }

    /**
     * 主キーを返す。
     *
     * @return 主キー
     */
    public Long getId() {
        return id;
    }

    /**
     * 主キーを設定する。
     *
     * @param id 主キー
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * ファイルの絶対パスを返す。
     *
     * @return ファイルパス
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * ファイルの絶対パスを設定する。
     *
     * @param filePath ファイルパス
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * ファイル名を返す。
     *
     * @return ファイル名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * ファイル名を設定する。
     *
     * @param fileName ファイル名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * ファイルサイズ（バイト）を返す。
     *
     * @return ファイルサイズ
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * ファイルサイズ（バイト）を設定する。
     *
     * @param fileSize ファイルサイズ
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 幅（ピクセル）を返す。
     *
     * @return 幅
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * 幅（ピクセル）を設定する。
     *
     * @param width 幅
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * 高さ（ピクセル）を返す。
     *
     * @return 高さ
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * 高さ（ピクセル）を設定する。
     *
     * @param height 高さ
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * メディアタイプを返す。
     *
     * @return メディアタイプ（{@code "image"} または {@code "video"}）
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * メディアタイプを設定する。
     *
     * @param mediaType メディアタイプ
     */
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * 作者IDを返す。
     *
     * @return 作者ID（未設定の場合は {@code null}）
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 作者IDを設定する。
     *
     * @param authorId 作者ID
     */
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    /**
     * 作品IDを返す。
     *
     * @return 作品ID（未設定の場合は {@code null}）
     */
    public Long getWorkId() {
        return workId;
    }

    /**
     * 作品IDを設定する。
     *
     * @param workId 作品ID
     */
    public void setWorkId(Long workId) {
        this.workId = workId;
    }

    /**
     * ページ番号を返す。
     *
     * @return ページ番号（1始まり、未設定の場合は {@code null}）
     */
    public Integer getPageNumber() {
        return pageNumber;
    }

    /**
     * ページ番号を設定する。
     *
     * @param pageNumber ページ番号（1始まり）
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Star評価を返す。
     *
     * @return Star評価（0〜5）
     */
    public int getStar() {
        return star;
    }

    /**
     * Star評価を設定する。
     *
     * @param star Star評価（0〜5）
     */
    public void setStar(int star) {
        this.star = star;
    }

    /**
     * 非表示フラグを返す。
     *
     * @return 非表示の場合 {@code true}
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * 非表示フラグを設定する。
     *
     * @param hidden 非表示フラグ
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * 作成日時（ISO8601形式）を返す。
     *
     * @return 作成日時
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 作成日時（ISO8601形式）を設定する。
     *
     * @param createdAt 作成日時
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * インポート日時（ISO8601形式）を返す。
     *
     * @return インポート日時
     */
    public String getImportedAt() {
        return importedAt;
    }

    /**
     * インポート日時（ISO8601形式）を設定する。
     *
     * @param importedAt インポート日時
     */
    public void setImportedAt(String importedAt) {
        this.importedAt = importedAt;
    }

    @Override
    public String toString() {
        return "Image{id=" + id + ", filePath='" + filePath + "', mediaType='" + mediaType + "'}";
    }
}
