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
    // メモリの上の Room を作って local の DataSource に渡すため
    testImplementation(libs.androidx.room.runtime)
    // Room は Context が要るため、JVM のテストは Robolectric の上で動かす
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
