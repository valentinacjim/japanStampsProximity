plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"

}

android {
    namespace = "com.mapclover.stampquest"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mapclover.stampquest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}
dependencies {

    // Compose
    implementation(libs.androidx.core.ktx.v1170)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.activity.compose.v190)
    implementation(libs.androidx.ui)
    // Google Maps
    implementation(libs.play.services.maps.v1820)
    implementation(libs.play.services.location)
    // Para KML
    implementation(libs.android.maps.utils.v230)
    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.osmdroid.android)
    testImplementation(kotlin("test"))

}
