package com.ixeken.drafto.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object DraftoImageCropUtils {

    suspend fun loadBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            var inSampleSize = 1
            while ((srcWidth / inSampleSize) > maxDimension || (srcHeight / inSampleSize) > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (decodedBitmap == null) return@withContext null

            val rotation = getExifOrientation(context, uri)
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                return@withContext rotated
            }

            return@withContext decodedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun cropAndSaveBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        cropRectPercent: Rect,
        folderName: String = "cropped"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val bmpWidth = sourceBitmap.width
            val bmpHeight = sourceBitmap.height

            val left = (cropRectPercent.left * bmpWidth).toInt().coerceIn(0, bmpWidth - 1)
            val top = (cropRectPercent.top * bmpHeight).toInt().coerceIn(0, bmpHeight - 1)
            val width = (cropRectPercent.width * bmpWidth).toInt().coerceIn(1, bmpWidth - left)
            val height = (cropRectPercent.height * bmpHeight).toInt().coerceIn(1, bmpHeight - top)

            val cropped = Bitmap.createBitmap(sourceBitmap, left, top, width, height)

            val dir = File(context.filesDir, folderName)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (cropped != sourceBitmap) {
                cropped.recycle()
            }
            return@withContext Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
