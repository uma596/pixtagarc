package com.example.pixtagarc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * アプリケーション設定クラス（シングルトン）。
 *
 * <p>サムネイルサイズ・検索上限件数などのアプリ設定を管理する。
 * 設定はアプリケーション実行ディレクトリ直下の {@code db/settings.properties} に永続化される。
 * 定数（ディレクトリパス・拡張子セット等）は static メソッドで提供する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class AppConfig {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /** アプリケーションデータディレクトリ名。 */
    private static final String APP_DIR_NAME = "db";

    /** サムネイルキャッシュディレクトリ名。 */
    private static final String THUMBNAILS_DIR_NAME = "thumbnails";

    /** ログディレクトリ名。 */
    private static final String LOGS_DIR_NAME = "logs";

    /** 設定ファイル名。 */
    private static final String SETTINGS_FILE_NAME = "settings.properties";

    // ===== 設定キー名 =====

    /** 検索上限件数の設定キー。 */
    public static final String KEY_SEARCH_LIMIT = "search.limit";

    /** サムネイルサイズの設定キー。 */
    public static final String KEY_THUMBNAIL_SIZE = "thumbnail.size";

    // ===== 状態保持キー名 =====

    /** キーワードの状態保持キー。 */
    public static final String KEY_STATE_KEYWORD = "state.keyword";

    /** タグフィルターの状態保持キー。 */
    public static final String KEY_STATE_TAG_IDS = "state.tag_ids";

    /** 作者フィルターの状態保持キー。 */
    public static final String KEY_STATE_AUTHOR_FILTER = "state.author_filter";

    /** Starフィルターの状態保持キー。 */
    public static final String KEY_STATE_STAR_FILTER = "state.star_filter";

    /** 非表示除外の状態保持キー。 */
    public static final String KEY_STATE_EXCLUDE_HIDDEN = "state.exclude_hidden";

    /** 現在ページの状態保持キー。 */
    public static final String KEY_STATE_CURRENT_PAGE = "state.current_page";

    /** ページサイズの状態保持キー。 */
    public static final String KEY_STATE_PAGE_SIZE = "state.page_size";

    /** 表示モードの状態保持キー。 */
    public static final String KEY_STATE_DISPLAY_MODE = "state.display_mode";

    /** サムネイルサイズの状態保持キー。 */
    public static final String KEY_STATE_THUMBNAIL_SIZE = "state.thumbnail_size";

    /** タグ使用履歴の保持キー。 */
    public static final String KEY_TAG_HISTORY = "tag.history";

    // ===== 定数 =====

    /** サムネイル小サイズ（ピクセル）。 */
    public static final int THUMBNAIL_SIZE_SMALL = 80;

    /** サムネイル中サイズ（ピクセル）。 */
    public static final int THUMBNAIL_SIZE_MEDIUM = 150;

    /** サムネイル大サイズ（ピクセル）。 */
    public static final int THUMBNAIL_SIZE_LARGE = 250;

    /** LRUキャッシュの最大保持件数。 */
    public static final int THUMBNAIL_CACHE_MAX_SIZE = 500;

    /** インポート時の並列スレッド数。 */
    public static final int IMPORT_THREAD_POOL_SIZE = 4;

    /** 検索上限件数のデフォルト値。 */
    public static final int DEFAULT_SEARCH_LIMIT = 5000;

    /** 検索上限件数の最小値。 */
    public static final int MIN_SEARCH_LIMIT = 100;

    /** 検索上限件数の最大値。 */
    public static final int MAX_SEARCH_LIMIT = 100000;

    /** サポートする画像拡張子のセット。 */
    public static final java.util.Set<String> SUPPORTED_IMAGE_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp");

    /** サポートする動画拡張子のセット。 */
    public static final java.util.Set<String> SUPPORTED_VIDEO_EXTENSIONS =
            java.util.Set.of("webm", "mp4", "avi", "mkv", "mov");

    // ===== シングルトン =====

    /** シングルトンインスタンス。 */
    private static volatile AppConfig instance;

    /** 設定プロパティ。 */
    private final Properties properties = new Properties();

    /** 設定ファイルのパス。 */
    private final Path settingsFilePath;

    /**
     * プライベートコンストラクタ。設定ファイルを読み込む。
     */
    private AppConfig() {
        settingsFilePath = getAppDataDirectory().resolve(SETTINGS_FILE_NAME);
        loadSettings();
    }

    /**
     * シングルトンインスタンスを返す。
     *
     * @return {@link AppConfig} のシングルトンインスタンス
     */
    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    // ===== 設定値のアクセサ =====

    /**
     * 検索上限件数を返す。
     *
     * @return 検索上限件数（デフォルト: {@value #DEFAULT_SEARCH_LIMIT}）
     */
    public int getSearchLimit() {
        try {
            int value = Integer.parseInt(properties.getProperty(KEY_SEARCH_LIMIT,
                    String.valueOf(DEFAULT_SEARCH_LIMIT)));
            return Math.min(MAX_SEARCH_LIMIT, Math.max(MIN_SEARCH_LIMIT, value));
        } catch (NumberFormatException e) {
            log.warn("検索上限件数の設定値が不正です。デフォルト値を使用します: {}", DEFAULT_SEARCH_LIMIT);
            return DEFAULT_SEARCH_LIMIT;
        }
    }

    /**
     * 検索上限件数を設定して保存する。
     *
     * @param limit 検索上限件数（{@value #MIN_SEARCH_LIMIT} 〜 {@value #MAX_SEARCH_LIMIT}）
     * @throws IllegalArgumentException 値が範囲外の場合
     */
    public void setSearchLimit(int limit) {
        if (limit < MIN_SEARCH_LIMIT || limit > MAX_SEARCH_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("検索上限件数は %d〜%d の範囲で指定してください: %d",
                            MIN_SEARCH_LIMIT, MAX_SEARCH_LIMIT, limit));
        }
        properties.setProperty(KEY_SEARCH_LIMIT, String.valueOf(limit));
        saveSettings();
        log.info("検索上限件数を更新しました: {}", limit);
    }

    /**
     * サムネイルサイズ名を返す（"小" / "中" / "大"）。
     *
     * @return サムネイルサイズ名（デフォルト: "中"）
     */
    public String getThumbnailSizeName() {
        return properties.getProperty(KEY_THUMBNAIL_SIZE, "中");
    }

    /**
     * サムネイルサイズ名を設定して保存する。
     *
     * @param sizeName サムネイルサイズ名（"小" / "中" / "大"）
     */
    public void setThumbnailSizeName(String sizeName) {
        properties.setProperty(KEY_THUMBNAIL_SIZE, sizeName);
        saveSettings();
        log.info("サムネイルサイズを更新しました: {}", sizeName);
    }

    // ===== 状態保持アクセサ =====

    /**
     * 状態プロパティを取得する。
     *
     * @param key          プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値（未設定の場合はデフォルト値）
     */
    public String getState(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 状態プロパティを設定して即時保存する。
     *
     * @param key   プロパティキー
     * @param value プロパティ値
     */
    public void setState(String key, String value) {
        properties.setProperty(key, value != null ? value : "");
        saveSettings();
    }

    /**
     * 状態プロパティを一括設定して保存する（複数キーをまとめて1回の書き込み）。
     *
     * @param entries キーと値のマップ
     */
    public void setStates(java.util.Map<String, String> entries) {
        for (java.util.Map.Entry<String, String> entry : entries.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        saveSettings();
    }

    // ===== static ユーティリティメソッド =====

    /**
     * アプリケーションデータディレクトリのパスを返す。
     *
     * <p>ディレクトリが存在しない場合は作成する。
     * アプリケーション実行ディレクトリ直下の {@code db/} フォルダに配置される。
     *
     * @return アプリデータディレクトリのパス（{@code <アプリ実行ディレクトリ>/db/}）
     */
    public static Path getAppDataDirectory() {
        Path appDir = Paths.get(System.getProperty("user.dir"), APP_DIR_NAME);
        ensureDirectoryExists(appDir);
        return appDir;
    }

    /**
     * サムネイルキャッシュディレクトリのパスを返す。
     *
     * @return サムネイルキャッシュディレクトリのパス
     */
    public static Path getThumbnailCacheDirectory() {
        Path thumbnailDir = getAppDataDirectory().resolve(THUMBNAILS_DIR_NAME);
        ensureDirectoryExists(thumbnailDir);
        return thumbnailDir;
    }

    /**
     * ログディレクトリのパスを返す。
     *
     * @return ログディレクトリのパス
     */
    public static Path getLogsDirectory() {
        Path logsDir = getAppDataDirectory().resolve(LOGS_DIR_NAME);
        ensureDirectoryExists(logsDir);
        return logsDir;
    }

    /**
     * 指定されたファイル拡張子が画像としてサポートされているかを返す。
     *
     * @param extension ファイル拡張子（小文字）
     * @return サポートされている場合 {@code true}
     */
    public static boolean isSupportedImageExtension(String extension) {
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 指定されたファイル拡張子が動画としてサポートされているかを返す。
     *
     * @param extension ファイル拡張子（小文字）
     * @return サポートされている場合 {@code true}
     */
    public static boolean isSupportedVideoExtension(String extension) {
        return SUPPORTED_VIDEO_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 指定されたファイル拡張子がサポートされているかを返す（画像・動画両方）。
     *
     * @param extension ファイル拡張子（小文字）
     * @return サポートされている場合 {@code true}
     */
    public static boolean isSupportedExtension(String extension) {
        return isSupportedImageExtension(extension) || isSupportedVideoExtension(extension);
    }

    // ===== 設定ファイルの読み書き =====

    /**
     * 設定ファイルを読み込む。ファイルが存在しない場合はデフォルト値を使用する。
     */
    private void loadSettings() {
        if (!Files.exists(settingsFilePath)) {
            log.info("設定ファイルが存在しないためデフォルト値を使用します: {}", settingsFilePath);
            return;
        }
        try (InputStream is = Files.newInputStream(settingsFilePath)) {
            properties.load(is);
            log.info("設定ファイルを読み込みました: {}", settingsFilePath);
        } catch (IOException e) {
            log.error("設定ファイルの読み込みに失敗しました: {}", settingsFilePath, e);
        }
    }

    /**
     * 設定ファイルに書き込む。
     */
    private void saveSettings() {
        try (OutputStream os = Files.newOutputStream(settingsFilePath)) {
            properties.store(os, "pixtagarc Settings");
            log.debug("設定ファイルを保存しました: {}", settingsFilePath);
        } catch (IOException e) {
            log.error("設定ファイルの保存に失敗しました: {}", settingsFilePath, e);
        }
    }

    /**
     * ディレクトリが存在しない場合に作成する。
     *
     * @param directory 作成するディレクトリのパス
     */
    private static void ensureDirectoryExists(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                log.info("ディレクトリを作成しました: {}", directory);
            } catch (IOException e) {
                log.error("ディレクトリの作成に失敗しました: {}", directory, e);
                throw new RuntimeException("ディレクトリの作成に失敗しました: " + directory, e);
            }
        }
    }
}
