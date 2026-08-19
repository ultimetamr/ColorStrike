plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.pico.swan.colorstrike"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pico.swan.colorstrike"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters.add("arm64-v8a") }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.spatial.bom))
    implementation(libs.spatial.core)
    implementation(libs.spatial.ui.platform)
    implementation(libs.spatial.ui.foundation)
    implementation(libs.spatial.ui.design)
    implementation(libs.spatial.ui.sense)
    implementation(libs.spatial.ui.tracking)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}
