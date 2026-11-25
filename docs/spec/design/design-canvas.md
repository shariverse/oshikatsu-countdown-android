<!--
  app-specs から生成したファイル。直接編集しない。
  元: shariverse/app-specs@adc1e9bc4db070f1e6cd9972ed59c6dbe33040e0 oshikatsu-countdown/design/design-canvas.md
  変えるときは https://github.com/shariverse/app-specs に PR を出す。
-->

# デザインキャンバスの配色

デザインキャンバス「推し活カウントダウン 画面デザイン」の M3 の値。アプリ全体のアクセント（brand）は推し色のピンクと同じ。

| 役割 | ライト | ダーク |
|---|---|---|
| background（画面の地） | `#FCF8FA` | `#141218` |
| surface（カード・シート） | `#FFFFFF` | `#211F26` |
| surfaceContainer（ナビゲーションバー・入力欄の地） | `#F3EDF1` | `#1D1B20` |
| surfaceContainerHigh（進み具合のバーの溝・区切り） | `#E2DCE3` | `#36343B` |
| onSurface（本文） | `#1D1B20` | `#E6E0E9` |
| onSurfaceVariant（補助テキスト） | `#49454F` | `#CAC4D0` |
| outline（チップの枠） | `#79747E` | `#938F99` |
| outlineVariant（区切り線） | `#CAC4D0` | `#49454F` |
| brand / onBrand | `#C8326A` / `#FFFFFF` | `#FFB1C8` / `#5E1133` |
| brandContainer / onBrandContainer（選択中の印の地と、その上の文字） | `#F4D6E1` / `#1D1B20` | `#7E2A4A` / `#FFD9E2` |
| destructive（削除の文字） | `#B3261E` | `#F2B8B5` |

- デザインキャンバスはライトだけなので、ダークは M3 のダークの地に合わせた
- ライトの brandContainer は、brand を白に 18% 混ぜた色
