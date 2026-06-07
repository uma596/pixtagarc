package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * ビューアセッション情報DTO。
 *
 * <p>ビューアウィンドウの「開かれた条件」を保持し、
 * 次回起動時に同条件で再検索→ビューア復元するための情報を格納する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ViewerSession {

    /** セッションモード: "work"（作品表示）または "search"（検索結果表示）。 */
    private String mode = "search";

    /** 作品ID（mode="work" の場合）。 */
    private Long workId;

    /** 検索キーワード（mode="search" の場合）。 */
    private String keyword;

    /** タグIDリスト。 */
    private List<Long> tagIds = new ArrayList<>();

    /** 作者IDリスト。 */
    private List<Long> authorIds = new ArrayList<>();

    /** Star最小値フィルタ。 */
    private int minStar = 0;

    /** 非表示除外フラグ。 */
    private boolean excludeHidden = true;

    /** 表示方式（"SINGLE" or "SPREAD"）。 */
    private String pageMode = "SINGLE";

    /** 綴じ方向（"RIGHT_TO_LEFT" or "LEFT_TO_RIGHT"）。 */
    private String bindingDirection = "RIGHT_TO_LEFT";

    /** ウィンドウX座標。 */
    private double windowX;

    /** ウィンドウY座標。 */
    private double windowY;

    /** ウィンドウ幅。 */
    private double windowWidth = 1000;

    /** ウィンドウ高さ。 */
    private double windowHeight = 700;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Long getWorkId() { return workId; }
    public void setWorkId(Long workId) { this.workId = workId; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds != null ? tagIds : new ArrayList<>(); }

    public List<Long> getAuthorIds() { return authorIds; }
    public void setAuthorIds(List<Long> authorIds) { this.authorIds = authorIds != null ? authorIds : new ArrayList<>(); }

    public int getMinStar() { return minStar; }
    public void setMinStar(int minStar) { this.minStar = minStar; }

    public boolean isExcludeHidden() { return excludeHidden; }
    public void setExcludeHidden(boolean excludeHidden) { this.excludeHidden = excludeHidden; }

    public String getPageMode() { return pageMode; }
    public void setPageMode(String pageMode) { this.pageMode = pageMode; }

    public String getBindingDirection() { return bindingDirection; }
    public void setBindingDirection(String bindingDirection) { this.bindingDirection = bindingDirection; }

    public double getWindowX() { return windowX; }
    public void setWindowX(double windowX) { this.windowX = windowX; }

    public double getWindowY() { return windowY; }
    public void setWindowY(double windowY) { this.windowY = windowY; }

    public double getWindowWidth() { return windowWidth; }
    public void setWindowWidth(double windowWidth) { this.windowWidth = windowWidth; }

    public double getWindowHeight() { return windowHeight; }
    public void setWindowHeight(double windowHeight) { this.windowHeight = windowHeight; }

    /**
     * 検索条件に変換する（mode="search" の場合）。
     *
     * @return SearchCondition
     */
    public SearchCondition toSearchCondition() {
        SearchCondition condition = new SearchCondition();
        condition.setKeyword(keyword);
        condition.setTagIds(tagIds);
        condition.setAuthorIds(authorIds);
        condition.setMinStar(minStar);
        condition.setExcludeHidden(excludeHidden);
        return condition;
    }
}
