plugins {
    kotlin("jvm") version "2.2.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.javaaidev"
version = "0.1.0"

val mcpVersion = "0.6.0"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.18"
val ktorVersion = "3.1.1"
val picocliVersion = "4.7.7"
val swaggerParserVersion = "2.1.31"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.javaaidev.mcp.openapi.CliKt")
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:${mcpVersion}")
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("info.picocli:picocli:${picocliVersion}")
    implementation("io.swagger.parser.v3:swagger-parser:${swaggerParserVersion}")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}