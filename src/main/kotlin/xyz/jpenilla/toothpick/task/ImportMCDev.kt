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

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.tasks.TaskAction
import xyz.jpenilla.toothpick.ensureSuccess
import xyz.jpenilla.toothpick.gitCmd

private val json = Json { prettyPrint = true }

@Serializable
private data class ImportsContainer(
  val nmsImports: List<String> = emptyList(),
  val libraryImports: List<LibraryImport> = emptyList()
)

@Serializable
private data class LibraryImport(val group: String, val library: String, val prefix: String, val file: String)

public open class ImportMCDev : ToothpickInternalTask() {
  private val upstreamServer = toothpick.serverProject.baseDir
  private val importLog = arrayListOf("Extra mc-dev imports")

  private fun importNMS(className: String) {
    logger.lifecycle("Importing n.m.s.$className")
    val source = toothpick.paperWorkDir.resolve("spigot/net/minecraft/server/$className.java")
    if (!source.exists()) error("Missing NMS: $className")
    val target = upstreamServer.resolve("src/main/java/net/minecraft/server/$className.java")
    if (target.exists()) return
    source.copyTo(target)
    importLog.add("Imported n.m.s.$className")
  }

  private fun importLibrary(import: LibraryImport) {
    val (group, lib, prefix, file) = import
    logger.lifecycle("Importing $group.$lib $prefix/$file")
    val source = toothpick.paperWorkDir.resolve("libraries/$group/$lib/$prefix/$file.java")
    if (!source.exists()) error("Missing Base: $lib $prefix/$file")
    val targetDir = upstreamServer.resolve("src/main/java/$prefix")
    val target = targetDir.resolve("$file.java")
    targetDir.mkdirs()
    source.copyTo(target)
    importLog.add("Imported $group.$lib $prefix/$file")
  }

  @TaskAction
  private fun importMCDev() {
    logger.lifecycle(">>> Importing mc-dev")
    val lastCommitIsMCDev = gitCmd(
      "log", "-1", "--oneline",
      dir = upstreamServer
    ).output?.contains("Extra mc-dev imports") == true
    if (lastCommitIsMCDev) {
      ensureSuccess(
        gitCmd("reset", "--hard", "HEAD~1", dir = upstreamServer, printOut = true)
      )
    }

    (toothpick.serverProject.patchesDir.listFiles() ?: error("No patches in server?")).asSequence()
      .flatMap { it.readLines().asSequence() }
      .filter { it.startsWith("+++ b/src/main/java/net/minecraft/server/") }
      .distinct()
      .map { it.substringAfter("/server/").substringBefore(".java") }
      .filter { !upstreamServer.resolve("src/main/java/net/minecraft/server/$it.java").exists() }
      .map { toothpick.paperWorkDir.resolve("spigot/net/minecraft/server/$it.java") }
      .filter {
        val exists = it.exists()
        if (!it.exists()) logger.lifecycle("NMS ${it.nameWithoutExtension} is either missing, or is a new file added through a patch")
        exists
      }
      .map { it.nameWithoutExtension }
      .forEach(::importNMS)

    // Imports from mcdevimports.json
    val importsFile = toothpick.project.projectDir.resolve("mcdevimports.json")
    if (!importsFile.exists()) {
      importsFile.writeText(json.encodeToString(ImportsContainer()))
    }
    val imports: ImportsContainer = json.decodeFromString(importsFile.readText())
    imports.nmsImports.forEach(::importNMS)
    imports.libraryImports.forEach(::importLibrary)

    val add = gitCmd("add", ".", "-A", dir = upstreamServer).exitCode == 0
    val commit = gitCmd("commit", "-m", importLog.joinToString("\n"), dir = upstreamServer).exitCode == 0
    if (!add || !commit) {
      logger.lifecycle(">>> Didn't import any extra files")
    }
    logger.lifecycle(">>> Done importing mc-dev")
  }
}
