import xyz.jpenilla.runpaper.task.RunServer
import kotlin.text.replace

plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.2.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "dev.misieur"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(project(":"))
}

listOf(
    "1.21.11",
    "1.21.10",
    "1.21.8",
    "1.21.5",
    "1.21.4",
    "1.21.1",
).forEach {
    registerPaperTask(it)
}

fun registerPaperTask(
    version: String
) {
    listOf(version).forEach { taskName ->
        tasks.register(taskName, RunServer::class) {
            val jarFile = layout.buildDirectory.file("libs/plugin-1.0-SNAPSHOT.jar")
            dependsOn("shadowJar")
            pluginJars(jarFile.get().asFile.absolutePath)

            group = "runPaper"
            minecraftVersion(version)

            runDirectory = layout.projectDirectory.dir("${project.projectDir}/runPaper/${version.replace("\\.", "")}")
            systemProperties["com.mojang.eula.agree"] = true
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.shadowJar {
    archiveClassifier.set("")
}