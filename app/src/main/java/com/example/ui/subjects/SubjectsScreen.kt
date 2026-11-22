package com.example.ui.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CachedQuestionSetEntity
import com.example.data.model.Chapter
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UiState
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.subjectIconForName
import com.example.ui.theme.SuccessGreen
import com.example.utils.AppStrings

@Composable
fun SubjectsScreen(
    strings: AppStrings,
    isOnline: Boolean,
    subjectsState: UiState<List<Subject>>,
    cachedSubjects: List<Subject>,
    selectedSubjectId: String?,
    chapters: List<Chapter>,
    selectedChapterId: String?,
    topics: List<Topic>,
    questionSetsState: UiState<List<QuestionSet>>,
    downloadedSets: List<CachedQuestionSetEntity>,
    onSelectSubject: (String?) -> Unit,
    onSelectChapter: (String?) -> Unit,
    onPracticeSubject: (Subject) -> Unit,
    onPracticeChapter: (Subject, Chapter) -> Unit,
    onPracticeSet: (QuestionSet) -> Unit,
    onDownloadSetOffline: (QuestionSet) -> Unit
) {
    val subjectList = when (subjectsState) {
        is UiState.Success -> subjectsState.data
        else -> if (!isOnline) cachedSubjects else emptyList()
    }
    val selectedSubject = subjectList.firstOrNull { it.id == selectedSubjectId }
    val downloadedIds = downloadedSets.map { it.id }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedSubject?.displayName(strings.isBn)
                        ?: if (strings.isBn) "সকল বিষয় ও অধ্যায়" else "Subjects & Chapters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (selectedSubject != null) {
                    OutlinedButton(onClick = { onSelectSubject(null) }) {
                        Text(if (strings.isBn) "সব বিষয়" else "All Subjects")
                    }
                }
            }
        }

        if (selectedSubject == null) {
            when {
                subjectsState is UiState.Loading && isOnline -> item { LoadingCard() }
                subjectsState is UiState.Error && subjectList.isEmpty() -> item { ErrorBannerCard(message = subjectsState.message) }
                subjectList.isEmpty() -> item {
                    BengaliEmptyState(icon = Icons.Default.MenuBook, message = strings.emptySubjects)
                }
                else -> {
                    items(subjectList, key = { it.id }) { subject ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubject(subject.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = subjectIconForName(subject.iconName),
                                        contentDescription = subject.displayName(strings.isBn),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = subject.displayName(strings.isBn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (subject.description.isNotBlank()) {
                                            Text(
                                                text = subject.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Button(
                    onClick = { onPracticeSubject(selectedSubject) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (strings.isBn) "${selectedSubject.displayName(true)} - অনুশীলন শুরু করুন"
                        else "Practice ${selectedSubject.displayName(false)}"
                    )
                }
            }

            item {
                Text(
                    text = if (strings.isBn) "অধ্যায়সমূহ" else "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (chapters.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.FolderOpen,
                        message = if (strings.isBn) "এই বিষয়ে এখনো কোনো অধ্যায় নেই।" else "No chapters in this subject yet."
                    )
                }
            } else {
                items(chapters, key = { it.id }) { chapter ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectChapter(if (selectedChapterId == chapter.id) null else chapter.id)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.displayName(strings.isBn),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = { onPracticeChapter(selectedSubject, chapter) }) {
                                    Text(if (strings.isBn) "অনুশীলন" else "Practice")
                                }
                            }
                            if (selectedChapterId == chapter.id && topics.isNotEmpty()) {
                                Text(
                                    text = topics.joinToString(" • ") { if (strings.isBn) it.nameBn else it.nameEn },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (questionSetsState is UiState.Success) {
                val filteredSets = questionSetsState.data.filter { it.subjectId == selectedSubject.id }
                if (filteredSets.isNotEmpty()) {
                    item {
                        Text(
                            text = if (strings.isBn) "প্রশ্ন সেট (অফলাইন ডাউনলোডযোগ্য)" else "Question Sets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredSets, key = { it.id }) { qSet ->
                        val isDownloaded = qSet.id in downloadedIds
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(qSet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${qSet.questionCount} MCQ", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (!isDownloaded && isOnline) onDownloadSetOffline(qSet) },
                                        enabled = isOnline && !isDownloaded
                                    ) {
                                        Icon(
                                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                            contentDescription = "Download Offline",
                                            tint = if (isDownloaded) SuccessGreen else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Button(onClick = { onPracticeSet(qSet) }) {
                                        Text(if (strings.isBn) "শুরু" else "Start")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
