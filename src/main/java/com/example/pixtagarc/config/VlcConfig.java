package com.example.pixtagarc.config;

import com.example.pixtagarc.exception.VlcNotInstalledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * VLC設定クラス。
 *
 * <p>VLCJライブラリをリフレクションで動的に検出・初期化する。
 * VLCJはGPL v3ライセンスのため、compileOnly依存として配布物に含めない。
 * ユーザーがVLCをインストールしクラスパスにVLCJが存在する場合のみ動画再生が有効化される。
 *
 * <p>VLCが見つからない場合は {@link VlcNotInstalledException} をスローする。
 *
 * @author pixtagarc
 * @since 1.0.0
 */
public class VlcConfig {

    /** ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(VlcConfig.class);

    /** VLCが利用可能かどうかのフラグ。 */
    private static volatile boolean vlcAvailable = false;

    /** VLC初期化済みフラグ。 */
    private static volatile boolean initialized = false;

    /** VLCJライブラリがクラスパスに存在するかどうか。 */
    private static volatile boolean vlcjOnClasspath = false;

    /**
     * プライベートコンストラクタ（インスタンス化禁止）。
     */
    private VlcConfig() {
    }

    /**
     * VLCJライブラリの存在を確認し、VLCネイティブライブラリを検出・初期化する。
     *
     * <p>リフレクションでVLCJの {@code NativeDiscovery} クラスを検索し、
     * 存在する場合のみVLCの検出を試みる。VLCJがクラスパスにない場合や
     * VLCがインストールされていない場合は {@link VlcNotInstalledException} をスローする。
     *
     * @throws VlcNotInstalledException VLCJが見つからない、またはVLCがインストールされていない場合
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        synchronized (VlcConfig.class) {
            if (initialized) {
                return;
            }
            log.info("VLCJライブラリの存在を確認しています...");
            try {
                // VLCJがクラスパスに存在するか確認
                Class<?> discoveryClass = Class.forName("uk.co.caprica.vlcj.factory.discovery.NativeDiscovery");
                vlcjOnClasspath = true;
                log.info("VLCJライブラリがクラスパスに見つかりました");

                // NativeDiscovery#discover() をリフレクションで呼び出す
                Object discovery = discoveryClass.getDeclaredConstructor().newInstance();
                Method discoverMethod = discoveryClass.getMethod("discover");
                boolean found = (boolean) discoverMethod.invoke(discovery);

                if (found) {
                    vlcAvailable = true;
                    log.info("VLCネイティブライブラリが見つかりました");
                } else {
                    vlcAvailable = false;
                    log.warn("VLCネイティブライブラリが見つかりませんでした。動画再生機能は無効化されます。");
                    throw new VlcNotInstalledException();
                }
            } catch (ClassNotFoundException e) {
                // VLCJがクラスパスにない場合（compileOnlyのため通常はこのケース）
                vlcjOnClasspath = false;
                vlcAvailable = false;
                log.info("VLCJライブラリがクラスパスにありません。動画再生機能は無効化されます。");
                throw new VlcNotInstalledException(
                        "VLCJライブラリが見つかりません（オプション依存）", e);
            } catch (VlcNotInstalledException e) {
                throw e;
            } catch (Exception e) {
                vlcAvailable = false;
                log.error("VLCライブラリの初期化中にエラーが発生しました", e);
                throw new VlcNotInstalledException(
                        "VLCライブラリの初期化に失敗しました: " + e.getMessage(), e);
            } finally {
                initialized = true;
            }
        }
    }

    /**
     * VLCが利用可能かどうかを返す。
     *
     * <p>{@link #initialize()} を呼び出す前は常に {@code false} を返す。
     *
     * @return VLCが利用可能な場合 {@code true}
     */
    public static boolean isVlcAvailable() {
        return vlcAvailable;
    }

    /**
     * VLC初期化済みかどうかを返す。
     *
     * @return 初期化済みの場合 {@code true}
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * VLCJライブラリがクラスパスに存在するかを返す。
     *
     * @return VLCJが存在する場合 {@code true}
     */
    public static boolean isVlcjOnClasspath() {
        return vlcjOnClasspath;
    }
}
