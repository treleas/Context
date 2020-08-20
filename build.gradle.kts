plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "4.1.4"
}

group = "ru.rubbergrief"
version = "0.1.4-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains:annotations:20.0.0")
    compileOnly("it.unimi.dsi:fastutil:8.2.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}