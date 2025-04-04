plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "3.0.3"
}

android {
    namespace = "com.example.hervoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hervoice"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.osmdroid)
    implementation(platform(libs.firebase.bom)) // Firebase BOM
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth) // Firebase Authentication
    implementation(libs.firebase.database) // Firebase Database
    implementation(libs.viewpager2) // For ViewPager2
    implementation(libs.play.services.base)
    implementation(libs.play.services.auth)
    implementation(libs.glide) // Glide for image loading
    annotationProcessor(libs.glide.compiler) // Glide compiler for annotations
    implementation(libs.firebase.appcheck.playintegrity) // Add the dependencies for the App Check libraries
    implementation(libs.firebase.analytics) // Add the dependencies for Firebase Analytics
    implementation(libs.firebase.crashlytics) // Add the dependencies for Firebase Crashlytics
    implementation(libs.firebase.perf) // Add the dependencies for Firebase Performance Monitoring)
    implementation(libs.firebase.config) // Add the dependencies for Firebase Remote Config
    implementation(libs.firebase.messaging) // Add the dependencies for Firebase Cloud Messaging
}