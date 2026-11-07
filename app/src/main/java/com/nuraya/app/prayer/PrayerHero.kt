package com.nuraya.app.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.profile.*
import com.nuraya.app.ui.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PrayerHero(language:String,onNavigate:(String)->Unit) {
    val (preferences, _) = rememberPrayerPreferences()
    val profile=rememberProfile()
    val now=rememberCurrentInstant()
    val zone=preferences.location?.zone ?: ZoneId.systemDefault()
    val local=now.atZone(zone)
    val next=remember(preferences,now.epochSecond/60){preferences.location?.let{PrayerCalculator.next(now,it,preferences.minuteAdjustment)}}
    val day=remember(preferences,local.toLocalDate()){preferences.location?.let{PrayerCalculator.calculate(local.toLocalDate(),it,preferences.minuteAdjustment)}}
    var locationDialog by remember{mutableStateOf(false)}
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart=32.dp,bottomEnd=32.dp)).testTag("prayer_dashboard")) {
        BrandImage(landscapeForHour(local.hour),Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xAA122440),Color(0x55344968),Color(0xF0223556)))))
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={locationDialog=true},modifier=Modifier.weight(1f).testTag("home_location"),contentPadding=PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.LocationOn,null,tint=Color.White,modifier=Modifier.size(22.dp))
                    Text(preferences.location?.name ?: label(language,"Pilih lokasi","Choose location"),color=Color.White,modifier=Modifier.padding(start=6.dp).weight(1f),maxLines=2)
                    Icon(Icons.Outlined.ExpandMore,null,tint=Color.White)
                }
                IconButton(onClick={onNavigate("settings")},modifier=Modifier.testTag("settings_button")){Icon(Icons.Outlined.Settings,label(language,"Pengaturan","Settings"),tint=Color.White)}
                Box(Modifier.size(56.dp).clip(CircleShape).clickable{onNavigate("profile")}.testTag("home_profile"),contentAlignment=Alignment.Center){ProfileAvatar(profile.avatar,Modifier.size(44.dp))}
            }
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                Text(runCatching{HijriCalendar.label(local.toLocalDate(),preferences.hijriAdjustment,language)}.getOrDefault(""),color=Color.White,style=MaterialTheme.typography.bodyMedium)
                Text(next?.let{"${it.name(language)} · ${it.instant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))}"}
                    ?: label(language,"Waktunya mendekat","A moment to reconnect"),color=Color.White,fontWeight=FontWeight.Bold,fontSize=28.sp,textAlign=TextAlign.Center)
                Text(next?.let{PrayerCalculator.countdown(now,it.instant)+label(language," menuju salat"," until prayer")}
                    ?: label(language,"Pilih lokasi untuk jadwal salat","Choose a location for prayer times"),color=Color.White,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                AdhanToggle(language)
                Text(local.format(DateTimeFormatter.ofPattern("HH:mm")),color=Color.White,style=MaterialTheme.typography.titleMedium)
                FilledTonalIconButton(onClick={onNavigate("qibla")},modifier=Modifier.size(56.dp).testTag("home_qibla"),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color(0xFFF4E6B9),contentColor=Color(0xFF5E4922))){Icon(Icons.Outlined.Explore,label(language,"Arah kiblat","Qibla direction"))}
            }
            Surface(onClick={onNavigate("prayer")},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(vertical=16.dp,horizontal=8.dp),horizontalArrangement=Arrangement.SpaceEvenly){
                    val moments=day?.moments?.filter{it.isPrayer}.orEmpty()
                    val names=listOf("Subuh","Zuhur","Asar","Magrib","Isya")
                    repeat(5){index -> Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Icon(listOf(Icons.Outlined.WbTwilight,Icons.Outlined.LightMode,Icons.Outlined.WbSunny,Icons.Outlined.WbTwilight,Icons.Outlined.DarkMode)[index],null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(22.dp))
                        Text(moments.getOrNull(index)?.name(language) ?: names[index],style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)
                        Text(moments.getOrNull(index)?.instant?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
                    }}
                }
            }
        }
    }
    if(locationDialog)AlertDialog(onDismissRequest={locationDialog=false},title={Text(label(language,"Ubah lokasi?","Change location?"))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(label(language,"Pilih lokasi otomatis dari HP atau pilih kota secara manual.","Use your device location or choose a city manually."))
        DeviceLocationButton(language,onLocated={locationDialog=false},onManualLocation={locationDialog=false;onNavigate("location")})
    }},confirmButton={TextButton(onClick={locationDialog=false;onNavigate("location")}){Text(label(language,"Ubah manual","Choose manually"))}},dismissButton={TextButton(onClick={locationDialog=false}){Text(label(language,"Batal","Cancel"))}})
}
