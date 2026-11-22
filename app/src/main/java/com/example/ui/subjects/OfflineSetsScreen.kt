package com.example.ui.subjects

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CachedQuestionSetEntity
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.OnlineStatusChip
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSetsScreen(
    strings: AppStrings,
    isOnline: Boolean,
    downloadedSets: List<CachedQuestionSetEntity>,
    pendingSyncCount: Int,
    onSyncNow: () -> Unit,
    onPracticeOfflineSet: (CachedQuestionSetEntity) -> Unit,
    onDeleteOfflineSet: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isBn) "অফলাইন প্রশ্ন সেট" else "Offline Question Sets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OnlineStatusChip(
                        isOnline = isOnline,
                        onlineText = strings.onlineLabel,
                        offlineText = strings.offlineLabel,
                        modifier = Modifier.padding(end = 12.dp)
                    )
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
            if (pendingSyncCount > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (strings.isBn) "${pendingSyncCount}টি অফলাইন অনুশীলন সিঙ্কের অপেক্ষায়"
                                    else "$pendingSyncCount offline attempts pending sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(onClick = onSyncNow, enabled = isOnline) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isBn) "সিঙ্ক" else "Sync")
                            }
                        }
                    }
                }
            }

            if (downloadedSets.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.CloudOff,
                        message = if (strings.isBn) "কোনো অফলাইন সেট ডাউনলোড করা নেই।" else "No offline question sets downloaded.",
                        subtitle = if (strings.isBn)
                            "বিষয়সমূহ থেকে যেকোনো প্রশ্ন সেট ডাউনলোড করে ইন্টারনেট ছাড়াই অনুশীলন করুন।"
                        else
                            "Download any question set from Subjects to practice offline."
                    )
                }
            } else {
                items(downloadedSets, key = { it.id }) { setEntity ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(setEntity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${setEntity.subjectName} • ${setEntity.questionCount} MCQ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onDeleteOfflineSet(setEntity.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Offline")
                                }
                                Button(onClick = { onPracticeOfflineSet(setEntity) }) {
                                    Text(if (strings.isBn) "অনুশীলন" else "Practice")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
