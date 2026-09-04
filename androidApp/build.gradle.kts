import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { localProperties.load(it) }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.foundation)
    debugImplementation(libs.compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
}

android {
    namespace = "com.vaibhav.relive"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.vaibhav.relive"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        val googleWebClientId = project.findProperty("RELIVE_GOOGLE_WEB_CLIENT_ID")?.toString()
            ?: System.getenv("RELIVE_GOOGLE_WEB_CLIENT_ID")
            ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        val revenueCatApiKey = project.findProperty("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")?.toString()
            ?: System.getenv("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")
            ?: localProperties.getProperty("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")
            ?: "RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY"
        buildConfigField("String", "REVENUECAT_PUBLIC_API_KEY", "\"$revenueCatApiKey\"")
        val termsOfServiceUrl = project.findProperty("RELIVE_TERMS_OF_SERVICE_URL")?.toString()
            ?: System.getenv("RELIVE_TERMS_OF_SERVICE_URL")
            ?: localProperties.getProperty("RELIVE_TERMS_OF_SERVICE_URL")
            ?: ""
        buildConfigField("String", "TERMS_OF_SERVICE_URL", "\"$termsOfServiceUrl\"")
        val privacyPolicyUrl = project.findProperty("RELIVE_PRIVACY_POLICY_URL")?.toString()
            ?: System.getenv("RELIVE_PRIVACY_POLICY_URL")
            ?: localProperties.getProperty("RELIVE_PRIVACY_POLICY_URL")
            ?: ""
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")
    }
}
