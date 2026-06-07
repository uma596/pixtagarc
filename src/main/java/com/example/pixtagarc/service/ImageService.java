package com.example.pixtagarc.service;

import com.example.pixtagarc.domain.Image;
import com.example.pixtagarc.exception.ImageNotFoundException;
import com.example.pixtagarc.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 画像サービスクラス。
 *
 * <p>画像メタデータの管理に関するビジネスロジックを提供する。
 * 作者の更新・非表示フラグの更新などの操作を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ImageService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /** 画像リポジトリ。 */
    private final ImageRepository imageRepository;

    /**
     * コンストラクタ。
     *
     * @param imageRepository 画像リポジトリ
     */
    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * 指定された画像の作者を更新する。
     *
     * @param imageId  更新する画像ID
     * @param authorId 新しい作者ID（nullの場合は作者なしに設定）
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     */
    public void updateAuthor(Long imageId, Long authorId) {
        log.info("画像の作者を更新します: imageId={}, authorId={}", imageId, authorId);
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> ImageNotFoundException.ofId(imageId));
        image.setAuthorId(authorId);
        imageRepository.update(image);
        log.info("画像の作者を更新しました: imageId={}, authorId={}", imageId, authorId);
    }

    /**
     * 指定された画像の非表示フラグを更新する。
     *
     * @param imageId  更新する画像ID
     * @param isHidden 非表示フラグ
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     */
    public void updateHidden(Long imageId, boolean isHidden) {
        log.info("非表示フラグを更新します: imageId={}, isHidden={}", imageId, isHidden);
        imageRepository.updateHidden(imageId, isHidden);
        log.info("非表示フラグを更新しました: imageId={}, isHidden={}", imageId, isHidden);
    }

    /**
     * 指定された画像のStar評価を更新する。
     *
     * @param imageId 更新する画像ID
     * @param star    Star評価（0〜5）
     * @throws ImageNotFoundException    指定されたIDの画像が存在しない場合
     * @throws IllegalArgumentException  starが0〜5の範囲外の場合
     */
    public void updateStar(Long imageId, int star) {
        if (star < 0 || star > 5) {
            throw new IllegalArgumentException("Star評価は0〜5の範囲で指定してください: star=" + star);
        }
        log.info("Star評価を更新します: imageId={}, star={}", imageId, star);
        imageRepository.updateStar(imageId, star);
        log.info("Star評価を更新しました: imageId={}, star={}", imageId, star);
    }

    /**
     * 指定されたIDの画像を返す。
     *
     * @param imageId 検索する画像ID
     * @return 画像エンティティ
     * @throws ImageNotFoundException 指定されたIDの画像が存在しない場合
     */
    public Image findById(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> ImageNotFoundException.ofId(imageId));
    }

    /**
     * 指定されたファイルパスの画像を返す。
     *
     * @param filePath 検索するファイルパス
     * @return 画像エンティティのOptional
     */
    public Optional<Image> findByFilePath(String filePath) {
        return imageRepository.findByFilePath(filePath);
    }

    /**
     * 指定された画像のタグを同一作品の全画像に反映する。
     *
     * @param imageId    反映元の画像ID
     * @param tagService タグサービス（タグ一覧取得用）
     * @param imageTagRepository 画像-タグ中間テーブルリポジトリ
     * @return 反映先の画像件数
     */
    public int applyTagsToWork(Long imageId,
                               com.example.pixtagarc.repository.ImageTagRepository imageTagRepository) {
        Image image = findById(imageId);
        if (image.getWorkId() == null) return 0;

        java.util.List<Image> workImages = imageRepository.findByWorkId(image.getWorkId());
        java.util.List<com.example.pixtagarc.domain.Tag> tags = imageTagRepository.findTagsByImageId(imageId);

        for (Image img : workImages) {
            for (com.example.pixtagarc.domain.Tag tag : tags) {
                imageTagRepository.addTag(img.getId(), tag.getId());
            }
            // FTS5更新
            String tagsText = imageTagRepository.getTagsTextForImage(img.getId());
            imageRepository.insertFts(img.getId(), img.getFileName(), tagsText, "");
        }
        log.info("タグを作品全体に反映しました: imageId={}, workId={}, count={}",
                imageId, image.getWorkId(), workImages.size());
        return workImages.size();
    }

    /**
     * 指定された画像のStar評価を同一作品の全画像に反映する。
     *
     * @param imageId 反映元の画像ID
     * @return 反映先の画像件数
     */
    public int applyStarToWork(Long imageId) {
        Image image = findById(imageId);
        if (image.getWorkId() == null) return 0;

        java.util.List<Image> workImages = imageRepository.findByWorkId(image.getWorkId());
        for (Image img : workImages) {
            imageRepository.updateStar(img.getId(), image.getStar());
        }
        log.info("Starを作品全体に反映しました: imageId={}, workId={}, star={}, count={}",
                imageId, image.getWorkId(), image.getStar(), workImages.size());
        return workImages.size();
    }

    /**
     * 指定された作品の全画像の非表示フラグを一括更新する。
     *
     * @param workId   作品ID
     * @param isHidden 非表示フラグ
     * @return 更新された画像件数
     */
    public int toggleHiddenForWork(Long workId, boolean isHidden) {
        int count = imageRepository.updateHiddenByWorkId(workId, isHidden);
        log.info("作品全体の非表示を更新しました: workId={}, isHidden={}, count={}", workId, isHidden, count);
        return count;
    }
}
