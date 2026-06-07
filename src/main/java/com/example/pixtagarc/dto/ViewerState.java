package com.example.pixtagarc.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * ビューア状態DTO。
 *
 * <p>ビューア画面の表示状態を保持するデータ転送オブジェクト。
 * 各ビューアウィンドウが独立したインスタンスを持つ。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ViewerState {

    /**
     * 表示モード列挙型。
     */
    public enum DisplayMode {
        /** 単ページ表示。 */
        SINGLE,
        /** 見開き表示。 */
        SPREAD
    }

    /**
     * 綴じ方向列挙型。
     */
    public enum BindingDirection {
        /** 右綴じ（右から左へ読む）。 */
        RIGHT_TO_LEFT,
        /** 左綴じ（左から右へ読む）。 */
        LEFT_TO_RIGHT
    }

    /** 現在の表示モード。デフォルトは単ページ表示。 */
    private DisplayMode displayMode = DisplayMode.SINGLE;

    /** 綴じ方向。デフォルトは右綴じ。 */
    private BindingDirection bindingDirection = BindingDirection.RIGHT_TO_LEFT;

    /** 現在表示中のインデックス（imageListの0始まりインデックス）。 */
    private int currentIndex = 0;

    /**
     * 見開きページオフセット。
     * 0の場合は先頭を単独表示、+1の場合は先頭から見開き表示。
     */
    private int pageOffset = 0;

    /** このビューアが表示する画像リスト（検索結果のスナップショット）。 */
    private List<ImageSummary> imageList = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public ViewerState() {
    }

    /**
     * 画像リストと初期インデックスを指定するコンストラクタ。
     *
     * @param imageList    表示する画像リスト
     * @param currentIndex 初期表示インデックス
     */
    public ViewerState(List<ImageSummary> imageList, int currentIndex) {
        this.imageList = imageList != null ? new ArrayList<>(imageList) : new ArrayList<>();
        this.currentIndex = currentIndex;
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
     * 綴じ方向を返す。
     *
     * @return 綴じ方向
     */
    public BindingDirection getBindingDirection() {
        return bindingDirection;
    }

    /**
     * 綴じ方向を設定する。
     *
     * @param bindingDirection 綴じ方向
     */
    public void setBindingDirection(BindingDirection bindingDirection) {
        this.bindingDirection = bindingDirection;
    }

    /**
     * 現在のインデックスを返す。
     *
     * @return 現在のインデックス（0始まり）
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 現在のインデックスを設定する。
     *
     * @param currentIndex 現在のインデックス
     */
    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    /**
     * ページオフセットを返す。
     *
     * @return ページオフセット
     */
    public int getPageOffset() {
        return pageOffset;
    }

    /**
     * ページオフセットを設定する。
     *
     * @param pageOffset ページオフセット
     */
    public void setPageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
    }

    /**
     * 画像リストを返す。
     *
     * @return 画像リスト
     */
    public List<ImageSummary> getImageList() {
        return imageList;
    }

    /**
     * 画像リストを設定する。
     *
     * @param imageList 画像リスト
     */
    public void setImageList(List<ImageSummary> imageList) {
        this.imageList = imageList != null ? new ArrayList<>(imageList) : new ArrayList<>();
    }

    /**
     * 現在表示中の画像サマリーを返す。
     *
     * @return 現在の画像サマリー。リストが空の場合は {@code null}。
     */
    public ImageSummary getCurrentImage() {
        if (imageList.isEmpty() || currentIndex < 0 || currentIndex >= imageList.size()) {
            return null;
        }
        return imageList.get(currentIndex);
    }

    /**
     * 前のページに移動できるかを返す。
     *
     * @return 前のページが存在する場合 {@code true}
     */
    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    /**
     * 次のページに移動できるかを返す。
     *
     * @return 次のページが存在する場合 {@code true}
     */
    public boolean hasNext() {
        return currentIndex < imageList.size() - 1;
    }

    /**
     * 画像リストの総件数を返す。
     *
     * @return 総件数
     */
    public int getTotalCount() {
        return imageList.size();
    }
}
