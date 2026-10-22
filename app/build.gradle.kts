plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val foundationVersion: String by project

android {
    namespace = "com.example.lookaheadlatchrepro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.lookaheadlatchrepro"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.compose.foundation:foundation:$foundationVersion")
    implementation("androidx.compose.ui:ui:$foundationVersion")
    implementation("androidx.activity:activity-compose:1.13.0")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$foundationVersion")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest:$foundationVersion")
}
