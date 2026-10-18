package com.elsllak.jadwal

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs
import kotlin.math.max

/** Reads text from a photo with ML Kit (on the phone, no internet needed). */
object Ocr {
    class Out(val text: String, val results: List<Parser.Result>, val pass: Int)

    fun load(c: Context, uri: Uri, maxSide: Int = 2800): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val o = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) } ?: return null
        // respect the EXIF turn
        try {
            c.contentResolver.openInputStream(uri)?.use {
                val ex = android.media.ExifInterface(it)
                val deg = when (ex.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
                }
                if (deg != 0f) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
            }
        } catch (_: Exception) {}
        val m = max(bmp.width, bmp.height)
        if (m > maxSide) {
            val k = maxSide.toFloat() / m
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
        }
        return bmp
    }

    /** Grey, stretched contrast version for hard photos. */
    fun enhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val k = 1.6f; val t = (-0.5f * k + 0.5f) * 255f
        cm.postConcat(ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, t, 0f, k, 0f, 0f, t, 0f, 0f, k, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        return out
    }

    /** Runs recognition (blocking: call from a background thread). Lines are joined into table rows. */
    fun read(bmp: Bitmap): String {
        val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = Tasks.await(rec.process(InputImage.fromBitmap(bmp, 0)))
            class Ln(val t: String, val r: Rect)
            val lines = ArrayList<Ln>()
            for (b in text.textBlocks) for (l in b.lines) l.boundingBox?.let { lines.add(Ln(l.text, it)) }
            if (lines.isEmpty()) return text.text
            lines.sortBy { it.r.centerY() }
            val rows = ArrayList<MutableList<Ln>>()
            for (l in lines) {
                val row = rows.lastOrNull()
                if (row != null) {
                    val cy = row.map { it.r.centerY() }.average()
                    val h = row.map { it.r.height() }.average()
                    if (abs(l.r.centerY() - cy) < h * 0.6) { row.add(l); continue }
                }
                rows.add(mutableListOf(l))
            }
            val sb = StringBuilder()
            for (r in rows) { r.sortBy { it.r.left }; sb.append(r.joinToString("   ") { it.t }).append('\n') }
            return sb.toString()
        } finally { rec.close() }
    }

    fun run(c: Context, uri: Uri, codes: List<String>, todayIso: String, onProgress: (Int) -> Unit): Out {
        onProgress(1)
        val bmp = load(c, uri) ?: throw IllegalStateException("decode")
        onProgress(2)
        var text = read(bmp)
        var res = Parser.parseExamText(text, codes, todayIso)
        var pass = 1
        if (res.count { it.found && it.date.isNotEmpty() } < codes.size) {
            onProgress(3)
            val t2 = read(enhance(bmp))
            val r2 = Parser.parseExamText(t2, codes, todayIso)
            fun score(r: Parser.Result) = (if (r.found) 1.0 else 0.0) + (if (r.date.isNotEmpty()) 1 else 0) + (if (r.s != null) 1 else 0) + (if (r.room.isNotEmpty()) 0.5 else 0.0)
            val merged = codes.indices.map { i -> if (score(r2[i]) > score(res[i])) r2[i] else res[i] }
            if (merged.indices.any { merged[it] !== res[it] }) pass = 2
            res = merged
            text = text + "\n----\n" + t2
        }
        return Out(text, res, pass)
    }
}
