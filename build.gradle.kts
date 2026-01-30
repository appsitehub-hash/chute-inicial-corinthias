plugins {
    kotlin("jvm") version "1.8.22"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

application {
    // main class in AppCorinthians.kt
    mainClass.set("com.corinthians.app.AppCorinthiansKt")
}

// Use jvm target for Kotlin compilation (don't force toolchain download)
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

java {
    // do not configure a strict toolchain here to avoid CI toolchain auto-download issues
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("AppCorinthians")
        archiveClassifier.set("all")
        archiveVersion.set("")
        mergeServiceFiles()
    }
}
