package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * タグリポジトリクラス。
 *
 * <p>データベースの {@code tags} テーブルに対するCRUD操作を提供する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class TagRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(TagRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public TagRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * タグを新規登録する。
     *
     * @param tag 登録するタグエンティティ
     * @return 生成されたIDが設定されたタグエンティティ
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Tag save(Tag tag) {
        String sql = "INSERT INTO tags (name) VALUES (?)";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    tag.setId(rs.getLong(1));
                }
            }
            log.debug("タグを登録しました: id={}, name={}", tag.getId(), tag.getName());
            return tag;
        } catch (SQLException e) {
            log.error("タグの登録に失敗しました: name={}", tag.getName(), e);
            throw new RuntimeException("タグの登録に失敗しました", e);
        }
    }

    /**
     * タグ名を更新する。
     *
     * @param id      更新するタグID
     * @param newName 新しいタグ名
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void updateName(Long id, String newName) {
        String sql = "UPDATE tags SET name=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
            log.debug("タグ名を更新しました: id={}, newName={}", id, newName);
        } catch (SQLException e) {
            log.error("タグ名の更新に失敗しました: id={}", id, e);
            throw new RuntimeException("タグ名の更新に失敗しました", e);
        }
    }

    /**
     * 指定されたIDのタグを削除する。
     *
     * @param id 削除するタグID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM tags WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("タグを削除しました: id={}", id);
        } catch (SQLException e) {
            log.error("タグの削除に失敗しました: id={}", id, e);
            throw new RuntimeException("タグの削除に失敗しました", e);
        }
    }

    /**
     * 指定されたIDのタグを返す。
     *
     * @param id 検索するタグID
     * @return タグエンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Tag> findById(Long id) {
        String sql = "SELECT * FROM tags WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("タグの検索に失敗しました: id={}", id, e);
            throw new RuntimeException("タグの検索に失敗しました", e);
        }
    }

    /**
     * 指定された名前のタグを返す。
     *
     * @param name 検索するタグ名
     * @return タグエンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Tag> findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("タグの検索に失敗しました: name={}", name, e);
            throw new RuntimeException("タグの検索に失敗しました", e);
        }
    }

    /**
     * 全タグを名前順で返す。
     *
     * @return 全タグエンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags ORDER BY name";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Tag> tags = new ArrayList<>();
            while (rs.next()) {
                tags.add(mapRow(rs));
            }
            return tags;
        } catch (SQLException e) {
            log.error("全タグの取得に失敗しました", e);
            throw new RuntimeException("全タグの取得に失敗しました", e);
        }
    }

    /**
     * ResultSetの現在行を {@link Tag} エンティティにマッピングする。
     *
     * @param rs ResultSet
     * @return タグエンティティ
     * @throws SQLException マッピングに失敗した場合
     */
    private Tag mapRow(ResultSet rs) throws SQLException {
        return new Tag(rs.getLong("id"), rs.getString("name"));
    }
}
