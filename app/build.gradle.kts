plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.example.worktime"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.worktime"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { jvmToolchain(17) }
  // This ensures Gradle uses the toolchain resolver
  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(17)) // Match the version above
    }
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")


  implementation("androidx.room:room-ktx:2.6.1")
  implementation("androidx.navigation:navigation-compose:2.9.3")
  ksp("androidx.room:room-compiler:2.6.1")

  implementation("androidx.compose.ui:ui:1.7.2")
  implementation("androidx.compose.material3:material3:1.3.0")
  implementation("androidx.compose.ui:ui-tooling-preview:1.7.2")
  debugImplementation("androidx.compose.ui:ui-tooling:1.7.2")

  implementation("androidx.datastore:datastore-preferences:1.1.1")
  implementation("androidx.work:work-runtime-ktx:2.9.1")

  implementation("androidx.room:room-ktx:2.6.1")
  implementation("com.google.android.gms:play-services-location:21.3.0")
  implementation("com.google.android.gms:play-services-wearable:18.2.0")

  implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.3.0")

  wearApp(project(":wear"))
}
