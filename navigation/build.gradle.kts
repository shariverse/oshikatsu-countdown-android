plugins {
    alias(libs.plugins.oshikatsu.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
}
