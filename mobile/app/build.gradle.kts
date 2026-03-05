plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.smartjam.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.smartjam.app"
        minSdk = 29
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

    val nav_version = "2.9.7"
    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:$nav_version+")

    //network
    implementation("com.squareup.retrofit2:retrofit:2.11.+")
    implementation("com.squareup.okhttp3:okhttp:4.12.+")

    //serialization
    implementation("com.squareup.retrofit2:converter-gson:2.11.+")

    //logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.+")

    //database
    implementation("androidx.datastore:datastore-preferences:1.1.+")

    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.+")

    //ne pon
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.+")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}