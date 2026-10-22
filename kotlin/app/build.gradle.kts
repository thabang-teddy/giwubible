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

// ── Release signing ────────────────────────────────────────────────────────
//
// The keystore and its passwords come from the environment, never from the
// repository and never from gradle.properties. CI writes the keystore out of
// a secret before building; see the `release` job in
// ../../.github/workflows/ci.yml, and kotlin/README.md for how to create one.
//
// Every published APK MUST be signed with the same key. The app updates
// itself in place from GitHub Releases, and Android refuses an update signed
// with a different key — which is exactly what a per-machine debug keystore
// guarantees. So an unsigned release is allowed to fall back to debug keys
// locally (you need to be able to build one without the secret), but any
// build that will be handed out passes `-PgiwuRequireSigning=true` and fails
// loudly instead.
val giwuRequireSigning = (findProperty("giwuRequireSigning") as String?).toBoolean()

val giwuSigning: Map<String, String>? = run {
    val values = listOf(
        "GIWU_KEYSTORE_FILE",
        "GIWU_KEYSTORE_PASSWORD",
        "GIWU_KEY_ALIAS",
        "GIWU_KEY_PASSWORD",
    ).associateWith { System.getenv(it).orEmpty() }

    val missing = values.filterValues { it.isBlank() }.keys
    val keystore = values.getValue("GIWU_KEYSTORE_FILE")
        .takeIf { it.isNotBlank() }
        ?.let { file(it) }

    when {
        missing.isEmpty() && keystore?.exists() == true -> values

        !giwuRequireSigning -> null

        missing.isNotEmpty() -> error(
            "Release signing was required but these environment variables are " +
                "unset: ${missing.joinToString(", ")}.",
        )

        else -> error("Release signing was required but no keystore exists at $keystore.")
    }
}

android {
    namespace = "com.giwu.bible"
    compileSdk = 36

    defaultConfig {
        // The identity of the shipped Android app, inherited from the Flutter
        // build this replaced, so an install updates it rather than sitting
        // beside it.
        applicationId = "com.giwu.bible"
        minSdk = 21
        targetSdk = 36
        versionCode = giwuVersionCode
        versionName = giwuVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        giwuSigning?.let { values ->
            create("giwu") {
                storeFile = file(values.getValue("GIWU_KEYSTORE_FILE"))
                storePassword = values.getValue("GIWU_KEYSTORE_PASSWORD")
                keyAlias = values.getValue("GIWU_KEY_ALIAS")
                keyPassword = values.getValue("GIWU_KEY_PASSWORD")

                // v1 as well as v2/v3, because minSdk is 21 and APK Signature
                // Scheme v2 only arrived in 24.
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The real key when the environment supplies one, debug keys when
            // it does not — and with -PgiwuRequireSigning=true the build has
            // already failed above rather than reaching this fallback.
            signingConfig = signingConfigs.findByName("giwu")
                ?: signingConfigs.getByName("debug")
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
