plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    id("org.openapi.generator") version "7.22.0"

    alias(libs.plugins.google.services)

    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)


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
            //https://api.smartjam.ru/
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081\"")
        }
        //http://10.0.2.2:8081
        getByName("debug") {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081\"")
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

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.room.common.jvm)
    // Jetpack Compose integration
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.graphics)

    //network
    implementation(libs.retrofit)
    implementation(libs.okhttp)

    //audio player
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-ui-compose-material3:1.10.1")

    //serialization
    implementation(libs.converter.gson)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //logging
    implementation(libs.logging.interceptor)

    //database
    implementation(libs.androidx.datastore.preferences)

    //coroutines
    implementation(libs.kotlinx.coroutines.android)

    //ne pon
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.converter.scalars)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)



    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.media3.session)

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