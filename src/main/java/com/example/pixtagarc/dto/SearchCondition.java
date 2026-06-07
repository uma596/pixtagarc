package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 検索条件DTO。
 *
 * <p>画像検索に使用する条件をまとめたデータ転送オブジェクト。
 * キーワード・タグ・作者・日付範囲・非表示除外フラグ・ページング情報を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SearchCondition {

    /** キーワード（FTS5クエリ文字列）。空文字またはNULLの場合は全件対象。 */
    private String keyword;

    /** 絞り込むタグIDのリスト。空の場合はタグ絞り込みなし。 */
    private List<Long> tagIds = new ArrayList<>();

    /** 絞り込む作者IDのリスト。空の場合は作者絞り込みなし。 */
    private List<Long> authorIds = new ArrayList<>();

    /** 日付範囲FROM（ISO8601形式）。NULLの場合は下限なし。 */
    private String dateFrom;

    /** 日付範囲TO（ISO8601形式）。NULLの場合は上限なし。 */
    private String dateTo;

    /** 非表示除外フラグ。{@code true} の場合は非表示ファイルを除外する。デフォルトは {@code true}。 */
    private boolean excludeHidden = true;

    /** Star最小値フィルタ。0の場合はフィルタなし。 */
    private int minStar = 0;

    /** 作品ID絞り込み。{@code null} の場合はフィルタなし。 */
    private Long workId;

    /** ソート列名。デフォルトは {@code "created_at"}。 */
    private String sortColumn = "created_at";

    /** ソート順。{@code "ASC"} または {@code "DESC"}。デフォルトは {@code "DESC"}。 */
    private String sortOrder = "DESC";

    /** ページ番号（0始まり）。 */
    private int page = 0;

    /** 1ページあたりの件数。デフォルトは100件。 */
    private int pageSize = 100;

    /**
     * デフォルトコンストラクタ。
     */
    public SearchCondition() {
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
     * タグIDリストを返す。
     *
     * @return タグIDリスト
     */
    public List<Long> getTagIds() {
        return tagIds;
    }

    /**
     * タグIDリストを設定する。
     *
     * @param tagIds タグIDリスト
     */
    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds != null ? tagIds : new ArrayList<>();
    }

    /**
     * 作者IDリストを返す。
     *
     * @return 作者IDリスト
     */
    public List<Long> getAuthorIds() {
        return authorIds;
    }

    /**
     * 作者IDリストを設定する。
     *
     * @param authorIds 作者IDリスト
     */
    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds != null ? authorIds : new ArrayList<>();
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
     * 作品ID絞り込みを返す。
     *
     * @return 作品ID（{@code null} の場合はフィルタなし）
     */
    public Long getWorkId() {
        return workId;
    }

    /**
     * 作品ID絞り込みを設定する。
     *
     * @param workId 作品ID（{@code null} でフィルタ解除）
     */
    public void setWorkId(Long workId) {
        this.workId = workId;
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
     * ページ番号（0始まり）を返す。
     *
     * @return ページ番号
     */
    public int getPage() {
        return page;
    }

    /**
     * ページ番号（0始まり）を設定する。
     *
     * @param page ページ番号
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 1ページあたりの件数を返す。
     *
     * @return 1ページあたりの件数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 1ページあたりの件数を設定する。
     *
     * @param pageSize 1ページあたりの件数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * OFFSETを計算して返す。
     *
     * @return OFFSET値（page * pageSize）
     */
    public int getOffset() {
        return page * pageSize;
    }
}
