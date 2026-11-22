package com.example.ui.practice

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Bookmark
import com.example.data.model.QuestionPublic
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.MathText
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    strings: AppStrings,
    bookmarks: List<Bookmark>,
    onUpdateCollection: (bookmarkId: String, collectionName: String, reviewLater: Boolean) -> Unit,
    onRemoveBookmark: (QuestionPublic) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val collections = remember(bookmarks) {
        bookmarks.map { it.collectionName }.filter { it.isNotBlank() }.distinct()
    }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }

    val filtered = remember(bookmarks, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> bookmarks
            "REVIEW_LATER" -> bookmarks.filter { it.reviewLater }
            else -> bookmarks.filter { it.collectionName == selectedFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isBn) "বুকমার্ক করা প্রশ্ন" else "Bookmarked Questions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.previous)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bookmarks.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.Bookmark,
                        message = strings.emptyBookmarks
                    )
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text(if (strings.isBn) "সব (${bookmarks.size})" else "All (${bookmarks.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "REVIEW_LATER",
                                onClick = { selectedFilter = "REVIEW_LATER" },
                                label = { Text(if (strings.isBn) "পরে দেখুন" else "Review Later") }
                            )
                        }
                        items(collections) { col ->
                            FilterChip(
                                selected = selectedFilter == col,
                                onClick = { selectedFilter = col },
                                label = { Text(col) }
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { bm ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                                        Text(
                                            text = bm.collectionName,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (bm.reviewLater) {
                                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                                            Text(
                                                text = if (strings.isBn) "পরে দেখুন" else "Review Later",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingBookmark = bm }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit collection")
                                    }
                                    IconButton(onClick = {
                                        onRemoveBookmark(
                                            QuestionPublic(
                                                id = bm.questionId,
                                                questionText = bm.questionText,
                                                subjectId = bm.subjectId,
                                                subjectName = bm.subjectName,
                                                chapterId = bm.chapterId
                                            )
                                        )
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove bookmark")
                                    }
                                }
                            }

                            MathText(
                                text = bm.questionText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (bm.subjectName.isNotBlank()) {
                                Text(
                                    text = bm.subjectName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingBookmark?.let { bm ->
        var colName by remember(bm.id) { mutableStateOf(bm.collectionName) }
        var reviewLater by remember(bm.id) { mutableStateOf(bm.reviewLater) }
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text(if (strings.isBn) "বুকমার্ক সংগ্রহ সম্পাদনা" else "Edit Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text(if (strings.isBn) "সংগ্রহের নাম" else "Collection Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (strings.isBn) "পরে পুনরায় দেখার জন্য চিহ্নিত করুন" else "Mark for review later")
                        Switch(checked = reviewLater, onCheckedChange = { reviewLater = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateCollection(bm.id, colName.ifBlank { "Saved Questions" }, reviewLater)
                    editingBookmark = null
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBookmark = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
