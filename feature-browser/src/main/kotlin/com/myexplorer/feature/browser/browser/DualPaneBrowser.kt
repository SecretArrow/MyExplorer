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

package com.myexplorer.feature.browser.browser

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myexplorer.core.model.FileNode
import com.myexplorer.core.ui.adaptive.WindowWidth
import com.myexplorer.feature.browser.BrowserScreen
import com.myexplorer.feature.browser.BrowserState

/** Indeks pane kiri pada layout dua panel. */
private const val PANE_LEFT = 0

/** Indeks pane kanan pada layout dua panel. */
private const val PANE_RIGHT = 1

/**
 * Browser dua panel utk layar lebar (WindowWidth.MEDIUM/EXPANDED): dua instance
 * [BrowserState] independen dirender berdampingan, dipisah pembatas vertikal.
 * Pane AKTIF (terakhir disentuh) diberi bingkai primary 2.dp; tiap pane punya
 * FAB sendiri yang bekerja pada state pane tersebut, sehingga aksi pembuatan
 * selalu menunjuk pane yang terlihat aktif.
 *
 * @param leftState state browser pane kiri.
 * @param rightState state browser pane kanan (independen penuh dari pane kiri).
 * @param onZip dipanggil dgn state pane pengguna; null = aksi ZIP disembunyikan.
 * @param isFavorite apakah path folder aktif sebuah pane tersemat di akses cepat.
 * @param onToggleFavorite aksi semat/lepas path folder aktif pane; null = ikon
 *   bintang tidak ditampilkan (perilaku sama dgn BrowserScreen).
 */
@Composable
fun DualPaneBrowser(
    leftState: BrowserState,
    rightState: BrowserState,
    hasStorageAccess: Boolean,
    onRequestStorageAccess: () -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onZip: ((BrowserState) -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    isFavorite: (String) -> Boolean = { false },
    onToggleFavorite: ((String) -> Unit)? = null,
) {
    val leftUi by leftState.ui.collectAsState()
    val rightUi by rightState.ui.collectAsState()
    var activePane by remember { mutableStateOf(PANE_LEFT) }

    Row(modifier = Modifier.fillMaxSize()) {
        BrowserPane(
            isActive = activePane == PANE_LEFT,
            onActivate = { activePane = PANE_LEFT },
            modifier = Modifier.weight(1f),
        ) {
            BrowserScreen(
                state = leftState,
                hasStorageAccess = hasStorageAccess,
                onRequestStorageAccess = onRequestStorageAccess,
                onOpenFile = onOpenFile,
                onZip = onZip?.let { onPaneZip -> { onPaneZip(leftState) } },
                onSearch = onSearch,
                // Tiap pane selebar layar ponsel -> daftar 1 kolom (perilaku klasik dua panel).
                windowWidth = WindowWidth.COMPACT,
                isFavorite = isFavorite(leftUi.current.path),
                onToggleFavorite = onToggleFavorite?.let { onPaneToggle -> { onPaneToggle(leftUi.current.path) } },
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
        BrowserPane(
            isActive = activePane == PANE_RIGHT,
            onActivate = { activePane = PANE_RIGHT },
            modifier = Modifier.weight(1f),
        ) {
            BrowserScreen(
                state = rightState,
                hasStorageAccess = hasStorageAccess,
                onRequestStorageAccess = onRequestStorageAccess,
                onOpenFile = onOpenFile,
                onZip = onZip?.let { onPaneZip -> { onPaneZip(rightState) } },
                onSearch = onSearch,
                windowWidth = WindowWidth.COMPACT,
                isFavorite = isFavorite(rightUi.current.path),
                onToggleFavorite = onToggleFavorite?.let { onPaneToggle -> { onPaneToggle(rightUi.current.path) } },
            )
        }
    }
}

/**
 * Wadah satu pane: bingkai 2.dp berwarna primary bila [isActive], transparan bila
 * tidak, supaya pengguna selalu tahu pane mana yang aktif. Sentuhan pada area
 * non-interaktif pane mengaktifkannya (elemen interaktif di dalam pane tetap
 * memproses sentuhannya sendiri).
 */
@Composable
private fun BrowserPane(
    isActive: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .border(
                    width = 2.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                ).clickable(onClick = onActivate),
    ) {
        content()
    }
}
