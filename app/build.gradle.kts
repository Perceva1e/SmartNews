plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.diplom"
    compileSdk = 35
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "com.example.diplom"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas".toString()
                )
            }
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
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
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
    // Core
    implementation (libs.androidx.core.ktx.v1120)
    implementation (libs.androidx.appcompat.v161)
    implementation (libs.material)
    implementation (libs.androidx.constraintlayout.v214)

    // Lifecycle
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.lifecycle.viewmodel.ktx)

    // Room
    implementation (libs.androidx.room.runtime)
    implementation (libs.androidx.room.ktx)
    kapt (libs.androidx.room.compiler)

    // Retrofit
    implementation (libs.retrofit)
    implementation (libs.converter.gson)

    // Coroutines
    implementation (libs.kotlinx.coroutines.android)

    // Navigation
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)

    // OkHttp
    implementation (libs.logging.interceptor)

    // Test
    testImplementation (libs.junit)
    androidTestImplementation (libs.androidx.junit.v115)
    androidTestImplementation (libs.androidx.espresso.core.v351)

    implementation (libs.glide)
    kapt (libs.compiler)

    implementation(libs.dagger.hilt.android)
    kapt (libs.hilt.compiler)
    implementation (libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.material.v1110)
    implementation (libs.androidx.constraintlayout.v220alpha13)
    implementation (libs.firebase.auth.ktx)
    implementation (libs.play.services.auth)
    implementation(platform(libs.firebase.bom))
}