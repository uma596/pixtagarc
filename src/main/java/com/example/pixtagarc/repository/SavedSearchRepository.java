package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.SavedSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 保存済み検索条件リポジトリクラス。
 *
 * <p>データベースの {@code saved_searches} テーブルに対するCRUD操作を提供する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SavedSearchRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SavedSearchRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public SavedSearchRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 保存済み検索条件を新規登録する。
     *
     * @param savedSearch 登録する保存済み検索条件エンティティ
     * @return 生成されたIDが設定されたエンティティ
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public SavedSearch save(SavedSearch savedSearch) {
        String sql = """
                INSERT INTO saved_searches
                    (name, keyword, tag_ids, author_ids, date_from, date_to,
                     exclude_hidden, min_star, sort_column, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(ps, savedSearch);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    savedSearch.setId(rs.getLong(1));
                }
            }
            log.debug("保存済み検索を登録しました: id={}, name={}", savedSearch.getId(), savedSearch.getName());
            return savedSearch;
        } catch (SQLException e) {
            log.error("保存済み検索の登録に失敗しました: name={}", savedSearch.getName(), e);
            throw new RuntimeException("保存済み検索の登録に失敗しました", e);
        }
    }

    /**
     * 保存済み検索条件を更新する。
     *
     * @param savedSearch 更新する保存済み検索条件エンティティ
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void update(SavedSearch savedSearch) {
        String sql = """
                UPDATE saved_searches SET
                    name=?, keyword=?, tag_ids=?, author_ids=?, date_from=?, date_to=?,
                    exclude_hidden=?, min_star=?, sort_column=?, sort_order=?, updated_at=?
                WHERE id=?
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, savedSearch.getName());
            ps.setString(2, savedSearch.getKeyword());
            ps.setString(3, savedSearch.getTagIds());
            ps.setString(4, savedSearch.getAuthorIds());
            ps.setString(5, savedSearch.getDateFrom());
            ps.setString(6, savedSearch.getDateTo());
            ps.setInt(7, savedSearch.isExcludeHidden() ? 1 : 0);
            ps.setInt(8, savedSearch.getMinStar());
            ps.setString(9, savedSearch.getSortColumn());
            ps.setString(10, savedSearch.getSortOrder());
            ps.setString(11, savedSearch.getUpdatedAt());
            ps.setLong(12, savedSearch.getId());
            ps.executeUpdate();
            log.debug("保存済み検索を更新しました: id={}", savedSearch.getId());
        } catch (SQLException e) {
            log.error("保存済み検索の更新に失敗しました: id={}", savedSearch.getId(), e);
            throw new RuntimeException("保存済み検索の更新に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの保存済み検索条件を削除する。
     *
     * @param id 削除する保存済み検索条件ID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM saved_searches WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("保存済み検索を削除しました: id={}", id);
        } catch (SQLException e) {
            log.error("保存済み検索の削除に失敗しました: id={}", id, e);
            throw new RuntimeException("保存済み検索の削除に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの保存済み検索条件を返す。
     *
     * @param id 検索する保存済み検索条件ID
     * @return 保存済み検索条件エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<SavedSearch> findById(Long id) {
        String sql = "SELECT * FROM saved_searches WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("保存済み検索の検索に失敗しました: id={}", id, e);
            throw new RuntimeException("保存済み検索の検索に失敗しました", e);
        }
    }

    /**
     * 全保存済み検索条件を名前順で返す。
     *
     * @return 全保存済み検索条件エンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<SavedSearch> findAll() {
        String sql = "SELECT * FROM saved_searches ORDER BY name";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SavedSearch> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            log.error("全保存済み検索の取得に失敗しました", e);
            throw new RuntimeException("全保存済み検索の取得に失敗しました", e);
        }
    }

    /**
     * PreparedStatementにパラメータをセットする（INSERT用）。
     *
     * @param ps          PreparedStatement
     * @param savedSearch 保存済み検索条件エンティティ
     * @throws SQLException セットに失敗した場合
     */
    private void setParameters(PreparedStatement ps, SavedSearch savedSearch) throws SQLException {
        ps.setString(1, savedSearch.getName());
        ps.setString(2, savedSearch.getKeyword());
        ps.setString(3, savedSearch.getTagIds());
        ps.setString(4, savedSearch.getAuthorIds());
        ps.setString(5, savedSearch.getDateFrom());
        ps.setString(6, savedSearch.getDateTo());
        ps.setInt(7, savedSearch.isExcludeHidden() ? 1 : 0);
        ps.setInt(8, savedSearch.getMinStar());
        ps.setString(9, savedSearch.getSortColumn());
        ps.setString(10, savedSearch.getSortOrder());
        ps.setString(11, savedSearch.getCreatedAt());
        ps.setString(12, savedSearch.getUpdatedAt());
    }

    /**
     * ResultSetの現在行を {@link SavedSearch} エンティティにマッピングする。
     *
     * @param rs ResultSet
     * @return 保存済み検索条件エンティティ
     * @throws SQLException マッピングに失敗した場合
     */
    private SavedSearch mapRow(ResultSet rs) throws SQLException {
        SavedSearch ss = new SavedSearch();
        ss.setId(rs.getLong("id"));
        ss.setName(rs.getString("name"));
        ss.setKeyword(rs.getString("keyword"));
        ss.setTagIds(rs.getString("tag_ids"));
        ss.setAuthorIds(rs.getString("author_ids"));
        ss.setDateFrom(rs.getString("date_from"));
        ss.setDateTo(rs.getString("date_to"));
        ss.setExcludeHidden(rs.getInt("exclude_hidden") == 1);
        ss.setMinStar(rs.getInt("min_star"));
        ss.setSortColumn(rs.getString("sort_column"));
        ss.setSortOrder(rs.getString("sort_order"));
        ss.setCreatedAt(rs.getString("created_at"));
        ss.setUpdatedAt(rs.getString("updated_at"));
        return ss;
    }
}
