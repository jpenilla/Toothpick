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

  const val IMPORTS_CONFIGURATION_HEADER: String =
    "Extra mc-dev imports. Configure extra sources to import from NMS or decompiled libraries."

  const val NMS_IMPORTS_COMMENT: String = """Example nms-imports

nms-imports=[
    "com.mojang.math.Quaternion",
    "net.minecraft.network.syncher.DataWatcherRegistry",
    "net.minecraft.network.chat.IChatMutableComponent",
    "net.minecraft.network.protocol.game.PacketPlayOutAdvancements",
    "net.minecraft.world.entity.ai.goal.PathfinderGoalFishSchool",
    "net.minecraft.world.level.levelgen.feature.WorldGenFlowers"
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
