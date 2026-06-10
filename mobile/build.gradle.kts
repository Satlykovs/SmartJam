plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false

    id("com.diffplug.spotless") version "8.5.1" apply true
}


subprojects {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**/*.kt", "**/generated/**/*.kt")

            ktfmt("0.62").kotlinlangStyle()

            toggleOffOn()
        }
    }
}