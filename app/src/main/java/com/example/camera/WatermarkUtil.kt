package com.example.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkUtil {

    fun applyTimestampWatermark(
        imageFile: File,
        locationText: String? = null,
        customLabel: String = "CAMGUARD MON"
    ): File {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return imageFile
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val paint = Paint().apply {
                color = Color.YELLOW
                textSize = (mutableBitmap.height * 0.035f).coerceAtLeast(24f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
            val timeString = dateFormat.format(Date())
            val watermarkText = "$customLabel | $timeString${if (!locationText.isNull_Blank()) " | $locationText" else ""}"

            val margin = mutableBitmap.width * 0.03f
            val x = margin
            val y = mutableBitmap.height - margin

            // Draw translucent background bar
            val bgPaint = Paint().apply {
                color = Color.argb(160, 0, 0, 0)
            }
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)
            canvas.drawRect(
                x - 10f,
                y - textBounds.height() - 10f,
                x + textBounds.width() + 20f,
                y + 10f,
                bgPaint
            )

            canvas.drawText(watermarkText, x, y, paint)

            FileOutputStream(imageFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            return imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            return imageFile
        }
    }

    private fun String?.isNull_Blank(): Boolean = this == null || this.isBlank()
}
