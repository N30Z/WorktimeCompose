plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.example.worktime.wear"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.example.worktime.wear"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17" // Or your project's requirement, ensure it matches compileOptions
  }
  // This ensures Gradle uses the toolchain resolver
  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(17)) // Match the version above
    }
  }
}
kotlin {
  jvmToolchain(17)
}
dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui:1.7.2")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("com.google.android.gms:play-services-wearable:18.2.0")
}
