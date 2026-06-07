# Git 運用ポリシー

## 1. ブランチ戦略

### ブランチ構成
- `main` : リリース済みの安定コード。直接コミット禁止
- `develop` : 開発統合ブランチ。機能ブランチのマージ先。直接コミット禁止
- `feature/<issue番号>-<概要>` : 機能追加・改修（`develop` から分岐）
- `fix/<issue番号>-<概要>` : バグ修正（`develop` から分岐）
- `kiro/<親ブランチ名>` : Kiro による自動作業ブランチ（作業ブランチから分岐）
- `hotfix/<issue番号>-<概要>` : 本番緊急修正（`main` から分岐）
- `release/<バージョン>` : リリース準備（`develop` から分岐）

```
main
 └── hotfix/42-fix-crash-on-startup
develop
 ├── feature/10-add-download-queue
 │    └── kiro/feature/10-add-download-queue   ← Kiro作業ブランチ
 └── fix/15-fix-progress-bar
      └── kiro/fix/15-fix-progress-bar         ← Kiro作業ブランチ
```

### ブランチフローとマージ方法

```
kiro/* ──(squash merge)──→ feature/* or fix/*
feature/* or fix/* ──(squash merge)──→ develop
develop ──(merge commit)──→ main（リリース時）
```

| マージ元 | マージ先 | マージ方法 | 理由 |
|---------|---------|-----------|------|
| `kiro/*` | 作業ブランチ（`feature/*`, `fix/*`） | **Squash and Merge** | Kiroの細かいコミットを1つに集約 |
| 作業ブランチ | `develop` | **Squash and Merge** | 機能単位で履歴を整理 |
| `develop` | `main` | **Merge Commit** | リリース履歴を明確に残す |
| `hotfix/*` | `main` | **Merge Commit** | 緊急修正の履歴を残す |

### Kiro 作業ブランチの運用ルール

1. Kiro が作業を開始する際、現在の作業ブランチから `kiro/<作業ブランチ名>` を作成する
2. Kiro はこのブランチで随時コミットする（WIPコミット可、こまめにコミットすること）
3. 作業完了後、作業ブランチに squash merge する
4. squash merge のコミットメッセージは Conventional Commits 形式に従う
5. マージ完了後、Kiro 作業ブランチは削除する

```
例:
  feature/10-add-download-queue から kiro/feature/10-add-download-queue を作成
  → Kiro が複数コミット実施
  → feature/10-add-download-queue に squash merge
  → kiro/feature/10-add-download-queue を削除
```

### ブランチ削除ルール

- **すべてのブランチ（`feature/*`, `fix/*`, `kiro/*`）は、ユーザーから明示的に削除を指示されるまで削除しない**
- ブランチの削除が必要と思われる場合でも、Kiro は削除せずユーザーに確認を取ること

### ブランチ命名規則
- 英小文字・数字・ハイフン・スラッシュのみ使用する
- スペース・アンダースコア・日本語は使用しない
- 概要部分は動詞から始める（`add-`, `fix-`, `update-`, `remove-` など）

---

## 2. コミット規約

### コミットメッセージ形式
[Conventional Commits](https://www.conventionalcommits.org/) に準拠する。

```
<type>(<scope>): <概要>

[本文（任意）]

[フッター（任意）: BREAKING CHANGE, Closes #<issue番号>]
```

### type 一覧
| type | 用途 |
|------|------|
| `feat` | 新機能追加 |
| `fix` | バグ修正 |
| `docs` | ドキュメントのみの変更 |
| `style` | コードの意味に影響しない変更（フォーマット等） |
| `refactor` | バグ修正・機能追加を伴わないリファクタリング |
| `test` | テストの追加・修正 |
| `chore` | ビルドプロセス・補助ツールの変更 |
| `perf` | パフォーマンス改善 |
| `ci` | CI/CD 設定の変更 |

### コミットメッセージ例
```
feat(download): ダウンロードキューの並列実行数を設定可能にする

設定ファイル（config.properties）に max-concurrent-downloads キーを追加。
デフォルト値は 3。

Closes #10
```

```
fix(ui): 進捗バーがダウンロード完了後にリセットされない問題を修正

DownloadTask の状態遷移で COMPLETED 時に progress を 100 に固定するよう変更。

Closes #15
```

### コミットの粒度
- 1コミット1変更の原則を守る（複数の独立した変更を混在させない）
- ビルドが通る状態でコミットする（壊れた状態でコミットしない）
- `WIP` コミットは作業ブランチ内に留め、マージ前に `rebase -i` で整理する

---

## 3. プルリクエスト（PR）

### PR 作成ルール
- `main` および `develop` への直接プッシュは禁止
- 必ずブランチを作成し、PR 経由でマージする
- PR のタイトルはコミットメッセージの概要と同じ形式にする
- PR の本文には以下を記載する:

```markdown
## 概要
<!-- 変更内容を簡潔に説明する -->

## 変更内容
- 変更点1
- 変更点2

## テスト確認
- [ ] 単体テストを追加・更新した
- [ ] ローカルでビルドが通ることを確認した
- [ ] 動作確認を実施した

## 関連 Issue
Closes #<issue番号>
```

### PR のサイズ
- 差分は原則 400 行以内（java-policy.md のコードレビュー基準に準拠）
- 大きな変更は複数の PR に分割する

### マージ方法
- `kiro/*` から作業ブランチへのマージ: **Squash and Merge**（Kiroの細かいコミットを集約）
- 作業ブランチから `develop` へのマージ: **Squash and Merge**（機能単位で履歴を整理）
- `develop` から `main` へのマージ: **Merge Commit**（リリース履歴を明確に残す）
- `rebase merge` は履歴改変リスクがあるため原則禁止

---

## 4. タグ・バージョン管理

### バージョン番号
[Semantic Versioning](https://semver.org/lang/ja/) に従う。

```
MAJOR.MINOR.PATCH
例: 1.2.3
```

| 番号 | 更新タイミング |
|------|--------------|
| MAJOR | 後方互換性のない変更 |
| MINOR | 後方互換性のある機能追加 |
| PATCH | 後方互換性のあるバグ修正 |

### タグ付け
- リリース時は `main` ブランチに `v<バージョン>` 形式でタグを付ける
- タグには必ずリリースノートを添付する

```bash
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

---

## 5. .gitignore

以下のファイル・ディレクトリは必ず `.gitignore` に含める:

```
# ビルド成果物
target/
*.class
*.jar
*.war

# IDE 設定
.idea/
*.iml
.vscode/
.DS_Store

# 機密情報・環境依存設定
*.env
application-local.properties
application-local.yml
config/local/

# ログ
*.log
logs/

# OS 生成ファイル
Thumbs.db
```

- 機密情報（APIキー、パスワード）を含むファイルは絶対にコミットしない
- 誤ってコミットした場合は `git filter-branch` または `git filter-repo` で履歴から削除し、認証情報を即座に無効化する

---

## 6. 禁止事項

- `main` / `develop` への直接 `git push` 禁止（必ず PR 経由）
- `git push --force` 禁止（共有ブランチへの強制プッシュ）
  - 自分のみが使う作業ブランチ・Kiro作業ブランチへの `--force-with-lease` は許容
- リモートへの push はユーザーから明示的に指示された場合のみ行う。push が必要な場面ではユーザーに確認を取ること
- 機密情報・バイナリファイル（動画・画像等）の大容量ファイルのコミット禁止
- コミットメッセージの空文字・意味のない内容禁止（`fix`、`test`、`aaa` 等）

---

## 7. 緊急修正（Hotfix）フロー

```
1. main から hotfix/<issue番号>-<概要> ブランチを作成
2. 修正をコミット
3. main へ PR を作成・マージ（Merge Commit）
4. main にバージョンタグを付ける
5. develop へも同じ修正をバックポート（cherry-pick または PR）
```
