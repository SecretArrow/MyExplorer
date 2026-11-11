import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// Signing release dari environment (diisi oleh workflow release via secrets).
// Jika env tidak ada, build release menghasilkan APK/AAB unsigned.
val releaseStoreFile: String? = System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword: String? = System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("RELEASE_KEY_PASSWORD")

// Basis versionCode semua output rilis. Skema multi-APK ala panduan resmi:
// universal = base*1000, per-ABI = base*1000 + indeks ABI, sehingga versi baru
// SELALU lebih tinggi dari rilis sebelumnya apa pun kombinasi APK yang dipasang
// pengguna (v1.1.0: universal=2000, armeabi-v7a=2001, x86=2002, x86_64=2003,
// arm64-v8a=2004 — semuanya > versionCode 1 milik v1.0.0).
val baseVersionCode = 2
val abiVersionCodes = mapOf("armeabi-v7a" to 1, "x86" to 2, "x86_64" to 3, "arm64-v8a" to 4)

android {
    namespace = "com.myexplorer.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.myexplorer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = baseVersionCode
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // APK rilis dipecah per ABI: aplikasi membawa pustaka native (.so) untuk
    // armeabi-v7a/arm64-v8a/x86/x86_64, jadi APK universal mengepakkan keempatnya
    // sekaligus (23,6 MB). Split per ABI menghasilkan APK jauh lebih kecil per
    // perangkat; APK universal tetap dihasilkan sebagai fallback penuh.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// AGP 9: API varian lama (applicationVariants/versionCodeOverride) sudah dihapus,
// jadi versionCode per output diatur lewat API varian baru. Output tanpa filter
// = APK universal; output berfilter ABI mendapat base*1000 + indeks ABI.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.firstOrNull()?.identifier
            val abiIndex = abiVersionCodes[abi] ?: 0
            output.versionCode.set(baseVersionCode * 1000 + abiIndex)
        }
    }
}

// Kotlin 2.4: android.kotlinOptions deprecated-with-error -> DSL kotlin.compilerOptions.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-common"))
    implementation(project(":core-ui"))
    implementation(project(":data-local"))
    implementation(project(":feature-browser"))
    implementation(project(":feature-tools"))
    implementation(project(":feature-media"))
    implementation(project(":feature-transfer"))
    implementation(project(":feature-network"))
    implementation(project(":feature-sync"))
    implementation(project(":feature-apps"))
    implementation(project(":feature-settings"))
    // MainActivity memakai BiometricManager/BiometricPrompt.Authenticators langsung.
    implementation(libs.androidx.biometric)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // Widget "Akses cepat" (Glance AppWidget).
    implementation(libs.androidx.glance.appwidget)
    // Instalasi baseline profile (cold-start lebih cepat) — aturan di src/main/baseline-prof.txt.
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
