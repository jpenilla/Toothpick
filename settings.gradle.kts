enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://repo.stellardrift.ca/repository/snapshots/")
  }
}

plugins {
  id("ca.stellardrift.polyglot-version-catalogs") version "5.0.0-SNAPSHOT"
}

rootProject.name = "toothpick"
