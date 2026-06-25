import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    `my-conventions`
    id("io.papermc.paperweight.userdev")
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.vanillage.ssaspawnerantiesp"
version = "1.0.0"
description = "SmartSpawner addon: server-side DDA ray tracing to hide spawner blocks without line of sight."

java {
    disableAutoTargetJvm()
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.65-stable")
    compileOnly("com.github.NighterDevelopment:SmartSpawner:1.6.7")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.1")

    runtimeOnly(project(":paper_1_21_11"))
    runtimeOnly(project(":paper_26_1_2"))
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src/main/java"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "pluginVersion" to project.version,
                ),
            )
        }
    }

    jar {
        archiveBaseName.set("SSASpawnerAntiESP")
        archiveClassifier.set("plain")
        manifest.attributes("paperweight-mappings-namespace" to "mojang")
    }
}

tasks.shadowJar {
    archiveBaseName.set("SSASpawnerAntiESP")
    archiveClassifier.set("")

    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    dependsOn(":paper_1_21_11:jar", ":paper_26_1_2:jar")
    from(project(":paper_1_21_11").tasks.jar.map { zipTree(it.archiveFile) })
    from(project(":paper_26_1_2").tasks.jar.map { zipTree(it.archiveFile) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
