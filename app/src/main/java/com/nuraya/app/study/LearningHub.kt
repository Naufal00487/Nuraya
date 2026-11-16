package com.nuraya.app.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuraya.app.ui.icons.AppIcons as Icons
import com.nuraya.app.ui.label
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LearningPath(course: LearningCourse, progress: PracticeProgress, language: String, kind: LearningKind,
    onLesson: (LearningLesson)->Unit, onResume: ()->Unit, onCustom: ()->Unit = {}) {
    var level by rememberSaveable(kind) { mutableIntStateOf(0) }
    val straightPath=LocalConfiguration.current.screenWidthDp<360 || LocalDensity.current.fontScale>1.3f
    val lessons=course.lessons.filter { it.kind==kind && it.level==level }
    val active=progress.learning.active?.let { session -> course.lessons.firstOrNull { it.id==session.lessonId } }
    LazyColumn(Modifier.fillMaxSize().testTag("learning_path"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                StatPill(Icons.Outlined.EmojiEvents,"${progress.learning.xp} XP")
                StatPill(Icons.Outlined.Calendar,"${progress.streakOn(LocalDate.now())} ${label(language,"hari beruntun","day streak")}")
            }
        }
        item {
            Text(if(kind==LearningKind.TAJWID)label(language,"Tajwid, selangkah demi selangkah","Tajwid, one step at a time") else label(language,"Akrab dengan surah pendek","Get familiar with short surahs"),style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(label(language,"Pilih materi yang kamu inginkan. Semua level terbuka.","Choose what interests you. Every level is open."),color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(active!=null) item {
            Card(onClick=onResume,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),modifier=Modifier.fillMaxWidth().testTag("learning_resume")) {
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label(language,"Lanjutkan saat kamu siap","Continue when you're ready"),style=MaterialTheme.typography.titleSmall)
                        Text(active.title,style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf(label(language,"Dasar","Basic"),label(language,"Menengah","Intermediate"),label(language,"Lanjutan","Advanced")).forEachIndexed { index,title ->
                    FilterChip(selected=level==index,onClick={level=index},label={Text(title)},modifier=Modifier.heightIn(min=52.dp).testTag("learning_level_$index"))
                }
            }
        }
        item {
            Surface(color=MaterialTheme.colorScheme.primary,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(kind==LearningKind.TAJWID)Icons.Outlined.School else Icons.Outlined.MenuBook,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.onPrimary)
                    Column(Modifier.weight(1f)) {
                        Text(listOf("Kenali pola bacaan","Hubungkan dan terapkan","Perkuat pemahaman")[level],style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onPrimary)
                        Text("${lessons.size} ${label(language,"pelajaran singkat","short lessons")} · ${label(language,"bebas dipilih","your choice")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        itemsIndexed(lessons,key={_,lesson->lesson.id}) { index,lesson ->
            val completed=progress.learning.records.any { it.lessonId==lesson.id }
            val inset=if(straightPath)0 else listOf(0,20,40,20)[index%4]
            Column {
                if(index>0) {
                    val color=MaterialTheme.colorScheme.outlineVariant
                    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                        val previous=(if(straightPath)0 else listOf(0,20,40,20)[(index-1)%4]).dp.toPx()+34.dp.toPx()
                        val next=inset.dp.toPx()+34.dp.toPx()
                        drawPath(Path().apply{moveTo(previous,0f);cubicTo(previous,size.height*.6f,next,size.height*.4f,next,size.height)},color,style=Stroke(5.dp.toPx()))
                    }
                }
                Surface(onClick={onLesson(lesson)},shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerLow,
                    border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=1.dp,
                    modifier=Modifier.fillMaxWidth().padding(start=inset.dp).testTag("learning_lesson_${lesson.id}")) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        Surface(shape=CircleShape,color=if(completed)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation=4.dp,modifier=Modifier.size(56.dp)) {
                            Box(contentAlignment=Alignment.Center) { Icon(if(completed)Icons.Outlined.CheckCircle else if(kind==LearningKind.MEMORIZE)Icons.Outlined.MenuBook else Icons.Outlined.School,
                                null,Modifier.size(25.dp),tint=if(completed)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(lesson.title,style=MaterialTheme.typography.titleSmall)
                            Text(lesson.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(label(language,if(completed)"Sudah dilatih · boleh diulang" else "${course.stepCount(lesson.id)} langkah · mulai kapan saja",if(completed)"Practiced · review any time" else "${course.stepCount(lesson.id)} steps · start any time"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        if(kind==LearningKind.MEMORIZE) item {
            OutlinedButton(onClick=onCustom,modifier=Modifier.fillMaxWidth().testTag("hafalan_custom")) {
                Icon(Icons.Outlined.Tune,null);Spacer(Modifier.width(10.dp));Text(label(language,"Pilih rentang ayat sendiri","Choose your own verse range"))
            }
        }
        item { Text(label(language,"Pelengkap waktu belajarmu. Tidak ada materi yang terkunci atau batas kesalahan.","A companion to your own pace. No locked material or mistake limits."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(icon,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary);Text(text,style=MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun FreeLearning(course: LearningCourse, progress: PracticeProgress, language: String, onLesson:(LearningLesson)->Unit, onMemorize:()->Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("free_learning"),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Text(label(language,"Latihan pilihanmu","Practice your way"),style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp));Text(label(language,"Coba materi acak atau kembali ke pelajaran yang pernah dicoba.","Try a random topic or revisit a lesson.")) }
        item { PracticeTile(Icons.Outlined.Explore,label(language,"Acak materi tajwid","Random tajwid topic"),label(language,"Satu materi, lima langkah singkat","One topic, five short steps"),"practice_random") { onLesson(course.lessons.filter { it.kind==LearningKind.TAJWID }.random()) } }
        item { PracticeTile(Icons.Outlined.MenuBook,label(language,"Hafalan surah pendek","Short surah practice"),label(language,"Urutan ayat dan catatan hafalan pribadi","Verse order and your own recall notes"),"practice_short_surahs",onMemorize) }
        item { Text(label(language,"Pernah kamu latih","Your previous lessons"),style=MaterialTheme.typography.titleMedium) }
        val previous=progress.learning.records.sortedByDescending { it.lastDate }.mapNotNull { record -> course.lessons.firstOrNull { it.id==record.lessonId } }
        if(previous.isEmpty()) item { Text(label(language,"Setelah menyelesaikan pelajaran, kamu bisa menemukannya di sini untuk diulang.","Completed lessons will appear here for easy review."),color=MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(previous,key={_,it->it.id}) { _,lesson -> PracticeTile(Icons.Outlined.RepeatOne,lesson.title,lesson.summary,"review_${lesson.id}"){onLesson(lesson)} }
    }
}
@Composable
private fun PracticeTile(icon: androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,tag:String,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag(tag),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Icon(icon,null,Modifier.size(30.dp),tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LearningProgressScreen(progress: PracticeProgress, language: String, onGoal:(Int,Boolean)->Unit, onRead:(Int,Int)->Unit) {
    val learning=progress.learning;val today=LocalDate.now();val due=progress.dueOn(today)
    LazyColumn(Modifier.fillMaxSize().testTag("learning_progress"),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { Text(label(language,"Perjalanan belajar Anda","Your learning journey"),style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp));Text(label(language,"Catatan untuk diri sendiri. Belajar kapan pun kamu ingin.","A personal record. Learn whenever you feel like it."),color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("${learning.xp} XP",style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${learning.records.size} ${label(language,"pelajaran pernah dilatih","lessons practiced")} · ${learning.sessions} ${label(language,"sesi selesai","sessions completed")}")
                    Text("${progress.streakOn(today)} ${label(language,"hari belajar beruntun","day streak")}",style=MaterialTheme.typography.titleSmall)
                    Text(label(language,"Jika jedanya panjang, poin dan materi yang sudah selesai tetap tersimpan.","After a break, your points and completed lessons stay saved."),style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            OutlinedCard(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) { Text(label(language,"Target harian opsional","Optional daily goal"),style=MaterialTheme.typography.titleSmall);Text(label(language,"Tanpa pengingat atau kewajiban","No reminders or obligations"),style=MaterialTheme.typography.bodySmall) }
                        Switch(learning.goalEnabled,onCheckedChange={onGoal(learning.goal,it)},modifier=Modifier.testTag("learning_goal_enabled"))
                    }
                    if(learning.goalEnabled) {
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { (1..3).forEach { goal -> FilterChip(learning.goal==goal,onClick={onGoal(goal,true)},label={Text("$goal ${label(language,"pelajaran","lessons")}")},modifier=Modifier.testTag("learning_goal_$goal")) } }
                        LinearProgressIndicator(progress={(learning.countOn(today).toFloat()/learning.goal).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                        Text("${learning.countOn(today)} / ${learning.goal} ${label(language,"pelajaran hari ini","lessons today")}",style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Text(label(language,"Lencana pribadi","Personal badges"),style=MaterialTheme.typography.titleMedium) }
        val badges=listOf(Triple("first","Langkah pertama","Selesaikan satu pelajaran"),Triple("tajwid","Mengenal tajwid","Latih tiga materi tajwid"),Triple("surahs","Akrab dengan surah","Latih tiga surah pendek"),Triple("days","Tiga hari belajar","Belajar pada tiga hari berbeda"),Triple("xp","Seratus langkah kecil","Kumpulkan 100 XP"),Triple("review","Senang mengulang","Latih satu pelajaran tiga kali"))
        itemsIndexed(badges,key={_,it->it.first}) { _,badge ->
            val earned=badge.first in learning.badges
            Surface(color=if(earned)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().testTag("learning_badge_${badge.first}")) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Icon(if(earned)Icons.Outlined.Verified else Icons.Outlined.EmojiEvents,null,Modifier.size(30.dp),tint=MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)){Text(badge.second,style=MaterialTheme.typography.titleSmall);Text(if(earned)"Tercapai · ${badge.third}" else badge.third,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
            }
        }
        item { HorizontalDivider();Spacer(Modifier.height(12.dp));Text(label(language,"Catatan hafalan","Memorization notes"),style=MaterialTheme.typography.titleMedium)
            Text("${progress.reviews.size} ${label(language,"ayat pernah dicatat","verses reviewed")} · ${progress.completedSessions} ${label(language,"sesi hafalan","memorization sessions")}")
            Text(label(language,"Penilaian hafalan adalah catatanmu sendiri; poin bukan sertifikasi bacaan.","Recall ratings are your own notes; points are not a recitation certification."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        if(progress.completedLessons.isNotEmpty()) item { Text("${progress.completedLessons.size} ${label(language,"materi dari versi sebelumnya tetap tercatat pernah dibaca.","previous-version lessons remain recorded as read.")}",style=MaterialTheme.typography.bodySmall) }
        if(due.isNotEmpty()) item { Text(label(language,"Bisa diulang kembali","Available to review"),style=MaterialTheme.typography.titleMedium) }
        itemsIndexed(due.take(20),key={_,it->it.reference}) { _,review ->
            OutlinedButton(onClick={onRead(review.surah,review.ayah)},modifier=Modifier.fillMaxWidth()){Text("${label(language,"Buka ayat","Open verse")} ${review.reference}")}
        }
    }
}
