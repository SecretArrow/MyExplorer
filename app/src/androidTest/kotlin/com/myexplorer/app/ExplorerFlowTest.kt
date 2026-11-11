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

import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Uji e2e per area fitur utama: browser (buat folder lewat FAB, seleksi + hapus,
 * layar pencarian), tab Apps, hub Storage (analisis + vault), hub Network (lokasi
 * jaringan), layar server FTP, dan bagian About pada Settings.
 *
 * Semua literal UI disalin PERSIS dari `values/strings.xml` masing-masing modul
 * (lokal emulator CI = English): "New"/"New folder"/"Folder name"/"Create" (browser),
 * "Selection mode"/"Delete"/"Search" (ikon browser), "Settings"/"Apps"/"Storage"/
 * "Network" (tab bawah), "My Explorer 1.0.0" (settings_about_name_version dengan
 * versionName 1.0.0), "File or folder name" (search_query_hint), "Analysis"/"Vault"
 * (app_storage_section_*), "Storage analysis"/"Encrypted vault" (tools_*), "FTP
 * server"/"Locations"/"Sync" (app_network_section_* + transfer_title), "Inactive"/
 * "Port"/"Start" (transfer_*), "Saved connections"/"Add location" (network_*).
 *
 * Catatan alur nyata yang dipakai pengujian (sesuai kode, bukan asumsi):
 * - FileRow TIDAK punya handler long-press; mode seleksi diaktifkan lewat ikon
 *   "Selection mode" pada TopAppBar browser, lalu baris diklik untuk diseleksi.
 * - Penghapusan browser TIDAK memakai dialog konfirmasi (langsung pindah ke
 *   recycle bin internal), jadi tidak ada tombol konfirmasi yang perlu diklik.
 * - Seksi default hub Network adalah SERVER (FtpServerScreen) dan hub Storage
 *   adalah ANALYZER (StorageAnalyzerScreen).
 */
@RunWith(AndroidJUnit4::class)
class ExplorerFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Nama folder uji unik yang dibuat lewat UI pada eksekusi ini; dibersihkan di [After]. */
    private val createdFolders = mutableListOf<String>()

    /**
     * Best-effort: hapus folder uji lewat File API pada root penyimpanan eksternal.
     * Instrumentasi berjalan di proses aplikasi (UID sama) sehingga mewarisi izin
     * all-files; folder yang sudah terhapus lewat UI (pindah ke trash internal)
     * cukup diabaikan (deleteRecursively no-op).
     */
    @After
    fun deleteCreatedFolders() {
        val root = Environment.getExternalStorageDirectory() ?: return
        createdFolders.forEach { name ->
            runCatching { File(root, name).deleteRecursively() }
        }
        createdFolders.clear()
    }

    @Test
    fun testFabCreateFolder() {
        val name = createFolderThroughUi()

        // Baris folder baru muncul di daftar root (gulir bila perlu).
        scrollToText(name)
    }

    @Test
    fun testSelectAndDeleteFolder() {
        val name = createFolderThroughUi()
        scrollToText(name)

        // Aktifkan mode seleksi (FileRow tidak mendukung long-press), pilih baris,
        // lalu hapus lewat aksi Delete pada SelectionBar (langsung ke recycle bin).
        composeRule.onNodeWithContentDescription("Selection mode").performClick()
        composeRule.onNodeWithText(name).performClick()
        waitUntilContentDescriptionDisplayed("Delete")
        composeRule.onNodeWithContentDescription("Delete").performClick()

        waitUntilGone(name)
    }

    @Test
    fun testSettingsAbout() {
        composeRule.onNodeWithText("Settings").performClick()

        // Judul layar "Settings" + label tab bawah "Settings" -> minimal 2 node teks.
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().size >= 2
        }

        // Baris About: "My Explorer %1$s" dengan versionName 1.0.0; kartu bisa
        // berada di bawah lipatan daftar sehingga digulir bila perlu.
        scrollToText("My Explorer 1.0.0")
    }

    @Test
    fun testSearchScreen() {
        composeRule.onNodeWithContentDescription("Search").performClick()

        // Layar pencarian penuh: judul + kolom kueri (label search_query_hint).
        waitUntilDisplayed("Search")
        waitUntilDisplayed("File or folder name")

        composeRule.onNode(hasSetTextAction()).performTextInput("zzznomatch")

        // Kueri tersimpan di kolom dan layar tetap tampil (tanpa crash).
        composeRule.onNodeWithText("zzznomatch").assertIsDisplayed()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun testAppsTab() {
        composeRule.onNodeWithText("Apps").performClick()

        // Judul layar "Apps" (apps_title) + label tab bawah "Apps" -> minimal 2 node;
        // di tab browser hanya ada 1 node "Apps" sehingga ini membuktikan tab terbuka.
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Apps").fetchSemanticsNodes().size >= 2
        }
    }

    @Test
    fun testStorageHub() {
        composeRule.onNodeWithText("Storage").performClick()

        // Segmen hub + judul seksi default (StorageAnalyzerScreen).
        waitUntilDisplayed("Analysis")
        waitUntilDisplayed("Vault")
        waitUntilDisplayed("Storage analysis")

        // Pindah ke seksi vault (sekarang "Vault" unik karena VaultScreen belum
        // di-compose) lalu tunggu judulnya.
        composeRule.onNodeWithText("Vault").performClick()
        waitUntilDisplayed("Encrypted vault")
    }

    @Test
    fun testNetworkHub() {
        composeRule.onNodeWithText("Network").performClick()

        // Tiga seksi hub; "FTP server" muncul dua kali (segmen + judul layar FTP),
        // cukup periksa salah satunya tampil.
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            try {
                composeRule.onAllNodesWithText("FTP server").onFirst().assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        waitUntilDisplayed("Locations")
        waitUntilDisplayed("Sync")

        // Buka seksi Locations: daftar sambungan tersimpan (kosong di instalasi baru).
        composeRule.onNodeWithText("Locations").performClick()
        waitUntilDisplayed("Saved connections")
        waitUntilContentDescriptionDisplayed("Add location")
    }

    @Test
    fun testFtpServerScreen() {
        composeRule.onNodeWithText("Network").performClick()

        // Seksi default SERVER = FtpServerScreen: kartu status nonaktif, kolom port,
        // dan tombol mulai (digulir bila perlu pada layar kecil).
        waitUntilDisplayed("Inactive")
        waitUntilDisplayed("Port")
        scrollToText("Start")
    }

    /**
     * Buat folder lewat UI aplikasi sendiri: FAB "New" -> menu "New folder" ->
     * dialog: isi kolom "Folder name" dengan nama unik -> tombol "Create".
     * Nama dicatat agar bisa dibersihkan di [deleteCreatedFolders].
     */
    private fun createFolderThroughUi(): String {
        val name = "E2EFolder" + System.currentTimeMillis()

        composeRule.onNodeWithContentDescription("New").performClick()
        composeRule.onNodeWithText("New folder").performClick()
        waitUntilDisplayed("Folder name")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        composeRule.onNodeWithText("Create").performClick()

        createdFolders.add(name)
        return name
    }

    /** Polling sampai node dengan teks persis [text] tampil; AssertionError ditelan lalu dicoba lagi. */
    private fun waitUntilDisplayed(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            try {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /** Polling sampai node ikon dengan contentDescription [description] tampil. */
    private fun waitUntilContentDescriptionDisplayed(description: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            try {
                composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /** Polling sampai node dengan teks [text] hilang dari pohon semantik (mis. baris terhapus). */
    private fun waitUntilGone(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            try {
                composeRule.onNodeWithText(text).assertDoesNotExist()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /**
     * Tunggu teks tampil; bila belum, gulir container bergulir satu layar lalu
     * periksa lagi. Item LazyColumn di luar viewport belum di-compose sehingga
     * harus digulir masuk dulu sebelum bisa ditemukan; layar dengan verticalScroll
     * biasa (mis. FTP) meng-compose seluruh anaknya, tinggal digulir agar terlihat.
     */
    private fun scrollToText(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            try {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                try {
                    composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
                } catch (_: AssertionError) {
                    // Tidak ada (atau lebih dari satu) node bergulir; lanjut polling.
                }
                false
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
