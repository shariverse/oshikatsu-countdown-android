plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.eventdetail"
}

dependencies {
    // 削除中に戻る操作を止める BackHandler のため
    implementation(libs.androidx.activity.compose)
}
