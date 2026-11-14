package com.nuraya.app.hadith

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.ui.QuranArabicFont
import com.nuraya.app.ui.icons.AppIcons as Icons
import com.nuraya.app.ui.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HadithScreen(language: String) {
    val context = LocalContext.current
    val repository = remember { HadithRepository(context) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var collection by rememberSaveable { mutableStateOf<String?>(null) }
    var searched by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf<HadithSearchResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<Hadith?>(null) }
    fun search(more: Boolean = false) {
        if (busy || query.trim().isEmpty()) return
        val requestQuery = query.trim()
        val requestCollection = collection
        keyboard?.hide(); busy = true; error = false
        scope.launch {
            try {
                val page = repository.search(requestQuery, requestCollection, if (more) result?.items?.size ?: 0 else 0)
                result = if (more) page.copy(items = result!!.items + page.items) else page
                searched = requestQuery
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true }
            finally { busy = false }
        }
    }
    LaunchedEffect(selected) {
        current = null; error = false
        try { current = selected?.let { repository.get(it) }; if (selected != null && current == null) error = true }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
    }
    BackHandler(selected != null) { selected = null }
    if (selected != null) {
        LazyColumn(Modifier.fillMaxSize().testTag("hadith_reader"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { TextButton(onClick = { selected = null }) { Icon(Icons.Outlined.ArrowBack, null); Spacer(Modifier.width(8.dp)); Text(label(language,"Hasil pencarian","Search results")) } }
            current?.let { hadith ->
                item { Text(hadith.reference, style = MaterialTheme.typography.headlineSmall) }
                if (hadith.section.isNotBlank()) item { Text(hadith.section, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                item { Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(label(language,"Teks Arab","Arabic text"),style=MaterialTheme.typography.labelLarge)
                        if (hadith.arabic.isNotBlank()) SelectionContainer { Text(hadith.arabic, fontFamily = QuranArabicFont, fontSize = 28.sp, lineHeight = 48.sp, textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth().testTag("hadith_arabic")) }
                        else Text(label(language,"Teks Arab belum tersedia pada entri sumber ini.","Arabic text is unavailable in this source entry."))
                        HorizontalDivider()
                        Text("Indonesia",style=MaterialTheme.typography.labelLarge)
                        SelectionContainer { Text(hadith.indonesian.ifBlank { label(language,"Terjemahan Indonesia belum tersedia pada entri sumber ini.","The Indonesian translation is unavailable in this source entry.") }, modifier=Modifier.testTag("hadith_indonesian"),style=MaterialTheme.typography.bodyLarge) }
                    }
                } }
                item { SourceNote(language) }
            } ?: item { if (error) Text(label(language,"Hadis belum dapat dibuka. Kembali ke hasil pencarian dan coba lagi.","Unable to open this hadith. Return to the results and retry.")) else LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("hadith_search_screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text(label(language,"Temukan hadis untuk dipelajari","Find hadiths to study"),style=MaterialTheme.typography.headlineSmall) }
        item { Text(label(language,"Cari kata, tema, atau rujukan seperti “Bukhari 1”. Kitab Bukhari dan Muslim tersedia offline.","Search words, topics, or a reference such as “Bukhari 1”. Bukhari and Muslim are available offline."),color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(query,{query=it.take(200)},modifier=Modifier.fillMaxWidth().testTag("hadith_query"),label={Text(label(language,"Cari hadis","Find hadith"))},placeholder={Text(label(language,"Contoh: niat, sedekah, orang tua","Examples: niat, sedekah, orang tua"))},leadingIcon={Icon(Icons.Outlined.Search,null)},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={search()}),singleLine=true,shape=RoundedCornerShape(18.dp)) }
        item { FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(null to label(language,"Semua","All"),"bukhari" to "Bukhari","muslim" to "Muslim").forEach { (key,title) ->
                FilterChip(collection==key,onClick={collection=key;result=null;searched=""},enabled=!busy,label={Text(title)})
            }
        } }
        item { Button(onClick={search()},enabled=!busy&&query.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("hadith_search")) { Icon(Icons.Outlined.Search,null);Spacer(Modifier.width(8.dp));Text(label(language,"Cari hadis","Find hadith")) } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error) item { Text(label(language,"Data hadis belum dapat dibuka. Coba lagi.","Hadith data could not be opened. Please retry."),color=MaterialTheme.colorScheme.error) }
        result?.let { page ->
            item { Text(if(page.total==0)label(language,"Belum ada hasil. Coba kata yang lebih singkat atau nomor hadis.","No results yet. Try fewer words or a hadith number.") else label(language,"${page.total} hasil untuk “$searched”","${page.total} results for “$searched”"),style=MaterialTheme.typography.titleSmall) }
            items(page.items,key={it.id}) { hadith ->
                OutlinedCard(onClick={selected=hadith.id},modifier=Modifier.fillMaxWidth().testTag("hadith_result_${hadith.collection}_${hadith.number}"),shape=RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) { Text(hadith.reference,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);Icon(Icons.Outlined.ChevronRight,null) }
                        Text(hadith.indonesian.ifBlank { label(language,"Buka teks Arab dan rujukannya.","Open the Arabic text and source.") }.take(220)+(if(hadith.indonesian.length>220)"…" else ""),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if(page.items.size<page.total) item { OutlinedButton(onClick={search(true)},enabled=!busy&&query.trim()==searched,modifier=Modifier.fillMaxWidth()) { Text(label(language,"Hasil berikutnya","More results")) } }
        }
        item { SourceNote(language) }
    }
}
@Composable private fun SourceNote(language:String) {
    Text(label(language,"Sumber: fawazahmed0/hadith-api · edisi Arab & Indonesia. Nomor mengikuti edisi dataset; kutipan dan terjemahan ditampilkan sesuai sumber. Sebagian entri belum memiliki terjemahan Indonesia.","Source: fawazahmed0/hadith-api · Arabic & Indonesian editions. Numbering follows the dataset edition; quotations and translations retain their source. Some entries have no Indonesian translation."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
