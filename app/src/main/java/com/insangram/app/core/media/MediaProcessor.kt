package com.insangram.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Editor adjustment values, all normalised to -1f..1f unless noted. */
data class ImageAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val warmth: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val blur: Float = 0f,
    val sharpen: Float = 0f,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val cropRect: Rect? = null,
) {
    val isIdentity: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && exposure == 0f &&
            warmth == 0f && highlights == 0f && shadows == 0f && vignette == 0f &&
            blur == 0f && sharpen == 0f && rotationDegrees == 0 && !flipHorizontal &&
            cropRect == null
}

/** Original Insangram looks, each expressed as a preset adjustment stack. */
enum class InsangramFilter(val displayName: String, val adjustments: ImageAdjustments) {
    ORIGINAL("Original", ImageAdjustments()),
    AURORA("Aurora", ImageAdjustments(saturation = 0.25f, contrast = 0.12f, warmth = -0.15f)),
    EMBER("Ember", ImageAdjustments(warmth = 0.35f, contrast = 0.15f, shadows = 0.1f)),
    NEON("Neon", ImageAdjustments(saturation = 0.5f, contrast = 0.25f, brightness = 0.05f)),
    MONO("Mono", ImageAdjustments(saturation = -1f, contrast = 0.2f)),
    SOFTLIGHT("Softlight", ImageAdjustments(brightness = 0.12f, contrast = -0.1f, highlights = 0.15f)),
    DUSK("Dusk", ImageAdjustments(warmth = -0.25f, shadows = 0.2f, vignette = 0.3f)),
    CANDY("Candy", ImageAdjustments(saturation = 0.35f, warmth = 0.15f, brightness = 0.08f)),
    FILM("Film", ImageAdjustments(contrast = 0.18f, saturation = -0.15f, vignette = 0.25f)),
}

/**
 * All bitmap work runs on the IO dispatcher and decodes with inSampleSize so a
 * full-resolution image is never loaded just to render a preview.
 */
@Singleton
class MediaProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun decodeSampled(uri: Uri, maxDimension: Int): Bitmap? = withContext(dispatchers.io) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@withContext null
        applyExifRotation(uri, decoded)
    }

    fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while ((w / 2) >= maxDimension && (h / 2) >= maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun apply(source: Bitmap, adjustments: ImageAdjustments): Bitmap =
        withContext(dispatchers.default) {
            if (adjustments.isIdentity) return@withContext source

            var working = source
            adjustments.cropRect?.let { rect ->
                val safe = Rect(
                    rect.left.coerceIn(0, working.width - 1),
                    rect.top.coerceIn(0, working.height - 1),
                    rect.right.coerceIn(1, working.width),
                    rect.bottom.coerceIn(1, working.height),
                )
                if (safe.width() > 0 && safe.height() > 0) {
                    working = Bitmap.createBitmap(
                        working, safe.left, safe.top, safe.width(), safe.height(),
                    )
                }
            }

            if (adjustments.rotationDegrees != 0 || adjustments.flipHorizontal) {
                val matrix = Matrix().apply {
                    if (adjustments.flipHorizontal) postScale(-1f, 1f)
                    if (adjustments.rotationDegrees != 0) {
                        postRotate(adjustments.rotationDegrees.toFloat())
                    }
                }
                working = Bitmap.createBitmap(
                    working, 0, 0, working.width, working.height, matrix, true,
                )
            }

            val output = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(buildColorMatrix(adjustments))
            }
            canvas.drawBitmap(working, 0f, 0f, paint)

            if (adjustments.vignette > 0f) drawVignette(canvas, output, adjustments.vignette)
            output
        }

    /** Combines every colour adjustment into a single 4x5 matrix. */
    fun buildColorMatrix(adjustments: ImageAdjustments): ColorMatrix {
        val matrix = ColorMatrix()

        if (adjustments.saturation != 0f) {
            matrix.postConcat(ColorMatrix().apply { setSaturation(1f + adjustments.saturation) })
        }
        if (adjustments.contrast != 0f) {
            val scale = 1f + adjustments.contrast
            val translate = (1f - scale) * 128f
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
        val luminanceShift = (adjustments.brightness * 90f) + (adjustments.exposure * 70f) +
            (adjustments.shadows * 30f) - (adjustments.highlights * 25f)
        val warmthShift = adjustments.warmth * 45f
        if (luminanceShift != 0f || warmthShift != 0f) {
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, luminanceShift + warmthShift,
                        1f.let { 0f }, 1f, 0f, 0f, luminanceShift,
                        0f, 0f, 1f, 0f, luminanceShift - warmthShift,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
        return matrix
    }

    private fun drawVignette(canvas: Canvas, bitmap: Bitmap, strength: Float) {
        val radius = maxOf(bitmap.width, bitmap.height) * 0.75f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                bitmap.width / 2f,
                bitmap.height / 2f,
                radius,
                intArrayOf(0x00000000, android.graphics.Color.argb((strength * 170).toInt(), 0, 0, 0)),
                floatArrayOf(0.55f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }

    /** Compresses to JPEG under the configured upload ceiling. */
    suspend fun compressToCache(
        bitmap: Bitmap,
        fileName: String,
        quality: Int = 85,
    ): File = withContext(dispatchers.io) {
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out)
        }
        if (file.length() > InsangramConstants.MAX_IMAGE_UPLOAD_BYTES && quality > 45) {
            compressToCache(bitmap, fileName, quality - 15)
        } else {
            file
        }
    }

    /**
     * Copies picked media into the app's own storage so a queued upload keeps
     * working after the source content URI permission is revoked.
     */
    suspend fun copyIntoAppStorage(localUri: String): String? = withContext(dispatchers.io) {
        runCatching {
            val source = Uri.parse(localUri)
            val directory = File(context.filesDir, "uploads").apply { mkdirs() }
            val name = source.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "media"
            val target = File(directory, System.currentTimeMillis().toString() + "-" + name)
            val copied = context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) target.absolutePath else null
        }.getOrNull()
    }

    suspend fun videoThumbnail(uri: Uri): Bitmap? = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0)
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun videoDurationMs(uri: Uri): Long = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (error: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * SHA-256 of the media bytes, used for local duplicate-upload detection.
     * Reads in chunks so large videos never sit fully in memory.
     */
    suspend fun mediaHash(uri: Uri): String? = withContext(dispatchers.io) {
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            } ?: return@runCatching null
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
