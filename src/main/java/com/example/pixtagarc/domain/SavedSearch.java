package com.example.pixtagarc.domain;

/**
 * 保存済み検索条件エンティティ。
 *
 * <p>データベースの {@code saved_searches} テーブルに対応するドメインオブジェクト。
 * ユーザーが保存した検索条件を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SavedSearch {

    /** 主キー。新規作成時は {@code null}。 */
    private Long id;

    /** 保存名。UNIQUE制約あり。 */
    private String name;

    /** キーワード（FTS5クエリ文字列）。NULL可。 */
    private String keyword;

    /** タグIDのカンマ区切り文字列。NULL可。 */
    private String tagIds;

    /** 作者IDのカンマ区切り文字列。NULL可。 */
    private String authorIds;

    /** 日付範囲FROM（ISO8601形式）。NULL可。 */
    private String dateFrom;

    /** 日付範囲TO（ISO8601形式）。NULL可。 */
    private String dateTo;

    /** 非表示除外フラグ。{@code true} の場合は非表示を除外。 */
    private boolean excludeHidden = true;

    /** Star最小値フィルタ。0の場合はフィルタなし。 */
    private int minStar = 0;

    /** ソート列名。デフォルトは {@code "created_at"}。 */
    private String sortColumn = "created_at";

    /** ソート順。{@code "ASC"} または {@code "DESC"}。デフォルトは {@code "DESC"}。 */
    private String sortOrder = "DESC";

    /** 作成日時（ISO8601形式）。 */
    private String createdAt;

    /** 更新日時（ISO8601形式）。 */
    private String updatedAt;

    /**
     * デフォルトコンストラクタ。
     */
    public SavedSearch() {
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
     * 保存名を返す。
     *
     * @return 保存名
     */
    public String getName() {
        return name;
    }

    /**
     * 保存名を設定する。
     *
     * @param name 保存名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * キーワードを返す。
     *
     * @return キーワード
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * キーワードを設定する。
     *
     * @param keyword キーワード
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * タグIDのカンマ区切り文字列を返す。
     *
     * @return タグIDs
     */
    public String getTagIds() {
        return tagIds;
    }

    /**
     * タグIDのカンマ区切り文字列を設定する。
     *
     * @param tagIds タグIDs
     */
    public void setTagIds(String tagIds) {
        this.tagIds = tagIds;
    }

    /**
     * 作者IDのカンマ区切り文字列を返す。
     *
     * @return 作者IDs
     */
    public String getAuthorIds() {
        return authorIds;
    }

    /**
     * 作者IDのカンマ区切り文字列を設定する。
     *
     * @param authorIds 作者IDs
     */
    public void setAuthorIds(String authorIds) {
        this.authorIds = authorIds;
    }

    /**
     * 日付範囲FROMを返す。
     *
     * @return 日付範囲FROM（ISO8601形式）
     */
    public String getDateFrom() {
        return dateFrom;
    }

    /**
     * 日付範囲FROMを設定する。
     *
     * @param dateFrom 日付範囲FROM
     */
    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    /**
     * 日付範囲TOを返す。
     *
     * @return 日付範囲TO（ISO8601形式）
     */
    public String getDateTo() {
        return dateTo;
    }

    /**
     * 日付範囲TOを設定する。
     *
     * @param dateTo 日付範囲TO
     */
    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    /**
     * 非表示除外フラグを返す。
     *
     * @return 非表示を除外する場合 {@code true}
     */
    public boolean isExcludeHidden() {
        return excludeHidden;
    }

    /**
     * 非表示除外フラグを設定する。
     *
     * @param excludeHidden 非表示除外フラグ
     */
    public void setExcludeHidden(boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
    }

    /**
     * Star最小値フィルタを返す。
     *
     * @return Star最小値（0の場合はフィルタなし）
     */
    public int getMinStar() {
        return minStar;
    }

    /**
     * Star最小値フィルタを設定する。
     *
     * @param minStar Star最小値（0〜5）
     */
    public void setMinStar(int minStar) {
        this.minStar = minStar;
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
     * 更新日時を返す。
     *
     * @return 更新日時（ISO8601形式）
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 更新日時を設定する。
     *
     * @param updatedAt 更新日時
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SavedSearch{id=" + id + ", name='" + name + "'}";
    }
}
