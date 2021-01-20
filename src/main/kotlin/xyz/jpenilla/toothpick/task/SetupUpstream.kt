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
package xyz.jpenilla.toothpick.task

import org.gradle.api.Project
import org.gradle.api.Task
import xyz.jpenilla.toothpick.bashCmd
import xyz.jpenilla.toothpick.gitHash
import xyz.jpenilla.toothpick.lastUpstream
import xyz.jpenilla.toothpick.taskGroup
import xyz.jpenilla.toothpick.toothpick
import xyz.jpenilla.toothpick.upstreamDir

internal fun Project.createSetupUpstreamTask(
  receiver: Task.() -> Unit = {}
): Task = tasks.create("setupUpstream") {
  receiver(this)
  group = taskGroup
  doLast {
    val setupUpstreamCommand = if (upstreamDir.resolve(toothpick.upstreamLowercase).exists()) {
      "./${toothpick.upstreamLowercase} patch"
    } else if (
      upstreamDir.resolve("build.gradle.kts").exists()
      && upstreamDir.resolve("subprojects/server.gradle.kts").exists()
      && upstreamDir.resolve("subprojects/api.gradle.kts").exists()
    ) {
      "./gradlew applyPatches"
    } else {
      error("Don't know how to setup upstream!")
    }
    val result = bashCmd(setupUpstreamCommand, dir = upstreamDir, printOut = true)
    if (result.exitCode != 0) {
      error("Failed to apply upstream patches: script exited with code ${result.exitCode}")
    }
    lastUpstream.writeText(gitHash(upstreamDir))
  }
}
