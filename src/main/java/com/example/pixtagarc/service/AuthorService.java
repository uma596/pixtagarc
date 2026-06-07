package com.example.pixtagarc.service;

import com.example.pixtagarc.domain.Author;
import com.example.pixtagarc.repository.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 作者サービスクラス。
 *
 * <p>作者の管理に関するビジネスロジックを提供する。
 * 作者の作成・検索などの操作を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class AuthorService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);

    /** 作者リポジトリ。 */
    private final AuthorRepository authorRepository;

    /**
     * コンストラクタ。
     *
     * @param authorRepository 作者リポジトリ
     */
    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * 新しい作者を作成する。
     *
     * <p>同名の作者が既に存在する場合は既存の作者を返す。
     *
     * @param name 作者名
     * @return 作成または既存の作者エンティティ
     * @throws IllegalArgumentException 作者名が空の場合
     */
    public Author createOrGet(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("作者名は空にできません");
        }
        String trimmedName = name.trim();
        Optional<Author> existing = authorRepository.findByName(trimmedName);
        if (existing.isPresent()) {
            log.debug("既存の作者を返します: name={}", trimmedName);
            return existing.get();
        }
        Author author = new Author(trimmedName);
        authorRepository.save(author);
        log.info("作者を作成しました: id={}, name={}", author.getId(), author.getName());
        return author;
    }

    /**
     * 作者を削除する。
     *
     * <p>作者を削除すると、関連する画像の {@code author_id} はNULLに設定される（ON DELETE SET NULL）。
     *
     * @param authorId 削除する作者ID
     */
    public void deleteAuthor(Long authorId) {
        log.info("作者を削除します: id={}", authorId);
        authorRepository.deleteById(authorId);
        log.info("作者を削除しました: id={}", authorId);
    }

    /**
     * 全作者を名前順で返す。
     *
     * @return 全作者エンティティのリスト
     */
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    /**
     * 指定された名前の作者を返す。
     *
     * @param name 作者名
     * @return 作者エンティティのOptional
     */
    public Optional<Author> findByName(String name) {
        return authorRepository.findByName(name);
    }

    /**
     * 指定されたIDの作者を返す。
     *
     * @param id 作者ID
     * @return 作者エンティティのOptional
     */
    public Optional<Author> findById(Long id) {
        return authorRepository.findById(id);
    }
}
