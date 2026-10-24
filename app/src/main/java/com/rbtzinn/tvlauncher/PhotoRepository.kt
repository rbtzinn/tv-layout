package com.rbtzinn.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * A foto da tela inicial NÃO vem dentro do app (o repositório é público).
 * Ela é lida da pasta privada do app na TV:
 *   /sdcard/Android/data/com.rbtzinn.tvlauncher/files/
 * Qualquer .jpg/.png colocado lá é usado (o mais recente).
 */
object PhotoRepository {

    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    /** Pasta onde a foto deve ser colocada. Chamar isso também cria a pasta. */
    fun photoDir(context: Context): File? = context.getExternalFilesDir(null)

    fun findPhoto(context: Context): File? =
        photoDir(context)
            ?.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in EXTENSIONS }
            ?.maxByOrNull { it.lastModified() }

    /** Carrega a foto reduzida (a TV não precisa de 12 MP) e já na orientação certa. */
    fun load(file: File, maxSide: Int = 1280): ImageBitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

            val bitmap = BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return null

            rotateFromExif(file, bitmap).asImageBitmap()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun rotateFromExif(file: File, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(file.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
