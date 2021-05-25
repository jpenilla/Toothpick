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

import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.toothpick.task.ApplyPatches
import xyz.jpenilla.toothpick.task.ImportMCDev
import xyz.jpenilla.toothpick.task.InitGitSubmodules
import xyz.jpenilla.toothpick.task.Paperclip
import xyz.jpenilla.toothpick.task.RebuildPatches
import xyz.jpenilla.toothpick.task.RepackageNMS
import xyz.jpenilla.toothpick.task.SetupUpstream
import xyz.jpenilla.toothpick.task.UpdateUpstream
import xyz.jpenilla.toothpick.task.UpstreamCommit
import xyz.jpenilla.toothpick.task.registerRunTasks

internal fun Project.initToothpickTasks() {
  gradle.taskGraph.whenReady {
    val fast = project.hasProperty(Constants.Properties.FAST)
    tasks.withType<Test> {
      onlyIf { !fast }
    }
    tasks.withType<Javadoc> {
      onlyIf { !fast || gradle.taskGraph.allTasks.any { it.name.contains("publish", ignoreCase = true) } }
    }
  }

  val build = tasks.named("build") {
    doFirst {
      val readyToBuild =
        toothpick.upstreamDir.resolve(".git").exists()
          && toothpick.subprojects.all { it.projectDir.exists() && it.baseDir.exists() }
      if (!readyToBuild) {
        error("Workspace has not been setup. Try running './gradlew applyPatches' first")
      }
    }
  }

  val initGitSubmodules = tasks.register<InitGitSubmodules>("initGitSubmodules")

  val setupUpstream = tasks.register<SetupUpstream>("setupUpstream")

  afterEvaluate {
    setupUpstream.configure {
      if (!toothpick.upstreamDir.resolve(".git").exists()) {
        dependsOn(initGitSubmodules)
      }
    }
  }

  val importMCDev = tasks.register<ImportMCDev>("importMCDev") {
    mustRunAfter(setupUpstream)
  }

  val paperclip = tasks.register<Paperclip>("paperclip") {
    dependsOn(build)
  }

  afterEvaluate {
    paperclip.configure {
      dependsOn(toothpick.apiProject.project.tasks.getByName("build"))
      dependsOn(toothpick.serverProject.project.tasks.getByName("build"))
      val shadowJar = toothpick.serverProject.project.tasks.shadowJar
      patchedJar.set(shadowJar.archiveFile)
    }
  }

  val applyPatches = tasks.register<ApplyPatches>("applyPatches") {
    // If Paper has not been setup yet or if we modified the submodule (i.e. upstream update), patch
    mustRunAfter(setupUpstream)
    dependsOn(importMCDev)
  }

  afterEvaluate {
    applyPatches.configure {
      with(toothpick) {
        if (!lastUpstream.exists()
          || !upstreamDir.resolve(".git").exists()
          || lastUpstream.readText() != gitHash(upstreamDir)
        ) {
          dependsOn(setupUpstream)
        }
      }
    }
  }

  tasks.register<RebuildPatches>("rebuildPatches")

  val updateUpstream = tasks.register<UpdateUpstream>("updateUpstream") {
    finalizedBy(setupUpstream)
  }

  afterEvaluate {
    updateUpstream.configure {
      if (!toothpick.upstreamDir.resolve(".git").exists()) {
        dependsOn(initGitSubmodules)
      }
    }
  }

  tasks.register<UpstreamCommit>("upstreamCommit")

  registerRunTasks()

  val cleanSubprojects = tasks.register<Delete>("cleanSubprojects") {
    description = "Deletes the Server and API project folders. Warning! This is irreversible, and could cause you to lose work if used by mistake!"
  }

  afterEvaluate {
    cleanSubprojects.configure {
      delete(toothpick.apiProject.projectDir, toothpick.serverProject.projectDir)
    }
  }

  val cleanToothpick = tasks.register<Delete>("cleanToothpick") {
    description = "Deletes the Server and API project folders, as well as the upstream folder. Warning! This is irreversible, and could cause you to lose work if used by mistake!"
  }

  afterEvaluate {
    cleanToothpick.configure {
      delete(toothpick.apiProject.projectDir, toothpick.serverProject.projectDir, toothpick.upstreamDir)
    }
  }

  tasks.register<RepackageNMS>("repackageNMS") {
    description = "Fix patches for application after Spigot's repackaging of NMS."
  }
}
