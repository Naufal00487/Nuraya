package com.nuraya.app.prayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nuraya.app.ui.label
import com.nuraya.app.ui.SectionArtwork
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun rememberCurrentInstant(): Instant {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { now = Instant.now(); delay(1_000) }
        }
    }
    return now
}

private fun displayHijri(date: LocalDate, prefs: PrayerPreferences, language: String) =
    runCatching { HijriCalendar.label(date, prefs.hijriAdjustment, language) }.getOrElse {
        label(language, "Tanggal di luar rentang kalender Hijriah", "Date outside the Hijri calendar range")
    }

@Composable
fun PrayerDashboardCard(language: String, onOpen: () -> Unit) {
    val (preferences, _) = rememberPrayerPreferences()
    val now = rememberCurrentInstant()
    val zone = preferences.location?.zone ?: ZoneId.systemDefault()
    val localNow = now.atZone(zone)
    val minute = now.epochSecond / 60
    val next = remember(preferences, minute) {
        preferences.location?.let { PrayerCalculator.next(now, it, preferences.minuteAdjustment) }
    }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("prayer_dashboard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(displayHijri(localNow.toLocalDate(), preferences, language), style = MaterialTheme.typography.titleMedium)
                    Text(preferences.location?.name ?: label(language, "Waktu perangkat · pilih kota", "Device time · choose a city"), style = MaterialTheme.typography.bodySmall)
                }
                Text(localNow.format(DateTimeFormatter.ofPattern("HH:mm:ss")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(if (next != null) "${label(language, "Menuju", "Until")} ${next.name(language)}  ${PrayerCalculator.countdown(now, next.instant)}"
                else if (preferences.location == null) label(language, "Pilih kota untuk jadwal salat", "Choose a city for prayer times")
                else label(language, "Jadwal belum dapat dihitung", "Prayer times unavailable"), style = MaterialTheme.typography.titleMedium)
            Text(label(language, "Buka jadwal salat & arah kiblat →", "Open prayer times & Qibla →"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PrayerScreen(language: String, initialTab: String? = null) {
    val (preferences, save) = rememberPrayerPreferences()
    val now = rememberCurrentInstant()
    val zone = preferences.location?.zone ?: ZoneId.systemDefault()
    val localNow = now.atZone(zone)
    var tab by rememberSaveable { mutableStateOf(initialTab ?: if (preferences.location == null) "location" else "schedule") }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val date = localNow.toLocalDate().plusDays(dayOffset.toLong())
    val day = remember(date, preferences) { preferences.location?.let { PrayerCalculator.calculate(date, it, preferences.minuteAdjustment) } }
    val minute = now.epochSecond / 60
    val next = remember(preferences, minute) { preferences.location?.let { PrayerCalculator.next(now, it, preferences.minuteAdjustment) } }
    val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("id-ID")
    val listState = rememberLazyListState()
    LaunchedEffect(tab) { listState.scrollToItem(0) }
    LazyColumn(Modifier.fillMaxSize().testTag("prayer_screen"), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionArtwork("prayer",next?.let{"${it.name(language)} · ${it.instant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))}"}
                ?: label(language,"Jadwal salat","Prayer times"),preferences.location?.name ?: label(language,"Pilih lokasi Anda","Choose your location"))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().testTag("prayer_calendar"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(displayHijri(localNow.toLocalDate(),preferences,language),style=MaterialTheme.typography.titleMedium)
                    Text(localNow.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",locale)),style=MaterialTheme.typography.bodySmall)
                    if(next!=null)Text(PrayerCalculator.countdown(now,next.instant)+label(language," menuju salat"," until prayer"),modifier=Modifier.testTag("prayer_countdown"),style=MaterialTheme.typography.bodyMedium)
                }
                AdhanToggle(language)
            }
        } }
        item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("schedule" to label(language, "Jadwal", "Times"), "qibla" to label(language, "Kiblat", "Qibla"), "location" to label(language, "Lokasi", "Place")).forEach { (id, title) ->
                    FilledTonalButton(onClick = { tab = id }, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("prayer_tab_$id"),
                        contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (tab == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (tab == id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)) { Text(title) }
                }
            }
        } }
        when (tab) {
            "location" -> {
                item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label(language, "Lokasi untuk salat & kiblat", "Location for prayer & Qibla"), style = MaterialTheme.typography.headlineSmall)
                    Text(label(language, "Gunakan lokasi HP agar koordinat terisi otomatis. Lokasi disimpan di HP dan tidak dilacak di latar belakang. Layanan Android dapat memakai internet untuk mencari nama kecamatan. Pilihan kota tetap tersedia jika lokasi HP belum dapat digunakan.",
                        "Use your phone’s location to fill coordinates automatically. The selection stays on this phone, without background tracking. You can still choose a city if device location is unavailable."), style = MaterialTheme.typography.bodyLarge)
                } }
                item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeviceLocationButton(language, onLocated = { tab = "schedule"; dayOffset = 0 })
                } }
                item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(cityQuery, { cityQuery = it.take(80) }, singleLine = true,
                        label = { Text(label(language, "Cari kota", "Search cities")) },
                        modifier = Modifier.fillMaxWidth().testTag("prayer_city_search"))
                    Spacer(Modifier.height(12.dp))
                    ManualLocation(preferences.location, language) { location ->
                        save(preferences.copy(location = location)); tab = "schedule"; dayOffset = 0
                    }
                } }
                val cities = PrayerCities.all.filter { cityQuery.isBlank() || it.name.contains(cityQuery.trim(), ignoreCase = true) }
                if (cities.isEmpty()) item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language, "Kota belum ada dalam daftar. Pilih Isi lokasi lain / koordinat di atas.",
                    "This city is not listed. Use Enter another place / coordinates above.")) } }
                items(cities, key = { it.name }) { city ->
                    OutlinedButton(onClick = { save(preferences.copy(location = city)); tab = "schedule"; dayOffset = 0 },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("prayer_city_${city.name}")) {
                        Text(city.name + if (preferences.location == city) " ✓" else "", Modifier.weight(1f))
                        Text(when (city.zoneId) { "Asia/Makassar" -> "WITA"; "Asia/Jayapura" -> "WIT"; else -> "WIB" })
                    }
                }
                item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { PreferenceAdjustments(preferences, save, language) } }
                if (preferences.location != null) item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { save(preferences.copy(location = null)); dayOffset = 0 },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(label(language, "Hapus lokasi tersimpan", "Clear saved location"))
                    }
                } }
            }
            "qibla" -> {
                val location = preferences.location
                if (location == null) item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { SelectLocationPrompt(language) { tab = "location" } } }
                else item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { QiblaPanel(location, language) } }
            }
            else -> {
                if (preferences.location == null) item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { SelectLocationPrompt(language) { tab = "location" } } }
                else {
                    item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(label(language,"Pengingat salat harian","Daily prayer reminders"),style=MaterialTheme.typography.titleLarge)
                        Text(label(language,"Aktifkan salat yang ingin diingatkan: 10 menit sebelum dan saat waktunya. Suara azan dapat diatur melalui ikon pengeras suara.","Choose prayers to receive reminders ten minutes before and at their time. Control adhan sound with the speaker icon."),style=MaterialTheme.typography.bodyMedium)
                    } }
                    item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)), style = MaterialTheme.typography.titleLarge)
                        Text(displayHijri(date, preferences, language), style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { dayOffset-- }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = dayOffset > -366,
                                contentPadding = PaddingValues(horizontal = 6.dp)) { Text(label(language, "Kemarin", "Previous")) }
                            OutlinedButton(onClick = { dayOffset = 0 }, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)) { Text(label(language, "Hari ini", "Today")) }
                            OutlinedButton(onClick = { dayOffset++ }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = dayOffset < 366,
                                contentPadding = PaddingValues(horizontal = 6.dp)) { Text(label(language, "Besok", "Next")) }
                        }
                    } }
                    if (day?.available == true) items(day.moments.filter { it.isPrayer }, key = { it.id }) { moment ->
                        Surface(color = if (dayOffset == 0 && next != null && moment.instant == next.instant) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(when(moment.id){"fajr"->Icons.Outlined.WbTwilight;"dhuhr"->Icons.Outlined.LightMode;"asr"->Icons.Outlined.WbSunny;"maghrib"->Icons.Outlined.WbTwilight;else->Icons.Outlined.DarkMode},null,tint=MaterialTheme.colorScheme.secondary)
                                Column(Modifier.weight(1f)) {
                                    Text(moment.name(language),style=MaterialTheme.typography.titleLarge)
                                    Text(moment.instant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                                }
                                PrayerReminderSwitch(moment.id,moment.name(language),language)
                            }
                        }
                    } else item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language, "Perhitungan tidak tersedia untuk lokasi/tanggal ini. Periksa jadwal otoritas setempat; aplikasi tidak membuat waktu pengganti.",
                        "Calculation is unavailable for this location/date. Consult your local authority; the app does not invent replacement times."), style = MaterialTheme.typography.bodyLarge) } }
                    if (day?.highLatitude == true) item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(label(language, "Lintang tinggi: Subuh dan Isya memakai batas porsi malam berbasis sudut. Cocokkan dengan pedoman wilayah Anda.",
                        "High latitude: Fajr and Isha use twilight-angle night limits. Check the guidance for your region."), style = MaterialTheme.typography.bodyLarge) } }
                    item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(label(language, "Perhitungan offline: Subuh 20°, Isya 18°, Asar Syafii. Koreksi waktu ${preferences.minuteAdjustment} menit (terbit tanpa koreksi). Ini bukan jadwal resmi Kemenag; cocokkan dengan jadwal masjid setempat.",
                            "Offline calculation: Fajr 20°, Isha 18°, Shafi Asr. Time correction: ${preferences.minuteAdjustment} minutes (sunrise unchanged). This is not an official ministry timetable; compare with your local mosque."), style = MaterialTheme.typography.bodySmall)
                        Text(label(language, "Kalender Hijriah memakai Umm al-Qura berdasarkan tanggal sipil dan dapat berbeda dari keputusan rukyat setempat. Koreksi hari tersedia di Lokasi.",
                            "The Hijri calendar uses Umm al-Qura for the civil date and may differ from local moon-sighting decisions. Day adjustment is available under Place."), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { tab = "location" }, modifier = Modifier.heightIn(min = 56.dp)) { Text(label(language, "Ubah lokasi & koreksi waktu", "Change location & corrections")) }
                    } }
                }
            }
        }
    }
}

@Composable
private fun SelectLocationPrompt(language: String, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label(language, "Aktifkan lokasi untuk jadwal & kiblat", "Set a location for prayer & Qibla"), style = MaterialTheme.typography.titleLarge)
        Button(onClick = onSelect, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label(language, "Gunakan lokasi HP", "Use device location")) }
    }
}

@Composable
private fun ManualLocation(current: PrayerLocation?, language: String, onSave: (PrayerLocation) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(current?.name.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(current?.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(current?.longitude?.toString().orEmpty()) }
    var zone by rememberSaveable { mutableStateOf(current?.zoneId ?: "Asia/Jakarta") }
    var error by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(label(language, "Isi lokasi lain / koordinat", "Enter another place / coordinates"))
        }
        if (expanded) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text(label(language, "Nama lokasi", "Location name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(latitude, { latitude = it.take(20) }, label = { Text(label(language, "Lintang (−89 sampai 89)", "Latitude (−89 to 89)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            OutlinedTextField(longitude, { longitude = it.take(20) }, label = { Text(label(language, "Bujur (−180 sampai 180)", "Longitude (−180 to 180)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            OutlinedTextField(zone, { zone = it.take(80) }, label = { Text(label(language, "Zona waktu, contoh Asia/Jakarta", "Time zone, e.g. Asia/Jakarta")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (error) Text(label(language, "Periksa nama, koordinat, dan zona waktu. Gunakan titik untuk desimal dan zona seperti Asia/Jakarta, Asia/Makassar, atau Asia/Jayapura.",
                "Check the name, coordinates and time zone. Use decimal points and a zone such as Asia/Jakarta, Asia/Makassar or Asia/Jayapura."), color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                val location = runCatching { PrayerLocation(name.trim(), latitude.trim().toDouble(), longitude.trim().toDouble(), zone.trim()) }.getOrNull()
                error = location == null
                if (location != null) onSave(location)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label(language, "Simpan lokasi", "Save location")) }
        }
    }
}

@Composable
private fun PreferenceAdjustments(preferences: PrayerPreferences, save: (PrayerPreferences) -> Unit, language: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label(language, "Penyesuaian jadwal", "Schedule adjustments"), style = MaterialTheme.typography.titleLarge)
        Text(label(language, "Koreksi kalender Hijriah: ${preferences.hijriAdjustment} hari", "Hijri date adjustment: ${preferences.hijriAdjustment} days"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { save(preferences.copy(hijriAdjustment = preferences.hijriAdjustment - 1)) }, enabled = preferences.hijriAdjustment > -2,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(label(language, "Kurangi 1 hari", "Minus 1 day")) }
            OutlinedButton(onClick = { save(preferences.copy(hijriAdjustment = preferences.hijriAdjustment + 1)) }, enabled = preferences.hijriAdjustment < 2,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(label(language, "Tambah 1 hari", "Plus 1 day")) }
        }
        Text(label(language, "Koreksi waktu salat: ${preferences.minuteAdjustment} menit", "Prayer time adjustment: ${preferences.minuteAdjustment} minutes"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { save(preferences.copy(minuteAdjustment = preferences.minuteAdjustment - 1)) }, enabled = preferences.minuteAdjustment > -30,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(label(language, "Kurangi 1 menit", "Minus 1 minute")) }
            OutlinedButton(onClick = { save(preferences.copy(minuteAdjustment = preferences.minuteAdjustment + 1)) }, enabled = preferences.minuteAdjustment < 30,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(label(language, "Tambah 1 menit", "Plus 1 minute")) }
        }
        Text(label(language, "Koreksi menit berlaku untuk kelima waktu salat. Pilihan ini tidak mengubah jam perangkat.",
            "Minute correction applies to all five prayers. These settings do not change the device clock."), style = MaterialTheme.typography.bodySmall)
    }
}
