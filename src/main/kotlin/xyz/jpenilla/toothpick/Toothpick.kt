/*
 * This file is part of Toothpick, licensed under the MIT License.
 *
 * Copyright (c) 2020-2021 Jason Penilla & Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.toothpick

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.toothpick.Constants.Dependencies
import xyz.jpenilla.toothpick.Constants.Repositories

public class Toothpick : Plugin<Project> {
  override fun apply(project: Project) {
    project.extensions.create<ToothpickExtension>("toothpick", project)

    project.tasks.withType<Jar> {
      // We assume the parent project should not produce any artifacts
      onlyIf { false }
    }

    project.initToothpickTasks()
    project.afterEvaluate {
      if (toothpick.subprojects.isEmpty()) {
        error("You have not configured the API and Server subprojects with the ToothpickExtension!")
      }

      sequenceOf(project, toothpick.apiProject.project, toothpick.serverProject.project).forEach {
        it.group = toothpick.groupId
        it.version = "${toothpick.minecraftVersion}-${toothpick.nmsRevision}"
      }

      toothpick.apiProject.project.configureApiProject(toothpick.apiProject)
      toothpick.serverProject.project.configureServerProject(toothpick.serverProject)

      for (subproject in toothpick.subprojects) {
        configureRepositories(subproject)
        configureDependencies(subproject)
      }

      logger.lifecycle("Toothpick Gradle Plugin Version '{}'", Toothpick::class.java.`package`.implementationVersion)
      logger.lifecycle("Configured for '{}' version '{}' (Minecraft {})", toothpick.forkName, toothpick.forkVersion, toothpick.minecraftVersion)
    }
  }

  private fun configureRepositories(subproject: ToothpickSubproject) {
    subproject.project.repositories {
      mavenCentral()
      maven(Repositories.MINECRAFT)
      maven(Repositories.AIKAR)
      maven(Repositories.PAPER) {
        content {
          includeModule(Dependencies.paperMojangApi.groupId, Dependencies.paperMojangApi.artifactId)
        }
      }
      mavenLocal {
        content {
          includeModule(Dependencies.paperMinecraftServer.groupId, Dependencies.paperMinecraftServer.artifactId)
        }
      }
      loadRepositories(subproject)
    }
  }

  private fun configureDependencies(subproject: ToothpickSubproject) {
    subproject.project.dependencies {
      loadDependencies(subproject)
    }
  }
}
