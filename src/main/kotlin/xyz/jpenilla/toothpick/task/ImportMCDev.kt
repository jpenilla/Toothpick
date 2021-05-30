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

import io.leangen.geantyref.GenericTypeReflector.erase
import org.gradle.api.tasks.TaskAction
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import xyz.jpenilla.toothpick.Constants
import xyz.jpenilla.toothpick.ensureSuccess
import xyz.jpenilla.toothpick.gitCmd
import java.io.File

internal data class ImportsContainer(
  @Comment(Constants.NMS_IMPORTS_COMMENT) val nmsImports: Set<String> = emptySet(),
  @Comment(Constants.LIBRARY_IMPORTS_COMMENT) val libraryImports: Set<LibraryImport> = emptySet()
)

internal data class LibraryImport(val group: String, val library: String, val prefix: String, val file: String)

public abstract class ImportMCDev : ToothpickInternalTask() {
  private val upstreamServer
    get() = toothpick.serverProject.baseDir
  private val importLog = ArrayList<String>()

  private fun importNMS(className: String) {
    logger.lifecycle("Importing $className")
    val classPath = "${className.replace(".", "/")}.java"
    val source = toothpick.paperDecompDir.resolve("spigot/$classPath")
    if (!source.exists()) error("Missing NMS: $className")
    val target = upstreamServer.resolve("src/main/java/$classPath")
    if (isDuplicateImport(target, className)) return
    target.parentFile.mkdirs()
    source.copyTo(target)
    importLog.add("Imported $className")
  }

  private fun importLibrary(import: LibraryImport) {
    val (group, lib, prefix, file) = import
    val className = "${prefix.replace("/", ".")}.$file"
    logger.lifecycle("Importing $className from $group.$lib")
    val source = toothpick.paperDecompDir.resolve("libraries/$group/$lib/$prefix/$file.java")
    if (!source.exists()) error("Missing Base: $lib $prefix/$file")
    val targetDir = upstreamServer.resolve("src/main/java/$prefix")
    val target = targetDir.resolve("$file.java")
    if (isDuplicateImport(target, className)) return
    targetDir.mkdirs()
    source.copyTo(target)
    importLog.add("Imported $className from $group.$lib")
  }

  private fun isDuplicateImport(target: File, className: String): Boolean {
    if (!target.exists()) return false
    val message =
      "Skipped import for $className, a class with that name already exists in the source tree. Is there an extra entry in mcdevimports.conf?"
    project.gradle.buildFinished {
      logger.warn(message)
    }
    logger.warn(message)
    return true
  }

  private fun import(imports: ImportsContainer) {
    imports.nmsImports.forEach { importNMS(it) }
    imports.libraryImports.forEach { importLibrary(it) }
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

    // Get server patches
    val patches = toothpick.serverProject.patchesDir.listFiles()?.toList() ?: emptyList()

    // If we have debug enabled, run it before importing anything
    if (project.hasProperty("importMCDevDebug")) {
      dumpPossibleImports()
      dumpNeededImports(patches)
    }

    // Imports needed to apply patches
    import(findNeededImports(patches))

    // Extra imports from mcdevimports.conf
    loadImportsConfiguration()?.run {
      import(this)
    }

    val commitMessage = mutableListOf("Extra mc-dev imports", "")
      .apply { addAll(importLog) }
      .joinToString("\n", limit = 1000, truncated = "... commit message truncated due to excessive size")
    val add = gitCmd("add", ".", "-A", dir = upstreamServer).exitCode == 0
    val commit = gitCmd("commit", "-m", commitMessage, dir = upstreamServer).exitCode == 0
    if (!add || !commit) {
      logger.lifecycle(">>> Didn't import any extra files")
    }
    logger.lifecycle(">>> Done importing mc-dev")
  }

  private fun findNeededImports(patches: List<File>): ImportsContainer {
    val lines = patches.asSequence()
      .flatMap { it.readLines().asSequence() }
      .filter { line -> line.startsWith("+++ b/src/main/java/") }
      .toList()
    return ImportsContainer(
      findNeededNMSImports(lines),
      findNeededLibraryImports(lines)
    )
  }

  private fun findPossibleNMSImports(): Set<String> {
    val spigotDir = toothpick.paperDecompDir.resolve("spigot")
    val spigotDirPath = spigotDir.path
    return spigotDir.walk()
      .filter { it.isFile && it.name.endsWith(".java") }
      .map {
        it.path.substringAfter(spigotDirPath)
          .substring(1).replace(".java", "").replace("/", ".")
      }
      .toSet()
  }

  private fun findNeededNMSImports(patchLines: List<String>): Set<String> = patchLines.asSequence()
    .filter { line ->
      line.startsWith("+++ b/src/main/java/net/minecraft/")
        || line.startsWith("+++ b/src/main/java/com/mojang/math/")
    }
    .distinct()
    .map { it.substringAfter("+++ b/src/main/java/") }
    .filter { !upstreamServer.resolve("src/main/java/$it").exists() }
    .filter {
      val sourceFile = toothpick.paperDecompDir.resolve("spigot/$it")
      val exists = sourceFile.exists()
      if (!sourceFile.exists()) logger.lifecycle("$it is either missing, or is a new file added through a patch")
      exists
    }
    .map { it.replace("/", ".").substringBefore(".java") }
    .toSet()

  private fun findPossibleLibraryImports(): Set<LibraryImport> {
    val librariesDir = toothpick.paperDecompDir.resolve("libraries")
    val knownLibraryImports = HashSet<LibraryImport>()
    val groups = librariesDir.listFiles() ?: error("Unable to list library groups")
    for (groupFolder in groups) {
      val group = groupFolder.name
      val libraries = groupFolder.listFiles() ?: error("Unable to list libraries in group '$group'")
      for (libraryFolder in libraries) {
        val library = libraryFolder.name
        libraryFolder.walk()
          .filter { it.isFile && it.name.endsWith(".java") }
          .map { sourceFile ->
            val prefix = sourceFile.relativeTo(libraryFolder).path.replace('\\', '/').substringBeforeLast('/')
            LibraryImport(group, library, prefix, sourceFile.nameWithoutExtension)
          }
          .toCollection(knownLibraryImports)
      }
    }
    return knownLibraryImports
  }

  private fun findNeededLibraryImports(patchLines: List<String>): Set<LibraryImport> {
    val knownImportMap = findPossibleLibraryImports()
      .associateBy { "${it.prefix}/${it.file}.java" }
    return patchLines.asSequence()
      .distinct()
      .map { it.substringAfter("+++ b/src/main/java/") }
      .map { knownImportMap[it] }
      .filterNotNull()
      .filter { !upstreamServer.resolve("src/main/java/${it.prefix}/${it.file}.java").exists() }
      .toSet()
  }

  private fun createImportsConfigurationLoader(file: File): HoconConfigurationLoader =
    HoconConfigurationLoader.builder().apply {
      file(file)
      defaultOptions { options ->
        options
          .header(Constants.IMPORTS_CONFIGURATION_HEADER)
          .serializers { builder ->
            builder.register(
              { with(erase(it)) { kotlin.isData || isAnnotationPresent(ConfigSerializable::class.java) } },
              objectMapperFactory().asTypeSerializer()
            )
          }
      }
    }.build()

  private fun loadImportsConfiguration(): ImportsContainer? {
    val importsFile = toothpick.project.projectDir.resolve("mcdevimports.conf")
    val loader = createImportsConfigurationLoader(importsFile)
    val extraImports = try {
      loader.load().get(ImportsContainer())
    } catch (ex: ConfigurateException) {
      logger.warn("Failed to read mcdevimports.conf. Ensure you do not have syntax errors.", ex)
      return null
    }
    val node = loader.createNode().set(extraImports)
    loader.save(node)
    return extraImports
  }

  private fun dumpPossibleImports() {
    val dumpFile = toothpick.project.projectDir.resolve("mcdevimports-possible-dump.conf")
    val allImports = ImportsContainer(
      findPossibleNMSImports(),
      findPossibleLibraryImports()
    )
    val loader = createImportsConfigurationLoader(dumpFile)
    loader.save(loader.createNode().set(allImports))
  }

  private fun dumpNeededImports(patches: List<File>) {
    val dumpFile = toothpick.project.projectDir.resolve("mcdevimports-needed-dump.conf")
    val allImports = findNeededImports(patches)
    val loader = createImportsConfigurationLoader(dumpFile)
    loader.save(loader.createNode().set(allImports))
  }
}
