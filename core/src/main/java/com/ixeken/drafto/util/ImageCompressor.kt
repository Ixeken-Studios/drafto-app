package com.ixeken.drafto.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidad de optimización y compresión de imágenes en memoria y disco.
 *
 * Utiliza decodificación por muestreo (`inSampleSize`) para evitar sobrecargar la memoria RAM
 * con imágenes de alta resolución y preserva la orientación EXIF correcta.
 */
object ImageCompressor {

    /**
     * Comprime y redimensiona una imagen desde una [Uri] de origen y la guarda en [destinationFile].
     *
     * @param context Contexto de la aplicación.
     * @param sourceUri URI del archivo o contenido de imagen original.
     * @param destinationFile Archivo de destino donde se escribirá la imagen optimizada.
     * @param maxDimension Dimensión máxima en píxeles (ancho o alto). Por defecto 1280px para fotos o 384px para avatares.
     * @param quality Calidad de compresión JPEG (1-100). Por defecto 85.
     * @return `true` si la operación fue exitosa, `false` en caso de error.
     */
    fun compressAndSaveImage(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        maxDimension: Int = 1280,
        quality: Int = 85
    ): Boolean {
        return runCatching {
            destinationFile.parentFile?.mkdirs()

            // 1. Obtener dimensiones originales sin cargar el mapa de bits en RAM
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return false

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return false

            // 2. Calcular inSampleSize óptimo
            options.inSampleSize = calculateInSampleSize(origWidth, origHeight, maxDimension, maxDimension)
            options.inJustDecodeBounds = false

            // 3. Decodificar el bitmap escalado
            val sampledBitmap = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return false

            // 4. Corregir rotación EXIF si está disponible
            val rotatedBitmap = fixOrientation(context, sourceUri, sampledBitmap)

            // 5. Guardar en destino comprimido en JPEG
            FileOutputStream(destinationFile).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.flush()
            }

            if (rotatedBitmap != sampledBitmap) {
                sampledBitmap.recycle()
            }
            rotatedBitmap.recycle()
            true
        }.getOrDefault(false)
    }

    private fun calculateInSampleSize(
        actualWidth: Int,
        actualHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (actualHeight > reqHeight || actualWidth > reqWidth) {
            val halfHeight = actualHeight / 2
            val halfWidth = actualWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return runCatching {
            val orientation = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }
}
