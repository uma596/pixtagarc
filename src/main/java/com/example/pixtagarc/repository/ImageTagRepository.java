package com.example.pixtagarc.repository;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.domain.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 画像-タグ中間テーブルリポジトリクラス。
 *
 * <p>データベースの {@code image_tags} テーブルに対する操作を提供する。
 * 画像とタグの関連付け・解除・検索を行う。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageTagRepository {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ImageTagRepository.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public ImageTagRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 画像にタグを追加する。
     *
     * <p>既に関連付けが存在する場合は何もしない（INSERT OR IGNORE）。
     *
     * @param imageId 画像ID
     * @param tagId   タグID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void addTag(Long imageId, Long tagId) {
        String sql = "INSERT OR IGNORE INTO image_tags (image_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, imageId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
            log.debug("タグを追加しました: imageId={}, tagId={}", imageId, tagId);
        } catch (SQLException e) {
            log.error("タグの追加に失敗しました: imageId={}, tagId={}", imageId, tagId, e);
            throw new RuntimeException("タグの追加に失敗しました", e);
        }
    }

    /**
     * 画像からタグを削除する。
     *
     * @param imageId 画像ID
     * @param tagId   タグID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void removeTag(Long imageId, Long tagId) {
        String sql = "DELETE FROM image_tags WHERE image_id=? AND tag_id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, imageId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
            log.debug("タグを削除しました: imageId={}, tagId={}", imageId, tagId);
        } catch (SQLException e) {
            log.error("タグの削除に失敗しました: imageId={}, tagId={}", imageId, tagId, e);
            throw new RuntimeException("タグの削除に失敗しました", e);
        }
    }

    /**
     * 指定された画像IDに関連するタグを全て削除する。
     *
     * @param imageId 画像ID
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public void removeAllTagsFromImage(Long imageId) {
        String sql = "DELETE FROM image_tags WHERE image_id=?";
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, imageId);
            ps.executeUpdate();
            log.debug("画像の全タグを削除しました: imageId={}", imageId);
        } catch (SQLException e) {
            log.error("画像の全タグ削除に失敗しました: imageId={}", imageId, e);
            throw new RuntimeException("画像の全タグ削除に失敗しました", e);
        }
    }

    /**
     * 指定された画像IDに関連するタグのリストを返す。
     *
     * @param imageId 画像ID
     * @return タグエンティティのリスト
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public List<Tag> findTagsByImageId(Long imageId) {
        String sql = """
                SELECT t.id, t.name
                FROM tags t
                INNER JOIN image_tags it ON t.id = it.tag_id
                WHERE it.image_id = ?
                ORDER BY t.name
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, imageId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Tag> tags = new ArrayList<>();
                while (rs.next()) {
                    tags.add(new Tag(rs.getLong("id"), rs.getString("name")));
                }
                return tags;
            }
        } catch (SQLException e) {
            log.error("タグの検索に失敗しました: imageId={}", imageId, e);
            throw new RuntimeException("タグの検索に失敗しました", e);
        }
    }

    /**
     * 指定された画像IDに関連するタグ名をスペース区切りで結合した文字列を返す。
     *
     * <p>FTS5インデックスの更新に使用する。
     *
     * @param imageId 画像ID
     * @return タグ名をスペース区切りで結合した文字列
     * @throws RuntimeException データベース操作に失敗した場合
     */
    public String getTagsTextForImage(Long imageId) {
        List<Tag> tags = findTagsByImageId(imageId);
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tag.getName());
        }
        return sb.toString();
    }
}
