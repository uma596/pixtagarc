package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Image;
import com.example.pixtagarc.exception.ImageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 画像リポジトリクラス。
 *
 * <p>データベースの {@code images} テーブルに対するCRUD操作を提供する。
 * すべてのSQLはPreparedStatementを使用してSQLインジェクションを防ぐ。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ImageRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public ImageRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * FTS5インデックスにレコードを挿入する。
     *
     * <p>トリガーを使わず手動でFTS5テーブルを更新する。
     * タグや作者が変更された際にも呼び出す。
     *
     * @param imageId    画像ID
     * @param fileName   ファイル名
     * @param tagsText   タグ名をスペース区切りで結合した文字列
     * @param authorName 作者名（未設定の場合は空文字）
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void insertFts(Long imageId, String fileName, String tagsText, String authorName) {
        // 既存レコードを削除してから挿入（upsert相当）
        String deleteSql = "INSERT INTO image_fts(image_fts, rowid, file_name, tags_text, author_name) VALUES('delete', ?, ?, ?, ?)";
        String insertSql = "INSERT INTO image_fts(rowid, file_name, tags_text, author_name) VALUES(?, ?, ?, ?)";
        try {
            // まず既存エントリを削除（存在しない場合はエラーを無視）
            try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(deleteSql)) {
                ps.setLong(1, imageId);
                ps.setString(2, fileName);
                ps.setString(3, tagsText);
                ps.setString(4, authorName);
                ps.executeUpdate();
            } catch (SQLException ignored) {
                // 存在しない場合のエラーは無視
            }
            // 新規挿入
            try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(insertSql)) {
                ps.setLong(1, imageId);
                ps.setString(2, fileName);
                ps.setString(3, tagsText != null ? tagsText : "");
                ps.setString(4, authorName != null ? authorName : "");
                ps.executeUpdate();
            }
            log.debug("FTS5インデックスを更新しました: id={}", imageId);
        } catch (SQLException e) {
            log.error("FTS5インデックスの更新に失敗しました: id={}", imageId, e);
            throw new RuntimeException("FTS5インデックスの更新に失敗しました", e);
        }
    }

    /**
     * 画像を新規登録する。
     *
     * @param image 登録する画像エンティティ（idはnullであること）
     * @return 生成されたIDが設定された画像エンティティ
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Image save(Image image) {
        String sql = """
                INSERT INTO images (file_path, file_name, file_size, width, height,
                    media_type, author_id, work_id, page_number, star, is_hidden, created_at, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, image.getFilePath());
            ps.setString(2, image.getFileName());
            setNullableLong(ps, 3, image.getFileSize());
            setNullableInt(ps, 4, image.getWidth());
            setNullableInt(ps, 5, image.getHeight());
            ps.setString(6, image.getMediaType());
            setNullableLong(ps, 7, image.getAuthorId());
            setNullableLong(ps, 8, image.getWorkId());
            setNullableInt(ps, 9, image.getPageNumber());
            ps.setInt(10, image.getStar());
            ps.setInt(11, image.isHidden() ? 1 : 0);
            ps.setString(12, image.getCreatedAt());
            ps.setString(13, image.getImportedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    image.setId(rs.getLong(1));
                }
            }
            log.debug("画像を登録しました: id={}, path={}", image.getId(), image.getFilePath());
            return image;
        } catch (SQLException e) {
            log.error("画像の登録に失敗しました: path={}", image.getFilePath(), e);
            throw new RuntimeException("画像の登録に失敗しました", e);
        }
    }

    /**
     * 画像情報を更新する。
     *
     * @param image 更新する画像エンティティ
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     * @throws RuntimeException       データベース操作に失敗した場合
     */
    public void update(Image image) {
        String sql = """
                UPDATE images SET file_name=?, file_size=?, width=?, height=?,
                    media_type=?, author_id=?, work_id=?, page_number=?, star=?, is_hidden=?, created_at=?
                WHERE id=?
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, image.getFileName());
            setNullableLong(ps, 2, image.getFileSize());
            setNullableInt(ps, 3, image.getWidth());
            setNullableInt(ps, 4, image.getHeight());
            ps.setString(5, image.getMediaType());
            setNullableLong(ps, 6, image.getAuthorId());
            setNullableLong(ps, 7, image.getWorkId());
            setNullableInt(ps, 8, image.getPageNumber());
            ps.setInt(9, image.getStar());
            ps.setInt(10, image.isHidden() ? 1 : 0);
            ps.setString(11, image.getCreatedAt());
            ps.setLong(12, image.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw ImageNotFoundException.ofId(image.getId());
            }
            log.debug("画像を更新しました: id={}", image.getId());
        } catch (SQLException e) {
            log.error("画像の更新に失敗しました: id={}", image.getId(), e);
            throw new RuntimeException("画像の更新に失敗しました", e);
        }
    }

    /**
     * 画像の非表示フラグを更新する。
     *
     * @param id       更新する画像ID
     * @param isHidden 非表示フラグ
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     * @throws RuntimeException       データベース操作に失敗した場合
     */
    public void updateHidden(Long id, boolean isHidden) {
        String sql = "UPDATE images SET is_hidden=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, isHidden ? 1 : 0);
            ps.setLong(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw ImageNotFoundException.ofId(id);
            }
            log.debug("非表示フラグを更新しました: id={}, isHidden={}", id, isHidden);
        } catch (SQLException e) {
            log.error("非表示フラグの更新に失敗しました: id={}", id, e);
            throw new RuntimeException("非表示フラグの更新に失敗しました", e);
        }
    }

    /**
     * 画像のStar評価を更新する。
     *
     * @param id   更新する画像ID
     * @param star Star評価（0〜5）
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     * @throws RuntimeException       データベース操作に失敗した場合
     */
    public void updateStar(Long id, int star) {
        String sql = "UPDATE images SET star=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, star);
            ps.setLong(2, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw ImageNotFoundException.ofId(id);
            }
            log.debug("Star評価を更新しました: id={}, star={}", id, star);
        } catch (SQLException e) {
            log.error("Star評価の更新に失敗しました: id={}", id, e);
            throw new RuntimeException("Star評価の更新に失敗しました", e);
        }
    }

    /**
     * 画像のページ番号を更新する。
     *
     * @param id         更新する画像ID
     * @param pageNumber ページ番号（1始まり）
     */
    public void updatePageNumber(Long id, Integer pageNumber) {
        String sql = "UPDATE images SET page_number=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            if (pageNumber != null) {
                ps.setInt(1, pageNumber);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("ページ番号の更新に失敗しました: id={}", id, e);
            throw new RuntimeException("ページ番号の更新に失敗しました", e);
        }
    }

    /**
     * 画像の作品情報を更新する。
     *
     * @param id         更新する画像ID
     * @param workId     作品ID（nullで作品紐付けを解除）
     * @param pageNumber ページ番号（nullでページ番号なし）
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     * @throws RuntimeException       データベース操作に失敗した場合
     */
    public void updateWork(Long id, Long workId, Integer pageNumber) {
        String sql = "UPDATE images SET work_id=?, page_number=? WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            setNullableLong(ps, 1, workId);
            setNullableInt(ps, 2, pageNumber);
            ps.setLong(3, id);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw ImageNotFoundException.ofId(id);
            }
            log.debug("作品情報を更新しました: id={}, workId={}, pageNumber={}", id, workId, pageNumber);
        } catch (SQLException e) {
            log.error("作品情報の更新に失敗しました: id={}", id, e);
            throw new RuntimeException("作品情報の更新に失敗しました", e);
        }
    }

    /**
     * 指定された作品IDの画像をページ番号昇順で返す。
     *
     * @param workId 作品ID
     * @return 画像エンティティのリスト（page_number昇順）
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Image> findByWorkId(Long workId) {
        String sql = "SELECT * FROM images WHERE work_id=? ORDER BY page_number ASC";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, workId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Image> images = new ArrayList<>();
                while (rs.next()) {
                    images.add(mapRow(rs));
                }
                return images;
            }
        } catch (SQLException e) {
            log.error("作品IDによる画像検索に失敗しました: workId={}", workId, e);
            throw new RuntimeException("作品IDによる画像検索に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの画像を削除する。
     *
     * @param id 削除する画像ID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM images WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("画像を削除しました: id={}", id);
        } catch (SQLException e) {
            log.error("画像の削除に失敗しました: id={}", id, e);
            throw new RuntimeException("画像の削除に失敗しました", e);
        }
    }

    /**
     * 全画像を削除する。
     *
     * @return 削除された件数
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public int deleteAll() {
        String sql = "DELETE FROM images";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            int count = ps.executeUpdate();
            log.debug("全画像を削除しました: count={}", count);
            return count;
        } catch (SQLException e) {
            log.error("全画像の削除に失敗しました", e);
            throw new RuntimeException("全画像の削除に失敗しました", e);
        }
    }

    /**
     * 指定された作品の全画像の非表示フラグを一括更新する。
     *
     * @param workId   作品ID
     * @param isHidden 非表示フラグ
     * @return 更新された行数
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public int updateHiddenByWorkId(Long workId, boolean isHidden) {
        String sql = "UPDATE images SET is_hidden = ? WHERE work_id = ?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, isHidden ? 1 : 0);
            ps.setLong(2, workId);
            int count = ps.executeUpdate();
            log.debug("作品全体の非表示を更新しました: workId={}, isHidden={}, count={}", workId, isHidden, count);
            return count;
        } catch (SQLException e) {
            log.error("作品全体の非表示更新に失敗しました: workId={}", workId, e);
            throw new RuntimeException("作品全体の非表示更新に失敗しました", e);
        }
    }

    /**
     * 指定されたIDの画像を返す。
     *
     * @param id 検索する画像ID
     * @return 画像エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Image> findById(Long id) {
        String sql = "SELECT * FROM images WHERE id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("画像の検索に失敗しました: id={}", id, e);
            throw new RuntimeException("画像の検索に失敗しました", e);
        }
    }

    /**
     * 指定されたファイルパスの画像を返す。
     *
     * @param filePath 検索するファイルパス
     * @return 画像エンティティのOptional
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public Optional<Image> findByFilePath(String filePath) {
        String sql = "SELECT * FROM images WHERE file_path=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("画像の検索に失敗しました: path={}", filePath, e);
            throw new RuntimeException("画像の検索に失敗しました", e);
        }
    }

    /**
     * 全画像を返す。
     *
     * @return 全画像エンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Image> findAll() {
        String sql = "SELECT * FROM images ORDER BY created_at DESC";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Image> images = new ArrayList<>();
            while (rs.next()) {
                images.add(mapRow(rs));
            }
            return images;
        } catch (SQLException e) {
            log.error("全画像の取得に失敗しました", e);
            throw new RuntimeException("全画像の取得に失敗しました", e);
        }
    }

    /**
     * ResultSetの現在行を {@link Image} エンティティにマッピングする。
     *
     * @param rs ResultSet
     * @return 画像エンティティ
     * @throws SQLException マッピングに失敗した場合
     */
    private Image mapRow(ResultSet rs) throws SQLException {
        Image image = new Image();
        image.setId(rs.getLong("id"));
        image.setFilePath(rs.getString("file_path"));
        image.setFileName(rs.getString("file_name"));
        long fileSize = rs.getLong("file_size");
        image.setFileSize(rs.wasNull() ? null : fileSize);
        int width = rs.getInt("width");
        image.setWidth(rs.wasNull() ? null : width);
        int height = rs.getInt("height");
        image.setHeight(rs.wasNull() ? null : height);
        image.setMediaType(rs.getString("media_type"));
        long authorId = rs.getLong("author_id");
        image.setAuthorId(rs.wasNull() ? null : authorId);
        long workId = rs.getLong("work_id");
        image.setWorkId(rs.wasNull() ? null : workId);
        int pageNumber = rs.getInt("page_number");
        image.setPageNumber(rs.wasNull() ? null : pageNumber);
        image.setStar(rs.getInt("star"));
        image.setHidden(rs.getInt("is_hidden") == 1);
        image.setCreatedAt(rs.getString("created_at"));
        image.setImportedAt(rs.getString("imported_at"));
        return image;
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
