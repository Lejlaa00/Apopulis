plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JSoup for HTML parsing
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Kotlin coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // skrape{it} for web scraping
    implementation("it.skrape:skrapeit:1.2.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    //Povezovanje
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.json:json:20210307")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}