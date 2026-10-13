plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.settings"
}

dependencies {
    implementation(libs.aboutlibraries.compose.m3)
}
