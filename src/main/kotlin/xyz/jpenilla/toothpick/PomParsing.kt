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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.Project

private val mapper = XmlMapper.builder()
  .addModule(kotlinModule())
  .build()

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class MavenPom(
  val properties: Map<String, String> = emptyMap(),
  val repositories: List<DeclaredRepository> = emptyList(),
  val dependencies: List<DeclaredDependency> = emptyList(),
  val dependencyManagement: DependencyManagement?,
  val build: BuildSection
)

internal data class DependencyManagement(
  val dependencies: List<DeclaredDependency> = emptyList()
)

internal data class DeclaredRepository(
  val id: String,
  val url: String
)

internal data class DeclaredDependency(
  val groupId: String,
  val artifactId: String,
  val version: String?,
  val scope: String?,
  val classifier: String?,
  val type: String?,
  val exclusions: List<DeclaredDependency> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class BuildSection(
  val plugins: List<MavenPlugin> = emptyList()
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "artifactId",
  defaultImpl = MavenPlugin::class
)
@JsonSubTypes(Type(ShadePlugin::class, name = "maven-shade-plugin"))
@JsonIgnoreProperties(ignoreUnknown = true)
internal open class MavenPlugin

internal data class ShadePlugin(
  val executions: List<ShadeExecution> = emptyList(),
  val configuration: ShadeConfiguration = ShadeConfiguration()
) : MavenPlugin()

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ShadeConfiguration(
  val relocations: List<Relocation> = emptyList(),
  val filters: List<Filter> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ShadeExecution(
  val phase: String,
  val configuration: ShadeConfiguration = ShadeConfiguration()
)

internal data class Filter(
  val artifact: String,
  val excludes: List<String> = emptyList()
)

internal data class Relocation(
  val pattern: String,
  val shadedPattern: String,
  val rawString: Boolean = false,
  val excludes: List<String> = emptyList()
)

internal fun Project.parsePomToTree(): JsonNode? {
  val pomContents = readPom() ?: return null
  return mapper.readTree(pomContents)
}

internal fun Project.parsePom(): MavenPom? {
  val pomContents = readPom() ?: return null
  return mapper.readValue<MavenPom>(pomContents)
}

private fun Project.readPom(): String? {
  val file = file("pom.xml")
  if (!file.exists()) {
    return null
  }
  val contents = file.readText()

  val propertiesMap = mapper.readValue<MavenPom>(contents).properties.toMutableMap()

  propertiesMap["project.version"] = project.version.toString()
  propertiesMap["minecraft.version"] = toothpick.minecraftVersion
  propertiesMap["minecraft_version"] = toothpick.nmsPackage

  return contents.replaceProperties(propertiesMap)
}

private fun String.replaceProperties(
  properties: Map<String, String>
): String {
  var result = this
  for ((key, value) in properties) {
    result = result.replace("\${$key}", value)
  }
  return result
}
