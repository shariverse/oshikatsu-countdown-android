import com.android.build.api.dsl.ApplicationExtension
import com.basekeita.oshikatsucountdown.buildlogic.OshikatsuBuildConfig
import com.basekeita.oshikatsucountdown.buildlogic.configureAndroid
import com.basekeita.oshikatsucountdown.buildlogic.libs
import com.basekeita.oshikatsucountdown.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** アプリ本体（app）の設定。common を内部で適用し、Compose を有効にする。 */
class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))
            pluginManager.apply(libs.pluginId("kotlin-compose"))
            pluginManager.apply(libs.pluginId("oshikatsu-common"))

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig {
                    applicationId = "com.basekeita.oshikatsucountdown"
                    targetSdk = OshikatsuBuildConfig.TARGET_SDK
                    versionCode = 1
                    versionName = "1.0"
                }
                buildFeatures.compose = true
            }
        }
    }
}
