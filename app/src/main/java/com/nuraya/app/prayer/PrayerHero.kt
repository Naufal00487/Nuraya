package com.nuraya.app.prayer

import com.nuraya.app.ui.icons.AppIcons as Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Locale
import androidx.compose.ui.platform.LocalDensity
import java.time.format.DateTimeFormatter

@Composable
fun PrayerHero(language:String,onNavigate:(String)->Unit) {
    PrayerOverview(language,onNavigate,home=true)
}

@Composable
fun PrayerOverview(language:String,onNavigate:(String)->Unit,home:Boolean=false) {
    val (preferences, _) = rememberPrayerPreferences()
    val profile=rememberProfile()
    val now=rememberCurrentInstant()
    val zone=preferences.location?.zone ?: ZoneId.systemDefault()
    val local=now.atZone(zone)
    val locale=if(language=="en")Locale.ENGLISH else Locale.forLanguageTag("id-ID")
    val next=remember(preferences,now.epochSecond/60){preferences.location?.let{PrayerCalculator.next(now,it,preferences.minuteAdjustment)}}
    val day=remember(preferences,local.toLocalDate()){preferences.location?.let{PrayerCalculator.calculate(local.toLocalDate(),it,preferences.minuteAdjustment)}}
    var locationDialog by remember{mutableStateOf(false)}
    val fontScale=LocalDensity.current.fontScale
    val textShadow = if (home) androidx.compose.ui.graphics.Shadow(Color(0xFF102038), androidx.compose.ui.geometry.Offset(0f, 1f), 5f) else androidx.compose.ui.graphics.Shadow.None
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(if(home)0.dp else 26.dp,if(home)0.dp else 26.dp,26.dp,26.dp)).background(Brush.linearGradient(listOf(Color(0xFF2C4066),Color(0xFF17253B)))).testTag(if(home)"prayer_dashboard" else "prayer_calendar")) {
        if(home) {
            BrandImage(landscapeForHour(local.hour),Modifier.matchParentSize())
        }
        Column(Modifier.fillMaxWidth().then(if(home)Modifier.statusBarsPadding() else Modifier).padding(if(home)20.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={locationDialog=true},modifier=Modifier.weight(1f).testTag(if(home)"home_location" else "prayer_location"),contentPadding=PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.LocationOn,null,tint=Color.White,modifier=Modifier.size(20.dp));Spacer(Modifier.width(6.dp))
                    Text(preferences.location?.name ?: label(language,"Pilih lokasi","Choose location"),color=Color.White,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleSmall.copy(shadow=textShadow))
                    Icon(Icons.Outlined.ExpandMore,null,tint=Color.White,modifier=Modifier.size(16.dp))
                }
                if(home) {
                    IconButton(onClick={onNavigate("settings")},modifier=Modifier.testTag("settings_button")){Icon(Icons.Outlined.Settings,label(language,"Pengaturan","Settings"),tint=Color.White)}
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable{onNavigate("profile")}.testTag("home_profile"),contentAlignment=Alignment.Center){ProfileAvatar(profile.avatar,Modifier.size(44.dp))}
                } else AdhanToggle(language)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.Top) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(local.format(DateTimeFormatter.ofPattern("HH:mm")),style=MaterialTheme.typography.bodyLarge.copy(shadow=textShadow),color=Color.White,fontSize=(if(fontScale>1.2f)34 else 46).sp,lineHeight=(if(fontScale>1.2f)42 else 54).sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("prayer_clock"))
                    if(next!=null) {
                        Text(label(language,"Menuju ${next.name(language)}","Until ${next.name(language)}"),color=Color.White,style=MaterialTheme.typography.bodySmall.copy(shadow=textShadow))
                        Text(PrayerCalculator.countdown(now,next.instant),color=Color.White,style=MaterialTheme.typography.titleSmall.copy(shadow=textShadow),modifier=Modifier.testTag("prayer_countdown"))
                    }
                    Text(runCatching{HijriCalendar.label(local.toLocalDate(),preferences.hijriAdjustment,language)}.getOrDefault(""),color=Color.White,style=MaterialTheme.typography.bodySmall.copy(shadow=textShadow),modifier=Modifier.testTag("prayer_hijri_date"))
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(onClick={onNavigate("qibla")},color=Color(0xFF25395B),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().testTag(if(home)"home_qibla" else "prayer_open_qibla")) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Qibla,null,Modifier.size(25.dp),tint=Color.White)
                            Text(label(language,"Arah kiblat","Qibla"),color=Color.White,style=MaterialTheme.typography.labelLarge,modifier=Modifier.weight(1f))
                        }
                    }
                    Text(local.format(DateTimeFormatter.ofPattern("EEEE",locale)),color=Color.White,style=MaterialTheme.typography.bodySmall.copy(shadow=textShadow))
                    Text(local.format(DateTimeFormatter.ofPattern("d MMMM yyyy",locale)),color=Color.White,style=MaterialTheme.typography.bodySmall.copy(shadow=textShadow),modifier=Modifier.testTag("prayer_gregorian_date"))
                }
            }
            if(preferences.location==null)Text(label(language,"Aktifkan lokasi untuk menampilkan jadwal salat.","Set a location to see prayer times."),color=Color.White,style=MaterialTheme.typography.bodySmall.copy(shadow=textShadow))
            Surface(onClick={onNavigate("prayer")},shape=RoundedCornerShape(20.dp),color=Color(0xFF17253B),modifier=Modifier.fillMaxWidth().testTag("prayer_times_strip")) {
                Row(Modifier.padding(6.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                    val moments=day?.moments?.filter{it.isPrayer}.orEmpty()
                    val names=if(language=="en")listOf("Fajr","Dhuhr","Asr","Maghrib","Isha") else listOf("Subuh","Zuhur","Asar","Magrib","Isya")
                    repeat(5) { index ->
                        val moment=moments.getOrNull(index);val upcoming=moment!=null&&moment.instant==next?.instant
                        Surface(color=if(upcoming)Color(0xFF486CA2) else Color(0xFF17253B),shape=RoundedCornerShape(14.dp),modifier=Modifier.weight(1f)) {
                            Column(Modifier.padding(horizontal=2.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text(moment?.name(language)?:names[index],color=Color.White,fontSize=12.sp,lineHeight=18.sp,textAlign=TextAlign.Center)
                                Text(moment?.instant?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm"))?:"—",color=if(upcoming)Color.White else Color(0xFFBACDF4),fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
    if(locationDialog)AlertDialog(onDismissRequest={locationDialog=false},title={Text(label(language,"Ubah lokasi?","Change location?"))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(label(language,"Pilih lokasi otomatis dari HP atau pilih kota secara manual.","Use your device location or choose a city manually."))
        DeviceLocationButton(language,onLocated={locationDialog=false},onManualLocation={locationDialog=false;onNavigate("location")})
    }},confirmButton={TextButton(onClick={locationDialog=false;onNavigate("location")}){Text(label(language,"Ubah manual","Choose manually"))}},dismissButton={TextButton(onClick={locationDialog=false}){Text(label(language,"Batal","Cancel"))}})
}
