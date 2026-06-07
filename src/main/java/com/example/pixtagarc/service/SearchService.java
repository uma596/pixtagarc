package com.example.pixtagarc.service;

import com.example.pixtagarc.config.DatabaseConfig;
import com.example.pixtagarc.dto.ImageSummary;
import com.example.pixtagarc.dto.SearchCondition;
import com.example.pixtagarc.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 検索サービスクラス。
 *
 * <p>画像の検索ロジックを提供する。
 * FTS5を使用したキーワード検索、タグ・作者・日付範囲による絞り込み、
 * ページングをサポートする。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SearchService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    /** データベース設定。 */
    private final DatabaseConfig dbConfig;

    /** 許可されたソート列名のセット（SQLインジェクション対策）。 */
    private static final java.util.Set<String> ALLOWED_SORT_COLUMNS = java.util.Set.of(
            "created_at", "imported_at", "file_name", "file_size", "width", "height"
    );

    /**
     * コンストラクタ。
     *
     * @param dbConfig データベース設定
     */
    public SearchService(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 検索条件に基づいて画像を検索する。
     *
     * <p>検索フロー:
     * <ol>
     *   <li>FTS5でキーワード検索（file_name / tags_text / author_name）</li>
     *   <li>タグ絞り込み（image_tags JOIN）</li>
     *   <li>作者絞り込み（author_id）</li>
     *   <li>非表示除外フラグが ON の場合 WHERE is_hidden = 0 を付加</li>
     *   <li>Star最小値フィルタ</li>
     *   <li>作品ID絞り込み</li>
     *   <li>日付範囲フィルタ</li>
     *   <li>ページング（LIMIT / OFFSET）</li>
     * </ol>
     *
     * @param condition 検索条件
     * @return 検索結果
     */
    public SearchResult search(SearchCondition condition) {
        log.debug("検索を実行します: keyword={}, tagIds={}, authorIds={}",
                condition.getKeyword(), condition.getTagIds(), condition.getAuthorIds());

        try {
            long totalCount = countResults(condition);
            List<ImageSummary> items = fetchResults(condition);
            SearchResult result = new SearchResult(items, totalCount,
                    condition.getPage(), condition.getPageSize());
            log.debug("検索完了: totalCount={}, items.size={}", totalCount, items.size());
            return result;
        } catch (SQLException e) {
            log.error("検索に失敗しました", e);
            throw new RuntimeException("検索に失敗しました", e);
        }
    }

    /**
     * 検索条件に一致する総件数を返す。
     *
     * @param condition 検索条件
     * @return 総件数
     * @throws SQLException データベース操作に失敗した場合
     */
    private long countResults(SearchCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(DISTINCT i.id) FROM images i ");
        appendJoins(sql, params, condition);
        appendWhereClause(sql, params, condition);

        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql.toString())) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * 検索条件に一致する画像サマリーのリストを返す。
     *
     * @param condition 検索条件
     * @return 画像サマリーのリスト
     * @throws SQLException データベース操作に失敗した場合
     */
    private List<ImageSummary> fetchResults(SearchCondition condition) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("""
                SELECT DISTINCT i.id, i.file_path, i.file_name, i.file_size,
                    i.width, i.height, i.media_type, i.is_hidden, i.created_at,
                    i.work_id, i.page_number, i.star,
                    a.name AS author_name
                FROM images i
                LEFT JOIN authors a ON i.author_id = a.id
                LEFT JOIN works w ON i.work_id = w.id
                """);
        appendJoins(sql, params, condition);
        appendWhereClause(sql, params, condition);
        appendOrderBy(sql, condition);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(condition.getPageSize());
        params.add(condition.getOffset());

        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql.toString())) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImageSummary> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapToSummary(rs));
                }
                // 各画像のタグ名を取得
                for (ImageSummary summary : results) {
                    summary.setTagNames(fetchTagNamesForImage(summary.getId()));
                }
                return results;
            }
        }
    }

    /**
     * JOINクローズを構築する。
     *
     * @param sql       SQLビルダー
     * @param params    パラメータリスト
     * @param condition 検索条件
     */
    private void appendJoins(StringBuilder sql, List<Object> params, SearchCondition condition) {
        // FTS5検索が必要な場合はJOIN
        if (hasKeyword(condition)) {
            sql.append("INNER JOIN image_fts ON image_fts.rowid = i.id ");
        }
        // タグ絞り込みが必要な場合はJOIN
        if (!condition.getTagIds().isEmpty()) {
            sql.append("INNER JOIN image_tags it ON i.id = it.image_id ");
        }
    }

    /**
     * WHERE句を構築する。
     *
     * @param sql       SQLビルダー
     * @param params    パラメータリスト
     * @param condition 検索条件
     */
    private void appendWhereClause(StringBuilder sql, List<Object> params, SearchCondition condition) {
        List<String> conditions = new ArrayList<>();

        // FTS5キーワード検索
        if (hasKeyword(condition)) {
            conditions.add("image_fts MATCH ?");
            // FTS5クエリを構築（スペース区切りで各単語をAND検索）
            params.add(buildFtsQuery(condition.getKeyword()));
        }

        // タグ絞り込み（指定されたタグIDのいずれかを持つ画像）
        if (!condition.getTagIds().isEmpty()) {
            String placeholders = condition.getTagIds().stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(", "));
            conditions.add("it.tag_id IN (" + placeholders + ")");
            params.addAll(condition.getTagIds());
        }

        // 作者絞り込み
        if (!condition.getAuthorIds().isEmpty()) {
            String placeholders = condition.getAuthorIds().stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(", "));
            conditions.add("i.author_id IN (" + placeholders + ")");
            params.addAll(condition.getAuthorIds());
        }

        // 非表示除外
        if (condition.isExcludeHidden()) {
            conditions.add("i.is_hidden = 0");
        }

        // Star最小値フィルタ
        if (condition.getMinStar() > 0) {
            conditions.add("i.star >= ?");
            params.add(condition.getMinStar());
        }

        // 作品ID絞り込み
        if (condition.getWorkId() != null) {
            conditions.add("i.work_id = ?");
            params.add(condition.getWorkId());
        }

        // 日付範囲FROM
        if (condition.getDateFrom() != null && !condition.getDateFrom().isEmpty()) {
            conditions.add("i.created_at >= ?");
            params.add(condition.getDateFrom());
        }

        // 日付範囲TO
        if (condition.getDateTo() != null && !condition.getDateTo().isEmpty()) {
            conditions.add("i.created_at <= ?");
            params.add(condition.getDateTo());
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }
    }

    /**
     * ORDER BY句を構築する。
     *
     * <p>SQLインジェクション対策として、ソート列名はホワイトリストで検証する。
     *
     * @param sql       SQLビルダー
     * @param condition 検索条件
     */
    private void appendOrderBy(StringBuilder sql, SearchCondition condition) {
        // ホワイトリストで検証してSQLインジェクションを防ぐ
        String sortColumn = ALLOWED_SORT_COLUMNS.contains(condition.getSortColumn())
                ? condition.getSortColumn() : "created_at";
        String sortOrder = "ASC".equalsIgnoreCase(condition.getSortOrder()) ? "ASC" : "DESC";

        // 作品グループ化ソート:
        // 第1キー: ユーザー指定ソート列（作品所属の場合は作品の代表日時を使用）
        // 第2キー: work_idでグループ化（同一作品をまとめる）
        // 第3キー: page_number昇順（作品内ページ順）
        // 第4キー: file_name昇順（フォールバック）
        sql.append("ORDER BY ");
        sql.append("COALESCE(w.created_at, i.").append(sortColumn).append(") ").append(sortOrder).append(", ");
        sql.append("i.work_id, ");
        sql.append("i.page_number ASC, ");
        sql.append("i.file_name ASC ");
    }

    /**
     * FTS5クエリ文字列を構築する。
     *
     * <p>入力キーワードをスペースで分割し、各単語をAND検索するFTS5クエリを構築する。
     * 特殊文字はエスケープする。
     *
     * @param keyword 入力キーワード
     * @return FTS5クエリ文字列
     */
    private String buildFtsQuery(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }
        // スペースで分割して各単語をAND検索
        String[] words = keyword.trim().split("\\s+");
        return Arrays.stream(words)
                .map(word -> "\"" + word.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(" AND "));
    }

    /**
     * キーワード検索が必要かどうかを返す。
     *
     * @param condition 検索条件
     * @return キーワードが設定されている場合 {@code true}
     */
    private boolean hasKeyword(SearchCondition condition) {
        return condition.getKeyword() != null && !condition.getKeyword().trim().isEmpty();
    }

    /**
     * PreparedStatementにパラメータをセットする。
     *
     * @param ps     PreparedStatement
     * @param params パラメータリスト
     * @throws SQLException セットに失敗した場合
     */
    private void setParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof String) {
                ps.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                ps.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                ps.setInt(i + 1, (Integer) param);
            } else {
                ps.setObject(i + 1, param);
            }
        }
    }

    /**
     * ResultSetの現在行を {@link ImageSummary} にマッピングする。
     *
     * @param rs ResultSet
     * @return 画像サマリー
     * @throws SQLException マッピングに失敗した場合
     */
    private ImageSummary mapToSummary(ResultSet rs) throws SQLException {
        ImageSummary summary = new ImageSummary();
        summary.setId(rs.getLong("id"));
        summary.setFilePath(rs.getString("file_path"));
        summary.setFileName(rs.getString("file_name"));
        long fileSize = rs.getLong("file_size");
        summary.setFileSize(rs.wasNull() ? null : fileSize);
        int width = rs.getInt("width");
        summary.setWidth(rs.wasNull() ? null : width);
        int height = rs.getInt("height");
        summary.setHeight(rs.wasNull() ? null : height);
        summary.setMediaType(rs.getString("media_type"));
        summary.setHidden(rs.getInt("is_hidden") == 1);
        summary.setCreatedAt(rs.getString("created_at"));
        summary.setAuthorName(rs.getString("author_name"));
        long workId = rs.getLong("work_id");
        summary.setWorkId(rs.wasNull() ? null : workId);
        int pageNumber = rs.getInt("page_number");
        summary.setPageNumber(rs.wasNull() ? null : pageNumber);
        summary.setStar(rs.getInt("star"));
        return summary;
    }

    /**
     * ビューア向けに全件検索する（タグ名取得なし・軽量版）。
     *
     * <p>メイン画面のページングとは独立して全検索結果にアクセスする。
     * タグ名はビューア表示時に1件ずつ遅延取得する。
     *
     * @param condition 検索条件（page/pageSizeは無視して全件取得）
     * @return 画像サマリーのリスト（タグ名なし）
     */
    public List<ImageSummary> searchForViewer(SearchCondition condition) {
        log.debug("ビューア向け全件検索を実行します: keyword={}", condition.getKeyword());

        try {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sql.append("""
                    SELECT DISTINCT i.id, i.file_path, i.file_name, i.file_size,
                        i.width, i.height, i.media_type, i.is_hidden, i.created_at,
                        i.work_id, i.page_number, i.star,
                        a.name AS author_name
                    FROM images i
                    LEFT JOIN authors a ON i.author_id = a.id
                    LEFT JOIN works w ON i.work_id = w.id
                    """);
            appendJoins(sql, params, condition);
            appendWhereClause(sql, params, condition);
            appendOrderBy(sql, condition);
            // LIMITなし — 全件取得

            try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql.toString())) {
                setParameters(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ImageSummary> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapToSummary(rs));
                        // タグ名は取得しない（遅延取得）
                    }
                    log.debug("ビューア向け全件検索完了: {}件", results.size());
                    return results;
                }
            }
        } catch (SQLException e) {
            log.error("ビューア向け検索に失敗しました", e);
            throw new RuntimeException("ビューア向け検索に失敗しました", e);
        }
    }

    /**
     * 指定された画像IDのタグ名リストを返す。
     *
     * @param imageId 画像ID
     * @return タグ名リスト
     * @throws SQLException データベース操作に失敗した場合
     */
    private List<String> fetchTagNamesForImage(Long imageId) throws SQLException {
        String sql = """
                SELECT t.name FROM tags t
                INNER JOIN image_tags it ON t.id = it.tag_id
                WHERE it.image_id = ?
                ORDER BY t.name
                """;
        try (PreparedStatement ps = dbConfig.getConnection().prepareStatement(sql)) {
            ps.setLong(1, imageId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> names = new ArrayList<>();
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
                return names;
            }
        }
    }
}
