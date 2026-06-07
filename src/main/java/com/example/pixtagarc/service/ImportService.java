package com.example.pixtagarc.service;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.domain.Image;
import com.example.pixtagarc.repository.ImageRepository;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * インポートサービスクラス。
 *
 * <p>フォルダをスキャンして画像・動画ファイルをデータベースに登録する。
 * JavaFX {@link Task} を継承しており、ProgressBarとのバインドが可能。
 * ExecutorServiceで並列処理を行い、インポートを高速化する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImportService extends Task<Void> {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    /** ISO8601日時フォーマッター。 */
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 画像リポジトリ。 */
    private final ImageRepository imageRepository;

    /** サムネイルサービス。 */
    private final ThumbnailService thumbnailService;

    /** インポート対象のルートディレクトリ。 */
    private final Path rootDirectory;

    /** サブフォルダを含めるかどうか。 */
    private final boolean recursive;

    /** 既存ファイルをスキップするかどうか。 */
    private final boolean skipExisting;

    /**
     * コンストラクタ。
     *
     * @param imageRepository  画像リポジトリ
     * @param thumbnailService サムネイルサービス
     * @param rootDirectory    インポート対象のルートディレクトリ
     * @param recursive        サブフォルダを含める場合 {@code true}
     * @param skipExisting     既存ファイルをスキップする場合 {@code true}
     */
    public ImportService(ImageRepository imageRepository, ThumbnailService thumbnailService,
                         Path rootDirectory, boolean recursive, boolean skipExisting) {
        this.imageRepository = imageRepository;
        this.thumbnailService = thumbnailService;
        this.rootDirectory = rootDirectory;
        this.recursive = recursive;
        this.skipExisting = skipExisting;
    }

    /**
     * インポート処理を実行する（JavaFX Taskのメインメソッド）。
     *
     * <p>バックグラウンドスレッドで実行される。
     * 進捗はProgressBarにバインドされたプロパティを通じて更新される。
     *
     * @return null
     * @throws Exception インポート処理に失敗した場合
     */
    @Override
    protected Void call() throws Exception {
        log.info("インポートを開始します: directory={}, recursive={}", rootDirectory, recursive);
        updateMessage("ファイルをスキャン中...");

        // ファイル総数を先にカウント（リストをメモリに乗せない）
        int total = countFiles();
        log.info("スキャン完了: {} ファイルが見つかりました", total);

        if (total == 0) {
            updateMessage("インポート対象のファイルが見つかりませんでした");
            return null;
        }
        updateMessage(total + " ファイルが見つかりました。インポート中...");

        // 並列インポート処理
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        // daemonスレッドを使用 — JVMが強制終了されても残留しない
        ExecutorService executor = Executors.newFixedThreadPool(
                AppConfig.IMPORT_THREAD_POOL_SIZE,
                r -> {
                    Thread t = new Thread(r, "import-worker");
                    t.setDaemon(true);
                    return t;
                });
        try {
            List<Future<?>> futures = new ArrayList<>();

            // ファイルを逐次スキャンしてサブミット（全件リストをメモリに乗せない）
            Files.walkFileTree(rootDirectory,
                    java.util.Set.of(),
                    recursive ? Integer.MAX_VALUE : 1,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isCancelled()) return FileVisitResult.TERMINATE;

                            String fileName = file.getFileName().toString();
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                                if (AppConfig.isSupportedExtension(ext)) {
                                    Future<?> f = executor.submit(() -> {
                                        try {
                                            boolean wasImported = importFile(file, skipExisting);
                                            if (wasImported) imported.incrementAndGet();
                                            else skipped.incrementAndGet();
                                        } catch (OutOfMemoryError oom) {
                                            // OOM は個別ファイル単位で握りつぶしてインポートを継続
                                            log.error("メモリ不足のためファイルのインポートをスキップします: {}", file, oom);
                                            failed.incrementAndGet();
                                        } catch (Exception e) {
                                            log.warn("ファイルのインポートに失敗しました: {}", file, e);
                                            failed.incrementAndGet();
                                        } finally {
                                            int done = processed.incrementAndGet();
                                            updateProgress(done, total);
                                            updateMessage(String.format("インポート中... %d / %d", done, total));
                                        }
                                    });
                                    futures.add(f);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            log.warn("ファイルへのアクセスに失敗しました: {}", file, exc);
                            return FileVisitResult.CONTINUE;
                        }
                    });

            // 全タスクの完了を待機
            for (Future<?> future : futures) {
                if (isCancelled()) break;
                try {
                    future.get();
                } catch (Exception e) {
                    log.warn("インポートタスクが異常終了しました", e);
                }
            }
        } finally {
            // shutdownNow でデーモンスレッドも含めてタスクをキャンセル
            executor.shutdownNow();
        }

        String summary = String.format("インポート完了: %d件インポート, %d件スキップ, %d件失敗",
                imported.get(), skipped.get(), failed.get());
        log.info(summary);
        updateMessage(summary);
        updateProgress(total, total);
        return null;
    }

    /**
     * ルートディレクトリ内のサポートファイル数をカウントする。
     *
     * <p>全ファイルパスをリストに持たず件数のみ返すことでメモリを節約する。
     *
     * @return サポートされているファイルの件数
     * @throws IOException スキャンに失敗した場合
     */
    private int countFiles() throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        Files.walkFileTree(rootDirectory, java.util.Set.of(), maxDepth,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String fileName = file.getFileName().toString();
                        int dotIndex = fileName.lastIndexOf('.');
                        if (dotIndex > 0) {
                            String ext = fileName.substring(dotIndex + 1).toLowerCase();
                            if (AppConfig.isSupportedExtension(ext)) {
                                count.incrementAndGet();
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.warn("ファイルへのアクセスに失敗しました: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
        return count.get();
    }

    /**
     * 単一ファイルをインポートする。
     *
     * <p>メタデータを取得してDBに登録し、サムネイルを生成する。
     *
     * @param file         インポートするファイルのパス
     * @param skipExisting 既存ファイルをスキップする場合 {@code true}
     * @return インポートした場合 {@code true}、スキップした場合 {@code false}
     * @throws IOException インポートに失敗した場合
     */
    private boolean importFile(Path file, boolean skipExisting) throws IOException {
        String filePath = file.toAbsolutePath().toString();

        // 既存チェック
        if (skipExisting && imageRepository.findByFilePath(filePath).isPresent()) {
            log.debug("既存ファイルをスキップします: {}", filePath);
            return false;
        }

        String fileName = file.getFileName().toString();
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String mediaType = AppConfig.isSupportedVideoExtension(ext) ? "video" : "image";

        // ファイルサイズ取得
        long fileSize = Files.size(file);

        // 作成日時取得
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                attrs.creationTime().toInstant(), ZoneId.systemDefault());
        String createdAtStr = createdAt.format(DATETIME_FORMATTER);
        String importedAtStr = LocalDateTime.now().format(DATETIME_FORMATTER);

        // 画像の場合は解像度を取得（フルデコードを避けてメモリ節約）
        Integer width = null;
        Integer height = null;
        if ("image".equals(mediaType)) {
            try {
                // ImageReader を使ってヘッダー情報のみ読み込む（フルデコード不要）
                try (javax.imageio.stream.ImageInputStream iis =
                        javax.imageio.ImageIO.createImageInputStream(file.toFile())) {
                    if (iis != null) {
                        java.util.Iterator<javax.imageio.ImageReader> readers =
                                javax.imageio.ImageIO.getImageReaders(iis);
                        if (readers.hasNext()) {
                            javax.imageio.ImageReader reader = readers.next();
                            try {
                                reader.setInput(iis, true, true);
                                width = reader.getWidth(0);
                                height = reader.getHeight(0);
                            } finally {
                                reader.dispose();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("解像度の取得に失敗しました（スキップ）: {}", filePath, e);
            }
        }

        // DBに登録
        Image image = new Image();
        image.setFilePath(filePath);
        image.setFileName(fileName);
        image.setFileSize(fileSize);
        image.setWidth(width);
        image.setHeight(height);
        image.setMediaType(mediaType);
        image.setHidden(false);
        image.setCreatedAt(createdAtStr);
        image.setImportedAt(importedAtStr);
        imageRepository.save(image);

        // FTS5インデックスに追加（トリガーを使わず手動で挿入）
        try {
            imageRepository.insertFts(image.getId(), fileName, "", "");
        } catch (Exception e) {
            log.warn("FTS5インデックスへの追加に失敗しました（インポートは継続）: {}", filePath, e);
        }

        // サムネイル生成（画像のみ）
        if ("image".equals(mediaType)) {
            try {
                thumbnailService.getThumbnailPath(filePath, AppConfig.THUMBNAIL_SIZE_MEDIUM);
            } catch (Exception e) {
                // サムネイル生成失敗はインポート失敗とはしない
                log.warn("サムネイル生成に失敗しました（インポートは継続）: {}", filePath, e);
            }
        }

        log.debug("ファイルをインポートしました: {}", filePath);
        return true;
    }
}
