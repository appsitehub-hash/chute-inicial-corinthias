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

kotlin {
    jvmToolchain(11)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("AppCorinthians")
        archiveClassifier.set("all")
        archiveVersion.set("")
        mergeServiceFiles()
    }
}
