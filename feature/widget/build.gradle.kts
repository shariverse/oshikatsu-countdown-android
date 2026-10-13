plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.widget"
}

dependencies {
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.activity.compose)
}
