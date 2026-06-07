package com.example.pixtagarc.service;

import com.example.pixtagarc.config.VlcConfig;
import com.example.pixtagarc.exception.VlcNotInstalledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 動画再生サービスクラス。
 *
 * <p>VLCJをリフレクション経由で使用して動画ファイルの再生を管理する。
 * VLCJはGPL v3ライセンスのためcompileOnly依存であり、
 * 実行時にクラスパスに存在する場合のみ動画再生が有効になる。
 *
 * <p>すべてのVLCJ API呼び出しはリフレクション経由で行い、
 * コンパイル時のVLCJへの直接依存を排除している。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class VideoPlayerService {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(VideoPlayerService.class);

    /** MediaPlayerFactory インスタンス（リフレクション経由で保持）。 */
    private Object mediaPlayerFactory;

    /** EmbeddedMediaPlayer インスタンス（リフレクション経由で保持）。 */
    private Object mediaPlayer;

    /** 現在再生中のファイルパス。 */
    private String currentFilePath;

    /**
     * コンストラクタ。
     *
     * <p>VLCが利用可能な場合のみMediaPlayerFactoryをリフレクション経由で初期化する。
     *
     * @throws VlcNotInstalledException VLCがインストールされていない場合
     */
    public VideoPlayerService() {
        if (!VlcConfig.isVlcAvailable()) {
            throw new VlcNotInstalledException();
        }
        initializePlayer();
    }

    /**
     * MediaPlayerFactoryとEmbeddedMediaPlayerをリフレクション経由で初期化する。
     */
    private void initializePlayer() {
        log.info("VideoPlayerServiceを初期化します（リフレクション経由）");
        try {
            // MediaPlayerFactory を生成
            Class<?> factoryClass = Class.forName("uk.co.caprica.vlcj.factory.MediaPlayerFactory");
            mediaPlayerFactory = factoryClass.getDeclaredConstructor().newInstance();

            // factory.mediaPlayers().newEmbeddedMediaPlayer() を呼び出す
            Method mediaPlayersMethod = factoryClass.getMethod("mediaPlayers");
            Object mediaPlayersApi = mediaPlayersMethod.invoke(mediaPlayerFactory);

            Method newEmbeddedMethod = mediaPlayersApi.getClass().getMethod("newEmbeddedMediaPlayer");
            mediaPlayer = newEmbeddedMethod.invoke(mediaPlayersApi);

            log.info("VideoPlayerServiceの初期化が完了しました");
        } catch (Exception e) {
            log.error("VideoPlayerServiceの初期化に失敗しました", e);
            throw new RuntimeException("動画プレイヤーの初期化に失敗しました", e);
        }
    }

    /**
     * 指定されたファイルを再生する。
     *
     * @param filePath 再生するファイルのパス
     * @throws VlcNotInstalledException VLCが利用できない場合
     * @throws RuntimeException         再生に失敗した場合
     */
    public void play(String filePath) {
        if (mediaPlayer == null) {
            throw new VlcNotInstalledException();
        }
        log.info("動画を再生します: {}", filePath);
        currentFilePath = filePath;
        try {
            // mediaPlayer.media().play(filePath)
            Method mediaMethod = mediaPlayer.getClass().getMethod("media");
            Object mediaApi = mediaMethod.invoke(mediaPlayer);
            Method playMethod = mediaApi.getClass().getMethod("play", String.class, String[].class);
            playMethod.invoke(mediaApi, filePath, new String[0]);
        } catch (Exception e) {
            log.error("動画の再生に失敗しました: {}", filePath, e);
            throw new RuntimeException("動画の再生に失敗しました", e);
        }
    }

    /**
     * 再生を一時停止する。
     */
    public void pause() {
        if (mediaPlayer == null) return;
        try {
            // mediaPlayer.controls().pause()
            Object controls = getControls();
            if (controls != null && isPlaying()) {
                Method pauseMethod = controls.getClass().getMethod("pause");
                pauseMethod.invoke(controls);
                log.debug("動画を一時停止しました");
            }
        } catch (Exception e) {
            log.error("一時停止に失敗しました", e);
        }
    }

    /**
     * 再生を再開する。
     */
    public void resume() {
        if (mediaPlayer == null) return;
        try {
            // mediaPlayer.controls().play()
            Object controls = getControls();
            if (controls != null) {
                Method playMethod = controls.getClass().getMethod("play");
                playMethod.invoke(controls);
                log.debug("動画の再生を再開しました");
            }
        } catch (Exception e) {
            log.error("再生の再開に失敗しました", e);
        }
    }

    /**
     * 再生を停止する。
     */
    public void stop() {
        if (mediaPlayer == null) return;
        try {
            Object controls = getControls();
            if (controls != null) {
                Method stopMethod = controls.getClass().getMethod("stop");
                stopMethod.invoke(controls);
            }
            currentFilePath = null;
            log.debug("動画を停止しました");
        } catch (Exception e) {
            log.error("停止に失敗しました", e);
        }
    }

    /**
     * 指定された位置にシークする。
     *
     * @param position 再生位置（0.0〜1.0）
     */
    public void seek(float position) {
        if (mediaPlayer == null) return;
        try {
            Object controls = getControls();
            if (controls != null) {
                Method setPositionMethod = controls.getClass().getMethod("setPosition", float.class);
                setPositionMethod.invoke(controls, position);
                log.debug("シークしました: position={}", position);
            }
        } catch (Exception e) {
            log.error("シークに失敗しました", e);
        }
    }

    /**
     * 現在再生中かどうかを返す。
     *
     * @return 再生中の場合 {@code true}
     */
    public boolean isPlaying() {
        if (mediaPlayer == null) return false;
        try {
            // mediaPlayer.status().isPlaying()
            Method statusMethod = mediaPlayer.getClass().getMethod("status");
            Object status = statusMethod.invoke(mediaPlayer);
            Method isPlayingMethod = status.getClass().getMethod("isPlaying");
            return (boolean) isPlayingMethod.invoke(status);
        } catch (Exception e) {
            log.debug("再生状態の取得に失敗しました", e);
            return false;
        }
    }

    /**
     * 内部のMediaPlayerオブジェクトを返す。
     *
     * <p>JavaFXのCanvasへの埋め込みに使用する。
     * 呼び出し元でVLCJの型にキャストする必要がある。
     *
     * @return MediaPlayerオブジェクト（Object型）
     */
    public Object getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * 現在再生中のファイルパスを返す。
     *
     * @return 現在のファイルパス（再生していない場合は {@code null}）
     */
    public String getCurrentFilePath() {
        return currentFilePath;
    }

    /**
     * リソースを解放する。
     *
     * <p>アプリケーション終了時またはビューアウィンドウを閉じる際に呼び出す。
     */
    public void release() {
        if (mediaPlayer != null) {
            try {
                // mediaPlayer.controls().stop() → mediaPlayer.release()
                Object controls = getControls();
                if (controls != null) {
                    Method stopMethod = controls.getClass().getMethod("stop");
                    stopMethod.invoke(controls);
                }
                Method releaseMethod = mediaPlayer.getClass().getMethod("release");
                releaseMethod.invoke(mediaPlayer);
                mediaPlayer = null;
                log.debug("EmbeddedMediaPlayerを解放しました");
            } catch (Exception e) {
                log.error("EmbeddedMediaPlayerの解放に失敗しました", e);
            }
        }
        if (mediaPlayerFactory != null) {
            try {
                Method releaseMethod = mediaPlayerFactory.getClass().getMethod("release");
                releaseMethod.invoke(mediaPlayerFactory);
                mediaPlayerFactory = null;
                log.debug("MediaPlayerFactoryを解放しました");
            } catch (Exception e) {
                log.error("MediaPlayerFactoryの解放に失敗しました", e);
            }
        }
    }

    /**
     * mediaPlayer.controls() をリフレクションで取得する。
     *
     * @return controlsオブジェクト（取得失敗時は {@code null}）
     */
    private Object getControls() {
        try {
            Method controlsMethod = mediaPlayer.getClass().getMethod("controls");
            return controlsMethod.invoke(mediaPlayer);
        } catch (Exception e) {
            log.error("controlsの取得に失敗しました", e);
            return null;
        }
    }
}
