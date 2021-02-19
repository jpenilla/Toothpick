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
import org.gradle.api.model.ObjectFactory
import java.io.File
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
public open class ToothpickExtension(objects: ObjectFactory) {
  public lateinit var project: Project
  public lateinit var forkName: String
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

  public var paperclipName: String = ""
    get(): String = if (field.isEmpty()) {
      "$forkNameLowercase-paperclip.jar"
    } else "$field.jar"

  public lateinit var serverProject: ToothpickSubproject
  public fun server(receiver: ToothpickSubproject.() -> Unit) {
    serverProject = ToothpickSubproject()
    receiver(serverProject)
  }

  public lateinit var apiProject: ToothpickSubproject
  public fun api(receiver: ToothpickSubproject.() -> Unit) {
    apiProject = ToothpickSubproject()
    receiver(apiProject)
  }

  public val subprojects: Map<String, ToothpickSubproject>
    get() = if (::forkName.isInitialized) mapOf(
      "$forkName-API" to apiProject,
      "$forkName-Server" to serverProject
    ) else emptyMap()

  internal val upstreamDir: File
    get() = project.projectDir.resolve(upstream)

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
    get() = project.projectDir.resolve("last-${upstreamLowercase}")

  internal val paperWorkDir: File
    get() = paperDir.resolve("work/Minecraft/${minecraftVersion}")

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
    try {
      if (cmd(*args, dir = project.projectDir).exitCode == 0) {
        return true
      }
    } catch (_: Exception) {
    }
    return false
  }
}
