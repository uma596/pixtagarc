package com.example.pixtagarc.service;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.exception.ThumbnailGenerationException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サムネイルサービスクラス。
 *
 * <p>Thumbnailatorを使用してサムネイル画像を生成・キャッシュ管理する。
 * LRUキャッシュ（最大500件）でメモリ使用量を制御する。
 * サムネイルはファイルパスのSHA-256ハッシュを使用してディレクトリ分散して保存する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ThumbnailService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

    /** サムネイルファイルの拡張子。 */
    private static final String THUMBNAIL_EXTENSION = ".jpg";

    /** ディレクトリ分散のプレフィックス長。 */
    private static final int DIR_PREFIX_LENGTH = 2;

    /**
     * LRUキャッシュ（サムネイルパスのメモリキャッシュ）。
     * キー: cacheKey（filePath@size）, 値: サムネイルファイルパス
     * synchronized で包んだ LinkedHashMap でスレッドセーフに管理する。
     */
    private final Map<String, String> lruCache;

    /** サムネイルキャッシュディレクトリ。 */
    private final Path cacheDirectory;

    /**
     * コンストラクタ。
     *
     * <p>LRUキャッシュを初期化し、キャッシュディレクトリを確認する。
     */
    public ThumbnailService() {
        this.cacheDirectory = AppConfig.getThumbnailCacheDirectory();
        // LinkedHashMapをLRUキャッシュとして使用し、synchronizedMapでスレッドセーフにする
        this.lruCache = Collections.synchronizedMap(
                new LinkedHashMap<>(AppConfig.THUMBNAIL_CACHE_MAX_SIZE, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > AppConfig.THUMBNAIL_CACHE_MAX_SIZE;
                    }
                });
        log.info("サムネイルサービスを初期化しました: cacheDir={}", cacheDirectory);
    }

    /**
     * 指定されたファイルのサムネイルパスを返す。
     *
     * <p>キャッシュに存在する場合はキャッシュから返す。
     * 存在しない場合はサムネイルを生成してキャッシュに追加する。
     * このメソッドはバックグラウンドスレッドから安全に呼び出せる。
     *
     * @param filePath    元ファイルのパス
     * @param sizePixels  サムネイルのサイズ（ピクセル）
     * @return サムネイルファイルのパス
     * @throws ThumbnailGenerationException サムネイル生成に失敗した場合
     */
    public String getThumbnailPath(String filePath, int sizePixels) {
        String cacheKey = filePath + "@" + sizePixels;
        // LRUキャッシュを確認
        if (lruCache.containsKey(cacheKey)) {
            String cachedPath = lruCache.get(cacheKey);
            if (Files.exists(Paths.get(cachedPath))) {
                return cachedPath;
            }
            // ファイルが削除されていた場合はキャッシュから除去
            lruCache.remove(cacheKey);
        }

        // サムネイルファイルのパスを計算
        String thumbnailPath = computeThumbnailPath(filePath, sizePixels);

        // 既にファイルが存在する場合はキャッシュに追加して返す
        if (Files.exists(Paths.get(thumbnailPath))) {
            lruCache.put(cacheKey, thumbnailPath);
            return thumbnailPath;
        }

        // サムネイルを生成
        generateThumbnail(filePath, thumbnailPath, sizePixels);
        lruCache.put(cacheKey, thumbnailPath);
        return thumbnailPath;
    }

    /**
     * サムネイルを生成する。
     *
     * @param sourcePath     元ファイルのパス
     * @param thumbnailPath  出力先サムネイルパス
     * @param sizePixels     サムネイルサイズ（ピクセル）
     * @throws ThumbnailGenerationException 生成に失敗した場合
     */
    private void generateThumbnail(String sourcePath, String thumbnailPath, int sizePixels) {
        log.debug("サムネイルを生成します: source={}, size={}", sourcePath, sizePixels);
        try {
            Path outputPath = Paths.get(thumbnailPath);
            Files.createDirectories(outputPath.getParent());
            Thumbnails.of(sourcePath)
                    .size(sizePixels, sizePixels)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toFile(outputPath.toFile());
            log.debug("サムネイルを生成しました: {}", thumbnailPath);
        } catch (IOException e) {
            log.error("サムネイル生成に失敗しました: source={}", sourcePath, e);
            throw ThumbnailGenerationException.ofFile(sourcePath, e);
        }
    }

    /**
     * サムネイルファイルのパスを計算する。
     *
     * <p>ファイルパスのSHA-256ハッシュを使用してディレクトリ分散する。
     * 例: {@code ~/.pixtagarc/thumbnails/ab/ab1234...@150.jpg}
     *
     * @param filePath   元ファイルのパス
     * @param sizePixels サムネイルサイズ
     * @return サムネイルファイルのパス文字列
     */
    private String computeThumbnailPath(String filePath, int sizePixels) {
        String hash = sha256(filePath + "@" + sizePixels);
        String dirPrefix = hash.substring(0, DIR_PREFIX_LENGTH);
        String fileName = hash + THUMBNAIL_EXTENSION;
        return cacheDirectory.resolve(dirPrefix).resolve(fileName).toString();
    }

    /**
     * 文字列のSHA-256ハッシュを16進数文字列で返す。
     *
     * @param input ハッシュ化する文字列
     * @return SHA-256ハッシュの16進数文字列
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256はJava標準で必ず利用可能なため、この例外は発生しない
            throw new RuntimeException("SHA-256アルゴリズムが利用できません", e);
        }
    }

    /**
     * LRUキャッシュをクリアする。
     */
    public void clearCache() {
        lruCache.clear();
        log.info("サムネイルキャッシュをクリアしました");
    }

    /**
     * 現在のキャッシュサイズを返す。
     *
     * @return キャッシュに保持されているエントリ数
     */
    public int getCacheSize() {
        return lruCache.size();
    }

    /**
     * サムネイルキャッシュディレクトリ内の全ファイルを削除し、メモリキャッシュもクリアする。
     *
     * <p>全データクリア＆再インポート時に使用する。
     * ディレクトリ自体は削除後に再作成する。
     */
    public void clearAllThumbnails() {
        lruCache.clear();
        try {
            if (Files.exists(cacheDirectory)) {
                Files.walk(cacheDirectory)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("サムネイルファイルの削除に失敗しました: {}", path, e);
                            }
                        });
            }
            Files.createDirectories(cacheDirectory);
            log.info("サムネイルキャッシュを全削除しました: {}", cacheDirectory);
        } catch (IOException e) {
            log.error("サムネイルキャッシュの全削除に失敗しました", e);
            throw new RuntimeException("サムネイルキャッシュの全削除に失敗しました", e);
        }
    }
}
