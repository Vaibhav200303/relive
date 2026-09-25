import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { localProperties.load(it) }

fun configurationValue(name: String): String? =
    project.findProperty(name)?.toString()
        ?: System.getenv(name)
        ?: localProperties.getProperty(name)

fun revenueCatPublicKey(name: String): String? = configurationValue(name)?.also { value ->
    require(!value.trim().startsWith("sk_")) {
        "$name is a RevenueCat secret key. Secret keys must never be embedded in a mobile build."
    }
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

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

    signingConfigs {
        val storePath = configurationValue("RELIVE_RELEASE_STORE_FILE")
        val storePasswordValue = configurationValue("RELIVE_RELEASE_STORE_PASSWORD")
        val keyAliasValue = configurationValue("RELIVE_RELEASE_KEY_ALIAS")
        val keyPasswordValue = configurationValue("RELIVE_RELEASE_KEY_PASSWORD")
        if (listOf(storePath, storePasswordValue, keyAliasValue, keyPasswordValue).all { !it.isNullOrBlank() }) {
            create("productionRelease") {
                storeFile = rootProject.file(storePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
            create("friendsRelease") {
                storeFile = rootProject.file(storePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    defaultConfig {
        applicationId = "com.vaibhav.relive"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("demo") {
            dimension = "distribution"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "IS_DEMO", "true")
            buildConfigField("boolean", "IS_FRIENDS", "false")
            val key = revenueCatPublicKey("RELIVE_REVENUECAT_DEMO_ANDROID_PUBLIC_API_KEY")
                ?: revenueCatPublicKey("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")
                ?: "RELIVE_REVENUECAT_DEMO_ANDROID_PUBLIC_API_KEY"
            buildConfigField("String", "REVENUECAT_PUBLIC_API_KEY", key.asBuildConfigString())
        }
        create("friends") {
            dimension = "distribution"
            applicationIdSuffix = ".friends"
            versionNameSuffix = "-friends"
            signingConfigs.findByName("friendsRelease")?.let { signingConfig = it }
            buildConfigField("boolean", "IS_DEMO", "false")
            buildConfigField("boolean", "IS_FRIENDS", "true")
            val key = revenueCatPublicKey("RELIVE_REVENUECAT_FRIENDS_ANDROID_PUBLIC_API_KEY")
                ?: revenueCatPublicKey("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")
                ?: "RELIVE_REVENUECAT_FRIENDS_ANDROID_PUBLIC_API_KEY"
            buildConfigField("String", "REVENUECAT_PUBLIC_API_KEY", key.asBuildConfigString())
        }
        create("production") {
            dimension = "distribution"
            signingConfigs.findByName("productionRelease")?.let { signingConfig = it }
            buildConfigField("boolean", "IS_DEMO", "false")
            buildConfigField("boolean", "IS_FRIENDS", "false")
            val key = revenueCatPublicKey("RELIVE_REVENUECAT_PRODUCTION_ANDROID_PUBLIC_API_KEY")
                ?: revenueCatPublicKey("RELIVE_REVENUECAT_ANDROID_PUBLIC_API_KEY")
                    ?.takeUnless { it.trim().startsWith("test_") }
                ?: "RELIVE_REVENUECAT_PRODUCTION_ANDROID_PUBLIC_API_KEY"
            buildConfigField("String", "REVENUECAT_PUBLIC_API_KEY", key.asBuildConfigString())
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        create("share") {
            initWith(getByName("release"))
            // Friends shares use the RevenueCat Test Store, which the SDK only permits in
            // debuggable APKs. This keeps release-like packaging while retaining that contract.
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
        release {
            // Kotlin 2.4 metadata currently triggers compatibility warnings in the bundled R8.
            // Keep release optimization off until the toolchain can shrink without metadata loss.
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
        val googleWebClientId = configurationValue("RELIVE_GOOGLE_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", googleWebClientId.asBuildConfigString())
        val termsOfServiceUrl = configurationValue("RELIVE_TERMS_OF_SERVICE_URL")
            ?: "https://relivemoments.app/terms"
        buildConfigField("String", "TERMS_OF_SERVICE_URL", termsOfServiceUrl.asBuildConfigString())
        val privacyPolicyUrl = configurationValue("RELIVE_PRIVACY_POLICY_URL")
            ?: "https://relivemoments.app/privacy"
        buildConfigField("String", "PRIVACY_POLICY_URL", privacyPolicyUrl.asBuildConfigString())
        val supportEmail = configurationValue("RELIVE_SUPPORT_EMAIL") ?: ""
        buildConfigField("String", "SUPPORT_EMAIL", supportEmail.asBuildConfigString())
    }
}

androidComponents {
    beforeVariants { variant ->
        if (variant.buildType == "share" && variant.productFlavors.none { it.second == "friends" }) {
            variant.enable = false
        }
    }
}
