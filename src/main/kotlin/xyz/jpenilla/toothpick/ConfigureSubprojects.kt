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

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.withType
import xyz.jpenilla.toothpick.data.JavadocPlugin
import xyz.jpenilla.toothpick.data.ShadePlugin
import xyz.jpenilla.toothpick.shadow.ModifiedLog4j2PluginsCacheFileTransformer
import xyz.jpenilla.toothpick.shadow.ToothpickRelocator
import java.text.SimpleDateFormat
import java.util.Date

internal fun Project.commonSubprojectConfiguration() {
  apply<JavaLibraryPlugin>()
  apply<MavenPublishPlugin>()

  tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
  }
  tasks.withType<Javadoc> {
    options.encoding = Charsets.UTF_8.name()
  }
}

private fun Project.setupPublication(subproject: ToothpickSubproject) {
  extensions.configure<PublishingExtension> {
    publications {
      create<MavenPublication>("mavenJava") {
        groupId = project.group as String
        version = project.version as String
        pom {
          name.set(project.name)
          url.set(subproject.toothpick.forkUrl)
        }
      }
    }
  }
}

private fun Project.configurePublication(configurer: MavenPublication.() -> Unit) {
  extensions.configure<PublishingExtension> {
    publications {
      getByName<MavenPublication>("mavenJava").apply(configurer)
    }
  }
}

internal fun Project.configureServerProject(subproject: ToothpickSubproject) {
  apply<ShadowPlugin>()
  setupPublication(subproject)

  val generatePomFileForMavenJavaPublication by tasks.getting(GenerateMavenPom::class) {
    destination = project.buildDir.resolve("tmp/pom.xml")
  }

  tasks.withType<Test> {
    // didn't bother to look into why these fail. paper excludes them in paperweight as well though
    exclude("org/bukkit/craftbukkit/inventory/ItemStack*Test.class")
  }

  tasks.getByName("jar", Jar::class) {
    archiveClassifier.set("unshaded")
  }

  val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("") // ShadowJar is the main server artifact
    dependsOn(generatePomFileForMavenJavaPublication)
    transform(ModifiedLog4j2PluginsCacheFileTransformer::class.java)
    mergeServiceFiles()

    manifest {
      attributes(
        "Main-Class" to "org.bukkit.craftbukkit.Main",
        "Implementation-Title" to "CraftBukkit",
        "Implementation-Version" to subproject.toothpick.forkVersion,
        "Implementation-Vendor" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date()),
        "Specification-Title" to "Bukkit",
        "Specification-Version" to project.version.toString(),
        "Specification-Vendor" to "Bukkit Team",
        "Multi-Release" to true
      )
    }

    from(project.buildDir.resolve("tmp/pom.xml")) {
      // dirty hack to make "java -Dpaperclip.install=true -jar paperclip.jar" work without forking paperclip
      into("META-INF/maven/io.papermc.paper/paper")
    }

    if (hasProperty(Constants.Properties.NO_RELOCATE)) return@getting
    // Import relocations from server pom
    val pom = subproject.pom ?: return@getting
    val shadePlugin = pom.build.plugins.filterIsInstance<ShadePlugin>().firstOrNull() ?: error("Could not find shade plugin in server pom!")
    for (relocation in shadePlugin.executions.first().configuration.relocations) {
      val (pattern, shadedPattern, rawString, includes, excludes) = relocation
      val modifiedExcludes = excludes.toMutableList()
      if (rawString) modifiedExcludes.add("net/minecraft/data/Main*")
      relocate(ToothpickRelocator(pattern, shadedPattern, rawString, includes, modifiedExcludes))
    }
  }

  tasks.getByName("build") {
    dependsOn(shadowJar)
  }

  extensions.configure<JavaPluginExtension> {
    withSourcesJar()
  }

  configurePublication {
    artifactId = subproject.toothpick.forkNameLowercase
    artifact(tasks["shadowJar"])
    artifact(tasks["sourcesJar"])
  }
}

internal fun Project.configureApiProject(subproject: ToothpickSubproject) {
  tasks.withType<Jar> {
    doFirst {
      buildDir.resolve("tmp/pom.properties")
        .writeText("version=${project.version}")
    }
    from(buildDir.resolve("tmp/pom.properties")) {
      into("META-INF/maven/${project.group}/${project.name}")
    }
    manifest {
      attributes("Automatic-Module-Name" to "org.bukkit")
    }
  }

  tasks.withType<Javadoc> {
    options {
      if (this !is StandardJavadocDocletOptions) return@options

      val currentVersion = JavaVersion.current().majorVersion.toInt()

      links(jdkApiDocs(currentVersion))

      if (currentVersion in 9..11) {
        // Apply workaround for https://bugs.openjdk.java.net/browse/JDK-8215291
        // Fixes search links, but breaks external doc links which use modules. Fixed in JDK 12+.
        val noModuleDirectories = addBooleanOption("-no-module-directories")
        noModuleDirectories.value = true
      }

      val pom = subproject.pom ?: return@options
      val javadocPlugin = pom.build.plugins.filterIsInstance<JavadocPlugin>().firstOrNull() ?: return@options
      for (link in javadocPlugin.configuration.links) {
        links(link)
      }
    }
  }

  extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
  }

  setupPublication(subproject)
  configurePublication {
    artifactId = project.name
    from(components["java"])
  }
}

private fun jdkApiDocs(javaVersion: Int): String = if (javaVersion >= 11) {
  "https://docs.oracle.com/en/java/javase/$javaVersion/docs/api"
} else {
  "https://docs.oracle.com/javase/$javaVersion/docs/api"
}
