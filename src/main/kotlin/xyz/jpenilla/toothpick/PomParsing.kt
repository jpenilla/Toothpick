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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.gradle.api.Project

internal fun Project.parsePom(): JsonNode? {
  val file = file("pom.xml")
  if (!file.exists()) {
    return null
  }
  val contents = file.readText()

  val mapper = XmlMapper.builder().build()
  val parsed = mapper.readTree(contents)
  val propertiesMap = parsed.path("properties").fields().asSequence().associateBy({ it.key }, { it.value.textValue() }).toMutableMap()

  propertiesMap["project.version"] = project.version.toString()
  propertiesMap["minecraft.version"] = toothpick.minecraftVersion
  propertiesMap["minecraft_version"] = toothpick.nmsPackage

  return mapper.readTree(contents.replaceProperties(propertiesMap))
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
