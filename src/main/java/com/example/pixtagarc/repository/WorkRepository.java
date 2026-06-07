package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作品リポジトリクラス。
 *
 * <p>データベースの {@code works} テーブルに対するCRUD操作を提供する。
 * すべてのSQLはPreparedStatementを使用してSQLインジェクションを防ぐ。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class WorkRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(WorkRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public WorkRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 作品を新規登録する。
     *
     * @param work 登録する作品エンティティ（idはnullであること）
     * @return 生成されたID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Long save(Work work) {
        String sql = """
                INSERT INTO works (title, author_id, external_id, total_pages, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, work.getTitle());
            setNullableLong(ps, 2, work.getAuthorId());
            ps.setString(3, work.getExternalId());
            setNullableInt(ps, 4, work.getTotalPages());
            ps.setString(5, work.getCreatedAt());
            ps.setString(6, work.getUpdatedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    work.setId(id);
                    log.debug("作品を登録しました: id={}, title={}", id, work.getTitle());
                    return id;
                }
            }
            throw new RuntimeException("作品の登録後にIDを取得できませんでした");
        } catch (SQLException e) {
            log.error("作品の登録に失敗しました: title={}", work.getTitle(), e);
            throw new RuntimeException("作品の登録に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの作品を返す。
     *
     * @param id 検索する作品ID
     * @return 作品エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Work> findById(Long id) {
        String sql = "SELECT * FROM works WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("作品の検索に失敗しました: id={}", id, e);
            throw new RuntimeException("作品の検索に失敗しました", e);
        }
    }

    /**
     * 指定された外部IDの作品を返す。
     *
     * @param externalId 検索する外部ID
     * @return 作品エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Work> findByExternalId(String externalId) {
        String sql = "SELECT * FROM works WHERE external_id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("作品の検索に失敗しました: externalId={}", externalId, e);
            throw new RuntimeException("作品の検索に失敗しました", e);
        }
    }

    /**
     * 全作品を返す。
     *
     * @return 全作品エンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Work> findAll() {
        String sql = "SELECT * FROM works ORDER BY title";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Work> works = new ArrayList<>();
            while (rs.next()) {
                works.add(mapRow(rs));
            }
            return works;
        } catch (SQLException e) {
            log.error("全作品の取得に失敗しました", e);
            throw new RuntimeException("全作品の取得に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの作品を削除する。
     *
     * @param id 削除する作品ID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM works WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("作品を削除しました: id={}", id);
        } catch (SQLException e) {
            log.error("作品の削除に失敗しました: id={}", id, e);
            throw new RuntimeException("作品の削除に失敗しました", e);
        }
    }

    /**
     * 全作品を削除する。
     *
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteAll() {
        String sql = "DELETE FROM works";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            int count = ps.executeUpdate();
            log.debug("全作品を削除しました: count={}", count);
        } catch (SQLException e) {
            log.error("全作品の削除に失敗しました", e);
            throw new RuntimeException("全作品の削除に失敗しました", e);
        }
    }

    /**
     * フォルダ作品の external_id の最大連番を返す。
     *
     * <p>{@code external_id} が {@code 'F'} + 10桁数字の形式のレコードから最大値を取得する。
     *
     * @return 最大連番（存在しない場合は 0）
     */
    public long getMaxFolderExternalId() {
        String sql = "SELECT MAX(CAST(SUBSTR(external_id, 2) AS INTEGER)) FROM works WHERE external_id LIKE 'F%'";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long value = rs.getLong(1);
                return rs.wasNull() ? 0 : value;
            }
            return 0;
        } catch (SQLException e) {
            log.error("フォルダ作品の最大連番取得に失敗しました", e);
            return 0;
        }
    }

    /**
     * 作品の総ページ数を更新する。
     *
     * @param id         作品ID
     * @param totalPages 総ページ数
     */
    public void updateTotalPages(Long id, int totalPages) {
        String sql = "UPDATE works SET total_pages = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, totalPages);
            ps.setString(2, java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            ps.setLong(3, id);
            ps.executeUpdate();
            log.debug("作品の総ページ数を更新しました: id={}, totalPages={}", id, totalPages);
        } catch (SQLException e) {
            log.error("作品の総ページ数更新に失敗しました: id={}", id, e);
            throw new RuntimeException("作品の総ページ数更新に失敗しました", e);
        }
    }

    /**
     * ResultSetの現在行を {@link Work} エンティティにマッピングする。
     *
     * @param rs ResultSet
     * @return 作品エンティティ
     * @throws SQLException マッピングに失敗した場合
     */
    private Work mapRow(ResultSet rs) throws SQLException {
        Work work = new Work();
        work.setId(rs.getLong("id"));
        work.setTitle(rs.getString("title"));
        long authorId = rs.getLong("author_id");
        work.setAuthorId(rs.wasNull() ? null : authorId);
        work.setExternalId(rs.getString("external_id"));
        int totalPages = rs.getInt("total_pages");
        work.setTotalPages(rs.wasNull() ? null : totalPages);
        work.setCreatedAt(rs.getString("created_at"));
        work.setUpdatedAt(rs.getString("updated_at"));
        return work;
    }

    /**
     * NullableなLong値をPreparedStatementにセットする。
     *
     * @param ps    PreparedStatement
     * @param index パラメータインデックス
     * @param value セットする値（nullの場合はNULLをセット）
     * @throws SQLException セットに失敗した場合
     */
    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    /**
     * NullableなInteger値をPreparedStatementにセットする。
     *
     * @param ps    PreparedStatement
     * @param index パラメータインデックス
     * @param value セットする値（nullの場合はNULLをセット）
     * @throws SQLException セットに失敗した場合
     */
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }
}
