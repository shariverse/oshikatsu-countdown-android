plugins {
    alias(libs.plugins.oshikatsu.library)
    alias(libs.plugins.oshikatsu.hilt)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.data.impl"
}

dependencies {
    implementation(projects.data.definition)
    implementation(projects.local)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    // 一時ファイルの DataStore を作って OshikatsuLocalDataSource に渡すため
    testImplementation(libs.androidx.datastore.preferences)
}
