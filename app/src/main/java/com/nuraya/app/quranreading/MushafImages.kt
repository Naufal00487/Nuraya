package com.nuraya.app.quranreading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class MushafPage(val number: Int, val url: String, val gitBlobSha1: String)
data class MushafReference(val surah: Int, val ayah: Int, val page: Int)

/** Original Madinah page images; this index does not describe Bahriyah editions. */
class MushafImages(private val context: Context) {
    val pages: List<MushafPage> by lazy {
        val root = context.assets.open("mushaf/madinah-images.json").bufferedReader().use { JSONObject(it.readText()) }
        val rows = root.getJSONArray("pages")
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            MushafPage(it.getInt("page"), it.getString("url"), it.getString("gitBlobSha1"))
        } }.also { require(it.map(MushafPage::number) == (1..604).toList()) }
    }
    val references: List<MushafReference> by lazy {
        val root = context.assets.open("mushaf/madinah-pages.json").bufferedReader().use { JSONObject(it.readText()) }
        val rows = root.getJSONArray("verses")
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            MushafReference(it.getInt("surah"), it.getInt("ayah"), it.getInt("page"))
        } }.also { require(it.size == 6236 && it.all { ref -> ref.page in 1..604 }) }
    }
    fun pageFor(surah: Int, ayah: Int): Int = references.firstOrNull { it.surah == surah && it.ayah == ayah }?.page ?: 1
    fun firstReference(page: Int): MushafReference = references.first { it.page == page }

    /** Hash includes Git's blob header, so downloaded bytes must match the pinned source tree. */
    fun verified(page: Int, bytes: ByteArray): Boolean {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        val hash = digest.digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        return hash == pages[page - 1].gitBlobSha1
    }

    suspend fun load(page: Int): Bitmap = withContext(Dispatchers.IO) {
        require(page in 1..604)
        val name = "%03d".format(java.util.Locale.US, page) + if (pages[page - 1].url.endsWith(".jpg")) ".jpg" else ".png"
        val bundled = try { context.assets.open("mushaf/$name").use { it.readBytes() } } catch (_: java.io.FileNotFoundException) { null }
        if (bundled != null && verified(page, bundled)) return@withContext decode(bundled)
        val directory = File(context.cacheDir, "madinah-v1").apply { mkdirs() }
        val file = File(directory, name)
        if (file.exists()) {
            val bytes = file.readBytes()
            if (verified(page, bytes)) {
                file.setLastModified(System.currentTimeMillis())
                return@withContext decode(bytes)
            }
            file.delete()
        }
        val address = URL(pages[page - 1].url)
        require(address.protocol == "https" && address.host == "raw.githubusercontent.com" &&
            (address.path.startsWith("/GovarJabbar/Quran-PNG/b189391148dfb4d7b44e7de40fe2cf942e7c84ac/") ||
                address.path == "/GovarJabbar/Quran-JPG/9aa696d923843acef00a54e7c4c872414494c4ba/003.jpg"))
        val connection = (address.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = false
        }
        val bytes = try {
            check(connection.responseCode == 200) { "Page download failed (${connection.responseCode})" }
            connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16384)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    check(output.size() + read <= 12 * 1024 * 1024) { "Page exceeds size limit" }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
        check(verified(page, bytes)) { "Page checksum does not match its source" }
        val bitmap = decode(bytes)
        val temporary = File.createTempFile("page-", ".tmp", directory)
        try {
            temporary.writeBytes(bytes)
            check(temporary.renameTo(file)) { "Unable to cache page" }
        } finally { temporary.delete() }
        // Bound storage while keeping the current page and recently opened pages available offline.
        val cached = directory.listFiles()?.filter { it.extension in setOf("png", "jpg") }?.sortedBy { it.lastModified() }.orEmpty()
        var total = cached.sumOf { it.length() }
        for (old in cached) if (total > 64L * 1024 * 1024 && old != file) {
            val length = old.length()
            if (old.delete()) total -= length
        }
        bitmap
    }

    private fun decode(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        check(bounds.outWidth in 100..10000 && bounds.outHeight in 100..15000) { "Invalid page image" }
        var sample = 1
        while (bounds.outWidth / sample > 1600) sample *= 2
        return checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 })) { "Unable to decode page" }
    }
}
