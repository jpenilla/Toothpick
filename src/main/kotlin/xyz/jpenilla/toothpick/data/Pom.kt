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
package xyz.jpenilla.toothpick.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class MavenPom(
  val properties: Map<String, String> = emptyMap(),
  val repositories: List<Repository> = emptyList(),
  val dependencies: List<Dependency> = emptyList(),
  val dependencyManagement: DependencyManagement?,
  val build: BuildSection
)

internal data class DependencyManagement(
  val dependencies: List<Dependency> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Repository(
  val id: String,
  val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Dependency(
  val groupId: String,
  val artifactId: String,
  val version: String?,
  val scope: String?,
  val classifier: String?,
  val type: String?,
  val exclusions: List<Dependency> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class BuildSection(
  val plugins: List<MavenPlugin> = emptyList()
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "artifactId",
  defaultImpl = MavenPlugin::class,
  visible = true
)
@JsonSubTypes(
  Type(ShadePlugin::class, name = ShadePlugin.PLUGIN_NAME),
  Type(JavadocPlugin::class, name = JavadocPlugin.PLUGIN_NAME)
)
@JsonIgnoreProperties(ignoreUnknown = true)
internal open class MavenPlugin(val artifactId: String) {
  override fun toString(): String = "MavenPlugin(artifactId=$artifactId)"
}
