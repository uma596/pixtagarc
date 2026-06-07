package com.example.pixtagarc.domain;

/**
 * 作者エンティティ。
 *
 * <p>データベースの {@code authors} テーブルに対応するドメインオブジェクト。
 * 画像の作者情報を保持する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class Author {

    /** 主キー。新規作成時は {@code null}。 */
    private Long id;

    /** 作者名。UNIQUE制約あり。 */
    private String name;

    /**
     * デフォルトコンストラクタ。
     */
    public Author() {
    }

    /**
     * IDと名前を指定するコンストラクタ。
     *
     * @param id   主キー
     * @param name 作者名
     */
    public Author(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 名前のみを指定するコンストラクタ（新規作成用）。
     *
     * @param name 作者名
     */
    public Author(String name) {
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
     * 作者名を返す。
     *
     * @return 作者名
     */
    public String getName() {
        return name;
    }

    /**
     * 作者名を設定する。
     *
     * @param name 作者名
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author)) return false;
        Author author = (Author) o;
        return id != null && id.equals(author.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
