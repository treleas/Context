plugins {
    java
    `maven-publish`
    id("io.freefair.lombok") version "2.7.3"
}

group = "ru.rubbergrief"
version = "0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}