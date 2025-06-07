plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.compose") version "1.5.10"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":data-fetching:web-scraper"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("io.github.serpro69:kotlin-faker:1.14.0")
    implementation("media.kamel:kamel-image:0.6.0")

}

kotlin {
    jvmToolchain(17)
}