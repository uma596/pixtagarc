package com.example.pixtagarc.domain;

/**
 * タグエンティティ。
 *
 * <p>データベースの {@code tags} テーブルに対応するドメインオブジェクト。
 * 画像に付与するタグ情報を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class Tag {

    /** 主キー。新規作成時は {@code null}。 */
    private Long id;

    /** タグ名。UNIQUE制約あり。 */
    private String name;

    /**
     * デフォルトコンストラクタ。
     */
    public Tag() {
    }

    /**
     * IDと名前を指定するコンストラクタ。
     *
     * @param id   主キー
     * @param name タグ名
     */
    public Tag(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 名前のみを指定するコンストラクタ（新規作成用）。
     *
     * @param name タグ名
     */
    public Tag(String name) {
        this.name = name;
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
     * タグ名を返す。
     *
     * @return タグ名
     */
    public String getName() {
        return name;
    }

    /**
     * タグ名を設定する。
     *
     * @param name タグ名
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", name='" + name + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return id != null && id.equals(tag.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
