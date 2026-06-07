package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作者リポジトリクラス。
 *
 * <p>データベースの {@code authors} テーブルに対するCRUD操作を提供する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class AuthorRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(AuthorRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public AuthorRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 作者を新規登録する。
     *
     * @param author 登録する作者エンティティ
     * @return 生成されたIDが設定された作者エンティティ
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Author save(Author author) {
        String sql = "INSERT INTO authors (name) VALUES (?)";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    author.setId(rs.getLong(1));
                }
            }
            log.debug("作者を登録しました: id={}, name={}", author.getId(), author.getName());
            return author;
        } catch (SQLException e) {
            log.error("作者の登録に失敗しました: name={}", author.getName(), e);
            throw new RuntimeException("作者の登録に失敗しました", e);
        }
    }

    /**
     * 作者名を更新する。
     *
     * @param id      更新する作者ID
     * @param newName 新しい作者名
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void updateName(Long id, String newName) {
        String sql = "UPDATE authors SET name=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
            log.debug("作者名を更新しました: id={}, newName={}", id, newName);
        } catch (SQLException e) {
            log.error("作者名の更新に失敗しました: id={}", id, e);
            throw new RuntimeException("作者名の更新に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの作者を削除する。
     *
     * @param id 削除する作者ID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM authors WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("作者を削除しました: id={}", id);
        } catch (SQLException e) {
            log.error("作者の削除に失敗しました: id={}", id, e);
            throw new RuntimeException("作者の削除に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの作者を返す。
     *
     * @param id 検索する作者ID
     * @return 作者エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Author> findById(Long id) {
        String sql = "SELECT * FROM authors WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("作者の検索に失敗しました: id={}", id, e);
            throw new RuntimeException("作者の検索に失敗しました", e);
        }
    }

    /**
     * 指定された名前の作者を返す。
     *
     * @param name 検索する作者名
     * @return 作者エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Author> findByName(String name) {
        String sql = "SELECT * FROM authors WHERE name=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("作者の検索に失敗しました: name={}", name, e);
            throw new RuntimeException("作者の検索に失敗しました", e);
        }
    }

    /**
     * 全作者を名前順で返す。
     *
     * @return 全作者エンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Author> findAll() {
        String sql = "SELECT * FROM authors ORDER BY name";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Author> authors = new ArrayList<>();
            while (rs.next()) {
                authors.add(mapRow(rs));
            }
            return authors;
        } catch (SQLException e) {
            log.error("全作者の取得に失敗しました", e);
            throw new RuntimeException("全作者の取得に失敗しました", e);
        }
    }

    /**
     * ResultSetの現在行を {@link Author} エンティティにマッピングする。
     *
     * @param rs ResultSet
     * @return 作者エンティティ
     * @throws SQLException マッピングに失敗した場合
     */
    private Author mapRow(ResultSet rs) throws SQLException {
        return new Author(rs.getLong("id"), rs.getString("name"));
    }
}
