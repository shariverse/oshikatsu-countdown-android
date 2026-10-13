import com.mikepenz.aboutlibraries.plugin.StrictMode

plugins {
    alias(libs.plugins.oshikatsu.application)
    alias(libs.plugins.oshikatsu.hilt)
    // 設定のライセンスの一覧（res/raw/aboutlibraries.json）を、アプリに入る依存から作る
    alias(libs.plugins.aboutlibraries.android)
}

android {
    namespace = "com.basekeita.oshikatsucountdown"

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

// 許可していないライセンスの依存が入ったら、一覧を作るタスク（assemble の途中で走る）でビルドを止める
aboutLibraries {
    license {
        strictMode = StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "BSD-3-Clause")
    }
}

dependencies {
    implementation(projects.feature.home)
    implementation(projects.feature.eventDetail)
    implementation(projects.feature.eventEditor)
    implementation(projects.feature.oshi)
    implementation(projects.feature.settings)
    implementation(projects.feature.widget)
    implementation(projects.ui)
    implementation(projects.navigation)
    implementation(projects.domain)
    implementation(projects.data.definition)
    implementation(projects.data.impl)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
