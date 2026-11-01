# oshikatsu-countdown-android

推し活の予定（推しのライブ・誕生日・発売日など）を登録し、その日まであと何日かを数える Android アプリ「推し活カウントダウン」。

仕様は [`docs/SPEC.md`](docs/SPEC.md)、設計は [`docs/architecture.md`](docs/architecture.md) にまとめてある。

## 環境

- AGP 9.4.1（Kotlin 2.2.10 内蔵）/ Gradle 9.6.0 / Compose BOM 2026.02.01
- minSdk 30 / targetSdk 37 / compileSdk 37
- Gradle デーモンは `gradle/gradle-daemon-jvm.properties` の toolchain（25）で動く

## セットアップ

プロジェクト直下に `local.properties` を作り、SDK を指す（Git に含めない）。

```properties
sdk.dir=/path/to/Android/Sdk
```

ビルド・テスト・静的チェック:

```shell
./gradlew assembleDebug
```

```shell
./gradlew test
```

```shell
./gradlew ktlintCheck lintDebug
```

エミュレータ・実機に入れて起動する:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.basekeita.oshikatsucountdown -c android.intent.category.LAUNCHER 1
```

## 構成

```
build-logic/convention/  convention plugin（oshikatsu.common / application / library / hilt / feature）
app/                     エントリポイント（OshikatsuApplication, MainActivity）、初回の導線、3 タブと NavDisplay
feature/
  home/                  ホーム
  event-detail/          予定の詳細（秒単位のカウントダウン）
  event-editor/          予定の追加・編集
  oshi/                  推しの一覧と登録・編集
  settings/              設定
  widget/                ホーム画面ウィジェット（Glance）と、ウィジェットに出す予定を選ぶ画面
ui/                      共通 UI（テーマ、推し色、部品、写真の読み込み、日付の書き方）
navigation/              ルート定義（NavKey）
domain/                  UseCase と計算の関数
data/
  definition/            ドメインモデル、Repository の interface
  impl/                  Repository の実装
local/                   Room（推し・予定）、DataStore（設定）、写真の取り込み先
```
