package com.nuraya.app.banners

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuraya.app.ui.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.HttpURLConnection

private val destinations=setOf("quran","murottal","practice","recitation","prayer","library","learn")
data class HomeBanner(val id:String,val title:String,val subtitle:String,val destination:String,val imageUrl:String?=null,val assetPath:String?=null)
fun safeBannerUrl(value:String):Boolean=runCatching{val uri=URI(value);uri.scheme=="https"&&uri.userInfo==null&&!uri.host.isNullOrBlank()&&!uri.host.equals("localhost",true)&&uri.host.contains('.')}.getOrDefault(false)
fun parseBanners(text:String):List<HomeBanner>{
    val rows=JSONObject(text).getJSONArray("banners")
    require(rows.length()<=12)
    return List(rows.length()){index->val row=rows.getJSONObject(index)
        val id=row.getString("id");val title=row.getString("title");val subtitle=row.optString("subtitle");val destination=row.getString("destination");val image=row.optString("imageUrl").takeIf{it.isNotBlank()}
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}"))&&title.length in 1..100&&subtitle.length<=180)
        require(destination in destinations||safeBannerUrl(destination));require(image==null||safeBannerUrl(image))
        HomeBanner(id,title,subtitle,destination,image)
    }.also{require(it.map{b->b.id}.distinct().size==it.size)}
}
private fun download(url:String,limit:Int):ByteArray{
    require(safeBannerUrl(url))
    val connection=URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout=5_000;connection.readTimeout=5_000;connection.instanceFollowRedirects=false
    return try{require(connection.responseCode==200);require(connection.contentLengthLong<=limit)
        connection.inputStream.use{input->val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=input.read(buffer)
            while(count>=0){require(output.size()+count<=limit);output.write(buffer,0,count);count=input.read(buffer)};output.toByteArray()}
    }finally{connection.disconnect()}
}
class BannerRepository(context:Context){
    private val prefs=context.applicationContext.getSharedPreferences("nuraya_banners",Context.MODE_PRIVATE)
    fun cached():List<HomeBanner> = runCatching{parseBanners(prefs.getString("catalog",null)?:return emptyList())}.getOrDefault(emptyList())
    fun refresh():List<HomeBanner>{
        if(System.currentTimeMillis()-prefs.getLong("checked",0) in 0..900_000)return cached()
        return runCatching{
            val bootstrap=JSONObject(download("https://raw.githubusercontent.com/Naufal00487/Nuraya/HEAD/content/banner-service.json",16_384).toString(Charsets.UTF_8))
            val base=bootstrap.optString("baseUrl").trimEnd('/')
            if(base.isBlank()){prefs.edit().remove("catalog").putLong("checked",System.currentTimeMillis()).apply();emptyList()}
            else{require(safeBannerUrl(base));val text=download("$base/banners",65_536).toString(Charsets.UTF_8);val list=parseBanners(text)
                prefs.edit().putString("catalog",text).putLong("checked",System.currentTimeMillis()).apply();list}
        }.getOrElse{cached()}
    }
}
@Composable fun BannerCarousel(language:String,onNavigate:(String)->Unit,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val repository=remember{BannerRepository(context)}
    var remote by remember{mutableStateOf(repository.cached())}
    LaunchedEffect(repository){remote=withContext(Dispatchers.IO){repository.refresh()}}
    val designed = listOf(
        HomeBanner("nuraya-learn", label(language, "Bacalah, dengarkan, hafalkan · AI belajar", "Read, listen, memorize · learning companion"), "", "learn", assetPath = "brand/banners/learn.png"),
        HomeBanner("nuraya-explore", label(language, "Jelajahi fitur Nuraya", "Explore Nuraya"), "", "quran", assetPath = "brand/banners/explore.png"))
    val banners = designed + remote.filter { candidate -> designed.none { it.id == candidate.id } }
    val pager=rememberPagerState(pageCount={banners.size})
    Column(modifier,verticalArrangement=Arrangement.spacedBy(10.dp)){
        HorizontalPager(state=pager,modifier=Modifier.fillMaxWidth().testTag("home_banners"),pageSpacing=12.dp){index->val banner=banners[index]
            val image by produceState<ImageBitmap?>(null,banner.imageUrl){value=withContext(Dispatchers.IO){runCatching{
                val bytes=download(banner.imageUrl?:return@withContext null,1_500_000)
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
                BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=2})?.asImageBitmap()
            }.getOrNull()}}
            val bundled = banner.assetPath?.let { com.nuraya.app.ui.rememberAssetBitmap(it) }
            if (banner.assetPath != null) {
                Box(Modifier.fillMaxWidth().aspectRatio(5300f / 1331f).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).testTag("home_banner_${banner.id}")
                    .clickable { onNavigate(banner.destination) }, contentAlignment = Alignment.Center) {
                    if (bundled != null) Image(bundled, banner.title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Text(banner.title, Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
                }
            } else {
            Box(Modifier.fillMaxWidth().heightIn(min=168.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF2C4066)).clickable{
                if(banner.destination in destinations)onNavigate(banner.destination)
                else if(safeBannerUrl(banner.destination))runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(banner.destination)))}
            }){
                if(image!=null)Image(image!!,null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xDD182D4D),Color(0x66304F77)))))
                Row(Modifier.padding(22.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(banner.title,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(banner.subtitle,color=Color(0xFFDCE6F8),style=MaterialTheme.typography.bodySmall)
                };Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.padding(start=14.dp),tint=Color(0xFFEAD191))}
            }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){repeat(banners.size){index->Box(Modifier.padding(horizontal=3.dp).size(if(index==pager.currentPage)8.dp else 6.dp).clip(CircleShape).background(if(index==pager.currentPage)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))}}
    }
}
