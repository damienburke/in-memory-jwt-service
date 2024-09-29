val ktorVersion = "2.3.12"

plugins {
    kotlin("jvm") version "2.0.20"
}

group = "com.damo"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("commons-codec:commons-codec:1.15")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.21")
}