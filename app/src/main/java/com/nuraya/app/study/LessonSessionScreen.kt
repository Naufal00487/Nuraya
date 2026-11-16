package com.nuraya.app.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.QuranArabicFont
import com.nuraya.app.ui.annotatedQuranText
import com.nuraya.app.ui.icons.AppIcons as Icons
import com.nuraya.app.ui.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonPreview(lesson: LearningLesson, app: NurayaApplication, settings: UserSettings, language: String,
    resume: Boolean, onClose:()->Unit, onStart:()->Unit) {
    DisposableEffect(lesson.id){onDispose{app.playback.stopReading()}}
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=28.dp).testTag("lesson_preview"),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text(lesson.title,style=MaterialTheme.typography.headlineSmall)
            Text(listOf("Dasar","Menengah","Lanjutan")[lesson.level]+" · ${if(lesson.kind==LearningKind.TAJWID)5 else 3+app.quran.verses(lesson.surah).size} langkah · "+if(lesson.kind==LearningKind.TAJWID)"Tajwid" else "Hafalan",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Text(lesson.explanation)
            LearningVerse(app,settings,lesson.surah,lesson.ayah,language)
            Text(lesson.sourceTitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick=onStart,modifier=Modifier.fillMaxWidth().heightIn(min=58.dp).testTag("lesson_start"),shape=RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(10.dp));Text(label(language,if(resume)"Lanjutkan latihan" else "Mulai latihan",if(resume)"Resume practice" else "Start practice"))
            }
        }
    }
}

@Composable
internal fun LearningVerse(app: NurayaApplication, settings: UserSettings, surah: Int, ayah: Int, language: String, showText: Boolean=true) {
    val verse=requireNotNull(app.quran.verse(surah,ayah))
    val playback by app.playback.state.collectAsStateWithLifecycle()
    val playing=playback.verseMode&&playback.surahId==surah&&playback.ayahId==ayah&&playback.isPlaying
    val study by produceState<StudyVerse?>(null,surah,ayah) { value=withContext(Dispatchers.IO){app.study.verse(surah,ayah)} }
    Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("${app.quran.surahs[surah-1].name} · $surah:$ayah",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(showText) Text(annotatedQuranText(verse.arabic,study,settings.tajwidColors,settings.darkMode),modifier=Modifier.fillMaxWidth().testTag("learning_verse_${surah}_$ayah"),
                fontFamily=QuranArabicFont,fontSize=settings.arabicSize.sp,lineHeight=(settings.arabicSize*1.8f).sp,textAlign=TextAlign.Right,style=MaterialTheme.typography.bodyLarge.copy(textDirection=TextDirection.Rtl))
            OutlinedButton(onClick={if(playing)app.playback.stopReading() else app.playback.playVerseRange(surah,ayah,ayah,settings.readerQariId)},modifier=Modifier.testTag("learning_listen")) {
                Icon(if(playing)Icons.Outlined.Stop else Icons.Outlined.VolumeUp,null);Spacer(Modifier.width(10.dp))
                Text(label(language,if(playing)"Hentikan" else "Dengarkan contoh",if(playing)"Stop" else "Listen to the example"))
            }
        }
    }
}

@Composable
internal fun LessonSessionScreen(app: NurayaApplication, course: LearningCourse, session: LearningSession, settings: UserSettings, language: String,
    busy: Boolean, onSubmit:(LearningExercise,List<String>)->Unit, onNext:()->Unit, onClose:()->Unit) {
    val lesson=course.lesson(session.lessonId)
    val exercises=remember(course,session.lessonId,session.seed){course.exercises(session.lessonId,session.seed)}
    val exercise=exercises[session.baseIndex]
    var choices by rememberSaveable(session.id,session.step){mutableStateOf<List<String>>(emptyList())}
    var pairs by rememberSaveable(session.id,session.step){mutableStateOf(List(exercise.left.size){""})}
    var matching by rememberSaveable(session.id,session.step){mutableIntStateOf(0)}
    var revealed by rememberSaveable(session.id,session.step){mutableStateOf(false)}
    val saved=session.answers.getOrNull(session.step)
    val response=if(exercise.kind==ExerciseKind.MATCH)pairs else choices
    val ready=if(exercise.kind==ExerciseKind.RECALL)choices.size==1&&revealed else response.size==exercise.expected.size&&response.none{it.isBlank()}
    val fraction by animateFloatAsState((session.step.toFloat()/session.totalSteps).coerceIn(0f,1f),label="Lesson progress")
    BackHandler(onBack=onClose)
    DisposableEffect(session.id) { onDispose { app.playback.stopReading() } }
    LaunchedEffect(session.step){app.playback.stopReading()}
    Scaffold(contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={
        Surface(color=MaterialTheme.colorScheme.surface,tonalElevation=2.dp,shadowElevation=4.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(session.checked) {
                    val correct=saved?.correct==true
                    Column(Modifier.heightIn(max=210.dp).verticalScroll(rememberScrollState()).testTag("lesson_feedback"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Icon(if(correct)Icons.Outlined.CheckCircle else Icons.Outlined.Info,null,tint=if(correct)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(if(!exercise.scored)label(language,"Catatan tersimpan","Note saved") else if(correct)label(language,"Tepat!","That's right!") else label(language,"Belum tepat, mari periksa","Not quite. Let's look again"),style=MaterialTheme.typography.titleSmall)
                        }
                        if(!correct&&exercise.scored) {
                            val answer=if(exercise.kind==ExerciseKind.MATCH)exercise.left.joinToString("\n"){ left -> "${left.text} → ${exercise.options.first { it.id==left.id }.text}" }
                                else exercise.expected.joinToString("\n"){ id -> exercise.options.first{it.id==id}.text }
                            Text(answer,fontFamily=if(exercise.arabicOptions)QuranArabicFont else null,fontSize=if(exercise.arabicOptions)28.sp else 18.sp,lineHeight=if(exercise.arabicOptions)48.sp else 27.sp)
                        }
                        Text(exercise.explanation,style=MaterialTheme.typography.bodyMedium)
                        if(!correct&&!session.reviewing)Text(label(language,"Kita coba lagi di akhir sesi.","We'll revisit this at the end."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                    }
                }
                Button(onClick={if(session.checked)onNext() else onSubmit(exercise,response)},enabled=!busy&&(session.checked||ready),modifier=Modifier.fillMaxWidth().heightIn(min=58.dp).testTag(if(session.checked)"lesson_next" else "lesson_check"),shape=RoundedCornerShape(18.dp)) {
                    Text(label(language,if(session.checked)if(session.step+1==session.totalSteps)"Selesaikan" else "Lanjut" else if(exercise.kind==ExerciseKind.RECALL)"Simpan catatan" else "Periksa jawaban",
                        if(session.checked)if(session.step+1==session.totalSteps)"Finish" else "Continue" else if(exercise.kind==ExerciseKind.RECALL)"Save note" else "Check answer"))
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("lesson_session")) {
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                IconButton(onClick=onClose,modifier=Modifier.testTag("lesson_pause")){Icon(Icons.Outlined.Close,label(language,"Simpan dan keluar","Save and leave"))}
                LinearProgressIndicator(progress={fraction},modifier=Modifier.weight(1f).height(10.dp),strokeCap=androidx.compose.ui.graphics.StrokeCap.Round)
                Text("${session.step+1}/${session.totalSteps}",style=MaterialTheme.typography.labelMedium)
            }
            LazyColumn(Modifier.fillMaxSize().testTag("lesson_content"),contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                item { Text(if(session.reviewing)label(language,"Ulangi bersama","Let's review") else lesson.title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp));Text(exercise.prompt,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.testTag("lesson_prompt")) }
                if(exercise.surah!=null&&exercise.ayah!=null) item { LearningVerse(app,settings,exercise.surah,exercise.ayah,language,showText=exercise.kind!=ExerciseKind.RECALL||revealed||session.checked) }
                if(exercise.kind==ExerciseKind.RECALL) {
                    if(!revealed&&!session.checked) item {
                        Text(label(language,"Baca dari ingatan atau dengarkan contohnya dahulu. Setelah itu tampilkan teks untuk memeriksa.","Try from memory, or listen first. Then reveal the text to check."))
                        OutlinedButton(onClick={revealed=true},modifier=Modifier.fillMaxWidth().testTag("lesson_reveal")){Icon(Icons.Outlined.Visibility,null);Spacer(Modifier.width(10.dp));Text(label(language,"Tampilkan & periksa","Reveal and check"))}
                    }
                    if(revealed||session.checked) {
                        item { Text(label(language,"Bagaimana menurutmu?","How did it feel?"),style=MaterialTheme.typography.titleSmall) }
                        itemsIndexed(RecallRating.entries) { _,rating ->
                            val title=when(rating){RecallRating.AGAIN->"Perlu mengulang";RecallRating.ASSISTED->"Masih memakai bantuan";RecallRating.CONFIDENT->"Lancar menurut saya"}
                            AnswerCard(title,(saved?.response?:choices).contains(rating.name),!session.checked&&!busy,false,"recall_${rating.name}"){choices=listOf(rating.name)}
                        }
                    }
                } else if(exercise.kind==ExerciseKind.MATCH) {
                    item { Text(label(language,"Ketuk hukum di kiri, lalu cirinya di kanan. Angka yang sama menunjukkan pasanganmu.","Tap a rule on the left, then its description on the right. Matching numbers show your pairs."),style=MaterialTheme.typography.bodySmall) }
                    itemsIndexed(exercise.left) { index,left ->
                        val right=exercise.options[index];val values=saved?.response?:pairs;val connected=values.indexOf(right.id)
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f).fillMaxHeight()) { AnswerCard("${index+1}. ${left.text}",matching==index,!session.checked&&!busy,false,"match_left_$index") {matching=index} }
                            Box(Modifier.weight(1f).fillMaxHeight()) { AnswerCard((if(connected>=0)"${connected+1}. " else "")+right.text,connected>=0,!session.checked&&!busy,false,"match_right_${right.id}") {
                                pairs=pairs.mapIndexed { i,value -> if(i==matching)right.id else if(value==right.id)"" else value }
                                pairs.indexOfFirst { it.isBlank() }.takeIf { it>=0 }?.let {matching=it}
                            } }
                        }
                    }
                } else {
                    if(exercise.kind==ExerciseKind.ORDER) item { Text(label(language,"Ketuk lagi untuk membatalkan pilihan. Angka menunjukkan urutan yang kamu pilih.","Tap again to remove a choice. Numbers show your chosen order."),style=MaterialTheme.typography.bodySmall) }
                    itemsIndexed(exercise.options,key={_,it->it.id}) { _,option ->
                        val selected=(saved?.response?:choices).contains(option.id)
                        val ordinal=if(exercise.kind==ExerciseKind.ORDER)(saved?.response?:choices).indexOf(option.id)+1 else 0
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            if(ordinal>0)Text("$ordinal",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                            AnswerCard(option.text,selected,!session.checked&&!busy,exercise.arabicOptions,"answer_${option.id}") {
                                choices=if(exercise.kind==ExerciseKind.ORDER)if(selected)choices-option.id else choices+option.id else listOf(option.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerCard(text:String,selected:Boolean,enabled:Boolean,arabic:Boolean,tag:String,onClick:()->Unit) {
    val color by animateColorAsState(if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,label="Answer choice")
    Surface(onClick=onClick,enabled=enabled,shape=RoundedCornerShape(18.dp),color=color,contentColor=MaterialTheme.colorScheme.onSurface,
        border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag(tag)) {
        Text(text,Modifier.padding(16.dp),fontFamily=if(arabic)QuranArabicFont else null,fontSize=if(arabic)30.sp else 18.sp,
            lineHeight=if(arabic)52.sp else 27.sp,textAlign=if(arabic)TextAlign.Right else TextAlign.Start,
            style=MaterialTheme.typography.bodyLarge.copy(textDirection=if(arabic)TextDirection.Rtl else TextDirection.Content))
    }
}

@Composable
internal fun LessonResultScreen(course: LearningCourse, result: LearningResult, progress: PracticeProgress, language:String,onClose:()->Unit) {
    var appear by remember {mutableStateOf(false)}
    LaunchedEffect(result.sessionId){appear=true}
    val scale by animateFloatAsState(if(appear)1f else .7f,tween(450),label="Completion")
    BackHandler(onBack=onClose)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp).testTag("lesson_result"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(12.dp))
        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(104.dp).graphicsLayer{scaleX=scale;scaleY=scale}) {
            Box(contentAlignment=Alignment.Center){Icon(Icons.Outlined.CheckCircle,null,Modifier.size(58.dp),tint=MaterialTheme.colorScheme.primary)}
        }
        Text(label(language,"Latihan selesai","Practice complete"),style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
        Text(course.lesson(result.lessonId).title,style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
        Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("+${result.xp} XP",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.testTag("lesson_xp"))
                Text("${result.correct} / ${result.total} ${label(language,"jawaban tepat pada percobaan pertama","correct on the first attempt")}",textAlign=TextAlign.Center)
                if(result.reviewed>0)Text("${result.reviewed} ${label(language,"soal sudah dibahas ulang","questions revisited")}",style=MaterialTheme.typography.bodySmall)
            }
        }
        if(result.lessonId.startsWith("hafalan_"))Text(label(language,"Catatan hafalanmu tersimpan. Penilaian lancar berasal dari pemeriksaanmu sendiri.","Your recall notes are saved. Confidence is based on your own assessment."),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
        if(progress.learning.goalEnabled)Text("${progress.learning.countOn(java.time.LocalDate.now())} / ${progress.learning.goal} ${label(language,"pelajaran hari ini","lessons today")}",style=MaterialTheme.typography.titleSmall)
        Text(label(language,"Cukup untuk sekarang juga boleh. Lanjutkan kapan saja.","It's fine to stop here. Continue whenever you like."),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onClose,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().heightIn(min=58.dp).testTag("lesson_done")){Text(label(language,"Selesai","Done"))}
    }
}
