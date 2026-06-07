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
}
