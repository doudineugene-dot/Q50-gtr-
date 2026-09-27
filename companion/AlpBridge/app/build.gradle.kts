plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.q50gtr.alpbridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.q50gtr.alpbridge"
        // MediaProjection существует с API 21, но обязательный foreground
        // service с уведомлением для захвата экрана требует более новую
        // платформу целиком (Android 8+ для каналов уведомлений, Android 10+
        // для самого требования запускать проекцию из foreground-службы).
        // Телефон в проекте — Samsung Galaxy S21 FE, современный Android;
        // задел под более старые телефоны здесь не нужен и не проверялся.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
