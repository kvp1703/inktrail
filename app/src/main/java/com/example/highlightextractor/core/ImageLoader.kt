package com.example.highlightextractor.core

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Loads bitmaps from content Uris, applies EXIF rotation and downscales the result so the
 * longest side is at most [MAX_DIMENSION] pixels. Always call from a background thread.
 */
object ImageLoader {

    private const val MAX_DIMENSION = 2000

    fun loadUprightBitmap(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver

        // First pass: read the size only, so we can pick a safe inSampleSize.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Could not read image dimensions")
        }

        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }

        val decoded = decodeWithRetry(resolver, uri, sample)
        val upright = applyExifOrientation(resolver, uri, decoded)
        return scaleDown(upright, MAX_DIMENSION)
    }

    /** Small bitmap for the horizontal thumbnail strip. */
    fun loadThumbnail(context: Context, uri: Uri, targetPx: Int): Bitmap {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Could not read image dimensions")
        }

        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= targetPx) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("Could not open image")
        val upright = applyExifOrientation(resolver, uri, decoded)
        return scaleDown(upright, targetPx)
    }

    private fun decodeWithRetry(resolver: ContentResolver, uri: Uri, initialSample: Int): Bitmap {
        var sample = initialSample
        while (true) {
            try {
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw IllegalStateException("Could not open image")
            } catch (e: OutOfMemoryError) {
                if (sample >= 64) throw IllegalStateException("Image is too large to load", e)
                sample *= 2
            }
        }
    }

    /** Rotates / flips the bitmap according to its EXIF orientation. Recycles the input if replaced. */
    private fun applyExifOrientation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (transformed !== bitmap) bitmap.recycle()
            transformed
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
