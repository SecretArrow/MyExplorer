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

package com.myexplorer.app

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * Memberi akses semua berkas (appops MANAGE_EXTERNAL_STORAGE) dari dalam
 * instrumentasi, SEBELUM aktivitas diluncurkan oleh compose rule (pemanggilan
 * harus lewat [org.junit.ClassRule] agar berjalan mendahului ActivityScenario).
 *
 * Ini menggantikan `adb install` + `appops set` manual di workflow: instalasi
 * manual terbukti KONFLIK dengan instalasi AGP/UTP (INSTALL_FAILED_UPDATE_
 * INCOMPATIBLE) yang ditelan UTP sebagai BUILD SUCCESSFUL — akibatnya test
 * instrumented app TIDAK PERNAH benar-benar berjalan pada rilis sebelumnya
 * (false green; lihat tools/verify_e2e.py yang kini mencegahnya terulang).
 *
 * UiAutomation menjalankan perintah shell sebagai UID shell sehingga appops
 * diterapkan tanpa interaksi pengguna; MainActivity kemudian melihat
 * Environment.isExternalStorageManager() == true saat diluncurkan.
 */
class StorageAccessRule : ExternalResource() {
    override fun before() {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("appops set com.myexplorer.app MANAGE_EXTERNAL_STORAGE allow")
            .close()
    }
}
