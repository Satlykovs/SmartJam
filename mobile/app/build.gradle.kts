plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")

    id("org.openapi.generator") version "7.21.0"
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
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.smartjam.com/\"")
        }

        getByName("debug") {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {

            kotlin.directories.add("${layout.buildDirectory.get()}/generated/openapi/src/main/kotlin")
        }
    }

    sourceSets {
        getByName("main") {

            kotlin.directories.add("${layout.buildDirectory.get()}/generated/openapi/src/main/kotlin")
        }
    }

}

dependencies {

    implementation(libs.androidx.room.common.jvm)
    val nav_version = "2.9.7"
    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:$nav_version+")

    //network
    implementation("com.squareup.retrofit2:retrofit:2.11.+")
    implementation("com.squareup.okhttp3:okhttp:4.12.+")

    //serialization
    implementation("com.squareup.retrofit2:converter-gson:2.11.+")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.5.0")

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
    implementation(libs.converter.scalars)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


val generateCommonModels =
    tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateCommonModels") {
        group = "smartjam"
        generatorName.set("kotlin")
        inputSpec.set("${rootDir}/../openapi-spec/common-models.yaml")
        outputDir.set("${layout.buildDirectory.get()}/generated/openapi")
        modelPackage.set("com.smartjam.app.model")
        configOptions.set(
            mapOf(
                "serializationLibrary" to "gson",
                "enumPropertyNaming" to "original"
            )
        )
    }

val generateApiContract =
    tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateApiContract") {
        group = "smartjam"
        generatorName.set("kotlin")
        library.set("jvm-retrofit2")
        inputSpec.set("${rootDir}/../openapi-spec/api.yaml")
        outputDir.set("${layout.buildDirectory.get()}/generated/openapi")
        apiPackage.set("com.smartjam.app.api")
        modelPackage.set("com.smartjam.app.model")


        inlineSchemaOptions.set(
            mapOf(
                "RESOLVE_INLINE_ENUMS" to "true"
            )
        )

        configOptions.set(
            mapOf(
                "serializationLibrary" to "gson",
                "useCoroutines" to "true",
                "enumPropertyNaming" to "original",
                "generateAliasAsModel" to "false",
                "dateLibrary" to "java8"
            )
        )

        typeMappings.set(
            mapOf(
                "DateTime" to "Instant"
            )
        )

        importMappings.set(
            mapOf(
                "Instant" to "java.time.Instant"
            )
        )


        dependsOn(generateCommonModels)
    }

tasks.register("generateAll") {
    group = "smartjam"
    description = "Generates all Kotlin DTOs and API interfaces from OpenAPI specs"
    dependsOn(generateApiContract)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("generateApiContract")
}

afterEvaluate {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        dependsOn("generateApiContract")
        dependsOn("generateCommonModels")
    }
}