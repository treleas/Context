plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "9.5.0"
}

group = "net.treleas"
version = "0.1.6-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains:annotations:26.0.2-1")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}