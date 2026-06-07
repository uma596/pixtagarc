package com.example.pixtagarc.service;

import com.example.pixtagarc.domain.SavedSearch;
import com.example.pixtagarc.dto.SearchCondition;
import com.example.pixtagarc.repository.SavedSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 保存済み検索サービスクラス。
 *
 * <p>保存済み検索条件のCRUD操作に関するビジネスロジックを提供する。
 * 検索条件の保存・読み込み・削除を担当する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SavedSearchService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SavedSearchService.class);

    /** ISO8601日時フォーマッター。 */
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 保存済み検索リポジトリ。 */
    private final SavedSearchRepository savedSearchRepository;

    /**
     * コンストラクタ。
     *
     * @param savedSearchRepository 保存済み検索リポジトリ
     */
    public SavedSearchService(SavedSearchRepository savedSearchRepository) {
        this.savedSearchRepository = savedSearchRepository;
    }

    /**
     * 検索条件を指定された名前で保存する。
     *
     * @param name      保存名
     * @param condition 保存する検索条件
     * @return 保存された {@link SavedSearch} エンティティ
     * @throws IllegalArgumentException 保存名が空の場合
     */
    public SavedSearch save(String name, SearchCondition condition) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("保存名は空にできません");
        }
        String now = LocalDateTime.now().format(DATETIME_FORMATTER);
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setName(name.trim());
        savedSearch.setKeyword(condition.getKeyword());
        savedSearch.setTagIds(idsToString(condition.getTagIds()));
        savedSearch.setAuthorIds(idsToString(condition.getAuthorIds()));
        savedSearch.setDateFrom(condition.getDateFrom());
        savedSearch.setDateTo(condition.getDateTo());
        savedSearch.setExcludeHidden(condition.isExcludeHidden());
        savedSearch.setMinStar(condition.getMinStar());
        savedSearch.setSortColumn(condition.getSortColumn());
        savedSearch.setSortOrder(condition.getSortOrder());
        savedSearch.setCreatedAt(now);
        savedSearch.setUpdatedAt(now);
        savedSearchRepository.save(savedSearch);
        log.info("検索条件を保存しました: name={}", name);
        return savedSearch;
    }

    /**
     * 保存済み検索条件を {@link SearchCondition} に変換して返す。
     *
     * @param savedSearchId 保存済み検索条件ID
     * @return 検索条件
     * @throws RuntimeException 指定されたIDの保存済み検索条件が存在しない場合
     */
    public SearchCondition toSearchCondition(Long savedSearchId) {
        SavedSearch savedSearch = savedSearchRepository.findById(savedSearchId)
                .orElseThrow(() -> new RuntimeException("保存済み検索が見つかりません: id=" + savedSearchId));
        return convertToCondition(savedSearch);
    }

    /**
     * 保存済み検索条件を {@link SearchCondition} に変換する。
     *
     * @param savedSearch 保存済み検索条件エンティティ
     * @return 検索条件
     */
    public SearchCondition convertToCondition(SavedSearch savedSearch) {
        SearchCondition condition = new SearchCondition();
        condition.setKeyword(savedSearch.getKeyword());
        condition.setTagIds(stringToIds(savedSearch.getTagIds()));
        condition.setAuthorIds(stringToIds(savedSearch.getAuthorIds()));
        condition.setDateFrom(savedSearch.getDateFrom());
        condition.setDateTo(savedSearch.getDateTo());
        condition.setExcludeHidden(savedSearch.isExcludeHidden());
        condition.setMinStar(savedSearch.getMinStar());
        condition.setSortColumn(savedSearch.getSortColumn());
        condition.setSortOrder(savedSearch.getSortOrder());
        return condition;
    }

    /**
     * 保存済み検索条件を削除する。
     *
     * @param savedSearchId 削除する保存済み検索条件ID
     */
    public void delete(Long savedSearchId) {
        log.info("保存済み検索を削除します: id={}", savedSearchId);
        savedSearchRepository.deleteById(savedSearchId);
        log.info("保存済み検索を削除しました: id={}", savedSearchId);
    }

    /**
     * 全保存済み検索条件を返す。
     *
     * @return 全保存済み検索条件エンティティのリスト
     */
    public List<SavedSearch> findAll() {
        return savedSearchRepository.findAll();
    }

    /**
     * 指定されたIDの保存済み検索条件を返す。
     *
     * @param id 保存済み検索条件ID
     * @return 保存済み検索条件エンティティのOptional
     */
    public Optional<SavedSearch> findById(Long id) {
        return savedSearchRepository.findById(id);
    }

    /**
     * IDリストをカンマ区切り文字列に変換する。
     *
     * @param ids IDリスト
     * @return カンマ区切り文字列（空の場合はnull）
     */
    private String idsToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * カンマ区切り文字列をIDリストに変換する。
     *
     * @param str カンマ区切り文字列
     * @return IDリスト（nullまたは空の場合は空リスト）
     */
    private List<Long> stringToIds(String str) {
        if (str == null || str.trim().isEmpty()) return List.of();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
