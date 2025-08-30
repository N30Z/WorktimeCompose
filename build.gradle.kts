buildscript {
  dependencies {
    classpath("com.android.tools.build:gradle:8.12.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
  }
}
plugins {
  id("com.android.application") version "8.12.2" apply false
  id("com.android.library") version "8.12.2" apply false
  id("org.jetbrains.kotlin.android") version "1.9.24" apply false
  id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
  id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
