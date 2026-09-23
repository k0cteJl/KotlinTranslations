plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenCentral()
}

dependencies {
    jmhImplementation(project(":core"))
}

kotlin {
    jvmToolchain(21)
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    resultFormat.set("TEXT")
}

tasks.withType<Jar> {
    archiveBaseName.set("kotlintranslations-benchmarks")
}
