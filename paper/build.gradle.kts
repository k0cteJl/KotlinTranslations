plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

tasks.withType<Jar> {
    archiveBaseName.set("kotlintranslations-paper")
}
