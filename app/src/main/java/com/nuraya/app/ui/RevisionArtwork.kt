package com.nuraya.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuraya.app.R
import kotlin.math.cos
import kotlin.math.sin

/** The only branded loading scene: never rendered inside the application scaffold. */
@Composable fun LaunchArtwork() {
    Box(Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing).testTag("launch_artwork")) {
        Image(painterResource(R.drawable.launch_artwork),"Nuraya",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    }
}

@Composable fun SectionArtwork(section:String,title:String,subtitle:String,modifier:Modifier=Modifier) {
    val dark=MaterialTheme.colorScheme.background.luminance()<.5f
    val asset=if(section=="prayer")"prayer_landscape" else section+if(dark)"_dark" else "_light"
    Box(modifier.fillMaxWidth().heightIn(min=190.dp).clip(RoundedCornerShape(24.dp)).testTag("artwork_$section")) {
        BrandImage(asset,Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x22152339),Color(0xE5223556)))))
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title,color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text(subtitle,color=Color.White,style=MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable fun SurahNumberMedallion(number:Int,modifier:Modifier=Modifier) {
    val fill=MaterialTheme.colorScheme.surface
    val ink=MaterialTheme.colorScheme.onSurface
    val gold=Color(0xFFB69950)
    Box(modifier.size(46.dp).testTag("surah_medallion_$number"),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            fun star(radius:Float)=Path().apply {
                repeat(16){i->val a=Math.toRadians(i*22.5-90);val r=radius*if(i%2==0)1f else .76f
                    val x=center.x+(cos(a)*r).toFloat();val y=center.y+(sin(a)*r).toFloat()
                    if(i==0)moveTo(x,y) else lineTo(x,y)
                };close()
            }
            val outline=star(size.minDimension*.46f)
            drawPath(outline,fill);drawPath(outline,gold,style=Stroke(1.5.dp.toPx()))
            drawPath(star(size.minDimension*.35f),gold.copy(alpha=.65f),style=Stroke(.7.dp.toPx()))
        }
        Text(number.toString(),style=MaterialTheme.typography.labelMedium,color=ink,fontWeight=FontWeight.Bold)
    }
}
