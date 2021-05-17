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

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.project
import xyz.jpenilla.toothpick.data.Dependency

internal fun RepositoryHandler.loadRepositories(subproject: ToothpickSubproject) {
  // Load repositories
  val mavenPom = subproject.pom ?: return
  for (repo in mavenPom.repositories) {
    maven(repo.url)
  }
}

internal fun DependencyHandlerScope.loadDependencies(subproject: ToothpickSubproject) {
  // Load dependencies
  val mavenPom = subproject.pom ?: return
  mavenPom.dependencyManagement?.dependencies?.forEach { dependency ->
    loadDependency(subproject, dependency)
  }
  mavenPom.dependencies.forEach { dependency ->
    loadDependency(subproject, dependency)
  }
}

@Suppress("unused_variable")
private fun DependencyHandlerScope.loadDependency(subproject: ToothpickSubproject, dependency: Dependency) {
  val project = subproject.project
  val (groupId, artifactId, version, scope, classifier, type, exclusions) = dependency

  val dependencyString = listOfNotNull(groupId, artifactId, version, classifier).joinToString(":")
  project.logger.debug("Read $scope scope dependency '$dependencyString' from '${project.name}' pom.xml")

  // Special case API
  if (artifactId == subproject.toothpick.apiProject.project.name
    || artifactId == "${subproject.toothpick.upstreamLowercase}-api"
  ) {
    if (project == subproject.toothpick.serverProject.project) {
      "api"(project(subproject.toothpick.apiProject.project.path))
    }
    return
  }

  when (scope) {
    "import" -> "api"(platform(dependencyString))
    "compile", null -> {
      "api"(dependencyString)
      if (version != null) {
        "annotationProcessor"(dependencyString)
      }
    }
    "provided" -> {
      "compileOnly"(dependencyString)
      "testImplementation"(dependencyString)
      "annotationProcessor"(dependencyString)
    }
    "runtime" -> "runtimeOnly"(dependencyString)
    "test" -> "testImplementation"(dependencyString)
  }
}
