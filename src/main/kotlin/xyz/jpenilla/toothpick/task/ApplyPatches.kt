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
import xyz.jpenilla.toothpick.ensureSuccess
import xyz.jpenilla.toothpick.gitCmd
import xyz.jpenilla.toothpick.reEnableGitSigning
import xyz.jpenilla.toothpick.temporarilyDisableGitSigning
import java.nio.file.Files
import java.util.LinkedList

public abstract class ApplyPatches : ToothpickTask() {
  public companion object {
    /**
     * The default git arguments to apply patches.
     *
     * `listOf("am", "--3way", "--ignore-whitespace")`
     */
    public val DEFAULT_APPLY_COMMAND: List<String> = listOf("am", "--3way", "--ignore-whitespace")
  }

  private val applyCommand = LinkedList(DEFAULT_APPLY_COMMAND)

  @TaskAction
  private fun applyPatches() {
    for (subproject in toothpick.subprojects) {
      val (baseDir, projectDir, patchesDir) = subproject
      val name = projectDir.name

      // Reset or initialize subproject
      logger.lifecycle(">>> Resetting subproject $name")
      if (projectDir.exists()) {
        ensureSuccess(gitCmd("fetch", "origin", dir = projectDir))
        ensureSuccess(gitCmd("reset", "--hard", "origin/master", dir = projectDir))
      } else {
        ensureSuccess(gitCmd("clone", baseDir.absolutePath, projectDir.absolutePath, dir = toothpick.project.projectDir))
      }
      logger.lifecycle(">>> Done resetting subproject $name")

      if (!patchesDir.exists()) {
        logger.lifecycle(">>> Patches directory for $name does not exist, skipping")
        continue
      }
      // Apply patches
      val patchPaths = Files.newDirectoryStream(patchesDir.toPath())
        .map { it.toFile() }
        .filter { it.name.endsWith(".patch") }
        .sorted()
        .takeIf { it.isNotEmpty() } ?: continue
      val patches = patchPaths.map { it.absolutePath }.toTypedArray()

      val wasGitSigningEnabled = temporarilyDisableGitSigning(projectDir)

      logger.lifecycle(">>> Applying patches to $name")

      val gitCommand = ArrayList<String>()
      gitCommand.addAll(applyCommand)
      gitCommand.addAll(patches)
      ensureSuccess(gitCmd(*gitCommand.toTypedArray(), dir = projectDir, printOut = true)) {
        if (wasGitSigningEnabled) reEnableGitSigning(projectDir)
      }

      if (wasGitSigningEnabled) reEnableGitSigning(projectDir)
      logger.lifecycle(">>> Done applying patches to $name")
    }
  }

  /**
   * Replaces the currently configured apply command.
   *
   * @param gitArguments new git arguments
   */
  public fun applyCommand(vararg gitArguments: String): Unit =
    applyCommand(gitArguments.toList())

  /**
   * Replaces the currently configured apply command.
   *
   * @param gitArguments new git arguments
   */
  public fun applyCommand(gitArguments: List<String>) {
    applyCommand.clear()
    applyCommand.addAll(gitArguments)
  }

  /**
   * Append the specified arguments to the currently configured apply command.
   *
   * @param arguments arguments to append
   */
  public fun argument(vararg arguments: String) {
    applyCommand.addAll(arguments)
  }

  /**
   * Gets the currently configured apply command.
   */
  public fun applyCommand(): List<String> = applyCommand
}
