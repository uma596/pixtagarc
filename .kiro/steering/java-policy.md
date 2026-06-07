# Java アプリケーション実装ポリシー

## 1. プロジェクト構成

### パッケージ構成
- ベースパッケージは `com.<組織名>.<アプリ名>` の形式に従う
- レイヤードアーキテクチャを基本とし、以下の構成を推奨する:
  ```
  com.example.app
  ├── controller    # プレゼンテーション層（REST API / UI）
  ├── service       # ビジネスロジック層
  ├── repository    # データアクセス層
  ├── domain        # ドメインモデル（Entity, VO）
  ├── dto           # データ転送オブジェクト
  ├── config        # 設定クラス
  └── exception     # 例外クラス
  ```

### ビルドツール
- Maven または Gradle を使用する
- 依存ライブラリのバージョンは必ず固定する（範囲指定禁止）

---

## 2. コメント規約

### JavaDoc コメント
- **すべてのクラス、インターフェース、列挙型**に JavaDoc コメントを付与する
- **すべての public / protected メソッド**に JavaDoc コメントを付与する
- **すべての public / protected フィールド**に JavaDoc コメントを付与する
- private メンバーにも可能な限り JavaDoc コメントを付与することを推奨する

#### クラスの JavaDoc
```java
/**
 * ユーザー情報を管理するサービスクラス。
 *
 * <p>ユーザーの登録・更新・削除・検索などのビジネスロジックを提供する。
 *
 * @author 作成者名
 * @since 1.0.0
 */
public class UserService {
```

#### メソッドの JavaDoc
```java
/**
 * 指定された ID のユーザーを取得する。
 *
 * @param userId 検索対象のユーザー ID（null 不可）
 * @return 該当するユーザー情報
 * @throws UserNotFoundException 指定された ID のユーザーが存在しない場合
 * @throws IllegalArgumentException userId が null の場合
 */
public User findUserById(Long userId) {
```

#### フィールドの JavaDoc
```java
/**
 * ユーザー情報を永続化するリポジトリ。
 */
private final UserRepository userRepository;

/**
 * パスワードのハッシュ化に使用するエンコーダー。
 */
private final PasswordEncoder passwordEncoder;
```

### 処理内コメント（インラインコメント）
- 処理内のコメントは「**何をしているか**」ではなく「**なぜそうしているか**」を説明する
- コードを読めば分かる自明な処理にコメントは不要
- 複雑なロジック、業務ルール、ワークアラウンドには必ずコメントを付ける
- TODO / FIXME コメントには担当者と日付を記載する

```java
// NG: コードの繰り返しに過ぎない
// i をインクリメントする
i++;

// NG: 自明な処理
// リストが空かチェック
if (list.isEmpty()) {

// OK: 理由を説明している
// 外部APIの仕様上、同一リクエストを1秒以内に連続送信すると429エラーになるため待機する
Thread.sleep(1000);

// OK: 業務ルールを説明している
// 消費税率は2019年10月以降の取引に対して10%を適用する（軽減税率対象品目を除く）
BigDecimal taxRate = transactionDate.isAfter(TAX_RATE_CHANGE_DATE) ? TAX_RATE_10 : TAX_RATE_8;

// OK: TODO に担当者と日付を記載
// TODO: [山田] 2026-06-01 までに外部API連携に切り替える（現在はモック実装）
```

### コメントの禁止事項
- コメントアウトしたコードをコミットに含めない（バージョン管理で履歴を追う）
- 自動生成されたままの意味のないコメント（`// TODO Auto-generated method stub` 等）を残さない
- 古くなって実態と乖離したコメントを放置しない（コード変更時にコメントも更新する）

---

## 3. コーディング規約

### 命名規則
| 対象 | 規則 | 例 |
|------|------|----|
| クラス | UpperCamelCase | `UserService` |
| メソッド | lowerCamelCase | `findUserById` |
| 変数 | lowerCamelCase | `userName` |
| 定数 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| パッケージ | 全小文字 | `com.example.app` |

### クラス設計
- 1クラス1責務の原則（Single Responsibility Principle）を守る
- クラスの行数は原則 300 行以内に収める
- メソッドの行数は原則 30 行以内に収める
- `public` フィールドは禁止。必ずアクセサメソッドを使用する

### インターフェースと抽象クラス
- ビジネスロジックはインターフェースで抽象化し、実装クラスを分離する
- 共通処理は抽象クラスに集約する

---

## 4. 例外処理

- チェック例外は呼び出し元で適切にハンドリングする
- 非チェック例外（RuntimeException）は業務エラーに使用する
- 例外をキャッチして何もしない（空の catch ブロック）は禁止
- ログ出力後に再スローするか、適切な例外に変換する
- カスタム例外クラスは `exception` パッケージに配置し、命名は `XxxException` とする

```java
// NG: 例外を握りつぶす
try {
    process();
} catch (Exception e) {
    // 何もしない
}

// OK: ログ出力して再スロー
try {
    process();
} catch (Exception e) {
    log.error("処理に失敗しました", e);
    throw new ApplicationException("処理に失敗しました", e);
}
```

---

## 5. ログ出力

- ロギングフレームワークは **SLF4J + Logback** を使用する
- `System.out.println` によるログ出力は禁止
- ログレベルの使い分け:
  | レベル | 用途 |
  |--------|------|
  | ERROR | システムエラー、業務継続不可能な異常 |
  | WARN  | 業務上の警告、リトライ可能なエラー |
  | INFO  | 業務上の重要なイベント（開始・終了など） |
  | DEBUG | デバッグ情報（本番環境では無効化） |
- ログメッセージに個人情報・パスワード等の機密情報を含めない

```java
// NG
System.out.println("ユーザーID: " + userId);

// OK
log.info("ユーザー処理開始: userId={}", userId);
```

---

## 6. セキュリティ

- SQL は必ずパラメータ化クエリ（PreparedStatement）を使用し、SQLインジェクションを防ぐ
- ユーザー入力値は必ずバリデーションを行う
- パスワードは平文で保存・ログ出力しない（BCrypt 等でハッシュ化）
- 機密情報（APIキー、パスワード）はソースコードにハードコードしない。環境変数または設定ファイルで管理する
- 外部からの入力を使ってファイルパスを構築する場合はパストラバーサル対策を行う

---

## 7. 非同期・並行処理

- スレッドは直接生成せず、`ExecutorService` または Spring の `@Async` を使用する
- 共有リソースへのアクセスは適切に同期化する（`synchronized`、`ReentrantLock`、`Atomic` クラス等）
- デッドロックを避けるため、ロックの取得順序を統一する
- スレッドセーフでないコレクション（`ArrayList`、`HashMap` 等）をマルチスレッド環境で共有しない

---

## 8. テスト

- 単体テストは **JUnit 5** を使用する
- モックは **Mockito** を使用する
- テストクラスの命名は `<対象クラス名>Test` とする
- テストメソッドの命名は `<メソッド名>_<条件>_<期待結果>` の形式を推奨
- サービス層・ユーティリティクラスは必ず単体テストを作成する
- テストカバレッジは主要ビジネスロジックで 80% 以上を目標とする

```java
@Test
void findUserById_存在するIDの場合_ユーザーを返す() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    User result = userService.findUserById(1L);

    // Assert
    assertThat(result).isEqualTo(testUser);
}
```

---

## 9. コードレビュー基準

- PR（プルリクエスト）は 400 行以内を目安とする
- レビュー前にセルフレビューを実施する
- 以下の観点でレビューを行う:
  - 本ポリシーへの準拠
  - ロジックの正確性
  - 例外・エラーハンドリングの適切さ
  - セキュリティリスクの有無
  - テストの網羅性

---

## 10. 依存ライブラリ管理

- 使用するライブラリは事前にチームで承認を得る
- 脆弱性が報告されたライブラリは速やかにアップデートする
- 不要な依存は定期的に削除する
- ライセンスを確認し、商用利用に問題のないものを選定する
