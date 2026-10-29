package com.qomg.webappcatalogs.webapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import kotlin.math.abs

/** Favicons reported by each web app's pages, stored as files under filesDir. */
object WebAppIcons {
    private const val DIR = "webapp_icons"
    private const val SHORTCUT_SIZE = 192

    private val palette = intArrayOf(
        0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(), 0xFFEF5350.toInt(), 0xFFAB47BC.toInt(),
        0xFF42A5F5.toInt(), 0xFFFFA726.toInt(), 0xFF66BB6A.toInt(), 0xFF8D6E63.toInt()
    )

    private fun file(context: Context, id: Long) =
        File(File(context.filesDir, DIR).apply { mkdirs() }, "$id.png")

    fun load(context: Context, id: Long): Bitmap? =
        file(context, id).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** Saves [icon] unless a larger one is already stored. Returns true if the file changed. */
    fun save(context: Context, id: Long, icon: Bitmap): Boolean {
        val target = file(context, id)
        if (target.exists()) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (bounds.outWidth >= icon.width) return false
        }
        target.outputStream().use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return true
    }

    fun delete(context: Context, id: Long) {
        file(context, id).delete()
    }

    fun colorFor(name: String): Int = palette[abs(name.hashCode()) % palette.size]

    fun initial(name: String): String =
        name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    /** A launcher-sized icon: the favicon centered on a white disc, or a colored initial as fallback. */
    fun shortcutIcon(context: Context, app: WebApp): Bitmap {
        val size = SHORTCUT_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val favicon = load(context, app.id)
        if (favicon != null) {
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            val inset = size * 0.2f
            canvas.drawBitmap(
                favicon,
                Rect(0, 0, favicon.width, favicon.height),
                RectF(inset, inset, size - inset, size - inset),
                paint
            )
        } else {
            paint.color = colorFor(app.name)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = size * 0.45f
            paint.textAlign = Paint.Align.CENTER
            val baseline = size / 2f - (paint.descent() + paint.ascent()) / 2
            canvas.drawText(initial(app.name), size / 2f, baseline, paint)
        }
        return bitmap
    }
}
