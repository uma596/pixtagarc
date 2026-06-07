package com.example.pixtagarc.service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最近使用したタグの履歴を管理するクラス。
 *
 * <p>アプリ起動中はメモリに保持し、変更のたびに settings.properties に永続化する。
 * タグを追加した操作を時系列で記録し、同一タグの重複は最新位置に集約する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class TagHistory {

    /** 最大保持件数。 */
    private static final int MAX_SIZE = 20;

    /** 履歴リスト（新しいものが先頭）。 */
    private final LinkedList<String> history = new LinkedList<>();

    /**
     * タグ名を履歴に追加する（先頭に挿入、既存は削除して再挿入）。
     *
     * @param tagName タグ名
     */
    public void add(String tagName) {
        if (tagName == null || tagName.isEmpty()) return;
        history.remove(tagName);
        history.addFirst(tagName);
        if (history.size() > MAX_SIZE) {
            history.removeLast();
        }
    }

    /**
     * 最近使用したタグを最大N件返す。
     *
     * @param limit 取得件数上限
     * @return タグ名リスト（新しい順）
     */
    public List<String> getRecent(int limit) {
        return history.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * CSV文字列から履歴を読み込む。
     *
     * @param csv カンマ区切りのタグ名文字列
     */
    public void loadFrom(String csv) {
        history.clear();
        if (csv != null && !csv.isEmpty()) {
            for (String s : csv.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    history.add(trimmed);
                }
            }
        }
    }

    /**
     * 履歴をCSV文字列に変換する。
     *
     * @return カンマ区切りのタグ名文字列
     */
    public String toCsv() {
        return String.join(",", history);
    }
}
