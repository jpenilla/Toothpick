plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish")
  id("net.kyori.indra")
  id("net.kyori.indra.license-header")
  id("net.kyori.indra.publishing.gradle-plugin")
}

group = "xyz.jpenilla"
version = "1.1.0-SNAPSHOT"
description = "Gradle plugin to assist in forking Paper"

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.indraGit)
  implementation(libs.bundles.jackson)
  implementation(libs.bundles.configurate)
  implementation(libs.shadow)
}

kotlin {
  explicitApi()
}

tasks {
  jar {
    manifest {
      attributes("Implementation-Version" to project.version)
    }
  }
  compileKotlin {
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.jvmTarget = "1.8"
  }
}

indra {
  javaVersions {
    target(8)
  }
  mitLicense()
  github("jpenilla", "Toothpick")
  publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots")
  configurePublications {
    pom {
      developers {
        developer {
          id.set("jmp")
          timezone.set("America/Los Angeles")
        }
      }
    }
  }
}

indraPluginPublishing {
  plugin(
    "toothpick",
    "xyz.jpenilla.toothpick.Toothpick",
    "Toothpick",
    project.description
  )
  plugin(
    "toothpick.settings",
    "xyz.jpenilla.toothpick.ToothpickSettingsPlugin",
    "Toothpick Settings",
    "Companion settings plugin for Toothpick"
  )
  bundleTags("minecraft", "paper", "forking", "patching")
  website("https://github.com/jpenilla/Toothpick")
}
