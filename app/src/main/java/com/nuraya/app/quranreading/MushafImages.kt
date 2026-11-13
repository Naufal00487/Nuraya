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

enum class MushafEdition(val id: String, val assetDirectory: String) {
    MADINAH("madinah", "mushaf"),
    INDONESIA("indonesia", "mushaf/indonesia");

    fun title(language: String) = when (this) {
        MADINAH -> if (language == "en") "Madinah mushaf" else "Mushaf Madinah"
        INDONESIA -> if (language == "en") "Indonesian mushaf" else "Mushaf Indonesia"
    }

    fun accepts(address: URL): Boolean = address.protocol == "https" && address.host == "raw.githubusercontent.com" && when (this) {
        MADINAH -> address.path.startsWith("/GovarJabbar/Quran-PNG/b189391148dfb4d7b44e7de40fe2cf942e7c84ac/") ||
            address.path == "/GovarJabbar/Quran-JPG/9aa696d923843acef00a54e7c4c872414494c4ba/003.jpg"
        INDONESIA -> address.path.startsWith("/siar08888000077/mushaf-indonesia/76abbcd2ebc88433ee8b530e1611a7219919d574/")
    }
}

/** Pinned source artwork, with a separate index and storage namespace for each edition. */
class MushafImages(private val context: Context, val edition: MushafEdition = MushafEdition.MADINAH,
                   val printedEdition: PrintedMushafEdition? = null) {
    private val assetDirectory = printedEdition?.let { "printed_mushaf/${it.id}" } ?: edition.assetDirectory
    private val namespace = printedEdition?.let { "printed-${it.id}" } ?: edition.id
    fun title(language: String) = printedEdition?.title(language) ?: edition.title(language)
    val totalBytes get() = printedManifest?.getJSONArray("pages")?.let { rows -> (0 until rows.length()).sumOf { rows.getJSONObject(it).getLong("bytes") } } ?: 0L
    private val printedManifest: JSONObject? by lazy {
        printedEdition?.let { selected ->
            val root = context.assets.open("printed_mushaf/catalog.json").bufferedReader().use { JSONObject(it.readText()) }
            require(root.getString("commit") == PrintedMushafEdition.COMMIT)
            val rows = root.getJSONArray("editions")
            (0 until rows.length()).map { rows.getJSONObject(it) }.single { it.getString("id") == selected.id }
        }
    }
    val pages: List<MushafPage> by lazy {
        val root = printedManifest ?: context.assets.open("mushaf/${edition.id}-images.json").bufferedReader().use { JSONObject(it.readText()) }
        val rows = root.getJSONArray("pages")
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            MushafPage(it.getInt("page"), it.getString("url"), it.getString("gitBlobSha1"))
        } }.also { require(it.map(MushafPage::number) == (1..604).toList()) }
    }
    val references: List<MushafReference> by lazy {
        val path = printedEdition?.let { "printed_mushaf/${it.index}-index.json" } ?: "mushaf/${edition.id}-pages.json"
        val root = context.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }
        val rows = root.getJSONArray("verses")
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            MushafReference(it.getInt("surah"), it.getInt("ayah"), it.getInt("page"))
        } }.also { require(it.size == 6236 && it.all { ref -> ref.page in 1..604 }) }
    }
    fun pageFor(surah: Int, ayah: Int): Int = references.firstOrNull { it.surah == surah && it.ayah == ayah }?.page ?: 1
    fun firstReference(page: Int): MushafReference = references.first { it.page == page }

    private val regions: Map<Int, List<MushafRegion>> by lazy {
        val group = printedEdition?.mapping
        if (group == null) emptyMap() else {
            val root = context.assets.open("printed_mushaf/$group-map.json").bufferedReader().use { JSONObject(it.readText()) }
            require(root.getInt("width") == printedEdition.width && root.getInt("height") == printedEdition.height)
            val pages = root.getJSONArray("pages")
            (0 until pages.length()).associate { p ->
                val row = pages.getJSONObject(p)
                val verses = row.getJSONArray("verses")
                row.getInt("page") to (0 until verses.length()).map { v ->
                    val verse = verses.getJSONObject(v)
                    val rectangles = verse.getJSONArray("rects")
                    MushafRegion(verse.getInt("surah"), verse.getInt("ayah"), (0 until rectangles.length()).map { r ->
                        val box = rectangles.getJSONArray(r)
                        MushafRect(box.getInt(0), box.getInt(1), box.getInt(2), box.getInt(3))
                    })
                }
            }
        }
    }

    fun regionsFor(page: Int): List<MushafRegion> = regions[page].orEmpty()

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
        val bundled = try { context.assets.open("$assetDirectory/$name").use { it.readBytes() } } catch (_: java.io.FileNotFoundException) { null }
        if (bundled != null && verified(page, bundled)) return@withContext decode(bundled)
        val saved = File(context.filesDir, "$namespace-saved-v1/$name")
        if (saved.isFile) {
            val bytes = saved.readBytes()
            if (verified(page, bytes)) return@withContext decode(bytes)
            saved.delete()
        }
        val directory = File(context.cacheDir, "$namespace-v1").apply { mkdirs() }
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
        require(if (printedEdition != null) address.toString().startsWith(PrintedMushafEdition.BASE_URL + printedEdition.id + "/") else edition.accepts(address))
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

    /** Explicitly saved pages survive the bounded recent-page cache. */
    suspend fun saveOffline(page: Int) = withContext(Dispatchers.IO) {
        require(page in 1..604)
        val name = "%03d".format(java.util.Locale.US, page) + if (pages[page - 1].url.endsWith(".jpg")) ".jpg" else ".png"
        val directory = File(context.filesDir, "$namespace-saved-v1").apply { check(mkdirs() || isDirectory) }
        val target = File(directory, name)
        if (target.isFile && verified(page, target.readBytes())) return@withContext
        load(page).recycle()
        val bytes = try { context.assets.open("$assetDirectory/$name").use { it.readBytes() } }
            catch (_: java.io.FileNotFoundException) { File(context.cacheDir, "$namespace-v1/$name").readBytes() }
        check(verified(page, bytes))
        val temp = File.createTempFile("save-", ".tmp", directory)
        try { temp.writeBytes(bytes); check(temp.renameTo(target)) }
        finally { temp.delete() }
    }

    private fun decode(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        check(bounds.outWidth in 100..10000 && bounds.outHeight in 100..15000) { "Invalid page image" }
        var sample = 1
        while (bounds.outWidth / sample > 1600) sample *= 2
        return checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = if (printedEdition != null) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565 })) { "Unable to decode page" }
    }
}
