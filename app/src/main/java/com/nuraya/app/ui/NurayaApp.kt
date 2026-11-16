package com.nuraya.app.ui

import com.nuraya.app.ui.icons.AppIcons as Icons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.NurayaApplication
import com.nuraya.app.learning.LearningScreen
import com.nuraya.app.prayer.PrayerScreen
import com.nuraya.app.prayer.LocationOnboarding
import com.nuraya.app.prayer.QiblaScreen
import com.nuraya.app.profile.ProfileScreen
import com.nuraya.app.profile.AboutScreen
import com.nuraya.app.quranreading.QuranModesScreen
import com.nuraya.app.quranreading.QuranReadingModes
import com.nuraya.app.study.PracticeScreen
import com.nuraya.app.recitation.RecitationPracticeScreen
import com.nuraya.app.storage.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurayaApp(app: NurayaApplication, settings: UserSettings) {
    val language = settings.language
    var screen by rememberSaveable { mutableStateOf("home") }
    var readerFrom by rememberSaveable { mutableStateOf("quran") }
    var surahId by rememberSaveable { mutableIntStateOf(1) }
    var ayahId by rememberSaveable { mutableIntStateOf(1) }
    var juzId by rememberSaveable { mutableIntStateOf(0) }
    var qariId by rememberSaveable { mutableStateOf("") }
    var mushafFrom by rememberSaveable { mutableStateOf("surahs") }
    var readingModeId by rememberSaveable { mutableStateOf("standard") }
    var settingsFrom by rememberSaveable { mutableStateOf("home") }
    var aboutFrom by rememberSaveable { mutableStateOf("profile") }
    var learningFrom by rememberSaveable { mutableStateOf("home") }
    var playerVisible by rememberSaveable { mutableStateOf(false) }
    var learningSessionVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(!app.launchArtworkShown) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val screenState = rememberSaveableStateHolder()
    val entries by app.library.entries.collectAsStateWithLifecycle(emptyList())
    val downloads by app.downloads.downloads.collectAsStateWithLifecycle(emptyList())
    val playback by app.playback.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val perform: (suspend () -> Unit) -> Unit = { block -> scope.launch {
        try { block() } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            snackbar.showSnackbar(label(language, "Tindakan belum berhasil. Coba kembali.", "That action could not be completed. Please try again."))
        }
    } }
    val chooseQari: (String) -> Unit = { id ->
        qariId = id
        com.nuraya.app.media.CuratedReciters.verseIdFor(id)?.let { verseId ->
            perform { app.settings.setReaderQari(verseId) }
        }
    }
    var readerWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(screen) {
        val readerOpen = screen in setOf("reader", "mushaf")
        if (readerWasOpen && !readerOpen) app.playback.stopReading()
        readerWasOpen = readerOpen
    }
    LaunchedEffect(retry) {
        val showLaunchArtwork = !app.launchArtworkShown
        loading = showLaunchArtwork
        val started=android.os.SystemClock.elapsedRealtime()
        loadError = false
        try { withContext(Dispatchers.IO) { app.quran.surahs; app.quran.verses(1) } }
        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; loadError = true }
        if (showLaunchArtwork) kotlinx.coroutines.delay((700L-(android.os.SystemClock.elapsedRealtime()-started)).coerceAtLeast(0L))
        if (!loadError) app.launchArtworkShown = true
        loading = false
    }
    LaunchedEffect(app) {
        val store=com.nuraya.app.prayer.PrayerPreferenceStore(app)
        val old=store.read().location
        if(old!=null && old.name in setOf("Lokasi HP","Device location","Lokasi tersimpan","Saved location") && com.nuraya.app.prayer.hasDeviceLocationPermission(app)) {
            val name=com.nuraya.app.prayer.DistrictNameResolver(app).resolve(old)
            val latest=store.read()
            if(name!=null && latest.location==old)store.save(latest.copy(location=old.copy(name=name)))
        }
    }
    if (loading) {
        LaunchArtwork()
        return
    }
    val profileStore = remember(app) { com.nuraya.app.profile.ProfileStore(app) }
    var profileComplete by remember { mutableStateOf(profileStore.isComplete()) }
    if (!profileComplete) {
        ScreenColors("profile", settings.darkMode) {
            com.nuraya.app.profile.ProfileOnboarding(language) { profileComplete = profileStore.isComplete() }
        }
        return
    }
    val openVerse: (Int, Int) -> Unit = { surah, ayah ->
        if (screen != "reader") readerFrom = screen
        if (screen !in listOf("reader", "surahs", "quran")) {
            val savedMode = entries.firstOrNull { it.type == "read_history" && it.key == "$surah:$ayah" }?.extra?.substringAfter(":")
            readingModeId = savedMode?.takeIf { id -> QuranReadingModes.all.any { it.id == id } } ?: "indonesia"
        }
        surahId = surah; ayahId = ayah; juzId = 0
        val openBook = screen != "reader" && QuranReadingModes.opensPageReader(readingModeId)
        if (openBook) mushafFrom = screen
        screen = if (openBook) "mushaf" else "reader"
    }
    val reminderRequest by com.nuraya.app.reminders.ReminderNavigation.requests.collectAsStateWithLifecycle()
    LaunchedEffect(reminderRequest) {
        reminderRequest?.let { request ->
            if(request.surah!=null&&request.ayah!=null&&app.quran.verse(request.surah,request.ayah)!=null) {
                // Notification references must not be replaced by a saved reading mode/position.
                mushafFrom = if(screen in setOf("reader","mushaf")) readerFrom else screen
                readerFrom = mushafFrom
                readingModeId="indonesia";surahId=request.surah;ayahId=request.ayah;juzId=0
                playerVisible=false;screen="mushaf"
            } else if(request.surah==null)screen="prayer"
            com.nuraya.app.reminders.ReminderNavigation.consume(request)
        }
    }
    LaunchedEffect(Unit) {
        if (QuranReadingModes.all.none { it.id == readingModeId }) { readingModeId = "standard"; screen = "quran" }
    }
    val back: () -> Unit = {
        when {
            screen == "reader" -> screen = readerFrom
            screen == "mushaf" -> screen = mushafFrom
            screen == "surahs" -> screen = "quran"
            screen == "settings" -> screen = settingsFrom
            screen == "about" -> screen = aboutFrom
            screen == "learn" -> screen = learningFrom
            screen == "progress" -> screen = "profile"
            screen == "murottal" && qariId.isNotEmpty() -> qariId = ""
            else -> screen = "home"
        }
    }
    BackHandler(screen != "home" && !playerVisible) { back() }
    val titles = mapOf("home" to "NURAYA", "quran" to "Al-Qur’an", "murottal" to "Murottal",
        "prayer" to label(language, "Salat & kiblat", "Prayer & qibla"),
        "qibla" to label(language,"Kiblat","Qibla"), "location" to label(language,"Lokasi","Location"),
        "profile" to label(language,"Profil saya","My profile"),"about" to label(language,"Tentang Nuraya","About Nuraya"),
        "surahs" to QuranReadingModes.find(readingModeId).title(language),
        "mushaf" to QuranReadingModes.find(readingModeId).title(language),
        "library" to label(language, "Koleksi saya", "My collection"),
        "settings" to label(language, "Pengaturan", "Settings"),
        "learn" to label(language, "Cari surah & ayat", "Find surahs & verses"),
        "hadith" to label(language, "Cari hadis", "Find hadith"),
        "practice" to label(language, "Latihan & hafalan", "Practice & memorization"),
        "progress" to label(language, "Progres belajarku", "My learning progress"),
        "recitation" to label(language, "AI bacaan", "Recitation AI"),
        "reader" to if (juzId > 0) "Juz $juzId" else if (!loading && !loadError) app.quran.surahs.firstOrNull { it.id == surahId }?.name.orEmpty() else "Al-Qur’an")
    ScreenColors(screen, settings.darkMode) {
    Scaffold(
        containerColor = if (screen == "qibla") androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(if (screen == "home") WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom) else WindowInsets.safeDrawing),
        contentWindowInsets = if (screen == "home") WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (screen !in setOf("home", "mushaf", "reader") && !learningSessionVisible) TopAppBar(title = { Text(titles[screen].orEmpty(), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (screen !in listOf("home", "quran", "murottal", "prayer") || screen == "murottal" && qariId.isNotEmpty()) {
                        TextButton(onClick = back, modifier = Modifier.testTag("back_button")) {
                            if (screen != "murottal") Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                            Text(label(language,"Kembali","Back"))
                        }
                    }
                }, actions = {
                    if (screen != "settings") IconButton(onClick = { settingsFrom = screen; screen = "settings" }, modifier = Modifier.size(56.dp).testTag("settings_button")) {
                        Icon(Icons.Outlined.Settings, label(language, "Pengaturan", "Settings"))
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
        },
        bottomBar = {
            Column {
                if (playback.surahId > 0 && screen !in setOf("mushaf", "reader") && !learningSessionVisible) MiniPlayer(playback, language, onOpen = { playerVisible = true }, onToggle = { app.playback.toggle() })
                if (screen !in listOf("reader", "mushaf", "learn", "hadith", "practice", "progress", "recitation", "qibla")) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent, tonalElevation = 0.dp) {
                    val items = listOf(
                        Triple("home", label(language, "Beranda", "Home"), Icons.Outlined.Home),
                        Triple("quran", "Al-Qur’an", Icons.Outlined.MenuBook),
                        Triple("murottal", "Murottal", Icons.Outlined.Headphones),
                        Triple("prayer", label(language, "Ibadah", "Prayer"), Icons.Outlined.Explore)
                    )
                    items.forEach { (key, title, icon) ->
                        NavigationBarItem(selected = screen == key || (key == "quran" && screen == "surahs"), onClick = { screen = key },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.ui.graphics.Color.Transparent),
                            icon = { Icon(icon, null) }, label = { Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 2) },
                            modifier = Modifier.testTag("nav_$key"))
                    }
                }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets), contentAlignment = Alignment.TopCenter) {
            Box(if (screen in setOf("mushaf")) Modifier.fillMaxSize() else Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                when {
                    loadError -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label(language, "Data bacaan belum dapat dibuka.", "The reading data could not be opened."))
                        Button(onClick = { retry++ }) { Text(label(language, "Coba lagi", "Try again")) }
                    }
                    else -> screenState.SaveableStateProvider(
                        if (screen in setOf("reader", "mushaf")) "$screen:$readingModeId:$surahId:$ayahId:$juzId" else screen
                    ) { when(screen) {
                        "home" -> HomeScreen(app, language, entries, onRead = openVerse, onNavigate = {
                            if (it == "settings") settingsFrom = "home"
                            if (it == "learn") learningFrom = "home"
                            if(it.startsWith("qari:")){chooseQari(it.removePrefix("qari:"));screen="murottal"} else screen=it
                        })
                        "quran" -> QuranModesScreen(app, language,
                            onMode = { readingModeId = it; screen = "surahs" },
                            onRead = { surah, ayah -> readingModeId = "thematic"; openVerse(surah, ayah) }, onSearch = { learningFrom = "quran"; screen = "learn" })
                        "surahs" -> QuranScreen(app.quran, language, onRead = openVerse, onJuz = {
                            readerFrom = "surahs"
                            if (QuranReadingModes.opensPageReader(readingModeId)) {
                                val first = app.quran.juz(it).first()
                                surahId = first.surah; ayahId = first.ayah; juzId = 0; mushafFrom = "surahs"; screen = "mushaf"
                            } else { juzId = it; ayahId = 1; screen = "reader" }
                        }, readingModeId = readingModeId, onChooseMode = { screen = "quran" })
                        "mushaf" -> MadinahScreen(language, surahId, ayahId,
                            showTajwidCompanion = QuranReadingModes.usesTajwid(readingModeId),
                            edition = QuranReadingModes.pageEdition(readingModeId), readingModeId = readingModeId,
                            printedEdition = com.nuraya.app.quranreading.PrintedMushafEdition.forMode(readingModeId),
                            app = app, settings = QuranReadingModes.settings(readingModeId, settings), onBack = back,
                            onReadVerses = { surah, ayah ->
                                if (mushafFrom != "reader") readerFrom = "mushaf"; surahId = surah; ayahId = ayah; juzId = 0; screen = "reader"
                            })
                        "reader" -> com.nuraya.app.quranreading.StudyBookScreen(app, settings, surahId, ayahId, juzId,
                            modeId = readingModeId, onBack = back, onOpenMushaf = { surah, ayah ->
                                surahId = surah; ayahId = ayah
                                if (readerFrom != "mushaf") mushafFrom = "reader"
                                screen = "mushaf"
                            })
                        "murottal" -> MurottalScreen(app, language, qariId, onQari = chooseQari, entries, downloads, playback, perform)
                        "prayer" -> PrayerScreen(language)
                        "location" -> PrayerScreen(language,"location")
                        "qibla" -> QiblaScreen(language,onManualLocation={screen="location"})
                        "profile" -> ProfileScreen(language) { if(it=="settings")settingsFrom="profile";if(it=="about")aboutFrom="profile";if(it=="progress")screenState.removeState("progress");screen=it }
                        "about" -> AboutScreen(language)
                        "library" -> LibraryScreen(app, language, entries, downloads, perform, onRead = openVerse)
                        "settings" -> SettingsScreen(app, settings, perform, onLearn = { learningFrom = "settings"; screen = "learn" }, onAbout = { aboutFrom="settings";screen = "about" })
                        "learn" -> LearningScreen(language, onRead = openVerse)
                        "hadith" -> com.nuraya.app.hadith.HadithScreen(language)
                        "practice", "progress" -> PracticeScreen(app, language, onRead = openVerse, initialTab = if(screen=="progress")3 else 1, onSessionVisibility={learningSessionVisible=it},
                            onPracticeRange = { surah, start, end, repeats ->
                                app.playback.playVerseRange(surah, start, end, settings.readerQariId, repeats)
                                openVerse(surah, start)
                            })
                        "recitation" -> RecitationPracticeScreen(app, language, onRead = openVerse)
                    } }
                }
            }
        }
    }
    var locationPromptVisible by remember { mutableStateOf(com.nuraya.app.prayer.LocationPromptStore(app).shouldIntroduce(com.nuraya.app.prayer.PrayerPreferenceStore(app).read().location != null)) }
    if (!loading && !loadError) LocationOnboarding(language, onManualLocation = { screen = "location" }, onVisibility = { locationPromptVisible = it })
    val offlineEvent by com.nuraya.app.reminders.NetworkNotice.events.collectAsStateWithLifecycle()
    com.nuraya.app.reminders.NotificationPermissionNotice(language, allowedToShow = screen=="home"&&!locationPromptVisible&&!playerVisible&&offlineEvent==null)
    com.nuraya.app.reminders.OfflineNotice(language, allowedToShow = !locationPromptVisible)
    if (playerVisible && !loading && !loadError) {
        ModalBottomSheet(onDismissRequest = { playerVisible = false }) {
            FullPlayer(app, language, playback, entries, downloads, perform, onClose = { playerVisible = false },
                onRead = { surah, ayah -> playerVisible = false; openVerse(surah, ayah) })
        }
    }
    }
}
