import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.basekeita.oshikatsucountdown.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("common") {
            id = libs.plugins.oshikatsu.common.get().pluginId
            implementationClass = "CommonConventionPlugin"
        }
        register("application") {
            id = libs.plugins.oshikatsu.application.get().pluginId
            implementationClass = "ApplicationConventionPlugin"
        }
        register("library") {
            id = libs.plugins.oshikatsu.library.get().pluginId
            implementationClass = "LibraryConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.oshikatsu.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("feature") {
            id = libs.plugins.oshikatsu.feature.get().pluginId
            implementationClass = "FeatureConventionPlugin"
        }
    }
}
