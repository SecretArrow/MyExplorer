/*
 * My Explorer - a free and open source file manager for Android.
 * Copyright (C) 2025-2026 My Explorer contributors
 *
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.myexplorer.feature.browser

import com.myexplorer.core.model.FileNode
import com.myexplorer.data.local.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Dua BrowserState (pane kiri/kanan pada layout dua panel, satu repositori
 * bersama ala MainActivity) harus independen penuh: navigasi maupun pembuatan
 * folder pada satu pane tidak mengubah path dan daftar berkas pane lainnya.
 */
class BrowserStateIndependenceTest {
    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private fun newRepo(): FileRepository {
        // Recycle bin di luar folder root agar tidak ikut muncul dalam listing
        val trash = tempFolder.newFolder()
        return FileRepository(trash)
    }

    @Test
    fun `navigate in left pane keeps right pane path and list unchanged`() =
        runBlocking {
            val leftRoot = tempFolder.newFolder()
            val rightRoot = tempFolder.newFolder()
            File(leftRoot, "left-sub").mkdir()
            File(File(leftRoot, "left-sub"), "inner.txt").writeText("x")
            File(rightRoot, "right-sub").mkdir()
            val repo = newRepo()
            val left = BrowserState(repo, leftRoot, ioDispatcher = Dispatchers.Unconfined)
            val right = BrowserState(repo, rightRoot, ioDispatcher = Dispatchers.Unconfined)

            left.open(FileNode.from(File(leftRoot, "left-sub")))

            assertEquals(File(leftRoot, "left-sub"), left.ui.value.current)
            assertEquals(listOf("inner.txt"), left.ui.value.items.map { it.name })
            assertEquals(rightRoot, right.ui.value.current)
            assertEquals(listOf("right-sub"), right.ui.value.items.map { it.name })
            Unit
        }

    @Test
    fun `create folder in left pane keeps right pane path and list unchanged`() =
        runBlocking {
            val leftRoot = tempFolder.newFolder()
            val rightRoot = tempFolder.newFolder()
            File(rightRoot, "right-sub").mkdir()
            val repo = newRepo()
            val left = BrowserState(repo, leftRoot, ioDispatcher = Dispatchers.Unconfined)
            val right = BrowserState(repo, rightRoot, ioDispatcher = Dispatchers.Unconfined)

            left.createFolder("left-new")

            assertEquals(listOf("left-new"), left.ui.value.items.map { it.name })
            assertEquals(rightRoot, right.ui.value.current)
            assertEquals(listOf("right-sub"), right.ui.value.items.map { it.name })
            Unit
        }
}
