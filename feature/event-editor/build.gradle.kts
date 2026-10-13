plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.eventeditor"
}

dependencies {
    // 保存中に戻る操作を止める BackHandler のため
    implementation(libs.androidx.activity.compose)
}
