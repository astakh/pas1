plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "si.unisa.sss.pocketvoiceassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "si.unisa.sss.pocketvoiceassistant"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // ВАЖНО: Porcupine нативно работает только с этими ABI (armeabi-v7a, arm64-v8a, x86_64).
        // Не добавляйте "x86" (32-битный эмулятор) — Porcupine его не поддерживает.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
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

    // Не сжимать модели Vosk/PPN (assets должны читаться как есть)
    androidResources {
        noCompress += listOf("ppn", "bin", "dat", "json")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Wake word — Picovoice Porcupine (офлайн, на устройстве)

    // Speech-to-Text — Vosk (офлайн; версия с Maven Central проверена 24.09.2026)
    // Пак в этой версии — org.vosk.* (Model, Recognizer, RecognizerCallback)
    implementation("com.alphacephei:vosk-android:0.3.47")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
