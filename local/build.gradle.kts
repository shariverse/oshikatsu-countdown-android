plugins {
    alias(libs.plugins.oshikatsu.library)
    alias(libs.plugins.oshikatsu.hilt)
}

android {
    namespace = "com.basekeita.oshikatsucountdown.local"
}

// 版を上げたときに前の版の形と比べられるよう、テーブルの形を JSON に書き出す
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    // Room は Context が要るため、JVM のテストは Robolectric の上で動かす
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
