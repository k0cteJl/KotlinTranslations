plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    archiveBaseName.set("kotlintranslations-core")
}
