package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 検索結果DTO。
 *
 * <p>画像検索の結果をまとめたデータ転送オブジェクト。
 * 検索結果のアイテムリストとページング情報を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SearchResult {

    /** 検索結果のアイテムリスト。 */
    private List<ImageSummary> items = new ArrayList<>();

    /** 検索条件に一致する総件数（ページングを無視した全件数）。 */
    private long totalCount;

    /** 現在のページ番号（0始まり）。 */
    private int page;

    /** 1ページあたりの件数。 */
    private int pageSize;

    /**
     * デフォルトコンストラクタ。
     */
    public SearchResult() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param items      検索結果アイテムリスト
     * @param totalCount 総件数
     * @param page       ページ番号
     * @param pageSize   1ページあたりの件数
     */
    public SearchResult(List<ImageSummary> items, long totalCount, int page, int pageSize) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 検索結果アイテムリストを返す。
     *
     * @return アイテムリスト
     */
    public List<ImageSummary> getItems() {
        return items;
    }

    /**
     * 検索結果アイテムリストを設定する。
     *
     * @param items アイテムリスト
     */
    public void setItems(List<ImageSummary> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    /**
     * 総件数を返す。
     *
     * @return 総件数
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * 総件数を設定する。
     *
     * @param totalCount 総件数
     */
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
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
     * 総ページ数を計算して返す。
     *
     * @return 総ページ数
     */
    public int getTotalPages() {
        if (pageSize <= 0) return 0;
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    /**
     * 次のページが存在するかを返す。
     *
     * @return 次のページが存在する場合 {@code true}
     */
    public boolean hasNextPage() {
        return page < getTotalPages() - 1;
    }

    /**
     * 前のページが存在するかを返す。
     *
     * @return 前のページが存在する場合 {@code true}
     */
    public boolean hasPreviousPage() {
        return page > 0;
    }

    @Override
    public String toString() {
        return "SearchResult{totalCount=" + totalCount + ", page=" + page
                + ", pageSize=" + pageSize + ", items.size=" + items.size() + "}";
    }
}
