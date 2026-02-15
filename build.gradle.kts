plugins {
    java
    `maven-publish`
}

group = "dev.misieur"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly(project(":compileOnly"))
    implementation("net.bytebuddy:byte-buddy:1.18.5")
    implementation("net.bytebuddy:byte-buddy-agent:1.18.5")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "justamaterial"
            from(components["java"])
        }
    }
}
