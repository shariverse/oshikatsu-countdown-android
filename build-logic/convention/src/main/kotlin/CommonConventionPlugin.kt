import com.basekeita.oshikatsucountdown.buildlogic.OshikatsuBuildConfig
import com.basekeita.oshikatsucountdown.buildlogic.libs
import com.basekeita.oshikatsucountdown.buildlogic.library
import com.basekeita.oshikatsucountdown.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Kotlin のモジュールなら種類を問わず共通の設定（JVM のバージョン、ktlint、JUnit）。
 * Android のモジュールは application / library が内部で適用し、domain（JVM）は直接適用する。
 */
class CommonConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ktlint"))

            pluginManager.withPlugin("java") {
                extensions.configure<JavaPluginExtension> {
                    sourceCompatibility = OshikatsuBuildConfig.javaVersion
                    targetCompatibility = OshikatsuBuildConfig.javaVersion
                }
            }

            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.fromTarget(OshikatsuBuildConfig.javaVersion.toString()))
            }

            extensions.configure<KtlintExtension> {
                filter {
                    exclude { element -> element.file.path.contains("generated") }
                }
            }

            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
    }
}
