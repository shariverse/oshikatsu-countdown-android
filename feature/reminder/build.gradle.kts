plugins {
    alias(libs.plugins.oshikatsu.feature)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.feature.reminder"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
}
