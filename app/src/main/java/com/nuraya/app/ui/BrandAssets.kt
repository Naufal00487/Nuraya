package com.nuraya.app.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
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
                    val bitmap=brandCache.get(path) ?: context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.also{brandCache.put(path,it)}
                    bitmap?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
fun BrandImage(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop, description: String? = null) {
    rememberAssetBitmap("brand/$name.jpg")?.let { Image(it, description, modifier, contentScale = scale) }
}

fun landscapeForHour(hour: Int): String = when (hour) {
    in 5..10 -> "morning"
    in 11..14 -> "day"
    in 15..17 -> "evening"
    else -> "night"
}
