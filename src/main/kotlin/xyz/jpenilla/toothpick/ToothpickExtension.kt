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
import java.io.File
import java.util.Locale

/**
 * An extension exposing configuration for [Toothpick].
 *
 * @param project the project owning the [ToothpickExtension]
 */
public open class ToothpickExtension(public val project: Project) {
  public var forkName: String = project.name
  public val forkNameLowercase: String
    get() = forkName.toLowerCase(Locale.ENGLISH)
  public lateinit var forkUrl: String
  public lateinit var forkVersion: String
  public lateinit var groupId: String
  public lateinit var minecraftVersion: String
  public lateinit var nmsRevision: String
  public lateinit var nmsPackage: String

  public lateinit var upstream: String
  public val upstreamLowercase: String
    get() = upstream.toLowerCase(Locale.ENGLISH)
  public lateinit var upstreamBranch: String

  /**
   * Allows for customizing the name of the final Paperclip jar.
   * Defaults to `$forkNameLowercase-paperclip.jar` if unset.
   *
   * Custom names will be automatically have the `.jar` extension appended.
   */
  public var paperclipName: String = ""
    get(): String = if (field.isEmpty()) {
      "$forkNameLowercase-paperclip.jar"
    } else "$field.jar"

  public lateinit var serverProject: ToothpickSubproject
    private set

  public lateinit var apiProject: ToothpickSubproject
    private set

  public fun server(receiver: ToothpickSubproject.() -> Unit) {
    if (::serverProject.isInitialized) error("Cannot initialize the server subproject a second time!")
    serverProject = ToothpickSubproject(project).apply(receiver)

    serverProject.project.commonSubprojectConfiguration()
  }

  public fun api(receiver: ToothpickSubproject.() -> Unit) {
    if (::apiProject.isInitialized) error("Cannot initialize the api subproject a second time!")
    apiProject = ToothpickSubproject(project).apply(receiver)

    apiProject.project.commonSubprojectConfiguration()
  }

  public val subprojects: Set<ToothpickSubproject>
    get() = if (::serverProject.isInitialized && ::apiProject.isInitialized) setOf(apiProject, serverProject) else emptySet()

  internal val upstreamDir: File
    get() = project.projectDir.resolve(upstream)

  /**
   * Get the latest commit hash from the Git repo residing in the project directory of [project].
   *
   * @param length the length to truncate commit hashes to, defaulting to 7. can be null to disable truncation
   * @return the commit hash, or null if the [project] is not in a git repository or has not had its initial commit
   */
  public fun commitHash(length: Int? = 7): String? {
    val result = gitCmd("rev-parse", "HEAD", dir = project.projectDir)
    if (result.exitCode != 0 || result.output == null) {
      return null
    }
    val hash = result.output.trim()
    return if (length == null) {
      hash
    } else {
      hash.substring(0, length)
    }
  }

  internal val paperDir: File by lazy {
    if (upstream == "Paper") {
      upstreamDir
    } else {
      upstreamDir.walk().find {
        it.name == "Paper" && it.isDirectory
          && it.resolve("work/Minecraft/${minecraftVersion}").exists()
      } ?: error("Failed to find Paper directory!")
    }
  }

  internal val lastUpstream: File
    get() = project.file("last-${upstreamLowercase}")

  internal val paperDecompDir: File
    get() = paperDir.resolve("work/Minecraft/${minecraftVersion}")

  internal val paperWorkDir: File
    get() = paperDir.resolve("work")

  internal val mavenCommand: String by lazy {
    if (exitsSuccessfully("mvn", "-v")) {
      return@lazy "mvn"
    }
    if (exitsSuccessfully("mvn.cmd", "-v")) {
      return@lazy "mvn.cmd"
    }
    error("Unable to locate maven. Please ensure you have maven installed and on your path.")
  }

  private fun exitsSuccessfully(vararg args: String): Boolean {
    return try {
      cmd(*args, dir = project.projectDir).exitCode == 0
    } catch (_: Exception) {
      false
    }
  }
}
