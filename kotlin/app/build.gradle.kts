plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The release version comes from CI so the APK, the git tag and the Windows
// installer all agree; that single version still lives in the Flutter app's
// pubspec.yaml, which is what the release job reads. A local build falls back
// to the values below.
val giwuVersionName = (findProperty("giwuVersionName") as String?) ?: "1.0.0"
val giwuVersionCode = (findProperty("giwuVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.giwu.bible"
    compileSdk = 36

    defaultConfig {
        // Distinct from the Flutter build's com.giwu.bible so both can be
        // installed side by side while the port is compared against it.
        applicationId = "com.giwu.bible.kt"
        minSdk = 21
        targetSdk = 36
        versionCode = giwuVersionCode
        versionName = giwuVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Debug keys for now, same as the Flutter build — replace with a
            // real signing config before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // The JVM android.jar throws on every framework call. The code
            // under test only reaches android.util.Log, so returning defaults
            // keeps logging out of the way instead of failing the test.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
