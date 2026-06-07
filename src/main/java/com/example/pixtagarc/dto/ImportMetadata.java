package com.example.pixtagarc.dto;

import java.util.List;

/**
 * インポート用メタデータDTO。
 *
 * <p>{@code -meta.json} ファイルのJSON構造をマッピングするデータ転送オブジェクト。
 * Gsonによるデシリアライズ対象クラス。
 *
 * <p>JSONフィールドのうち、インポートに必要なフィールドのみをマッピングする。
 * 未使用フィールドはGsonにより無視される。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImportMetadata {

    /** 作品の外部ID（数値）。 */
    private Long idNum;

    /** 作品のID文字列（例: "12345678_p0"）。 */
    private String id;

    /** 作品タイトル。 */
    private String title;

    /** 総ページ数。 */
    private Integer pageCount;

    /** ページインデックス（0始まり）。 */
    private Integer index;

    /** タグ名のリスト。 */
    private List<String> tags;

    /** 作者名。 */
    private String user;

    /** 作者の外部ID。 */
    private String userId;

    /** 画像の幅（ピクセル）。 */
    private Integer fullWidth;

    /** 画像の高さ（ピクセル）。 */
    private Integer fullHeight;

    /** ファイル拡張子。 */
    private String ext;

    /** 投稿日時（ISO8601形式、タイムゾーン付き）。 */
    private String date;

    /** アップロード日時（ISO8601形式、タイムゾーン付き）。 */
    private String uploadDate;

    /**
     * デフォルトコンストラクタ。Gsonデシリアライズ用。
     */
    public ImportMetadata() {
    }

    /**
     * 作品の外部ID（数値）を返す。
     *
     * @return 外部ID
     */
    public Long getIdNum() {
        return idNum;
    }

    /**
     * 作品の外部ID（数値）を設定する。
     *
     * @param idNum 外部ID
     */
    public void setIdNum(Long idNum) {
        this.idNum = idNum;
    }

    /**
     * 作品のID文字列を返す。
     *
     * @return ID文字列（例: "12345678_p0"）
     */
    public String getId() {
        return id;
    }

    /**
     * 作品のID文字列を設定する。
     *
     * @param id ID文字列
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 作品タイトルを返す。
     *
     * @return 作品タイトル
     */
    public String getTitle() {
        return title;
    }

    /**
     * 作品タイトルを設定する。
     *
     * @param title 作品タイトル
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 総ページ数を返す。
     *
     * @return 総ページ数
     */
    public Integer getPageCount() {
        return pageCount;
    }

    /**
     * 総ページ数を設定する。
     *
     * @param pageCount 総ページ数
     */
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * ページインデックス（0始まり）を返す。
     *
     * @return ページインデックス
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * ページインデックス（0始まり）を設定する。
     *
     * @param index ページインデックス
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * タグ名のリストを返す。
     *
     * @return タグ名リスト
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * タグ名のリストを設定する。
     *
     * @param tags タグ名リスト
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 作者名を返す。
     *
     * @return 作者名
     */
    public String getUser() {
        return user;
    }

    /**
     * 作者名を設定する。
     *
     * @param user 作者名
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * 作者の外部IDを返す。
     *
     * @return 作者外部ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 作者の外部IDを設定する。
     *
     * @param userId 作者外部ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 画像の幅（ピクセル）を返す。
     *
     * @return 幅
     */
    public Integer getFullWidth() {
        return fullWidth;
    }

    /**
     * 画像の幅（ピクセル）を設定する。
     *
     * @param fullWidth 幅
     */
    public void setFullWidth(Integer fullWidth) {
        this.fullWidth = fullWidth;
    }

    /**
     * 画像の高さ（ピクセル）を返す。
     *
     * @return 高さ
     */
    public Integer getFullHeight() {
        return fullHeight;
    }

    /**
     * 画像の高さ（ピクセル）を設定する。
     *
     * @param fullHeight 高さ
     */
    public void setFullHeight(Integer fullHeight) {
        this.fullHeight = fullHeight;
    }

    /**
     * ファイル拡張子を返す。
     *
     * @return 拡張子
     */
    public String getExt() {
        return ext;
    }

    /**
     * ファイル拡張子を設定する。
     *
     * @param ext 拡張子
     */
    public void setExt(String ext) {
        this.ext = ext;
    }

    /**
     * 投稿日時（ISO8601形式）を返す。
     *
     * @return 投稿日時
     */
    public String getDate() {
        return date;
    }

    /**
     * 投稿日時（ISO8601形式）を設定する。
     *
     * @param date 投稿日時
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * アップロード日時（ISO8601形式）を返す。
     *
     * @return アップロード日時
     */
    public String getUploadDate() {
        return uploadDate;
    }

    /**
     * アップロード日時（ISO8601形式）を設定する。
     *
     * @param uploadDate アップロード日時
     */
    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    @Override
    public String toString() {
        return "ImportMetadata{idNum=" + idNum + ", title='" + title + "', user='" + user + "'}";
    }
}
