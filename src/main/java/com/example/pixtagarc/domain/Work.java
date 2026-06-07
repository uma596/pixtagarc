package com.example.pixtagarc.domain;

/**
 * 作品エンティティ。
 *
 * <p>データベースの {@code works} テーブルに対応するドメインオブジェクト。
 * 複数の画像ファイルをひとまとまりの作品として管理する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class Work {

    /** 主キー。新規作成時は {@code null}。 */
    private Long id;

    /** 作品タイトル。 */
    private String title;

    /** 作者ID。{@code authors} テーブルへの外部キー。NULL可。 */
    private Long authorId;

    /** 外部ID（外部サービスでの識別子）。NULL可。UNIQUE制約あり。 */
    private String externalId;

    /** 総ページ数。NULL可。 */
    private Integer totalPages;

    /** 作成日時（ISO8601形式）。 */
    private String createdAt;

    /** 更新日時（ISO8601形式）。 */
    private String updatedAt;

    /**
     * デフォルトコンストラクタ。
     */
    public Work() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param id         主キー
     * @param title      作品タイトル
     * @param authorId   作者ID
     * @param externalId 外部ID
     * @param totalPages 総ページ数
     * @param createdAt  作成日時
     * @param updatedAt  更新日時
     */
    public Work(Long id, String title, Long authorId, String externalId,
                Integer totalPages, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.authorId = authorId;
        this.externalId = externalId;
        this.totalPages = totalPages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
     * 外部IDを返す。
     *
     * @return 外部ID（未設定の場合は {@code null}）
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * 外部IDを設定する。
     *
     * @param externalId 外部ID
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * 総ページ数を返す。
     *
     * @return 総ページ数（未設定の場合は {@code null}）
     */
    public Integer getTotalPages() {
        return totalPages;
    }

    /**
     * 総ページ数を設定する。
     *
     * @param totalPages 総ページ数
     */
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
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
     * 更新日時（ISO8601形式）を返す。
     *
     * @return 更新日時
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 更新日時（ISO8601形式）を設定する。
     *
     * @param updatedAt 更新日時
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Work{id=" + id + ", title='" + title + "', externalId='" + externalId + "'}";
    }
}
