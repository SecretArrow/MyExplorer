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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myexplorer.core.model.FileNode
import com.myexplorer.core.ui.adaptive.WindowWidth
import com.myexplorer.core.ui.adaptive.browserLayoutFor
import com.myexplorer.core.ui.components.EmptyState
import com.myexplorer.core.ui.components.FileRow
import java.io.File

/**
 * Layar penjelajah berkas.
 *
 * @param windowWidth Lebar jendela yang diklasifikasi; COMPACT (default) mempertahankan
 *   daftar 1 kolom persis perilaku lama.
 * @param isFavorite Folder aktif sedang tersemat di widget "Akses cepat".
 * @param onToggleFavorite Aksi semat/lepas folder aktif; null (default) = ikon bintang
 *   tidak ditampilkan (perilaku lama utk pemanggil lain tetap utuh).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserState,
    hasStorageAccess: Boolean,
    onRequestStorageAccess: () -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onZip: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    windowWidth: WindowWidth = WindowWidth.COMPACT,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val layout = remember(windowWidth) { browserLayoutFor(windowWidth) }
    val ui by state.ui.collectAsState()
    val clipboard by state.clipboard.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var fabMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (ui.current == ui.root) {
                        Text(text = stringResource(R.string.browser_title))
                    } else {
                        BrowserBreadcrumb(root = ui.root, current = ui.current)
                    }
                },
                navigationIcon = {
                    if (ui.current != ui.root) {
                        IconButton(onClick = { state.up() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.browser_cd_up))
                        }
                    }
                },
                actions = {
                    if (clipboard != null) {
                        IconButton(onClick = { state.paste() }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.browser_cd_paste))
                        }
                    }
                    IconButton(onClick = { state.setSelectionMode(!ui.selectionMode) }) {
                        Icon(
                            if (ui.selectionMode) Icons.Filled.Close else Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.browser_cd_selection_mode),
                        )
                    }
                    if (onToggleFavorite != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription =
                                    stringResource(
                                        if (isFavorite) R.string.browser_cd_unfavorite else R.string.browser_cd_favorite,
                                    ),
                            )
                        }
                    }
                    if (onSearch != null) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.browser_cd_search))
                        }
                    }
                    IconButton(onClick = { state.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.browser_cd_refresh))
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { fabMenuOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.browser_fab_new))
                }
                DropdownMenu(
                    expanded = fabMenuOpen,
                    onDismissRequest = { fabMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.browser_new_folder)) },
                        onClick = {
                            fabMenuOpen = false
                            showNewFolderDialog = true
                        },
                        leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                    )
                }
            }
        },
        bottomBar = {
            if (ui.selectionMode && ui.selection.isNotEmpty()) {
                SelectionBar(
                    count = ui.selection.size,
                    onDelete = { state.deleteSelected() },
                    onCopy = { state.copySelected() },
                    onCut = { state.cutSelected() },
                    onZip = onZip.takeIf { hasStorageAccess },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!hasStorageAccess) {
                StorageAccessBanner(onRequest = onRequestStorageAccess)
            }
            when {
                ui.error != null -> EmptyState(message = stringResource(R.string.browser_error_read_folder))
                ui.items.isEmpty() -> EmptyState(message = stringResource(R.string.browser_empty_folder))
                else ->
                    if (layout.isGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(layout.gridColumns),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(ui.items, key = { it.path }) { node ->
                                FileRow(
                                    node = node,
                                    selected = node.path in ui.selection,
                                    onClick = {
                                        when {
                                            ui.selectionMode -> state.toggleSelection(node)
                                            node.isDirectory -> state.open(node)
                                            else -> onOpenFile(node)
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(ui.items, key = { it.path }) { node ->
                                FileRow(
                                    node = node,
                                    selected = node.path in ui.selection,
                                    onClick = {
                                        when {
                                            ui.selectionMode -> state.toggleSelection(node)
                                            node.isDirectory -> state.open(node)
                                            else -> onOpenFile(node)
                                        }
                                    },
                                )
                            }
                        }
                    }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(text = stringResource(R.string.browser_new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(text = stringResource(R.string.browser_folder_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) state.createFolder(folderName.trim())
                        folderName = ""
                        showNewFolderDialog = false
                    },
                ) { Text(text = stringResource(R.string.browser_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text(text = stringResource(R.string.browser_cancel)) }
            },
        )
    }
}

@Composable
private fun StorageAccessBanner(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.browser_storage_access_message),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRequest) { Text(text = stringResource(R.string.browser_allow)) }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onZip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.browser_cd_delete)) }
            IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.browser_cd_copy)) }
            IconButton(onClick = onCut) { Icon(Icons.Filled.ContentCut, contentDescription = stringResource(R.string.browser_cd_cut)) }
            if (onZip != null) {
                IconButton(onClick = onZip) {
                    Icon(Icons.Filled.FolderZip, contentDescription = stringResource(R.string.browser_cd_zip))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.browser_selected_count, count, count),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Jumlah segmen breadcrumb maksimum yang ditampilkan; segmen lebih tua diganti elipsis. */
private const val BREADCRUMB_MAX_SEGMENTS = 3

/**
 * Segmen nama folder dari [root] menuju [current] (urut dangkal -> dalam).
 * Total: nama kosong (mis. "/") dilewati; iterasi berhenti di [root] atau di puncak
 * sistem berkas, sehingga tidak pernah melempar galat untuk path apa pun.
 */
private fun pathSegments(
    root: File,
    current: File,
): List<String> {
    val segments = ArrayList<String>()
    var node: File? = current
    while (node != null && node != root) {
        val name = node.name
        if (name.isNotEmpty()) segments.add(name)
        node = node.parentFile
    }
    return segments.asReversed()
}

/**
 * Judul TopAppBar untuk sub-folder: breadcrumb ringkas (paling banyak
 * [BREADCRUMB_MAX_SEGMENTS] segmen terakhir, elipsis bila terpotong) yang dapat
 * digulir horizontal. Di path ROOT judul tetap "My Explorer" (lihat BrowserScreen).
 */
@Composable
private fun BrowserBreadcrumb(
    root: File,
    current: File,
    modifier: Modifier = Modifier,
) {
    val segments = remember(root, current) { pathSegments(root, current) }
    val visible = segments.takeLast(BREADCRUMB_MAX_SEGMENTS)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (segments.size > visible.size) {
            Text(
                text = stringResource(R.string.browser_breadcrumb_truncated),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            BreadcrumbChevron()
        }
        visible.forEachIndexed { index, segment ->
            if (index > 0) BreadcrumbChevron()
            Text(
                text = segment,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BreadcrumbChevron() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
    )
}
