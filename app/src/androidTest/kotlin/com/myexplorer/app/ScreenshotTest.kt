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

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Menangkap tangkapan layar seluruh layar utama untuk dokumentasi rilis.
 *
 * Test ini HANYA dijalankan bila workflow memintanya lewat argumen instrumentation
 * `screenshots=true` (lihat .github/workflows/screenshots.yml); pada eksekusi e2e
 * biasa test ini di-skip lewat asumsi agar perubahan `wm size` / kepadatan /
 * mode malam tidak bocor ke test lain.
 *
 * Alur: ponsel portrait terang (browser, menu FAB, dialog folder baru, pencarian,
 * tab Apps/Storage/Network/Settings) -> dual-pane landscape (wm size 1920x1080,
 * density 240 => lebar 1280dp >= 600dp) terang -> dual-pane gelap -> ponsel
 * portrait gelap. Hasil PNG ditulis ke /sdcard/MyExplorerScreenshots lalu
 * ditarik workflow sebagai artifact.
 *
 * Catatan: setelah perubahan wm/uimode tidak dilakukan assert semantik —
 * aktivitas direkreasi dan pohon semantik rebuild; test hanya menunggu settle
 * (state aplikasi dipulihkan otomatis, tab aktif tetap Files).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var shotDir: File

    @Before
    fun onlyWhenRequested() {
        assumeTrue(
            "Berjalan hanya dengan argumen instrumentation screenshots=true",
            InstrumentationRegistry.getArguments().getString("screenshots") == "true",
        )
        shotDir = File(Environment.getExternalStorageDirectory(), SCREEN_DIR)
        check(shotDir.mkdirs() || shotDir.isDirectory) { "Tidak bisa membuat $shotDir" }
    }

    @After
    fun restoreDeviceState() {
        // Best-effort: emulator ephemeral per-run, tapi kembalikan agar aman bila
        // workflow menambah test lain setelah class ini.
        runCatching { shell("wm size reset") }
        runCatching { shell("wm density reset") }
        runCatching { shell("cmd uimode night no") }
    }

    @Test
    fun captureAllScreens() {
        // --- Ponsel portrait, tema terang ---
        waitUntilContentDescriptionDisplayed("New")
        capture("01-files")

        composeRule.onNodeWithContentDescription("New").performClick()
        waitUntilDisplayed("New folder")
        capture("02-fab-new-menu")

        composeRule.onNodeWithText("New folder").performClick()
        waitUntilDisplayed("Folder name")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("My Folder")
        capture("03-new-folder-dialog")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithContentDescription("Search").performClick()
        waitUntilDisplayed("File or folder name")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("down")
        capture("04-search")
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitUntilContentDescriptionDisplayed("New")

        composeRule.onNodeWithText("Apps").performClick()
        waitUntilTabShown("Apps")
        capture("05-apps")

        composeRule.onNodeWithText("Storage").performClick()
        waitUntilDisplayed("Storage analysis")
        capture("06-storage-analysis")

        composeRule.onNodeWithText("Vault").performClick()
        waitUntilDisplayed("Encrypted vault")
        capture("07-storage-vault")

        composeRule.onNodeWithText("Network").performClick()
        waitUntilDisplayed("Inactive")
        capture("08-network-ftp")

        composeRule.onNodeWithText("Locations").performClick()
        waitUntilDisplayed("Saved connections")
        capture("09-network-locations")

        composeRule.onNodeWithText("Settings").performClick()
        waitUntilTabShown("Settings")
        scrollToText("About")
        capture("10-settings-about")

        // --- Dual-pane landscape (>=600dp), tema terang ---
        composeRule.onNodeWithText("Files").performClick()
        waitUntilContentDescriptionDisplayed("New")
        shell("wm size 1920x1080")
        shell("wm density 240")
        Thread.sleep(RESIZE_SETTLE_MILLIS)
        capture("11-dualpane-landscape")

        // --- Dual-pane landscape, mode gelap ---
        shell("cmd uimode night yes")
        Thread.sleep(UIMODE_SETTLE_MILLIS)
        capture("12-dualpane-dark")

        // --- Kembali ke ponsel portrait, tetap mode gelap ---
        shell("wm size reset")
        shell("wm density reset")
        Thread.sleep(RESIZE_SETTLE_MILLIS)
        capture("13-files-dark")
    }

    /**
     * Simpan tangkapan layar penuh (UiAutomation mengambil display aktual,
     * termasuk dialog) sebagai PNG di [shotDir]; tunggu compose idle dulu agar
     * transisi selesai dan konten stabil.
     */
    private fun capture(name: String) {
        runCatching { composeRule.waitForIdle() }
        Thread.sleep(CAPTURE_SETTLE_MILLIS)
        val bitmap = composeRule.instrumentation.uiAutomation.takeScreenshot()
        val target = File(shotDir, "$name.png")
        FileOutputStream(target).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Gagal menulis ${target.path}"
            }
        }
    }

    /** Jalankan perintah shell sebagai UID shell (wm/cmd) dari dalam instrumentasi. */
    private fun shell(command: String) {
        composeRule.instrumentation.uiAutomation
            .executeShellCommand(command)
            .close()
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

    /** Polling sampai node dengan teks persis [text] tampil. */
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

    /**
     * Tunggu judul layar sebuah tab: label tab muncul minimal 2 node teks (label
     * tab bawah + judul layar) — di tab lain node tersebut hanya 1.
     */
    private fun waitUntilTabShown(label: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(label).fetchSemanticsNodes().size >= 2
        }
    }

    /**
     * Tunggu teks tampil; bila belum, gulir container bergulir satu layar lalu
     * periksa lagi (pola yang sama dengan ExplorerFlowTest.scrollToText).
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
        // Akses semua berkas (appops) SEBELUM aktivitas diluncurkan compose rule
        // — ClassRule statis berjalan paling awal (lihat StorageAccessRule).
        @JvmField
        @ClassRule
        val storageAccess = StorageAccessRule()

        const val SCREEN_DIR = "MyExplorerScreenshots"
        const val TIMEOUT_MILLIS = 10_000L

        /** Jeda agar animasi/transisi selesai sebelum tangkapan layar. */
        const val CAPTURE_SETTLE_MILLIS = 1_500L

        /** Jeda setelah wm size/density: aktivitas direkreasi + compose settle. */
        const val RESIZE_SETTLE_MILLIS = 6_000L

        /** Jeda setelah perubahan mode malam (config change + recompose). */
        const val UIMODE_SETTLE_MILLIS = 3_000L
    }
}
