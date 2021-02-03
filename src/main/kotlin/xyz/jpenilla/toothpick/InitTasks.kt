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
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.toothpick.task.ApplyPatches
import xyz.jpenilla.toothpick.task.ImportMCDev
import xyz.jpenilla.toothpick.task.InitGitSubmodules
import xyz.jpenilla.toothpick.task.Paperclip
import xyz.jpenilla.toothpick.task.RebuildPatches
import xyz.jpenilla.toothpick.task.SetupUpstream
import xyz.jpenilla.toothpick.task.UpdateUpstream
import xyz.jpenilla.toothpick.task.UpstreamCommit

internal fun Project.initToothpickTasks() {
  gradle.taskGraph.whenReady {
    val fast = project.hasProperty("fast")
    tasks.withType<Test> {
      onlyIf { !fast }
    }
    tasks.withType<Javadoc> {
      onlyIf { !fast || gradle.taskGraph.allTasks.any { it.name.contains("publish", ignoreCase = true) } }
    }
  }

  tasks.getByName("build") {
    doFirst {
      val readyToBuild =
        toothpick.upstreamDir.resolve(".git").exists()
          && toothpick.subprojects.values.all { it.projectDir.exists() && it.baseDir.exists() }
      if (!readyToBuild) {
        error("Workspace has not been setup. Try running './gradlew applyPatches' first")
      }
    }
  }

  val initGitSubmodules = tasks.register<InitGitSubmodules>("initGitSubmodules")

  val setupUpstream = tasks.register<SetupUpstream>("setupUpstream") {
    if (!toothpick.upstreamDir.resolve(".git").exists()) {
      dependsOn(initGitSubmodules)
    }
  }

  val importMCDev = tasks.register<ImportMCDev>("importMCDev") {
    mustRunAfter(setupUpstream)
  }

  tasks.register<Paperclip>("paperclip") {
    val shadowJar = toothpick.serverProject.project.tasks.getByName("shadowJar")
    dependsOn(shadowJar)
    patchedJar = shadowJar.outputs.files.singleFile
  }

  tasks.register<ApplyPatches>("applyPatches") {
    // If Paper has not been setup yet or if we modified the submodule (i.e. upstream update), patch
    with(toothpick) {
      if (!lastUpstream.exists()
        || !upstreamDir.resolve(".git").exists()
        || lastUpstream.readText() != gitHash(upstreamDir)
      ) {
        dependsOn(setupUpstream)
      }
    }
    mustRunAfter(setupUpstream)
    dependsOn(importMCDev)
  }

  tasks.register<RebuildPatches>("rebuildPatches")

  tasks.register<UpdateUpstream>("updateUpstream") {
    finalizedBy(setupUpstream)
  }

  tasks.register<UpstreamCommit>("upstreamCommit")
}
