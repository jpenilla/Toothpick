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

internal object Constants {
  const val TASK_GROUP = "toothpick"
  const val INTERNAL_TASK_GROUP = "toothpick_internal"

  object Properties {
    const val NO_RELOCATE = "noRelocate"
    const val FAST = "fast"
  }

  object Repositories {
    const val MINECRAFT = "https://libraries.minecraft.net/"
    const val AIKAR = "https://repo.aikar.co/content/groups/aikar/"
    const val PAPER = "https://papermc.io/repo/repository/maven-public/"
  }

  object Dependencies {
    data class DependencyInfo(val groupId: String, val artifactId: String)
    val paperMinecraftServer = DependencyInfo("io.papermc", "minecraft-server")
    val paperMojangApi = DependencyInfo("com.destroystokyo.paper", "paper-mojangapi")
  }

  const val IMPORTS_CONFIGURATION_HEADER: String =
    "Extra mc-dev imports. Configure extra files to import from NMS or library sources."

  const val NMS_IMPORTS_COMMENT: String = """Example nms-imports

nms-imports=[
    "com.mojang.math.Quaternion",
    "net.minecraft.network.chat.IChatMutableComponent",
    "net.minecraft.world.entity.ai.goal.PathfinderGoalFishSchool",
]"""

  const val LIBRARY_IMPORTS_COMMENT: String = """Example library-imports

library-imports=[
    {
        file=Bicontravariant
        group="com.mojang"
        library=datafixerupper
        prefix="com/mojang/datafixers/optics/profunctors"
    },
    {
        file=CommandSyntaxException
        group="com.mojang"
        library=brigadier
        prefix="com/mojang/brigadier/exceptions"
    }
]
"""
}
