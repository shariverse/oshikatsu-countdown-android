plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.settings"
}

dependencies {
    implementation(libs.aboutlibraries.compose.m3)
    // 通知の許可のダイアログ
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
