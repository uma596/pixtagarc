package com.example.pixtagarc.service;

import com.example.pixtagarc.config.AppConfig;
import com.example.pixtagarc.dto.ViewerSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * ビューアセッションの管理クラス（シングルトン）。
 *
 * <p>開いているビューアウィンドウのセッション情報を管理し、
 * settings.properties に JSON 配列として即時保存する。
 * 次回起動時にセッションを復元してビューアを再起動する。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class ViewerSessionManager {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(ViewerSessionManager.class);

    /** 設定キー。 */
    public static final String KEY_VIEWER_SESSIONS = "viewer.sessions";

    /** Gsonインスタンス。 */
    private static final Gson GSON = new Gson();

    /** シングルトンインスタンス。 */
    private static final ViewerSessionManager INSTANCE = new ViewerSessionManager();

    /** セッションリスト。 */
    private final List<ViewerSession> sessions = new ArrayList<>();

    /**
     * シングルトンインスタンスを返す。
     *
     * @return ViewerSessionManager インスタンス
     */
    public static ViewerSessionManager getInstance() {
        return INSTANCE;
    }

    private ViewerSessionManager() {
        load();
    }

    /**
     * セッションを追加して即時保存する。
     *
     * @param session 追加するセッション
     */
    public void add(ViewerSession session) {
        sessions.add(session);
        save();
        log.debug("ビューアセッションを追加しました: mode={}, total={}", session.getMode(), sessions.size());
    }

    /**
     * セッションを削除して即時保存する。
     *
     * @param session 削除するセッション
     */
    public void remove(ViewerSession session) {
        sessions.remove(session);
        save();
        log.debug("ビューアセッションを削除しました: total={}", sessions.size());
    }

    /**
     * 全セッションを返す。
     *
     * @return セッションリストのコピー
     */
    public List<ViewerSession> getAll() {
        return new ArrayList<>(sessions);
    }

    /**
     * 全セッションをクリアして保存する（起動時の復元完了後に呼び出す）。
     */
    public void clearAll() {
        sessions.clear();
        save();
    }

    /**
     * settings.properties に保存する。
     */
    public void save() {
        try {
            String json = GSON.toJson(sessions);
            AppConfig.getInstance().setState(KEY_VIEWER_SESSIONS, json);
        } catch (Exception e) {
            log.warn("ビューアセッションの保存に失敗しました", e);
        }
    }

    /**
     * settings.properties から読み込む。
     */
    private void load() {
        try {
            String json = AppConfig.getInstance().getState(KEY_VIEWER_SESSIONS, "[]");
            Type listType = new TypeToken<List<ViewerSession>>() {}.getType();
            List<ViewerSession> loaded = GSON.fromJson(json, listType);
            if (loaded != null) {
                sessions.clear();
                sessions.addAll(loaded);
            }
            log.info("ビューアセッションを読み込みました: {}件", sessions.size());
        } catch (Exception e) {
            log.warn("ビューアセッションの読み込みに失敗しました（無視します）", e);
        }
    }
}
