import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "com.vanillage.ssaspawnerantiesp"
version = "1.0.0"
description = "SmartSpawner addon: server-side DDA ray tracing to hide spawner blocks without line of sight."

data class PaperTarget(
    val paperVersion: String,
    val javaVersion: Int,
    val apiVersion: String,
    val nmsSourceDir: String,
)

val paperTargets = mapOf(
    "1.21.11" to PaperTarget(
        paperVersion = "1.21.11-R0.1-SNAPSHOT",
        javaVersion = 21,
        apiVersion = "1.21.11",
        nmsSourceDir = "src/nms/paper-1.21.11/java",
    ),
    "26.1.2" to PaperTarget(
        paperVersion = "26.1.2.build.65-stable",
        javaVersion = 25,
        apiVersion = "26.1.2",
        nmsSourceDir = "src/nms/paper-26.1.2/java",
    ),
)

val paperTargetName = (findProperty("paperTarget") as String?) ?: "26.1.2"
val paperTarget = paperTargets[paperTargetName]
    ?: throw GradleException("Unknown paperTarget '$paperTargetName'. Supported: ${paperTargets.keys.sorted()}")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(paperTarget.javaVersion))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle(paperTarget.paperVersion)
    val smartSpawnerVersion = if (paperTarget.javaVersion >= 25) "1.6.7" else "1.6.2"
    compileOnly("com.github.NighterDevelopment:SmartSpawner:$smartSpawnerVersion")
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

sourceSets {
    named("main") {
        java.setSrcDirs(
            listOf(
                "src/main/java",
                paperTarget.nmsSourceDir,
            ),
        )
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(paperTarget.javaVersion)
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "apiVersion" to paperTarget.apiVersion,
                    "pluginVersion" to project.version,
                ),
            )
        }
    }

    jar {
        archiveBaseName.set("SSASpawnerAntiESP")
        archiveClassifier.set(paperTargetName)
    }
}

tasks.assemble {
    dependsOn(tasks.jar)
}

tasks.build {
    dependsOn(tasks.jar)
}
