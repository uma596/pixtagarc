package com.example.pixtagarc.service;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.dto.SearchCondition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 最近の検索条件履歴を管理するサービスクラス。
 *
 * <p>検索実行のたびに条件を記録し、最大5件を保持する。
 * 同一条件（キーワード+タグ+作者+Star+非表示の組み合わせ）は重複除去する。
 * 永続化は settings.properties に JSON 配列として保存する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class SearchHistoryService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(SearchHistoryService.class);

    /** 最大保持件数。 */
    private static final int MAX_SIZE = 5;

    /** 設定キー。 */
    public static final String KEY_SEARCH_HISTORY = "search.history";

    /** Gsonインスタンス。 */
    private static final Gson GSON = new Gson();

    /** 履歴リスト（新しいものが先頭）。 */
    private final LinkedList<SearchCondition> history = new LinkedList<>();

    /**
     * コンストラクタ。settings.properties から履歴を読み込む。
     */
    public SearchHistoryService() {
        load();
    }

    /**
     * 検索条件を履歴に追加する。
     *
     * <p>同一条件が既に存在する場合は最新位置に移動する。
     *
     * @param condition 検索条件
     */
    public void add(SearchCondition condition) {
        if (condition == null) return;
        history.removeIf(c -> isSameCondition(c, condition));
        history.addFirst(condition);
        if (history.size() > MAX_SIZE) {
            history.removeLast();
        }
        save();
    }

    /**
     * 最近の検索条件履歴を返す。
     *
     * @return 検索条件リスト（新しい順、最大5件）
     */
    public List<SearchCondition> getRecent() {
        return List.copyOf(history);
    }

    /**
     * 検索条件の要約ラベルを生成する。
     *
     * @param condition 検索条件
     * @return 要約文字列
     */
    public String buildSummary(SearchCondition condition) {
        List<String> parts = new ArrayList<>();
        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
            parts.add(condition.getKeyword());
        }
        if (condition.getMinStar() > 0) {
            parts.add("★" + condition.getMinStar() + "以上");
        }
        if (!condition.getTagIds().isEmpty()) {
            parts.add(condition.getTagIds().size() + "タグ");
        }
        if (!condition.getAuthorIds().isEmpty()) {
            parts.add("作者指定");
        }
        if (!condition.isExcludeHidden()) {
            parts.add("非表示含む");
        }
        return parts.isEmpty() ? "(条件なし)" : String.join(" ", parts);
    }

    /**
     * 2つの検索条件が同一かどうかを判定する。
     */
    private boolean isSameCondition(SearchCondition a, SearchCondition b) {
        return Objects.equals(a.getKeyword(), b.getKeyword())
                && Objects.equals(a.getTagIds(), b.getTagIds())
                && Objects.equals(a.getAuthorIds(), b.getAuthorIds())
                && a.getMinStar() == b.getMinStar()
                && a.isExcludeHidden() == b.isExcludeHidden();
    }

    /**
     * settings.properties から履歴を読み込む。
     */
    private void load() {
        try {
            String json = AppConfig.getInstance().getState(KEY_SEARCH_HISTORY, "[]");
            Type listType = new TypeToken<List<SearchCondition>>() {}.getType();
            List<SearchCondition> loaded = GSON.fromJson(json, listType);
            if (loaded != null) {
                history.clear();
                history.addAll(loaded);
            }
        } catch (Exception e) {
            log.warn("検索履歴の読み込みに失敗しました（無視します）", e);
        }
    }

    /**
     * settings.properties に履歴を保存する。
     */
    private void save() {
        try {
            String json = GSON.toJson(history);
            AppConfig.getInstance().setState(KEY_SEARCH_HISTORY, json);
        } catch (Exception e) {
            log.warn("検索履歴の保存に失敗しました", e);
        }
    }
}
