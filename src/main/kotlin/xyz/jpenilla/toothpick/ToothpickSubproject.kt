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
import xyz.jpenilla.toothpick.data.MavenPom
import java.io.File

public class ToothpickSubproject internal constructor(internal val parentProject: Project) {
  public lateinit var project: Project
  public lateinit var patchesDir: File

  internal val baseDir: File by lazy {
    val name = project.name
    val upstream = parentProject.toothpick.upstream
    val suffix = if (name.endsWith("server")) "Server" else "API"
    parentProject.toothpick.upstreamDir.resolve("$upstream-$suffix")
  }

  internal val toothpick: ToothpickExtension
    get() = parentProject.toothpick

  internal val projectDir: File
    get() = project.projectDir

  internal val pom: MavenPom? by lazy { parsePom() }

  internal operator fun component1(): File = baseDir
  internal operator fun component2(): File = projectDir
  internal operator fun component3(): File = patchesDir
}
