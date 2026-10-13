# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## このリポジトリ

**推し活カウントダウン**（推し活の予定まであと何日かを数えるアプリ）の Android 版。
機能と画面の仕様は `docs/SPEC.md`、画面デザインの正は SPEC の冒頭にあるデザインキャンバス。設計は `docs/architecture.md`。

## Build Commands

```shell
# ビルド
./gradlew assembleDebug

# テスト（全モジュール）
./gradlew test

# 1 クラスだけ
./gradlew :domain:test --tests "com.basekeita.oshikatsucountdown.domain.countdown.CalculateCountdownUseCaseTest"

# 静的チェック
./gradlew ktlintCheck lintDebug
```

Gradle デーモンは `gradle/gradle-daemon-jvm.properties` の toolchain（25）で動く。

## 行動規約

- **タスク開始前に必ず `CLAUDE.md` と `docs/architecture.md` を読むこと**
- **ユーザーから質問された場合は、まず質問に答えること。** 回答によってその後の行動が変わる可能性があるため、作業が必要かを確認してから動く
- **推論ベースで実装・変更・削除を行うことを禁止する。** コード・ドキュメントを変更する前に該当ファイルを読んで現状を確認する。不明な点はユーザーに確認する
- **着手前に変更規模を見積もり、完遂できないと判断したら着手しない。** 完遂できる単位にスコープを縮小してから着手する
- **変数名・関数名は意味のある名前を付けること。** 1 文字変数で名前付けを回避することを禁止する
- **新しい定義を作る前に、同等のものがプロジェクト内に既に存在しないか Grep で検索すること**
- デザインの部品の寸法・形（iOS のガラス風のボタン・シートなど）は写さず、M3 の部品を使う。引き継ぐのは機能・文言・配色だけ

## 依存追加の規約

新しいライブラリは必ず `gradle/libs.versions.toml` にバージョンとエントリを追加し、`build.gradle.kts` では `libs.*` 経由で参照する。

- **Kotlin は AGP 9.4.1 内蔵の 2.2.10。** Kotlin 2.2 のコンパイラが読めないライブラリ（POM の `kotlin-stdlib` が 2.3 以上）は入れない。
  追加・更新の前に POM の `kotlin-stdlib` を確認する
- 新しい API を使う際は `@Experimental` の有無を確認し、必要な `@OptIn` を付ける
- Deprecated の警告が出たら、置き換え先を推測しない。ソース（Gradle キャッシュ）で `@Deprecated` の `replaceWith` を読んでから直す

## 設計規約

> 詳細は `docs/architecture.md`。

### モジュールと依存方向

依存は一方向のみ。逆方向・同層跨ぎの参照は禁止。

```
data:definition  ←  data:impl, domain, feature/*, app
domain           ←  feature/*, app
local            ←  data:impl
navigation       ←  feature/*, app
ui               ←  feature/*, app
data:impl        ←  app
feature/*        ←  app
```

- `data:definition` はドメインモデルと Repository の interface、`data:impl` は Repository の実装、`domain` は UseCase と計算の関数
- feature は `data:impl` / `local` / 他の feature に依存しない。Repository は `data:definition` の interface で注入し、実装は Hilt で `app` に集める
- 計算や複数の手順を含む処理は UseCase を呼ぶ。保存と読み込みだけなら ViewModel から Repository を直接呼ぶ
- feature 間の遷移は `XxxNavigationEvent` を `app` に返してつなぐ
- `build.gradle.kts` は convention plugin（`build-logic`）を使う: `oshikatsu.common` / `oshikatsu.application` / `oshikatsu.library` / `oshikatsu.hilt` / `oshikatsu.feature`
- 「今」の時刻は Hilt で渡す `Clock` から ViewModel が読み、domain には引数で渡す

### Material 3 に従う

- UI は Material 3 のガイドラインと標準コンポーネントに従う
- 押せる要素は 48×48dp 以上を確保する
- トップレベルのナビゲーションは `NavigationSuiteScaffold` で、ウィンドウの大きさに応じてナビゲーションバー / レールを M3 の基準で切り替える
- 推し色を地の上の文字に使うときは `OshiColorUiModel.onSurfaceColor()` を通す（ダークの地で読めるようにする）

### 禁止事項

- **DataSource（local）は Repository を参照してはいけない**
- **Repository は他の Repository に依存してはいけない**
- **ドメインモデルを UI 層（ViewModel の公開状態・Screen）で直接使ってはいけない**（UI 専用モデル `XxxUiModel` を定義してマッピングする）
- **UiState に isLoading や errorMessage を混在させてはいけない**（ViewState / UiState 分離パターンを使う）
- **保存・削除を始めてすぐに画面を閉じてはいけない**（終わってから閉じる。先に閉じると `viewModelScope` ごと書き込みが止まる）
- 下位層に値が必要な場合は、上位層からメソッド引数として渡す
- コードの配置はモジュールの責務に従う。一箇所でしか使われないコードを利便性を理由に共有モジュールに置かない

### ViewState / UiState の分離（全 ViewModel で必須）

```kotlin
sealed interface ViewState {
    data object Loading : ViewState
    data class Error(val message: String) : ViewState
    data class Success(val uiState: UiState) : ViewState
}

data class UiState(/* UI 専用モデルのみ */)
```

- `Loading` は単純に「ローディング中」。呼び出し経緯（初回・再ロード）で振る舞いを変えない
- UiState / ViewState で管理できる状態を別の StateFlow で新設しない

### ViewModel の onIntent を Composable にバケツリレーしない（厳守）

- 子 Composable のパラメータに `onIntent: (XxxIntent) -> Unit` や `viewModel: XxxViewModel` を持たせてはいけない
- ViewModel を保持する Composable は最上位の 1 つ（`XxxScreen` の Route）だけ。子には `onSaveClick: () -> Unit` のような意味のあるコールバックを個別に渡す

### 条件分岐のコーディング規約

- **`when { bool }` 形式の条件分岐を禁止する。** `if / else if / else` に `{}` を付けて書く
- **`else` 節がない単行ガード `if (cond) return` は許可する**
- **`else` 節がある `if/else` は必ず `{}` と改行で展開する。** `when` アームの中でも同様
- **ガード節を複数並べるとき、必ず空行で区切る**
