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

import android.graphics.BitmapFactory
import android.os.Environment
import android.os.ParcelFileDescriptor
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
import kotlin.math.max

/**
 * Menangkap tangkapan layar seluruh layar utama untuk dokumentasi rilis.
 *
 * Test ini HANYA dijalankan bila workflow memintanya lewat argumen instrumentation
 * `screenshots=true` (lihat .github/workflows/screenshots.yml); pada eksekusi e2e
 * biasa test ini DIKECUALIKAN lewat `notClass` (asumsi di @Before tetap ada sebagai
 * jaring pengaman — pelaporan XML UTP ternyata mencatat AssumptionViolatedException
 * @Before sebagai failure, jadi pengecualian notClass itu yang utama).
 *
 * Capture memakai `screencap` shell (via UiAutomation.executeShellCommand) —
 * UiAutomation.takeScreenshot() terbukti menghasilkan frame hitam penuh pada
 * emulator headless (swiftshader); screencap adalah path yang sama dengan
 * `adb exec-out screencap -p` yang terekam benar di ATD. Ukuran berkas hasil
 * divalidasi agar frame hitam/blank terdeteksi cepat, bukan lolos senyap.
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

    /** Instrumentasi aktif — AndroidComposeTestRule di versi ini tidak punya
     * properti `instrumentation`, jadi ambil lewat InstrumentationRegistry. */
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /**
     * Simpan tangkapan layar penuh sebagai PNG di [shotDir]; tunggu compose idle
     * dulu agar transisi selesai dan konten stabil.
     *
     * `screencap` dijalankan sebagai UID shell — berkasnya ditulis langsung ke
     * /sdcard (shell punya akses penuh emulated storage). executeShellCommand
     * bersifat asinkron, jadi stream fd-nya dikuras sampai EOF untuk menunggu
     * proses selesai sebelum navigasi berikutnya.
     */
    private fun capture(name: String) {
        runCatching { composeRule.waitForIdle() }
        Thread.sleep(CAPTURE_SETTLE_MILLIS)
        val target = File(shotDir, "$name.png")
        val fd =
            instrumentation.uiAutomation
                .executeShellCommand("screencap -p ${target.absolutePath}")
        fd.use { descriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
                val buffer = ByteArray(8192)
                while (stream.read(buffer) != -1) {
                    // Kuras output sampai screencap selesai.
                }
            }
        }
        check(target.exists() && target.length() > MIN_PNG_BYTES) {
            "screencap menghasilkan berkas kosong/hitam: ${target.path} (${target.length()} B)"
        }
        assertRendered(target)
    }

    /**
     * Validasi render: tangkapan layar sungguhan memuat banyak warna unik;
     * frame hitam/kosong dari emulator headless yang gagal komposisi hanya
     * punya 1-2 warna. Sampling piksel tiap langkah w/64 x h/64 cukup murah
     * dan gagal cepat sebelum test lanjut menangkap layar berikutnya.
     */
    private fun assertRendered(target: File) {
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bitmap = BitmapFactory.decodeFile(target.absolutePath, options)
        checkNotNull(bitmap) { "PNG tidak bisa didekode: ${target.path}" }
        val colors = HashSet<Int>()
        val stepX = max(1, bitmap.width / 64)
        val stepY = max(1, bitmap.height / 64)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                colors.add(bitmap.getPixel(x, y))
                x += stepX
            }
            y += stepY
        }
        bitmap.recycle()
        check(colors.size >= MIN_UNIQUE_COLORS) {
            "Tangkapan layar nyaris kosong (${colors.size} warna unik — render gagal?): ${target.path}"
        }
    }

    /** Jalankan perintah shell sebagai UID shell (wm/cmd) dari dalam instrumentasi. */
    private fun shell(command: String) {
        instrumentation.uiAutomation
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

        /** PNG UI sungguhan selalu > 2 KB; frame hitam solid ~1 KB -> gagal cepat. */
        const val MIN_PNG_BYTES = 2_000L

        /** Frame nyaris kosong (hitam/komposisi gagal) punya sangat sedikit warna. */
        const val MIN_UNIQUE_COLORS = 50

        /** Jeda agar animasi/transisi selesai sebelum tangkapan layar. */
        const val CAPTURE_SETTLE_MILLIS = 1_500L

        /** Jeda setelah wm size/density: aktivitas direkreasi + compose settle. */
        const val RESIZE_SETTLE_MILLIS = 6_000L

        /** Jeda setelah perubahan mode malam (config change + recompose). */
        const val UIMODE_SETTLE_MILLIS = 3_000L
    }
}
