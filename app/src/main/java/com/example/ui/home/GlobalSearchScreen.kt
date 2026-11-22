package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Bookmark
import com.example.data.model.QuestionPublic
import com.example.data.model.Subject
import com.example.data.model.UiState
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MathText
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    strings: AppStrings,
    subjects: List<Subject>,
    searchState: UiState<List<QuestionPublic>>,
    bookmarks: List<Bookmark>,
    onSearch: (query: String, subjectId: String?, difficulty: String?, language: String?) -> Unit,
    onToggleBookmark: (QuestionPublic) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var queryText by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.questionId }.toSet() }

    LaunchedEffect(Unit) {
        onSearch("", null, null, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isBn) "গ্লোবাল MCQ অনুসন্ধান" else "Global MCQ Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("global_search_input"),
                    placeholder = { Text(strings.searchMcqHint) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Button(
                    onClick = { onSearch(queryText, selectedSubjectId, selectedDifficulty, selectedLanguage) },
                    modifier = Modifier.testTag("global_search_submit")
                ) {
                    Text(if (strings.isBn) "খুঁজুন" else "Search")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = {
                            selectedSubjectId = null
                            onSearch(queryText, null, selectedDifficulty, selectedLanguage)
                        },
                        label = { Text(if (strings.isBn) "সব বিষয়" else "All Subjects") }
                    )
                }
                items(subjects, key = { it.id }) { subject ->
                    FilterChip(
                        selected = selectedSubjectId == subject.id,
                        onClick = {
                            selectedSubjectId = if (selectedSubjectId == subject.id) null else subject.id
                            onSearch(queryText, selectedSubjectId, selectedDifficulty, selectedLanguage)
                        },
                        label = { Text(subject.displayName(strings.isBn)) }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("easy" to "সহজ", "medium" to "মাঝারি", "hard" to "কঠিন").forEach { (key, bn) ->
                    item {
                        FilterChip(
                            selected = selectedDifficulty == key,
                            onClick = {
                                selectedDifficulty = if (selectedDifficulty == key) null else key
                                onSearch(queryText, selectedSubjectId, selectedDifficulty, selectedLanguage)
                            },
                            label = { Text(if (strings.isBn) bn else key.uppercase()) }
                        )
                    }
                }
                listOf("bn" to "বাংলা", "en" to "English").forEach { (code, label) ->
                    item {
                        FilterChip(
                            selected = selectedLanguage == code,
                            onClick = {
                                selectedLanguage = if (selectedLanguage == code) null else code
                                onSearch(queryText, selectedSubjectId, selectedDifficulty, selectedLanguage)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when (searchState) {
                is UiState.Loading -> LoadingCard()
                is UiState.Error -> ErrorBannerCard(message = searchState.message)
                is UiState.Success -> {
                    val items = searchState.data
                    if (items.isEmpty()) {
                        BengaliEmptyState(icon = Icons.Default.Search, message = strings.emptyMcqs)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items, key = { it.id }) { q ->
                                val isBookmarked = q.id in bookmarkedIds
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${q.subjectName.ifBlank { "MCQ" }} • ${q.difficulty.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            IconButton(onClick = { onToggleBookmark(q) }) {
                                                Icon(
                                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                    contentDescription = "Bookmark",
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                        MathText(text = q.questionText, style = MaterialTheme.typography.bodyLarge)
                                        Text("ক) ${q.optionA}", style = MaterialTheme.typography.bodySmall)
                                        Text("খ) ${q.optionB}", style = MaterialTheme.typography.bodySmall)
                                        if (q.optionC.isNotBlank()) Text("গ) ${q.optionC}", style = MaterialTheme.typography.bodySmall)
                                        if (q.optionD.isNotBlank()) Text("ঘ) ${q.optionD}", style = MaterialTheme.typography.bodySmall)
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
