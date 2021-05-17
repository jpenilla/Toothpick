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
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import java.util.Locale

/**
 * Companion settings plugin for [Toothpick].
 */
public class ToothpickSettingsPlugin : Plugin<Settings> {
  override fun apply(target: Settings) {
  }
}

/**
 * Configures an API and Server subproject using the standard Toothpick conventions for the provided
 * parent project and fork name.
 *
 * Standard conventions include the following:
 *  - API project named `$forkNameLowercase-api`
 *    - Project directory set to `$forkName-API`
 *    - Build script configured to `../api.gradle.kts`
 *  - Server project named `$forkNameLowercase-server`
 *    - Project directory set to `$forkName-Server`
 *    - Build script configured to `../server.gradle.kts`
 *
 * To customize any aspects of the configured subprojects, you can utilize the methods on [ToothpickSettings].
 *
 * @param parentProject parent project which the [Toothpick] plugin will be applied to
 * @param forkName name of the fork, can include uppercase letters but should not include spaces
 * @param configuration optional configuration block to customize toothpick subprojects
 * @return the [ToothpickSettings] instance
 */
public fun Settings.setupToothpickProject(
  parentProject: ProjectDescriptor,
  forkName: String,
  configuration: ToothpickSettings.() -> Unit = {}
): ToothpickSettings {
  val forkNameLowercase = forkName.toLowerCase(Locale.ROOT)

  fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit): ProjectDescriptor {
    val parentPath = parentProject.path
    val subprojectPath = "$parentPath${if (parentPath.endsWith(":")) "" else ":"}$name"
    include(subprojectPath)
    return project(subprojectPath).apply(block)
  }

  val api = setupSubproject("$forkNameLowercase-api") {
    projectDir = parentProject.projectDir.resolve("$forkName-API")
    buildFileName = "../subprojects/api.gradle.kts"
  }
  val server = setupSubproject("$forkNameLowercase-server") {
    projectDir = parentProject.projectDir.resolve("$forkName-Server")
    buildFileName = "../subprojects/server.gradle.kts"
  }

  return ToothpickSettings(parentProject, api, server).apply(configuration)
}

/**
 * Exposes methods to customize toothpick subproject configuration.
 */
public class ToothpickSettings(
  private val parentProject: ProjectDescriptor,
  public val apiProject: ProjectDescriptor,
  public val serverProject: ProjectDescriptor
) {
  /**
   * Configure the API subproject [ProjectDescriptor].
   *
   * @param configuration configuration block
   */
  public fun apiProject(configuration: ProjectDescriptor.() -> Unit): ProjectDescriptor = apiProject.apply(configuration)

  /**
   * Configure the Server subproject [ProjectDescriptor].
   *
   * @param configuration configuration block
   */
  public fun serverProject(configuration: ProjectDescriptor.() -> Unit): ProjectDescriptor = serverProject.apply(configuration)
}
