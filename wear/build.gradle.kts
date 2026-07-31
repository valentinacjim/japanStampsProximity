plugins {
    alias(libs.plugins.android.application)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.mapclover.stampquest.wear"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.mapclover.stampquest.wear"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main").assets.srcDir("../app/src/main/assets")
    }
}

dependencies {
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.ktx.v1170)
}
