plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "2.7.3"
}

group = "ru.rubbergrief"
version = "0.1.3-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains:annotations:16.0.3")
    compileOnly("it.unimi.dsi:fastutil:8.2.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}