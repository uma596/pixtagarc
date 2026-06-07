package com.example.pixtagarc.service;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.domain.Author;
import com.example.pixtagarc.domain.Image;
import com.example.pixtagarc.domain.Tag;
import com.example.pixtagarc.domain.Work;
import com.example.pixtagarc.dto.ImportMetadata;
import com.example.pixtagarc.repository.ImageRepository;
import com.example.pixtagarc.repository.ImageTagRepository;
import com.example.pixtagarc.repository.WorkRepository;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * インポートサービスクラス。
 *
 * <p>フォルダをスキャンして画像・動画ファイルをデータベースに登録する。
 * JavaFX {@link Task} を継承しており、ProgressBarとのバインドが可能。
 * ExecutorServiceで並列処理を行い、インポートを高速化する。
 *
 * <p>画像ファイルと同名の {@code -meta.json} ファイルが存在する場合は、
 * JSON内のタグ・作者・作品情報を自動的に付与する。
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

    /**
     * ファイル名から作品IDとページ番号を抽出する正規表現パターン。
     *
     * <p>例: {@code 143716458_p0003-作品タイトル.jpg} → group(1)="143716458", group(2)="0003"
     */
    private static final Pattern WORK_FILE_PATTERN =
            Pattern.compile("^(\\d+)_p(\\d{4})-(.+?)\\.[^.]+$");

    /**
     * メタデータJSON用ファイル名パターン。
     *
     * <p>例: {@code 143716458_p0000-作品タイトル-meta.json}
     */
    private static final Pattern META_JSON_PATTERN =
            Pattern.compile("^(\\d+)_p0000-(.+)-meta\\.json$");

    /** Gsonインスタンス（スレッドセーフ）。 */
    private static final Gson GSON = new Gson();

    /** 画像リポジトリ。 */
    private final ImageRepository imageRepository;

    /** サムネイルサービス。 */
    private final ThumbnailService thumbnailService;

    /** 作品リポジトリ。 */
    private final WorkRepository workRepository;

    /** 画像-タグ中間テーブルリポジトリ。 */
    private final ImageTagRepository imageTagRepository;

    /** タグサービス。 */
    private final TagService tagService;

    /** 作者サービス。 */
    private final AuthorService authorService;

    /** インポート対象のルートディレクトリ。 */
    private final Path rootDirectory;

    /** サブフォルダを含めるかどうか。 */
    private final boolean recursive;

    /** 既存ファイルをスキップするかどうか。 */
    private final boolean skipExisting;

    /** 全データクリア後に再インポートするかどうか。 */
    private final boolean clearAll;

    /** フォルダごとに1作品としてまとめるかどうか。 */
    private final boolean groupByFolder;

    /** 作者リポジトリ（クリア処理用）。 */
    private final com.example.pixtagarc.repository.AuthorRepository authorRepository;

    /** データベース設定（VACUUM用）。 */
    private final com.example.pixtagarc.config.DatabaseConfig dbConfig;

    /**
     * ディレクトリごとのメタデータキャッシュ。
     *
     * <p>キーは「作品ID（外部ID文字列）」、値はパース済みメタデータ。
     * 同一作品IDの複数ページで同じメタデータを再利用するためキャッシュする。
     */
    private final ConcurrentHashMap<String, ImportMetadata> metadataCache = new ConcurrentHashMap<>();

    /**
     * 作品IDに対するDBでのWorkエンティティのキャッシュ。
     *
     * <p>同一作品IDの複数ページで作品レコードの二重作成を防ぐ。
     */
    private final ConcurrentHashMap<String, Work> workCache = new ConcurrentHashMap<>();

    /**
     * フォルダパス → 作品エンティティのキャッシュ（フォルダ作品化用）。
     *
     * <p>同一フォルダ内の複数ファイルで作品レコードの二重作成を防ぐ。
     */
    private final ConcurrentHashMap<String, Work> folderWorkCache = new ConcurrentHashMap<>();

    /**
     * フォルダ作品の external_id 採番用カウンター。
     *
     * <p>インポート開始時にDBの最大値を取得して初期化する。
     */
    private final java.util.concurrent.atomic.AtomicLong folderIdSequence = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * コンストラクタ。
     *
     * @param imageRepository    画像リポジトリ
     * @param thumbnailService   サムネイルサービス
     * @param workRepository     作品リポジトリ
     * @param imageTagRepository 画像-タグ中間テーブルリポジトリ
     * @param tagService         タグサービス
     * @param authorService      作者サービス
     * @param authorRepository   作者リポジトリ（クリア処理用）
     * @param dbConfig           データベース設定（VACUUM用）
     * @param rootDirectory      インポート対象のルートディレクトリ
     * @param recursive          サブフォルダを含める場合 {@code true}
     * @param skipExisting       既存ファイルをスキップする場合 {@code true}
     * @param clearAll           全データクリア後に再インポートする場合 {@code true}
     * @param groupByFolder      フォルダごとに1作品としてまとめる場合 {@code true}
     */
    public ImportService(ImageRepository imageRepository, ThumbnailService thumbnailService,
                         WorkRepository workRepository, ImageTagRepository imageTagRepository,
                         TagService tagService, AuthorService authorService,
                         com.example.pixtagarc.repository.AuthorRepository authorRepository,
                         com.example.pixtagarc.config.DatabaseConfig dbConfig,
                         Path rootDirectory, boolean recursive, boolean skipExisting,
                         boolean clearAll, boolean groupByFolder) {
        this.imageRepository = imageRepository;
        this.thumbnailService = thumbnailService;
        this.workRepository = workRepository;
        this.imageTagRepository = imageTagRepository;
        this.tagService = tagService;
        this.authorService = authorService;
        this.authorRepository = authorRepository;
        this.dbConfig = dbConfig;
        this.rootDirectory = rootDirectory;
        this.recursive = recursive;
        this.skipExisting = skipExisting;
        this.clearAll = clearAll;
        this.groupByFolder = groupByFolder;
    }

    /**
     * インポート処理を実行する（JavaFX Taskのメインメソッド）。
     *
     * <p>バックグラウンドスレッドで実行される。
     * 進捗はProgressBarにバインドされたプロパティを通じて更新される。
     *
     * <p>処理の流れ:
     * <ol>
     *   <li>サポートファイルの総数をカウント</li>
     *   <li>メタデータJSON（{@code -meta.json}）をプリスキャンしてキャッシュ</li>
     *   <li>画像ファイルを並列でインポート（メタデータ適用含む）</li>
     * </ol>
     *
     * @return null
     * @throws Exception インポート処理に失敗した場合
     */
    @Override
    protected Void call() throws Exception {
        log.info("インポートを開始します: directory={}, recursive={}, clearAll={}, groupByFolder={}",
                rootDirectory, recursive, clearAll, groupByFolder);

        // 全データクリア処理
        if (clearAll) {
            updateMessage("全データをクリア中...");
            performClearAll();
        }

        // フォルダ作品化モードの連番初期化
        if (groupByFolder) {
            long maxId = workRepository.getMaxFolderExternalId();
            folderIdSequence.set(maxId);
        }

        updateMessage("ファイルをスキャン中...");

        // ファイル総数を先にカウント（リストをメモリに乗せない）
        int total = countFiles();
        log.info("スキャン完了: {} ファイルが見つかりました", total);

        if (total == 0) {
            updateMessage("インポート対象のファイルが見つかりませんでした");
            return null;
        }

        // メタデータJSONのプリスキャン
        updateMessage("メタデータJSONをスキャン中...");
        prescanMetadataFiles();
        log.info("メタデータスキャン完了: {}件のメタデータを読み込みました", metadataCache.size());

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

        // フォルダ作品化のページ番号を一括割り当て
        if (groupByFolder && !folderWorkCache.isEmpty()) {
            updateMessage("ページ番号を割り当て中...");
            assignFolderPageNumbers();
        }

        updateMessage(summary);
        updateProgress(total, total);
        return null;
    }

    /**
     * ルートディレクトリ内のメタデータJSONファイルをプリスキャンしてキャッシュに格納する。
     *
     * <p>{@code {作品ID}_p0000-{タイトル}-meta.json} パターンに一致するファイルを探し、
     * Gsonでパースして作品IDをキーとしてキャッシュする。
     *
     * @throws IOException ファイルスキャンに失敗した場合
     */
    private void prescanMetadataFiles() throws IOException {
        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        Files.walkFileTree(rootDirectory, java.util.Set.of(), maxDepth,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String fileName = file.getFileName().toString();
                        Matcher matcher = META_JSON_PATTERN.matcher(fileName);
                        if (matcher.matches()) {
                            String workExternalId = matcher.group(1);
                            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                                ImportMetadata metadata = GSON.fromJson(reader, ImportMetadata.class);
                                if (metadata != null) {
                                    metadataCache.put(workExternalId, metadata);
                                    log.debug("メタデータを読み込みました: workId={}, title={}",
                                            workExternalId, metadata.getTitle());
                                }
                            } catch (JsonSyntaxException e) {
                                log.warn("メタデータJSONのパースに失敗しました（スキップ）: {}", file, e);
                            } catch (IOException e) {
                                log.warn("メタデータJSONの読み込みに失敗しました（スキップ）: {}", file, e);
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
    }

    /**
     * 全データクリア処理を実行する。
     *
     * <p>FTS5インデックス → image_tags → images → works → authors の順で削除し、
     * サムネイルキャッシュを全削除した後にVACUUMを実行する。
     */
    private void performClearAll() {
        log.info("全データクリアを開始します");

        // 1. FTS5 インデックスをクリア
        dbConfig.clearFtsIndex();

        // 2. 画像-タグ紐付けを削除
        imageTagRepository.deleteAll();

        // 3. 画像を全削除
        imageRepository.deleteAll();

        // 4. 作品を全削除
        workRepository.deleteAll();

        // 5. 作者を全削除（タグマスタは残す）
        authorRepository.deleteAll();

        // 6. サムネイルキャッシュを全削除
        thumbnailService.clearAllThumbnails();

        // 7. VACUUM
        dbConfig.vacuum();

        log.info("全データクリアが完了しました");
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
     * ファイル名が作品パターンに一致し、対応するメタデータJSONが存在する場合は
     * 作品・タグ・作者情報を自動付与する。
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

        // ファイル名から作品ID・ページ番号を抽出
        Matcher workMatcher = WORK_FILE_PATTERN.matcher(fileName);
        String workExternalId = null;
        Integer pageNumber = null;

        if (workMatcher.matches()) {
            workExternalId = workMatcher.group(1);
            // ページ番号は0始まり→1始まりに変換
            pageNumber = Integer.parseInt(workMatcher.group(2)) + 1;
        }

        // メタデータから日時を取得（JSONのdateフィールドがあれば優先）
        ImportMetadata metadata = (workExternalId != null) ? metadataCache.get(workExternalId) : null;
        if (metadata != null && metadata.getDate() != null) {
            String parsedDate = parseIso8601ToLocal(metadata.getDate());
            if (parsedDate != null) {
                createdAtStr = parsedDate;
            }
        }

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

        // 作品・作者情報を取得（メタデータがある場合）
        Long workId = null;
        Long authorId = null;

        if (metadata != null && workExternalId != null) {
            // 作者を取得または作成
            authorId = resolveAuthorId(metadata);

            // 作品を取得または作成
            Work work = resolveWork(workExternalId, metadata, authorId);
            workId = work.getId();
        }

        // フォルダ作品化モード: メタデータによる作品化が行われなかったファイルをフォルダ単位で作品化
        if (groupByFolder && workId == null) {
            Path parentDir = file.getParent();
            if (parentDir != null) {
                // recursive=OFF: ルート直下のファイルをルートフォルダ名で1作品化
                // recursive=ON:  サブフォルダ内のファイルを各フォルダ名で作品化（ルート直下は作品未所属）
                boolean shouldGroupAsWork;
                if (!recursive) {
                    // サブフォルダOFF: ルート直下のファイルを1作品としてまとめる
                    shouldGroupAsWork = parentDir.equals(rootDirectory);
                } else {
                    // サブフォルダON: サブフォルダ内のファイルのみ作品化（ルート直下は未所属）
                    shouldGroupAsWork = !parentDir.equals(rootDirectory);
                }

                if (shouldGroupAsWork) {
                    String folderPath = parentDir.toAbsolutePath().toString();
                    String folderName = parentDir.getFileName().toString();

                    Work folderWork = folderWorkCache.computeIfAbsent(folderPath, key -> {
                        long nextId = folderIdSequence.incrementAndGet();
                        String externalId = String.format("F%010d", nextId);

                        Work newWork = new Work();
                        newWork.setTitle(folderName);
                        newWork.setExternalId(externalId);
                        newWork.setCreatedAt(java.time.LocalDateTime.now().format(DATETIME_FORMATTER));
                        newWork.setUpdatedAt(newWork.getCreatedAt());
                        synchronized (workRepository) {
                            workRepository.save(newWork);
                        }
                        log.info("フォルダ作品を作成しました: title={}, externalId={}", folderName, externalId);
                        return newWork;
                    });

                    workId = folderWork.getId();
                    // ページ番号は後でassignFolderPageNumbersで一括設定するためここではnull
                }
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
        image.setAuthorId(authorId);
        image.setWorkId(workId);
        image.setPageNumber(pageNumber);
        image.setHidden(false);
        image.setCreatedAt(createdAtStr);
        image.setImportedAt(importedAtStr);

        // synchronizedで画像登録（SQLiteの同時書き込み対策）
        synchronized (imageRepository) {
            imageRepository.save(image);
        }

        // タグを付与（メタデータがある場合）
        if (metadata != null && metadata.getTags() != null && !metadata.getTags().isEmpty()) {
            applyTags(image.getId(), metadata.getTags());
        }

        // FTS5インデックスに追加
        try {
            String tagsText = (metadata != null && metadata.getTags() != null)
                    ? String.join(" ", metadata.getTags()) : "";
            String authorName = (metadata != null && metadata.getUser() != null)
                    ? metadata.getUser() : "";
            synchronized (imageRepository) {
                imageRepository.insertFts(image.getId(), fileName, tagsText, authorName);
            }
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

    /**
     * メタデータから作者IDを解決する。
     *
     * <p>作者名が存在する場合、既存の作者を検索し、なければ新規作成する。
     * スレッドセーフに作者の作成/取得を行う。
     *
     * @param metadata インポートメタデータ
     * @return 作者ID（作者情報がない場合は {@code null}）
     */
    private Long resolveAuthorId(ImportMetadata metadata) {
        if (metadata.getUser() == null || metadata.getUser().trim().isEmpty()) {
            return null;
        }
        synchronized (authorService) {
            Author author = authorService.createOrGet(metadata.getUser().trim());
            return author.getId();
        }
    }

    /**
     * 作品を解決する（キャッシュから取得、またはDBから検索/新規作成）。
     *
     * <p>同一外部IDの作品が複数回参照されてもDB上で1レコードのみ作成されるよう、
     * キャッシュを使って二重作成を防ぐ。
     *
     * @param workExternalId 作品の外部ID文字列
     * @param metadata       インポートメタデータ
     * @param authorId       作者ID（nullable）
     * @return 作品エンティティ（ID付き）
     */
    private Work resolveWork(String workExternalId, ImportMetadata metadata, Long authorId) {
        // キャッシュに存在する場合はそのまま返す
        Work cached = workCache.get(workExternalId);
        if (cached != null) {
            return cached;
        }

        synchronized (workRepository) {
            // ダブルチェック — 他スレッドが先に作成した可能性
            cached = workCache.get(workExternalId);
            if (cached != null) {
                return cached;
            }

            // DB上に既に存在するか検索
            Optional<Work> existing = workRepository.findByExternalId(workExternalId);
            if (existing.isPresent()) {
                workCache.put(workExternalId, existing.get());
                return existing.get();
            }

            // 新規作成
            String now = LocalDateTime.now().format(DATETIME_FORMATTER);
            String workCreatedAt = now;
            if (metadata.getDate() != null) {
                String parsed = parseIso8601ToLocal(metadata.getDate());
                if (parsed != null) {
                    workCreatedAt = parsed;
                }
            }

            Work work = new Work();
            work.setTitle(metadata.getTitle() != null ? metadata.getTitle() : "Untitled");
            work.setAuthorId(authorId);
            work.setExternalId(workExternalId);
            work.setTotalPages(metadata.getPageCount());
            work.setCreatedAt(workCreatedAt);
            work.setUpdatedAt(now);
            workRepository.save(work);

            workCache.put(workExternalId, work);
            log.info("作品を登録しました: externalId={}, title={}", workExternalId, work.getTitle());
            return work;
        }
    }

    /**
     * 画像にタグを付与する。
     *
     * <p>タグ名ごとに既存タグを検索し、なければ新規作成してから画像に紐付ける。
     *
     * @param imageId  画像ID
     * @param tagNames タグ名リスト
     */
    private void applyTags(Long imageId, List<String> tagNames) {
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) continue;
            try {
                Tag tag;
                synchronized (tagService) {
                    tag = tagService.createOrGet(tagName.trim());
                }
                synchronized (imageTagRepository) {
                    imageTagRepository.addTag(imageId, tag.getId());
                }
            } catch (Exception e) {
                log.warn("タグの付与に失敗しました（スキップ）: imageId={}, tag={}",
                        imageId, tagName, e);
            }
        }
    }

    /**
     * フォルダ作品化時のページ番号を一括設定する。
     *
     * <p>インポート完了後に各フォルダ作品のページ番号をファイル名昇順で再付番する。
     * 並列インポートでは挿入順が保証されないため、完了後に一括で割り当てる。
     */
    private void assignFolderPageNumbers() {
        for (java.util.Map.Entry<String, Work> entry : folderWorkCache.entrySet()) {
            Long wId = entry.getValue().getId();
            List<com.example.pixtagarc.domain.Image> workImages = imageRepository.findByWorkId(wId);
            // ファイル名昇順ソート
            workImages.sort(java.util.Comparator.comparing(com.example.pixtagarc.domain.Image::getFileName));
            for (int i = 0; i < workImages.size(); i++) {
                imageRepository.updatePageNumber(workImages.get(i).getId(), i + 1);
            }
            // 総ページ数を更新
            workRepository.updateTotalPages(wId, workImages.size());
            log.debug("フォルダ作品のページ番号を割り当てました: workId={}, pages={}", wId, workImages.size());
        }
        log.info("フォルダ作品のページ番号割り当て完了: {}作品", folderWorkCache.size());
    }

    /**
     * ISO8601形式の日時文字列をローカル日時文字列に変換する。
     *
     * <p>タイムゾーン情報を考慮してシステムタイムゾーンのローカル日時に変換する。
     *
     * @param iso8601 ISO8601形式の日時文字列（例: "2026-04-18T15:07:00+00:00"）
     * @return ローカル日時文字列（yyyy-MM-dd'T'HH:mm:ss形式）、パース失敗時は {@code null}
     */
    private String parseIso8601ToLocal(String iso8601) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso8601);
            LocalDateTime localDt = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            return localDt.format(DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.debug("ISO8601日時のパースに失敗しました: {}", iso8601, e);
            return null;
        }
    }
}
