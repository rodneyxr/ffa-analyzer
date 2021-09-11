import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    application
}

group = "edu.utsa.fileflow"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // https://github.com/Kotlin/kotlinx-cli
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")

    // https://mvnrepository.com/artifact/org.antlr/antlr4-runtime
    implementation("org.antlr:antlr4-runtime:4.5")
    // https://mvnrepository.com/artifact/log4j/log4j
    implementation("log4j:log4j:1.2.14")

    // https://github.com/rodneyxr/ffa-framework
    implementation("com.github.rodneyxr:ffa-framework:gradle-SNAPSHOT")
    // https://github.com/rodneyxr/ffa-grammar
    implementation("com.github.rodneyxr:ffa-grammar:gradle-SNAPSHOT")
    // https://github.com/rodneyxr/brics-automaton
    implementation("com.github.rodneyxr:brics-automaton:gradle-SNAPSHOT")
    // https://github.com/rodneyxr/brics-jsa
    implementation("com.github.rodneyxr:brics-jsa:gradle-SNAPSHOT")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}