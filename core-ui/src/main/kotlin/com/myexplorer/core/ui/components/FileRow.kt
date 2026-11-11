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

package com.myexplorer.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myexplorer.core.model.FileCategory
import com.myexplorer.core.model.FileNode
import com.myexplorer.core.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_FORMAT = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())

private fun iconFor(category: FileCategory): ImageVector =
    when (category) {
        FileCategory.FOLDER -> Icons.Filled.Folder
        FileCategory.IMAGE -> Icons.Filled.Image
        FileCategory.VIDEO -> Icons.Filled.Movie
        FileCategory.AUDIO -> Icons.Filled.MusicNote
        FileCategory.DOCUMENT -> Icons.Filled.Description
        FileCategory.ARCHIVE -> Icons.Filled.FolderZip
        FileCategory.APK -> Icons.Filled.Android
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

/** Warna aksen kategori berkas: dipakai utk ikon (penuh) dan latar lingkaran ikon (alpha 0.12). */
@Composable
private fun categoryColor(category: FileCategory): Color =
    when (category) {
        FileCategory.FOLDER -> MaterialTheme.colorScheme.primary
        FileCategory.IMAGE -> MaterialTheme.colorScheme.tertiary
        FileCategory.VIDEO -> MaterialTheme.colorScheme.secondary
        FileCategory.AUDIO -> MaterialTheme.colorScheme.tertiary
        FileCategory.DOCUMENT -> MaterialTheme.colorScheme.secondary
        FileCategory.ARCHIVE -> MaterialTheme.colorScheme.secondary
        FileCategory.APK -> MaterialTheme.colorScheme.tertiary
        FileCategory.OTHER -> MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun subtitleFor(node: FileNode): String =
    if (node.isDirectory) {
        stringResource(R.string.core_folder)
    } else {
        node.humanSize() + " · " + DATE_FORMAT.format(Date(node.lastModified))
    }

@Composable
fun FileRow(
    node: FileNode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val badgeColor = categoryColor(node.category)
    val subtitle = subtitleFor(node)
    val label =
        buildFileRowLabel(
            isDirectory = node.isDirectory,
            name = node.name,
            subtitle = subtitle,
            typeWord = stringResource(if (node.isDirectory) R.string.core_folder else R.string.core_file),
        )
    val isSelected = selected
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp),
                ).clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    this.selected = isSelected
                }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(badgeColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(node.category),
                contentDescription = null,
                tint = badgeColor,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
