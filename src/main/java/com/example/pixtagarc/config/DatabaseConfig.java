package com.example.pixtagarc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * SQLite接続管理クラス（シングルトン）。
 *
 * <p>SQLiteデータベースへの接続を管理するシングルトンクラス。
 * データベースファイルはアプリケーション実行ディレクトリ直下の {@code db/pixtagarc.db} に配置される。
 * 初回アクセス時にスキーマを自動初期化する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class DatabaseConfig {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    /** データベースファイル名。 */
    private static final String DB_FILE_NAME = "pixtagarc.db";

    /** スキーマSQLリソースパス。 */
    private static final String SCHEMA_SQL_PATH = "/sql/schema.sql";

    /** シングルトンインスタンス。 */
    private static volatile DatabaseConfig instance;

    /** データベース接続。 */
    private Connection connection;

    /** データベースファイルのパス。 */
    private final String dbUrl;

    /**
     * プライベートコンストラクタ。
     *
     * <p>データベースURLを構築し、接続を初期化する。
     */
    private DatabaseConfig() {
        Path dbPath = AppConfig.getAppDataDirectory().resolve(DB_FILE_NAME);
        this.dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        log.info("データベースパス: {}", dbPath.toAbsolutePath());
        initializeDatabase();
    }

    /**
     * シングルトンインスタンスを返す。
     *
     * <p>スレッドセーフなダブルチェックロッキングで実装している。
     *
     * @return {@link DatabaseConfig} のシングルトンインスタンス
     */
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }

    /**
     * データベース接続を返す。
     *
     * <p>接続が閉じられている場合は再接続する。
     *
     * @return {@link Connection} インスタンス
     * @throws RuntimeException データベース接続に失敗した場合
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                log.debug("データベース接続を確立します: {}", dbUrl);
                connection = DriverManager.getConnection(dbUrl);
            }
            return connection;
        } catch (SQLException e) {
            log.error("データベース接続に失敗しました: {}", dbUrl, e);
            throw new RuntimeException("データベース接続に失敗しました", e);
        }
    }

    /**
     * データベースを初期化する。
     *
     * <p>スキーマSQLを読み込み、テーブルが存在しない場合は作成する。
     * SQLは1ステートメントずつ {@code Statement} で実行する。
     */
    private void initializeDatabase() {
        log.info("データベースを初期化します");
        try {
            Connection conn = getConnection();

            // PRAGMA を直接実行（スキーマとは別に処理）
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
            }

            // スキーマSQL（PRAGMA行を除いたもの）を行単位で処理
            String schemaSql = loadSchemaSql();
            StringBuilder current = new StringBuilder();

            for (String line : schemaSql.split("\n")) {
                String trimmedLine = line.trim();
                // コメント行・空行・PRAGMAはスキップ
                if (trimmedLine.isEmpty()
                        || trimmedLine.startsWith("--")
                        || trimmedLine.toUpperCase().startsWith("PRAGMA")) {
                    continue;
                }
                current.append(line).append("\n");
                // セミコロンで終わる行でステートメント確定
                if (trimmedLine.endsWith(";")) {
                    String sql = current.toString().trim();
                    if (!sql.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                            log.debug("SQL実行: {}", sql.substring(0, Math.min(80, sql.length())));
                        }
                    }
                    current = new StringBuilder();
                }
            }

            log.info("データベースの初期化が完了しました");
        } catch (SQLException e) {
            log.error("データベースの初期化に失敗しました", e);
            throw new RuntimeException("データベースの初期化に失敗しました", e);
        }
    }

    /**
     * スキーマSQLファイルを読み込んで文字列として返す。
     *
     * @return スキーマSQL文字列
     * @throws RuntimeException スキーマファイルの読み込みに失敗した場合
     */
    private String loadSchemaSql() {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_SQL_PATH)) {
            if (is == null) {
                throw new RuntimeException("スキーマSQLファイルが見つかりません: " + SCHEMA_SQL_PATH);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("スキーマSQLの読み込みに失敗しました", e);
            throw new RuntimeException("スキーマSQLの読み込みに失敗しました", e);
        }
    }

    /**
     * データベース接続を閉じる。
     *
     * <p>アプリケーション終了時に呼び出す。
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                log.info("データベース接続を閉じました");
            } catch (SQLException e) {
                log.error("データベース接続のクローズに失敗しました", e);
            }
        }
    }
}
