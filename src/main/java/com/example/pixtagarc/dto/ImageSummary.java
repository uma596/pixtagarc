package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 画像サマリーDTO。
 *
 * <p>サムネイル一覧・リスト表示用の軽量データ転送オブジェクト。
 * 画像の主要メタデータとサムネイルパスを保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageSummary {

    /** 画像ID。 */
    private Long id;

    /** ファイルの絶対パス。 */
    private String filePath;

    /** ファイル名。 */
    private String fileName;

    /** メディアタイプ（{@code "image"} または {@code "video"}）。 */
    private String mediaType;

    /** 作者名。未設定の場合は {@code null}。 */
    private String authorName;

    /** タグ名のリスト。 */
    private List<String> tagNames = new ArrayList<>();

    /** 非表示フラグ。 */
    private boolean hidden;

    /** 作成日時（ISO8601形式）。 */
    private String createdAt;

    /** サムネイル画像のファイルパス。未生成の場合は {@code null}。 */
    private String thumbnailPath;

    /** ファイルサイズ（バイト）。 */
    private Long fileSize;

    /** 幅（ピクセル）。 */
    private Integer width;

    /** 高さ（ピクセル）。 */
    private Integer height;

    /** Star評価（0〜5）。 */
    private int star;

    /** 作品ID。未設定の場合は {@code null}。 */
    private Long workId;

    /** ページ番号（1始まり）。未設定の場合は {@code null}。 */
    private Integer pageNumber;

    /**
     * デフォルトコンストラクタ。
     */
    public ImageSummary() {
    }

    /**
     * 画像IDを返す。
     *
     * @return 画像ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 画像IDを設定する。
     *
     * @param id 画像ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * ファイルパスを返す。
     *
     * @return ファイルパス
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * ファイルパスを設定する。
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
     * メディアタイプを返す。
     *
     * @return メディアタイプ
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
     * 作者名を返す。
     *
     * @return 作者名（未設定の場合は {@code null}）
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * 作者名を設定する。
     *
     * @param authorName 作者名
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * タグ名リストを返す。
     *
     * @return タグ名リスト
     */
    public List<String> getTagNames() {
        return tagNames;
    }

    /**
     * タグ名リストを設定する。
     *
     * @param tagNames タグ名リスト
     */
    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames != null ? tagNames : new ArrayList<>();
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
     * 作成日時を返す。
     *
     * @return 作成日時（ISO8601形式）
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 作成日時を設定する。
     *
     * @param createdAt 作成日時
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * サムネイルパスを返す。
     *
     * @return サムネイルパス（未生成の場合は {@code null}）
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    /**
     * サムネイルパスを設定する。
     *
     * @param thumbnailPath サムネイルパス
     */
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * ファイルサイズを返す。
     *
     * @return ファイルサイズ（バイト）
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * ファイルサイズを設定する。
     *
     * @param fileSize ファイルサイズ（バイト）
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
     * 解像度を文字列で返す（例: {@code "1920x1080"}）。
     *
     * @return 解像度文字列。幅または高さが未設定の場合は空文字。
     */
    public String getResolutionString() {
        if (width != null && height != null) {
            return width + "x" + height;
        }
        return "";
    }

    /**
     * ファイルサイズを人間が読みやすい形式で返す（例: {@code "3.2 MB"}）。
     *
     * @return ファイルサイズ文字列
     */
    public String getFileSizeString() {
        if (fileSize == null) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    @Override
    public String toString() {
        return "ImageSummary{id=" + id + ", fileName='" + fileName + "'}";
    }
}
