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
open class ToothpickExtension(objects: ObjectFactory) {
  lateinit var project: Project
  lateinit var forkName: String
  val forkNameLowercase
    get() = forkName.toLowerCase(Locale.ENGLISH)
  lateinit var forkUrl: String
  lateinit var forkVersion: String
  lateinit var groupId: String
  lateinit var minecraftVersion: String
  lateinit var nmsRevision: String
  lateinit var nmsPackage: String

  lateinit var upstream: String
  val upstreamLowercase
    get() = upstream.toLowerCase(Locale.ENGLISH)
  lateinit var upstreamBranch: String

  var paperclipName: String = ""
    get(): String = if (field.isEmpty()) {
      "$forkNameLowercase-paperclip.jar"
    } else "$field.jar"

  lateinit var serverProject: ToothpickSubproject
  fun server(receiver: ToothpickSubproject.() -> Unit) {
    serverProject = ToothpickSubproject()
    receiver(serverProject)
  }

  lateinit var apiProject: ToothpickSubproject
  fun api(receiver: ToothpickSubproject.() -> Unit) {
    apiProject = ToothpickSubproject()
    receiver(apiProject)
  }

  val subprojects: Map<String, ToothpickSubproject>
    get() = if (::forkName.isInitialized) mapOf(
      "$forkName-API" to apiProject,
      "$forkName-Server" to serverProject
    ) else emptyMap()

  val paperDir: File by lazy {
    if (upstream == "Paper") {
      project.upstreamDir
    } else {
      project.upstreamDir.walk().find {
        it.name == "Paper" && it.isDirectory
          && it.resolve("work/Minecraft/${minecraftVersion}").exists()
      } ?: error("Failed to find Paper directory!")
    }
  }

  val paperWorkDir: File
    get() = paperDir.resolve("work/Minecraft/${minecraftVersion}")
}
