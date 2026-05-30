package com.heracles.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

object WallpaperCache {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun getThumbnail(context: Context, uriStr: String, maxPx: Int = 240): Bitmap? {
        return cache[uriStr] ?: run {
            val uri = Uri.parse(uriStr)
            val bmp = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        val ratio = maxOf(1, (info.size.width / maxPx.toFloat()).coerceAtLeast(info.size.height / maxPx.toFloat()).toInt())
                        decoder.setTargetSize(info.size.width / ratio, info.size.height / ratio)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }.getOrNull()
            bmp?.also { cache[uriStr] = it }
        }
    }

    fun clear() = cache.clear()
}
