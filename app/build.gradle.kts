plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.indoorview"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.indoorview"
        minSdk = 24
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Mapbox ver 11.21.0
    implementation("com.mapbox.maps:android-ndk27:11.21.0")
    // Turf para validar punto dentro del polígono
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:7.10.0")

    //cardview:1.0.0 uso en el login
    implementation("androidx.cardview:cardview:1.0.0")
}