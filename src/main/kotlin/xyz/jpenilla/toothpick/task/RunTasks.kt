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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register
import xyz.jpenilla.toothpick.shadowJar
import xyz.jpenilla.toothpick.taskGroup
import xyz.jpenilla.toothpick.toothpick

internal fun Project.registerRunTasks() {
  tasks.register<Exec>("runServer") {
    group = taskGroup
    description = "Spin up a test server"
    workingDir = projectDir.resolve("run")
    standardInput = System.`in`
    val shadowJar = toothpick.serverProject.project.tasks.shadowJar
    dependsOn(shadowJar)
    val patchedJar = shadowJar.outputs.files.singleFile
    commandLine = listOf("java", "-jar", patchedJar.absolutePath, "nogui")
    doFirst {
      if (!workingDir.exists()) workingDir.mkdir()
    }
  }

  tasks.register<JavaExec>("runDevServer") {
    group = taskGroup
    description = "Spin up a non-relocated test server"
    workingDir = projectDir.resolve("run")
    standardInput = System.`in`
    classpath = toothpick.serverProject.project.convention.getPlugin(JavaPluginConvention::class.java)
      .sourceSets.getByName("main").runtimeClasspath
    main = "org.bukkit.craftbukkit.Main"
    args = listOf("nogui")
    systemProperty("disable.watchdog", true)
    doFirst {
      if (!workingDir.exists()) workingDir.mkdir()
    }
  }
}
