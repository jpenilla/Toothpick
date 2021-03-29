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

import org.gradle.api.tasks.TaskAction
import xyz.jpenilla.toothpick.bashCmd
import xyz.jpenilla.toothpick.gitHash
import java.io.File

public open class SetupUpstream : ToothpickTask() {
  @TaskAction
  private fun setupUpstream() {
    val upstreamDir = toothpick.upstreamDir
    if (upstreamDir.resolve(toothpick.upstreamLowercase).exists()) {
      setupPaperOrBYOFUpstream(upstreamDir)
    } else if (
      upstreamDir.resolve("build.gradle.kts").exists()
      && upstreamDir.resolve("subprojects/server.gradle.kts").exists()
      && upstreamDir.resolve("subprojects/api.gradle.kts").exists()
    ) {
      setupToothpickUpstream(upstreamDir)
    } else {
      error("Don't know how to setup upstream!")
    }
  }

  private fun setupPaperOrBYOFUpstream(upstreamDir: File) {
    runSetupUpstreamCommand("./${toothpick.upstreamLowercase} patch", upstreamDir)
  }

  private fun setupToothpickUpstream(upstreamDir: File) {
    runSetupUpstreamCommand("./gradlew applyPatches", upstreamDir)
  }

  private fun runSetupUpstreamCommand(setupUpstreamCommand: String, upstreamDir: File) {
    val result = bashCmd(setupUpstreamCommand, dir = upstreamDir, printOut = true)
    if (result.exitCode != 0) {
      error("Failed to apply upstream patches: script exited with code ${result.exitCode}")
    }
    toothpick.lastUpstream.writeText(gitHash(upstreamDir))
  }
}
