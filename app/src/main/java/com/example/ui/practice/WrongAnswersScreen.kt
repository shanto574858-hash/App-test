package com.example.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.WrongAnswer
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.MathText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongAnswersScreen(
    strings: AppStrings,
    wrongAnswers: List<WrongAnswer>,
    onPracticeWrongQuestions: (List<String>) -> Unit,
    onRemoveWrongAnswer: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val subjectNames = remember(wrongAnswers) {
        wrongAnswers.map { it.subjectName }.filter { it.isNotBlank() }.distinct()
    }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    val filtered = remember(wrongAnswers, selectedSubject) {
        if (selectedSubject == null) wrongAnswers
        else wrongAnswers.filter { it.subjectName == selectedSubject }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isBn) "ভুল উত্তর তালিকা" else "Wrong Answers") },
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
            if (wrongAnswers.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.ErrorOutline,
                        message = strings.emptyWrongAnswers
                    )
                }
            } else {
                if (subjectNames.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedSubject == null,
                                    onClick = { selectedSubject = null },
                                    label = { Text(if (strings.isBn) "সব (${wrongAnswers.size})" else "All (${wrongAnswers.size})") }
                                )
                            }
                            items(subjectNames) { name ->
                                FilterChip(
                                    selected = selectedSubject == name,
                                    onClick = { selectedSubject = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onPracticeWrongQuestions(filtered.map { it.questionId }.distinct()) },
                        enabled = filtered.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (strings.isBn)
                                "ভুল প্রশ্নগুলো পুনরায় অনুশীলন করুন (${filtered.size})"
                            else
                                "Practice Wrong Questions Again (${filtered.size})"
                        )
                    }
                }

                items(filtered, key = { it.id }) { item ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
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
                                    if (item.subjectName.isNotBlank()) {
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                                            Text(
                                                text = item.subjectName,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Surface(color = ErrorRedContainer, shape = CircleShape) {
                                        Text(
                                            text = if (strings.isBn) "${item.mistakeCount} বার ভুল" else "${item.mistakeCount}x mistake",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ErrorRed
                                        )
                                    }
                                }
                                IconButton(onClick = { onRemoveWrongAnswer(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = if (strings.isBn) "তালিকা থেকে সরান" else "Remove"
                                    )
                                }
                            }

                            MathText(
                                text = item.questionText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = ErrorRedContainer, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        text = if (strings.isBn) "আপনার উত্তর: ${item.selectedOption}" else "Selected: ${item.selectedOption}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ErrorRed
                                    )
                                }
                                Surface(color = SuccessGreenContainer, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        text = if (strings.isBn) "সঠিক উত্তর: ${item.correctAnswer}" else "Correct: ${item.correctAnswer}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SuccessGreen
                                    )
                                }
                            }

                            if (item.explanation.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (strings.isBn) "সঠিক ব্যাখ্যা:" else "Explanation:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        MathText(
                                            text = item.explanation,
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
