plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    // Uncomment when google-services.json is added:
    // alias(libs.plugins.google.services)
}

android {
    namespace = "com.swasthai.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.swasthai.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export for migration tracking
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.swasthai.com/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Native C/C++ AI kernels (hybrid Kotlin + NDK).
    ndkVersion = "27.0.12077973"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                // The on-device AI targets every real-world ABI.
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Exclude duplicate META-INF files from TFLite dependencies
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Don't compress TFLite model files
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // ── AndroidX Core ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ── Compose ──
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── Navigation ──
    implementation(libs.androidx.navigation.compose)

    // ── Hilt ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── Room ──
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── DataStore ──
    implementation(libs.datastore.preferences)

    // ── WorkManager ──
    implementation(libs.work.runtime.ktx)

    // ── Networking (Ktor) ──
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    debugImplementation(libs.ktor.client.logging)

    // ── TensorFlow Lite ──
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.gpu)

    // ── Image Loading ──
    implementation(libs.coil.compose)

    // ── SQLCipher ──
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)

    // ── Firebase (uncomment when google-services.json is added) ──
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.auth)
    // implementation(libs.firebase.messaging)

    // ── Splash Screen ──
    implementation(libs.splash.screen)

    // ── Google Fonts ──
    implementation(libs.google.fonts)

    // ── Accompanist ──
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // ── Testing ──
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

// ── Hot Reload dev loop ──
// `./gradlew :app:hotReloadDebug -t` watches Kotlin/Compose sources and, on
// every save, incrementally rebuilds, reinstalls, and relaunches the app.
// Pick a target device with -Pdevice=<serial> (e.g. -Pdevice=emulator-5554).
// See hot_reload.sh for a friendly wrapper that auto-detects the device.
tasks.register<Exec>("hotReloadDebug") {
    dependsOn("installDebug")
    group = "run"
    description = "Install the debug APK and relaunch MainActivity. Use with -t or --continuous to watch sources for hot reload."

    val launchArgs = mutableListOf("shell", "am", "start", "-n", "com.swasthai.app/.MainActivity")
    providers.gradleProperty("device").orNull?.let { serial ->
        launchArgs.add(0, serial)
        launchArgs.add(0, "-s")
    }
    commandLine("adb", *launchArgs.toTypedArray())
}
