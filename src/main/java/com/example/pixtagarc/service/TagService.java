package com.example.pixtagarc.service;

import com.example.pixtagarc.domain.Tag;
import com.example.pixtagarc.exception.TagDuplicateException;
import com.example.pixtagarc.repository.ImageTagRepository;
import com.example.pixtagarc.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * タグサービスクラス。
 *
 * <p>タグの管理に関するビジネスロジックを提供する。
 * タグの作成・削除・画像へのタグ付けなどの操作を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class TagService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    /** タグリポジトリ。 */
    private final TagRepository tagRepository;

    /** 画像-タグ中間テーブルリポジトリ。 */
    private final ImageTagRepository imageTagRepository;

    /**
     * コンストラクタ。
     *
     * @param tagRepository      タグリポジトリ
     * @param imageTagRepository 画像-タグ中間テーブルリポジトリ
     */
    public TagService(TagRepository tagRepository, ImageTagRepository imageTagRepository) {
        this.tagRepository = tagRepository;
        this.imageTagRepository = imageTagRepository;
    }

    /**
     * 新しいタグを作成する。
     *
     * <p>同名のタグが既に存在する場合は既存のタグを返す。
     *
     * @param name タグ名
     * @return 作成または既存のタグエンティティ
     * @throws IllegalArgumentException タグ名が空の場合
     */
    public Tag createOrGet(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("タグ名は空にできません");
        }
        String trimmedName = name.trim();
        Optional<Tag> existing = tagRepository.findByName(trimmedName);
        if (existing.isPresent()) {
            log.debug("既存のタグを返します: name={}", trimmedName);
            return existing.get();
        }
        Tag tag = new Tag(trimmedName);
        tagRepository.save(tag);
        log.info("タグを作成しました: id={}, name={}", tag.getId(), tag.getName());
        return tag;
    }

    /**
     * タグを削除する。
     *
     * <p>タグを削除すると、関連する {@code image_tags} レコードもCASCADEで削除される。
     *
     * @param tagId 削除するタグID
     */
    public void deleteTag(Long tagId) {
        log.info("タグを削除します: id={}", tagId);
        tagRepository.deleteById(tagId);
        log.info("タグを削除しました: id={}", tagId);
    }

    /**
     * 画像にタグを追加する。
     *
     * @param imageId 画像ID
     * @param tagId   タグID
     */
    public void addTagToImage(Long imageId, Long tagId) {
        log.debug("画像にタグを追加します: imageId={}, tagId={}", imageId, tagId);
        imageTagRepository.addTag(imageId, tagId);
    }

    /**
     * 画像からタグを削除する。
     *
     * @param imageId 画像ID
     * @param tagId   タグID
     */
    public void removeTagFromImage(Long imageId, Long tagId) {
        log.debug("画像からタグを削除します: imageId={}, tagId={}", imageId, tagId);
        imageTagRepository.removeTag(imageId, tagId);
    }

    /**
     * 指定された画像に関連するタグのリストを返す。
     *
     * @param imageId 画像ID
     * @return タグエンティティのリスト
     */
    public List<Tag> getTagsForImage(Long imageId) {
        return imageTagRepository.findTagsByImageId(imageId);
    }

    /**
     * 全タグを名前順で返す。
     *
     * @return 全タグエンティティのリスト
     */
    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    /**
     * 指定された名前のタグを返す。
     *
     * @param name タグ名
     * @return タグエンティティのOptional
     */
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name);
    }

    /**
     * タグのStar評価を更新する。
     *
     * @param tagId タグID
     * @param star  Star評価（0〜5）
     */
    public void updateStar(Long tagId, int star) {
        log.info("タグStar評価を更新します: id={}, star={}", tagId, star);
        tagRepository.updateStar(tagId, star);
    }
}
