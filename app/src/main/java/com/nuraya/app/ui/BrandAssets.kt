package com.nuraya.app.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val brandCache=object:LruCache<String,Bitmap>(14*1024*1024){
    override fun sizeOf(key:String,value:Bitmap)=value.byteCount
}

@Composable
fun rememberAssetBitmap(path: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            synchronized(brandCache) {
                runCatching {
                    val bitmap=brandCache.get(path) ?: context.assets.open(path).use { input ->
                        val bytes=input.readBytes()
                        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                        val maxWidth = if (path.startsWith("brand/covers/")) 512 else 1600
                        val options=BitmapFactory.Options().apply{while(bounds.outWidth/inSampleSize.coerceAtLeast(1)>maxWidth)inSampleSize=inSampleSize.coerceAtLeast(1)*2}
                        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
                    }?.also{brandCache.put(path,it)}
                    bitmap?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
fun BrandImage(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop, description: String? = null) {
    val path = if (name.startsWith("covers/")) "brand/" + (if (name == "covers/madinah_tajwid") "covers/madinah" else name) + ".png" else "brand/$name.jpg"
    val bitmap = rememberAssetBitmap(path)
    Box(modifier) { bitmap?.let { Image(it, description, Modifier.fillMaxSize().testTag("brand_image_$name"), contentScale = scale) } }
}

fun landscapeForHour(hour: Int): String = when (hour) {
    in 5..10 -> "morning"
    in 11..14 -> "day"
    in 15..17 -> "evening"
    else -> "night"
}
