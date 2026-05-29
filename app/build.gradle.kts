plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.indoorview"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.indoorview"
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
    packaging {
        resources.excludes.add("META-INF/NOTICE.md")
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/LICENSE.txt")
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

    //WORKER
    implementation("androidx.work:work-runtime:2.9.0")


    // BCrypt para hashear contraseñas
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Mapbox ver 11.21.0
    implementation("com.mapbox.maps:android-ndk27:11.21.0")
    // Turf para validar punto dentro del polígono
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:7.10.0")

    // Viewpager para poder visualizar imagenes
    implementation("androidx.viewpager:viewpager:1.0.0")

    //cardview:1.0.0 uso en el login
    implementation("androidx.cardview:cardview:1.0.0")
    //Barra de navegacion inferior
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Dependencias de google
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    // Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")
    // Cloud Storage
    implementation("com.google.firebase:firebase-storage")


    // OkHttp para peticiones HTTP
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Librerías necesarias para el envío de correos (JavaMail)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}