package com.example.pixtagarc.domain;

/**
 * 画像-タグ関連エンティティ。
 *
 * <p>データベースの {@code image_tags} テーブルに対応するドメインオブジェクト。
 * 画像とタグの多対多関係を表す中間テーブルのエンティティ。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageTag {

    /** 画像ID。{@code images} テーブルへの外部キー。 */
    private Long imageId;

    /** タグID。{@code tags} テーブルへの外部キー。 */
    private Long tagId;

    /**
     * デフォルトコンストラクタ。
     */
    public ImageTag() {
    }

    /**
     * 画像IDとタグIDを指定するコンストラクタ。
     *
     * @param imageId 画像ID
     * @param tagId   タグID
     */
    public ImageTag(Long imageId, Long tagId) {
        this.imageId = imageId;
        this.tagId = tagId;
    }

    /**
     * 画像IDを返す。
     *
     * @return 画像ID
     */
    public Long getImageId() {
        return imageId;
    }

    /**
     * 画像IDを設定する。
     *
     * @param imageId 画像ID
     */
    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    /**
     * タグIDを返す。
     *
     * @return タグID
     */
    public Long getTagId() {
        return tagId;
    }

    /**
     * タグIDを設定する。
     *
     * @param tagId タグID
     */
    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public String toString() {
        return "ImageTag{imageId=" + imageId + ", tagId=" + tagId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageTag)) return false;
        ImageTag imageTag = (ImageTag) o;
        return imageId != null && imageId.equals(imageTag.imageId)
                && tagId != null && tagId.equals(imageTag.tagId);
    }

    @Override
    public int hashCode() {
        int result = imageId != null ? imageId.hashCode() : 0;
        result = 31 * result + (tagId != null ? tagId.hashCode() : 0);
        return result;
    }
}
