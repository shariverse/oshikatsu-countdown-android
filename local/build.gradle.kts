plugins {
    alias(libs.plugins.oshikatsu.library)
    alias(libs.plugins.oshikatsu.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.local"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
