# 推し活カウントダウン Android 版 アーキテクチャ仕様書

## 概要

推し活の予定までの残り日数を数えるアプリ「推し活カウントダウン」の Android 版。機能と画面の仕様は [`SPEC.md`](SPEC.md)。

- Android 推奨アーキテクチャ（UI / Domain / Data 層、UDF、`StateFlow`）で組む

| 項目 | 内容 |
|---|---|
| UI | Jetpack Compose（Material3） |
| ツールチェーン | AGP 9.4.1（Kotlin 2.2.10 内蔵）/ Gradle 9.6.0 / JVM 17 |
| SDK | minSdk 30 / targetSdk 37 / compileSdk 37 |
| applicationId | `com.basekeita.oshikatsucountdown` |
| DI | Hilt（画面の引数を受ける ViewModel は assisted injection） |
| 画面遷移 | Navigation 3 |
| ローカル保存 | 推し・予定は Room、設定は DataStore（Preferences）、写真はアプリの内部ストレージ |
| 時刻 | `java.time`。「今」は Hilt で渡す `Clock` から読む |
| ホーム画面ウィジェット | Jetpack Glance（`glance-appwidget`） |
| 予定の通知・当日のカウントダウン | AlarmManager（正確なアラーム `setExactAndAllowWhileIdle`）と NotificationCompat |
| ライセンスの一覧 | AboutLibraries（ビルド時に app の依存から一覧を作る Gradle プラグインと、表示の `aboutlibraries-compose-m3`） |

---

## 画面構成

| タブ | 最初の画面 | そこから積む画面 |
|---|---|---|
| ホーム | `feature/home` | 予定の詳細（`feature/event-detail`）、予定の追加・編集（`feature/event-editor`） |
| 推し | `feature/oshi`（一覧） | 推しの登録・編集（`feature/oshi`） |
| 設定 | `feature/settings` | オープンソースライセンスの一覧（`feature/settings`） |

- タブは `NavigationSuiteScaffold` で、ウィンドウの大きさに応じて M3 の基準でナビゲーションバーとレールを切り替える。
  タブのバックスタックに画面を積んでいる間は `NavigationSuiteType.None` でナビゲーションを隠す
- ウィジェットから開く画面（予定の詳細・予定の追加）は、`MainActivity` の Intent の extra（`ExternalLaunchExtras`）で受け、
  ホームのタブを最初の画面まで戻してから積む。初回の導線の途中なら捨てる
- 初回の導線（設定の `onboardingCompleted` が false の間）はタブの代わりに別の `NavDisplay` で出す。
  推しの登録（`OshiEditorRoute(onboarding = true)`）を終えたら、バックスタックの 1 歩目を予定の追加（`EventEditorRoute(onboarding = true)`）に置き換える
- 起動画面（`core-splashscreen`）は `MainViewModel` が導線を出すかどうかを読み終えるまで出しておく

### UI の作り方（M3）

- 標準部品: タブ（`NavigationSuiteScaffold`）、見出し（`TopAppBar`）、行（`ListItem`）、絞り込み・選択（`FilterChip`）、
  追加（`ExtendedFloatingActionButton`）、入力（`OutlinedTextField`）、日付・時刻（`DatePickerDialog` / `TimePickerDialog`）、
  確認（`AlertDialog`）、メニュー（`DropdownMenu`）、切り替え（`Switch`）、進み具合（`LinearProgressIndicator`）、写真の選択（Photo Picker）
- 自作（プロダクトの見た目）: 先頭の予定のカード、残り日数の数字、推しのアバター、写真が無いときの推し色の縞、推し色の見本
- 押せる要素は 48×48dp 以上
- アイコンは Material Symbols（Outlined）の vector drawable（`ui/src/main/res/drawable`）。
  公式リポジトリ（google/material-design-icons の `symbols/android`）から取り、`android:tint` を外して置く。色は `Icon` の tint で付ける

---

## モジュール構成

```
build-logic/convention/   convention plugin（common / application / library / hilt / feature）
app/                      エントリポイント（@HiltAndroidApp, MainActivity, MainViewModel）、初回の導線、3 タブと NavDisplay、Clock の提供
feature/
  home/                   ホーム
  event-detail/           予定の詳細
  event-editor/           予定の追加・編集
  oshi/                   推しの一覧と登録・編集
  settings/               設定
  widget/                 ホーム画面ウィジェット（Glance）と、1 件のウィジェットに出す予定を選ぶ画面
  reminder/               予定の通知と当日のカウントダウン（予約・通知を出す・置き直すきっかけ）。画面は持たない
ui/                       共通 UI（テーマ・トークン、推し色、アバター・写真の部品、写真の読み込み、日付の書き方、UI 専用モデル）
navigation/               ルート定義（@Serializable の NavKey）、ウィジェットから開くときの Intent の extra
domain/                   UseCase と計算の関数（カウントダウン、推しの保存・削除、通知の計画）
data/
  definition/             ドメインモデル、Repository の interface
  impl/                   Repository の実装、Hilt の @Binds
local/                    Room（推し・予定）と DataStore（設定）、写真の取り込み先、Hilt の @Provides
```

### モジュール依存関係

```
data:definition  ←  data:impl, domain, feature/*, app
domain           ←  feature/*, app
local            ←  data:impl
navigation       ←  feature/*, app
ui               ←  feature/*, app
data:impl        ←  app
feature/*        ←  app
```

- 依存は一方向のみ。逆方向・同層跨ぎの参照は禁止
- feature は `data:impl` / `local` / 他の feature に依存しない。Repository は `data:definition` の interface で注入する
- ui は `data:definition` に依存しない。推し色・予定の種類は ui の `OshiColorUiModel` / `EventTypeUiModel` を使い、ドメインモデルとの対応は各 feature の ViewModel に書く
- feature 間の遷移は `XxxNavigationEvent` を `app` に返してつなぐ

### UseCase と Repository の使い分け

- 計算や複数の手順を含む処理は domain の UseCase を呼ぶ（予定の並び、カウントダウン、写真の差し替えを含む推しの保存、予定と写真を含む推しの削除）
- 保存と読み込みだけなら ViewModel から Repository を直接呼ぶ（予定の保存・削除・ピン留め、設定）

### convention plugin

| plugin | 中身 | 使うモジュール |
|---|---|---|
| `oshikatsu.common` | Kotlin のモジュールなら種類を問わず共通の設定（JVM 17、ktlint、JUnit） | domain、data:definition（JVM）が直接使う。他は library / application を通して受け取る |
| `oshikatsu.application` | common + `com.android.application`、SDK、applicationId、versionCode、Compose | app |
| `oshikatsu.library` | common + `com.android.library`、SDK、テストランナー | ui, data:impl, local, navigation |
| `oshikatsu.hilt` | Hilt プラグイン、KSP、`hilt-android`、`hilt-compiler` | app, data:impl, local |
| `oshikatsu.feature` | library + hilt + Compose と、どの feature も持つ依存 | feature × 7 |

SDK とバイトコードのバージョンは `build-logic/.../ProjectExtensions.kt` の `OshikatsuBuildConfig` だけに置く。

### バージョンの制約

AGP 9.4.1 は Kotlin 2.2.10 を内蔵する。Kotlin 2.2 のコンパイラは 1 つ先（2.3）までのメタデータしか読めないため、
POM の `kotlin-stdlib` が 2.3 以上を要求するライブラリは入れない（Hilt 2.59.2、kotlinx-serialization 1.9.0、AboutLibraries 13.1.0 に留める）。
Gradle プラグインは Gradle の Kotlin（2.3）の中で動くのでこの制約を受けない。AboutLibraries のプラグインは AGP 9 の新しい DSL で動く 15 系を使い、出力の JSON を 13.1.0 で読む。
Lint の「新しい版がある」の警告はこの制約によるもの。

依存と GitHub Actions の版上げは Dependabot が毎週 PR にする（`.github/dependabot.yml`）。
上の制約で止めている版と、AGP に合わせる Kotlin は `ignore` で除いている。それ以外の PR も、マージ前に POM の `kotlin-stdlib` を確かめる。

---

## 層の責務

### data:definition（JVM。依存は coroutines だけ）

- `model/`: `Oshi`、`OshiColor`、`OshiEvent`、`EventType`、`UserSettings`、`ReminderSettings`・`ReminderTiming`（通知の設定。アプリ全体で 1 つ）
  - 名前・タイトル・会場の正規化と上限はモデルに持たせる（`Oshi.normalizeName` / `MAX_NAME_LENGTH`、`OshiEvent.normalizeTitle` / `normalizeVenue`）
  - 文字数は見た目の 1 文字（書記素クラスタ）で数える（`takeCharacters`）
  - 推し・予定は登録した時刻（`createdAt`）を持つ。一覧の並び（登録した順）に使い、編集しても変えない
- `repository/`（interface）
  - `OshiRepository`: `oshis`、`save`（同じ id は置き換え）、`delete`（結び付いた予定も同じ書き込みで消す）
  - `EventRepository`: `events`、`save`、`delete`、`setPinned`（ピン留めは 1 件だけ）
  - `PhotoRepository`: `import(sourceUri)`（取り込んだファイルの絶対パス）、`delete(path)`
  - `UserSettingsRepository`: `settings`、`setShowPastEvents`、`setOnboardingCompleted`、通知の設定（`setRemindersEnabled`・`setReminderTimingEnabled`・`setReminderTimeOfDay`）、
    通知の許可を一度求めた印（`setNotificationPermissionRequested`）

### domain（JVM。依存は data:definition と `javax.inject` だけ）

- `countdown/`
  - `EventOccurrence`: `occurrenceOn(today)`（毎年くり返す予定の次の回）、`previousOccurrence`
  - `BuildEventScheduleUseCase`: 予定を今日から見た `EventSchedule`（先頭・これから・終わった）にする
  - `CalculateCountdownUseCase`: `now` の時点の残り・状態・経過の割合（`EventCountdown`）
- `oshi/`
  - `SaveOshiUseCase`: 写真を取り込んでから保存し、使わなくなった写真を消す（`PhotoChange` = Keep / Remove / Replace）。
    保存に失敗したら取り込んだ写真を消して `IOException` を投げる
  - `DeleteOshiUseCase`: 推し（結び付いた予定も同じ書き込みで消える）、写真の順に消す
- `reminder/`（予定の通知と当日のカウントダウン）
  - `PlanRemindersUseCase`: どの予定に、いつ通知を出すか（`PlannedReminder`）。規則は SPEC の「3. 規則」の通知の行。iOS 版の `ReminderPlan` を移したもの。
    近い順に 64 件（`MAXIMUM_COUNT`。iOS 版とそろえる）。識別子（`identifier`）は予定・回・タイミングで決まり、予約し直しても変わらない
  - `PlanLiveCountdownsUseCase`: 当日のカウントダウンをどの予定に、いつ出すか（`LiveCountdownPlan` の `current`・`scheduled`）。iOS 版の `LiveActivityPlan` を移したもの
- 時刻は引数で受ける（`today: LocalDate`、`now: Instant` と `zone: ZoneId`）。「今」を読むのは ViewModel。
  画面の無いところで動くもの（通知の予約の `ReminderScheduler`・`LiveCountdownScheduler`・`ReminderPoster`、ウィジェット）は ViewModel を持たないので、
  それぞれが `Provider<Clock>`（端末のタイムゾーンを読み直すため、呼ぶたびに作る）から読み、domain には引数で渡す

### local

- 推し・予定は Room に保存する。推しと予定はそれぞれのテーブルにし、予定は推しの id（`oshiId`）で推しに結び付ける。
  推し・予定は関係を持つ一覧で、1 件ずつの保存・削除と、推しを消したら結び付いた予定も消す整合が要るため
  （DataStore は小さなデータ向けで、部分的な更新と参照の整合を持たない。[DataStore](https://developer.android.com/topic/libraries/architecture/datastore)）
  - `database/`: `OshikatsuDatabase`（`oshikatsu.db`。テーブルの形は `local/schemas/` に書き出す）、`OshiDao` / `EventDao`。`database/model/` に `OshiEntity` / `EventEntity`、`database/util/` に列の型の `TypeConverter`（UUID・`LocalDate`・`LocalTime`・`Instant`）
  - 予定の `oshiId` は推しへの外部キー（`ON DELETE CASCADE`）。推しを消すと結び付いた予定も同じ書き込みで消える。結び付く推しが無い予定は保存できない
  - 一覧は登録した時刻（`createdAt`）の順に読む。時刻が同じ行どうしの並びを SQLite は決めないので、
    推しは名前・id、予定は id を 2 つ目以降の基準にして毎回同じ並びにする（予定の表示の並びは UseCase の安定ソートなので、この並びが残る）。保存は `@Upsert`（同じ id があれば行を書き換える）。
    行を消して入れ直す `REPLACE` は、推しなら結び付いた予定まで消すため使わない
  - ピン留めは 1 件だけ。ピン留めした予定の保存と、ほかの予定のピン留めを外すのは 1 つのトランザクションで行う
  - `OshiLocalDataSource` / `EventLocalDataSource` は DAO を受け取り、読み込みの `Flow` と書き込みを持つ。行は Entity で受け渡す。
    データベースの失敗（`SQLException`）は、読み込みなら空を流し、書き込みなら `IOException` にして投げる（ViewModel が受ける書き込みの失敗にそろえる）
- 設定（`showPastEvents` / `onboardingCompleted`）は DataStore（Preferences、`oshikatsu_prefs`）の Boolean のキーに置く（`UserSettingsLocalDataSource`）。
  通知の設定は `remindersEnabled`、タイミングごとの `reminderTiming.<キー>`、時刻は 0:00 からの分（`reminderMinutesOfDay`）、許可を求めた印は `notificationPermissionRequested`
- 保存の DataStore は local に置く。例外は通知の予約の記録（feature/reminder の `ReminderAlarmStore`、`reminder_alarms`）で、
  中身が AlarmManager の PendingIntent の識別子という予約の都合の値なので、local・data に出すとデータ層が AlarmManager のことを知ってしまう。そのため feature に閉じる
- `PhotoLocalDataSource`: Photo Picker の URI を `ImageDecoder` で読み、長辺 1440px 以下の JPEG にして `files/oshi_photos/` に書く。書き出しに失敗したら書きかけのファイルを消して `null`。消すのはその中のファイルだけ
- 推し・写真・予定は端末の外に出さない。自動バックアップは `allowBackup="false"` で切り、
  それだけでは止まらない機種がある Android 12 以降の端末間の移行も `data_extraction_rules.xml` で全部除く

### data:impl

- Repository の実装と `DataModule`（`@Binds`）
- Entity とドメインモデルの対応は `mapping/` にモデルごとに置く（`OshiMapping` / `EventMapping` の `toModel()` / `toEntity()`）。項目を詰め替えるだけ
  - 推し色・種類は、enum の名前ではなく data:impl の対応表（`storageKey`）で決めたキー（`pink`・`bday` など）で持つ。保存の形はデータ層が決め、ドメインの enum は保存のことを知らない。
    enum の名前を変えても保存の値は変わらない。キーを変えるときはデータベースの版を上げて移す。
    対応表に無いキーは既定（推し色はピンク、種類はその他）で読む。どのキーがあり得るかは読む版の対応表で決まり、移行を書き忘れたときや、値を足した版から前の版に戻したときにも入りうるため
  - 通知のタイミングも同じく data:impl の対応表（`ReminderMapping` の `storageKey`）のキーで持つ。書いていない項目は既定（ON・5 つすべて・9:00）、1 日の外の分は 9:00 で読む
  - 名前・タイトルは読むときに確かめ直さない。正規化と空のときの扱いは保存する前に済ませ、データベースにはこのアプリが保存した値しか入らないため

### ui（共通 UI）

- `theme/`: `OshikatsuDesignTokens`（ライト / ダークの `OshikatsuColorScheme`）、`OshikatsuTheme`（Material の部品の色もトークンで上書きする）、
  `OshikatsuTypography`（カウントダウンの数字）、`OshiAccent`（推し色を地の上の文字に使うときの色。ダークでは白に寄せる。外観を引数で受ける版はウィジェットが使う）
- `uimodel/`: `OshiColorUiModel`（12 色。白い文字に 4.5:1 以上）、`OshiUiModel`、`EventTypeUiModel`、`WriteFailureUiModel`（保存・削除の失敗）
- `component/`: `OshiAvatar`（写真か頭文字）、`StripedBackground`（推し色の斜めの縞）、`OshiPhoto`（写真か縞を敷く）、
  `WriteFailureSnackbar`（書き込みの失敗を Snackbar で知らせる）
- `photo/`: `rememberPhotoBitmap`（取り込んだ写真のパスか `content://` の URI を、表示の大きさに縮めて読む）
- `rule/`: 日付・時刻の書き方（`10月18日(日)`、`10.18 SUN`、`17:00` など）
- `NotificationAccess.kt`: 通知を出せるか（`canPostNotifications`・`canScheduleExactAlarms`）。端末の状態を読む関数で、UI の部品ではないが、
  feature/reminder（出すか）と feature/settings（案内）が同じ判定を使い、feature どうしは依存できないので、今は両方が依存する ui に置く。
  端末の機能の判定がもっと増えたら、それだけのモジュールに分ける

### feature/*

```
feature/<name>/src/main/kotlin/com/basekeita/oshikatsucountdown/feature/<name>/
  <Name>Screen.kt              Route（hiltViewModel を持つ唯一の Composable）と UI
  <Name>ViewModel.kt           @HiltViewModel。UiState をこのファイル内に定義
  <Name>Navigation.kt          EntryProviderScope<NavKey>.<name>Entry(onNavigationEvent)
  <Name>NavigationEvent.kt     sealed interface
  uimodel/                     この画面専用の UiModel
  ui/molecules/                この画面専用の部品
```

#### 画面の状態（UiState）

Android 公式の UI 層のガイド（[UI レイヤ](https://developer.android.com/topic/architecture/ui-layer)、
[UI の状態の生成](https://developer.android.com/topic/architecture/ui-layer/state-production)、
[UI イベント](https://developer.android.com/topic/architecture/ui-layer/events)）を土台にし、その上にこのリポジトリの決めごとを足す。

ガイドにあるもの:

- 画面の状態は不変の `data class XxxUiState` にし、`StateFlow` で公開する。読み込み中も `isLoading` のような項目で表す
- 項目から決まる値（`canSave`・`canClose` など）は項目として持たず、UiState のプロパティで計算する
- 入力など画面の操作で変わる状態は `MutableStateFlow` に持ち、`update { }` で変える。
  Repository の `Flow` と時刻の `flow` はそれと `combine` して UiState にし、`stateIn(WhileSubscribed(5_000), initialValue = XxxUiState())` で公開する
- `init` で非同期の処理を始めない（状態を作る処理は購読されてから始める）
- ViewModel から画面への知らせ（保存し終えた・書き込みの失敗）は一度きりのイベント（`Channel` / `SharedFlow`）にせず UiState の項目にし、
  画面が処理し終えたら ViewModel の関数で戻す

このリポジトリの決めごと（ガイドは、流れてくるデータから作る状態を sealed（Loading / Success など）にする例や、関係の無い状態を別の `StateFlow` で公開する例も載せている。
このリポジトリは入力の画面で状態が分かれないよう、全画面で次のようにそろえる）:

- 画面の状態は画面ごとに 1 つのフラットな UiState にまとめ、`StateFlow<XxxUiState>` 1 本で公開する。項目には既定値を置き、最初の値は `XxxUiState()` にする
  - 読めなかった（`isLoadFailed`）・予定が無い（`isNotFound`。予定の詳細）も UiState の項目にする。画面全体を sealed（Loading / Error / Success）で包まない
  - ドメインモデルは入れない
- 画面が持つ状態（入力中の値・絞り込み・保存中・終わった・書き込みの失敗・編集元から引き継ぐ値）は、private な `MutableStateFlow` 1 つにまとめる。
  状態のための `MutableStateFlow` や `var` を別に作らない。画面が持つ状態の無い画面は、Repository の `Flow` の `combine` だけで作る
- 保存を始めてよいかなど、判断に使う値は private な `MutableStateFlow` の今の値から読む（公開の状態は `combine` を通って遅れて届くため）
- 項目をまたぐ入力の規則（誕生日を選ぶと毎年くり返す、など）は UiModel の純粋な関数にし、ViewModel は `update` の中で呼ぶ。テストはその関数に対して書く
- 入力の画面の最初の値（編集元）は、`combine` の上流の `onStart` で 1 回だけ読んで private な `MutableStateFlow` に入れる。読めなければ `.catch` で `isLoadFailed` にする

#### 画面の引数・遷移・書き込み

- 画面の引数（予定・推しの id、導線かどうか）は `@HiltViewModel(assistedFactory = ...)` で ViewModel に渡し、Route は `hiltViewModel(creationCallback = ...)` で作る
- 保存・削除し終えてから画面を閉じる。入力の画面は UiState の `isFinished`、予定の詳細は `isNotFound` を見て遷移の要求を返す
  （先に閉じるとエントリの ViewModel ごと `viewModelScope` が止まり、書き込みが途中で切れるため）。
  閉じる画面（入力の画面・予定の詳細・ウィジェットの予定を選ぶ画面）は、書き込みの途中は UiState の `canClose` を false にし、
  閉じる・戻るボタンを押せなくして `BackHandler` で戻る操作を止める
- 書き込みの `IOException` は ViewModel で受け、UiState の `writeFailure`（`WriteFailureUiModel`）にして `WriteFailureSnackbar` で知らせる。
  出し終えたら `onWriteFailureShown` で null に戻す。保存中の状態も戻し、もう一度保存できるようにする
- 初回の導線を終えたことを保存できなくても、そのプロセスの間は終えたものとして扱う（`MainViewModel`）
- 時間で変わる表示は ViewModel の中の `flow` で時刻を流して作る。ホームは 1 分おきに日付、予定の詳細は秒の変わり目ごとに時刻
- 子 Composable には意味のあるコールバックを個別に渡す（`onIntent` / `viewModel` を渡さない）

### feature/settings

- 設定のバージョンは app が BuildConfig の `VERSION_NAME` を `@AppVersionName` の `String` で注入し、ViewModel が UiState に入れる
- ライセンスの一覧は app に当てた AboutLibraries のプラグインが、アプリに入る依存から `res/raw/aboutlibraries.json` をビルド時に作る
  （ライセンスの本文は SPDX から取るので、生成にはネットワークが要る）。許可するライセンス（Apache-2.0、BSD-3-Clause）を
  `aboutLibraries.license.allowedLicenses` に並べ、`strictMode = FAIL` でそれ以外を含む依存が入ったらこの生成でビルドを止める
  （CI の `assembleDebug` が検査を兼ねる）。app が `R.raw.aboutlibraries` を `licensesEntry` に渡し、
  画面は `produceLibraries(resId)` で読んで `LibrariesContainer` で出す。読み込みと表示はライブラリの部品に任せ、ViewModel を持たない
  （名前で探す `withContext` は R8 でリソースの名前が縮むと見つからないため、id で渡す）

### feature/settings の通知

- 通知の許可の状態（通知を出せるか・正確なアラームを使えるか）は端末から読むので、Route が前面に来るたびに（`LifecycleResumeEffect`）読んで `onNotificationPermissionCheck` で ViewModel に渡す。
  Android 12 で正確なアラームを使えなければ `EXACT_ALARM_DENIED` にし、「設定を開く」で端末の設定のこのアプリのアラームの画面（`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`）を開く。
  Android には「まだ聞いていない」の状態が無いので、許可を一度求めた印（`notificationPermissionRequested`）で「通知を許可する」と「設定を開く」を出し分ける（`notificationPermissionOf`）。
  Android 12 以前は許可のダイアログが無いので、通知が OFF なら「設定を開く」
- 許可のダイアログは、ViewModel が UiState の `isRequestingNotificationPermission` を立て、Route が出す。閉じたら `onNotificationPermissionResult` で戻し、印を残す
- 予約し直しは ViewModel からは呼ばない（設定の変更を `ReminderSync` が見ている）

### feature/reminder

```
feature/reminder/src/main/kotlin/com/basekeita/oshikatsucountdown/feature/reminder/
  ReminderScheduler.kt       予定の通知の計画のとおりに AlarmManager のアラームを置き換える
  LiveCountdownScheduler.kt  当日のカウントダウンを出す・予約する・消す。そのアラームの Receiver
  LiveCountdownAlarms.kt     当日のカウントダウンの入れるアラームと、知らせるかの判断（純粋な関数）
  LiveCountdownContent.kt    当日のカウントダウンの中身と、標準の通知の枠への置き方（`slotsAt`。純粋な関数）
  RecordReplacement.kt       予約の記録の置き換え方（足す前の記録・取り消すもの・終えた後の記録。純粋な関数）
  ReminderPoster.kt          届いた予定の通知を出し、置き直す（Receiver から呼ぶ手順をまとめたもの）
  ReminderReceivers.kt       予約した時刻の Receiver と、システムから届くきっかけ（`REMINDER_SYSTEM_ACTIONS`）の Receiver
  ReminderFiring.kt          届いた予約をまだ出すかの判断（純粋な関数）
  ReminderNotifications.kt   チャンネル・通知を出す・通知を出せるか
  ExactAlarms.kt             正確なアラームを入れる
  ReminderContent.kt         文面（タイトル・サブタイトル・本文）と、文言（`ReminderWording`）
  ReminderAlarmStore.kt      予約したアラームと出しているカウントダウンの識別子（DataStore、`reminder_alarms`）
  ReminderSync.kt            チャンネルを作り、予定・推し・通知の設定の変更で置き直す。Receiver が使う EntryPoint
```

- 画面を持たないが、Repository の interface と UseCase だけを使い、app 以外から見えないので、feature の依存の規則に合う（ウィジェットと同じ）。
  convention plugin は feature と同じ `oshikatsu.feature`（Compose を使うのは推し色の `toArgb` だけ）
- BroadcastReceiver は Hilt が作らないので `ReminderEntryPoint` から受け取る。Receiver は手順を持たず、手順をまとめたクラス
  （`ReminderPoster`・`LiveCountdownScheduler`・`ReminderSync`）だけを受け取る（Repository や UseCase を Receiver に見せない）
- 通知を出せるかの判定は `ui` の `NotificationAccess.kt`（[ui](#ui共通-ui)）

- 決めた時刻ちょうどに出す（iOS 版は日付と時刻を指定して予約する）ので、アラームは正確なアラーム `setExactAndAllowWhileIdle`（`RTC_WAKEUP`。端末の待機中も動く）。
  権限は Android 13 以降が `USE_EXACT_ALARM`（インストールのときに許可される。Google Play のポリシーで、イベントの通知を出すカレンダーのアプリなどに限られる）、
  Android 12 が `SCHEDULE_EXACT_ALARM`（`maxSdkVersion="32"`。利用者が端末の設定で許可する）。Android 11 は権限が要らない。
  正確なアラームを使えるか（`canScheduleExactAlarms`）も「通知を出せる」に入れ（`areReminderNotificationsAllowed`）、使えなければ予約を残さない
- アラームは UTC の時点で入るので、タイムゾーンが変わったら計画し直して、その土地の同じ日付・時刻に出す
- AlarmManager は予約したアラームを読み出せないので、予約した識別子を `ReminderAlarmStore` に持ち、計画から外れたものを取り消す。
  途中で止まっても（プロセスが死ぬ・scope が取り消される）後で取り消せるよう、足す前に「前の記録と今回のもの」を書き、終えてから今回のものだけにする（`recordReplacement`）。
  中身は作り直せる値なので、ファイルが壊れたら空にして読む（`ReplaceFileCorruptionHandler`）
- PendingIntent は識別子を data に入れて通知ごとに分ける（アラームは `oshikatsu-reminder:<識別子>`・`oshikatsu-countdown:<キー>`、押したときに開くのは `oshikatsu-notification:<識別子>`）。request code は 0
- 予約した時刻に届いたら、出す直前の時点の計画に同じ識別子・時刻の通知があるときだけ出す（`isReminderStillPlanned`。予約し直しが間に合わなかった古い予約を出さない）
- 予約し直すきっかけ: 予定と通知の設定の変更（`ReminderSync`。`OshikatsuApplication` がプロセスの開始時に動かす）、アプリが前面に来たとき（`MainActivity.onResume`。許可のダイアログを閉じたときも通る。
  `ReminderSync.requestRefreshAll` でプロセスの scope の `Dispatchers.Default` に投げる）、通知を出したとき（空いた枠を埋める。当日のカウントダウンも置き直す）、再起動・アプリの更新・端末の時刻・タイムゾーンの変更・正確なアラームの許可（`ReminderSystemReceiver`）。続けて呼ばれたら `Mutex` で順に置き換える
- 通知を出せない（Android 13 以降の許可が無い・端末の設定で OFF）ときは予約を残さない
- 予定・推し・通知の設定は、流れ（`events` など。読めなければ空・既定値）ではなく `readEvents`・`readOshis`・`readReminderSettings`（読めなければ `IOException`）で読む。
  読めないときは予約も当日のカウントダウンも変えずに終える（空・既定値として読むと、全部取り消す・OFF にした通知を入れてしまう。iOS 版も読めなければ何もしない）
- 日付が変わったとき（`ACTION_DATE_CHANGED`）も置き直す。`ReminderSync.start` が登録する Receiver で、アプリのプロセスが動いている間だけ受ける（iOS 版の `significantTimeChange` と同じ）
- 文面は通知を出すときに Room から読み直して作る（予約の後に推しの名前を変えても今の名前で出す）。押すとランチャーの Intent に `ExternalLaunchExtras.EVENT_ID` を付けて `MainActivity` を開く（ウィジェットと同じ）
- 通知は識別子をタグにして出す。iOS 版の `threadIdentifier` と同じく、予定の id をグループのキー（`setGroup`）にして同じ予定の通知をまとめる。束ねて見せるまとめ役の通知（`setGroupSummary`、タグは `reminder-group.<予定の id>`）を予定の通知を出すたびに出し直す。まとめ役は `GROUP_ALERT_CHILDREN` で鳴らさない。本文は `BigTextStyle` で広げたときに全文を出す。小アイコンは `ic_notification`（アプリのアイコンの前景と同じハート）
- チャンネル（`event_reminders`・`live_countdown`）は、アプリのプロセスの開始時（`ReminderSync.start`）に作る（最初の通知の前から端末の設定に並べる）。iOS 版と同じくバナーと音とバイブで知らせるよう、重要度を `IMPORTANCE_HIGH`（DEFAULT はバナーを出さない）にし、`setVibrationEnabled(true)`（チャンネルのバイブは既定で OFF）にする。
  どちらもバッジは付けない（`setShowBadge(false)`。iOS 版は通知でバッジを使わない）。
  重要度とバイブはチャンネルを作った後には変えられない（重要度は下げることだけできる）ので、変えるときは id を変えるか、入れ直す
- 当日のカウントダウン（`LiveCountdownScheduler`）
  - 時間帯に入っている予定は、出し続ける通知（`setOngoing`）で出す。開演までの残りは通知の時計（`setUsesChronometer`・`setChronometerCountDown`）で端末が数える。
    時計は見出しの行の末尾に出て、そのすぐ前がサブテキストなので、残りの見出し（`開演まで`）はサブテキストに入れ、推しの名前と開始の時刻は本文に出す（`slotsAt`）
  - 出し始める時刻・開演・開演の 1 時間後に正確なアラームを入れ（`countdownAlarmsOf`）、アラームで置き直す。開演を過ぎたら中身を「今日は当日！」に変え、1 時間後に消す（`setTimeoutAfter` も付ける）
  - 知らせる（`setSilent` を外す）のは、前の記録に出し始める時刻のアラームがあり、まだ出していないものだけ（`countdownsToAlert`）。どのきっかけの置き直しが先に通っても 1 回だけ鳴る
    （アラームでプロセスが起きると、`ReminderSync` の最初の値の置き直しと Receiver の置き直しが同じ `Mutex` を取り合うため、呼ばれた経路では決めない）。`setOnlyAlertOnce` で出し直しでは鳴らさない
  - 出している識別子を `ReminderAlarmStore` に持ち、計画から外れたものを消す。中身は置き直すたびに出し直す（同じタグで置き換わる）
  - チャンネルは `live_countdown`（設定の「予定を通知する」とは別。利用者は端末の設定でこのチャンネルを切り替える）。出し続ける通知なのでバッジは付けない（`setShowBadge(false)`）
  - 置き直すきっかけ: 予定・推しの変更（`ReminderSync`）、前面に来たとき、システムからのきっかけ（予定の通知と同じ）
- 予定を保存した直後の許可は app が求める（予定の入力画面は `EventEditorNavigationEvent.Saved` を返す）。
  `MainActivity` は端末の状態（許可のダイアログを出せる版か・もう許可されているか）を読んで `MainViewModel.onEventSave` に渡すだけで、求めるかは ViewModel が決める
  （`asksNotificationPermissionOnSave`。純粋な関数。まだ許可されておらず、まだ一度も求めておらず、いま求めている途中でもないとき）。
  設定の画面と同じく、ViewModel が UiState の `isRequestingNotificationPermission` を立て、`MainActivity` がダイアログを出し、閉じたら `onNotificationPermissionResult` で戻して印を残す。
  当日のカウントダウンにも同じ許可が要るので、「予定を通知する」が OFF でも求める

### feature/widget

```
feature/widget/src/main/kotlin/com/basekeita/oshikatsucountdown/feature/widget/
  glance/EventCountdownWidget.kt     カウントダウン（1 件）の GlanceAppWidget と Receiver
  glance/UpcomingEventsWidget.kt     推しカウント（一覧）の GlanceAppWidget と Receiver
  glance/WidgetStyles.kt             ウィジェットの色（配色のトークンからライト / ダーク）と外枠
  glance/WidgetLaunchIntents.kt      押したときに開く Intent
  WidgetConfigure{Activity,Screen,ViewModel}.kt   1 件のウィジェットに出す予定を選ぶ画面
  WidgetEventSelectionStore.kt       ウィジェットごとに選んだ予定の id
  WidgetRefresh.kt                   描き直し（0:00 の予約、時刻の変更、推し・予定の変更）
  WidgetModule.kt                    @Binds と、Glance から使う EntryPoint
  uimodel/                           WidgetEventUiModel と、予定の並びからの変換
```

- Glance のウィジェットは Hilt が作らないので、Repository・UseCase・`Clock` は `WidgetEntryPoint` から受け取る
- 中身は `provideContent` の中で Repository の `Flow` を `collectAsState` で読み、`BuildEventScheduleUseCase` で並べて UiModel にする。
  日付は組み直すたびに `Clock` から読む。描き直しの要求（`update` / `updateAll`）はセッションが動いていれば状態を読み直させるだけで
  `provideGlance` を通らないので、状態（`currentState`）を読んで組み直しの対象にしておく
- 1 件のウィジェットに出す予定の id は、Glance のウィジェットごとの状態（`PreferencesGlanceStateDefinition` の `selected_event_id`）に持つ。
  ウィジェットを外すと Glance が状態ごと消す
- 予定を選ぶ画面は `appWidgetConfigure` の Activity。最初に `RESULT_CANCELED` を返しておき、保存し終えてから `RESULT_OK` で閉じる
- 描き直すきっかけ
  - 推し・予定の変更: `WidgetDataSync` が Repository を見て `updateAll`。`OshikatsuApplication` がプロセスの開始時に動かす。
    ViewModel を通らないので、例外は `WidgetDataSync` が `.catch` で受けてログに残す（受けないとプロセスが落ちる）。止まってもウィジェットは前の表示のまま残る
  - 日付の変わり目: 描くたびに次の 0:00 を `AlarmManager.setWindow`（`RTC`、幅 10 分）で予約し、`WidgetRefreshReceiver` が描き直す。
    端末の時刻・タイムゾーンの変更（`TIME_SET` / `TIMEZONE_CHANGED`）も同じ Receiver で受ける。`updatePeriodMillis` は使わない
- 押したときはランチャーの Intent に `ExternalLaunchExtras` を付けて `MainActivity` を開く（`CLEAR_TOP | SINGLE_TOP` で開いているアプリに渡す）

### app

- `OshikatsuApplication`（`@HiltAndroidApp`。ウィジェットの `WidgetDataSync` と通知の `ReminderSync` を動かす）、`MainActivity`（`@AndroidEntryPoint`）、
  `MainViewModel`（導線を出すかどうか。起動画面のために購読を待たずに読む。ウィジェットから開くように頼まれた画面 `launchRoute`。予定を保存した直後に通知の許可を求めるか）
- `AppVersionModule`: 設定に出すバージョン（`BuildConfig.VERSION_NAME`）
- `ClockModule`: `Clock.systemDefaultZone()`。シングルトンにせず、ViewModel を作るたびに端末のタイムゾーンで作り直す
- タブごとに `rememberDecoratedNavEntries`（`SaveableStateHolder` と `ViewModelStore` の decorator）でエントリを作り、どのタブも毎回組み立てる
- 各 feature の遷移の要求は、そのタブのバックスタックに積む・外す。タブの最初の画面は外さない（`popIfNotRoot`）
- アイコンはアダプティブアイコン（推し色のピンクの地に白いハート。モノクロ層も同じ絵）

---

## テスト

- Fake の Repository は共通のモジュールに置かず、使うモジュールのテストに書く。同じモジュールの複数のテストで使うものは `FakeRepositories.kt` にまとめる
- 時刻は固定の `Clock`、または仮想時間とともに進む `Clock`（予定の詳細）を ViewModel に渡す
- 推し・予定（`local` / `data:impl`）はメモリの上の Room（`Room.inMemoryDatabaseBuilder`）で確かめる。Room は Context が要るため、Robolectric の上で動かす
- 設定（`local`）は一時ファイルの DataStore で確かめる。ファイルを渡す `PreferenceDataStoreFactory.create` は JVM のテストでは `File.renameTo` で書き換え、Windows では 2 回目の書き込みが失敗するため、
  `OkioStorage(FileSystem.SYSTEM, PreferencesSerializer)` を `create(storage = ...)` に渡す
- 推し色のコントラスト（白い文字・ライトとダークの地）は `ui` の `OshiColorContrastTest` で確かめる
- ウィジェットは、予定の並びからの変換（`WidgetMappingTest`）と予定を選ぶ画面の ViewModel を JVM で確かめる。Glance の描画はテストしない
- 通知は、計画（`PlanRemindersUseCase`・`PlanLiveCountdownsUseCase`。iOS 版の `ReminderPlanTests`・`LiveActivityPlanTests` と同じ場面）と、文面と当日のカウントダウンの中身（`ReminderContentTest`。文言は固定の値で渡す）、
  当日のカウントダウンのアラームと知らせるかの判断（`LiveCountdownAlarmsTest`）、届いた予約をまだ出すか（`ReminderFiringTest`）、
  予約の記録の置き換え方（`RecordReplacementTest`）、当日のカウントダウンの枠の置き方（`CountdownSlotsTest`）、
  システムから届くきっかけとマニフェストの登録が同じか（`ReminderSystemActionsTest`）、保存の直後に許可を求めるか（app の `MainViewModelTest`）を JVM で確かめる。
  AlarmManager への予約と通知を出すところはテストしない（判断は純粋な関数に切り出してテストする）
