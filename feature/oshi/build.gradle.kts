plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.oshi"
}

dependencies {
    // 写真の選択（Photo Picker）を rememberLauncherForActivityResult で開くため
    implementation(libs.androidx.activity.compose)
}
