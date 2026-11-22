package com.example.ui.results

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamReviewItem
import com.example.data.model.ExaminationResult
import com.example.data.model.StudentProgress
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.ChartBarData
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SubjectPerformanceBarChart
import com.example.ui.components.WeeklyProgressLineChart
import com.example.ui.examination.ExamReviewItemCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.utils.AppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsAndAnalyticsScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    resultsState: UiState<List<ExaminationResult>>,
    progressList: List<StudentProgress>,
    loadedReviewItems: List<ExamReviewItem>,
    isLoadingReview: Boolean,
    onOpenResultReview: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeReviewResult by remember { mutableStateOf<ExaminationResult?>(null) }

    if (activeReviewResult != null) {
        val result = activeReviewResult!!
        BackHandler { activeReviewResult = null }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(result.examTitle.ifBlank { if (strings.isBn) "পরীক্ষা রিভিউ" else "Exam Review" }) },
                    navigationIcon = {
                        IconButton(onClick = { activeReviewResult = null }) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (strings.isBn)
                                    "প্রাপ্ত নম্বর: ${String.format(Locale.US, "%.2f", result.obtainedMarks)} / ${result.totalMarks}"
                                else
                                    "Score: ${String.format(Locale.US, "%.2f", result.obtainedMarks)} / ${result.totalMarks}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isBn)
                                    "সঠিক: ${result.correctAnswers} • ভুল: ${result.incorrectAnswers} • অনুত্তরিত: ${result.unansweredQuestions}"
                                else
                                    "Correct: ${result.correctAnswers} • Wrong: ${result.incorrectAnswers} • Skipped: ${result.unansweredQuestions}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (isLoadingReview) {
                    item { LoadingCard() }
                } else {
                    items(loadedReviewItems, key = { it.questionId }) { item ->
                        ExamReviewItemCard(strings = strings, item = item)
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (strings.isBn) "পরীক্ষার ইতিহাস" else "Exam History") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (strings.isBn) "পারফরম্যান্স" else "Analytics") }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (resultsState) {
                    is UiState.Loading -> item { LoadingCard() }
                    is UiState.Error -> item { ErrorBannerCard(message = resultsState.message) }
                    is UiState.Success -> {
                        val results = resultsState.data
                        if (results.isEmpty()) {
                            item {
                                BengaliEmptyState(
                                    icon = Icons.Default.Assessment,
                                    message = strings.emptyResults
                                )
                            }
                        } else {
                            items(results, key = { it.id }) { res ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeReviewResult = res
                                            onOpenResultReview(res.id)
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = res.examTitle.ifBlank { if (strings.isBn) "পরীক্ষা" else "Exam" },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (strings.isBn)
                                                    "সঠিক: ${res.correctAnswers}/${res.totalQuestions} • প্রাপ্ত নম্বর: ${String.format(Locale.US, "%.2f", res.obtainedMarks)}"
                                                else
                                                    "Correct: ${res.correctAnswers}/${res.totalQuestions} • Marks: ${String.format(Locale.US, "%.2f", res.obtainedMarks)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${res.percentage.toInt()}%",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val totalAttempted = userProfile?.totalAttempted ?: 0L
            val totalCorrect = userProfile?.totalCorrect ?: 0L
            val accuracyPct = if (totalAttempted > 0L) ((totalCorrect * 100.0) / totalAttempted).toInt() else 0

            val subjectBars = remember(progressList) {
                progressList
                    .filter { it.subjectName.isNotBlank() && it.attempted > 0 }
                    .groupBy { it.subjectName }
                    .map { (name, items) ->
                        val att = items.sumOf { it.attempted }
                        val cor = items.sumOf { it.correct }
                        val pct = if (att > 0) (cor * 100f) / att else 0f
                        ChartBarData(label = name, percentage = pct)
                    }
            }

            val dailyPoints = remember(progressList) {
                progressList
                    .filter { it.dateKey.isNotBlank() && it.attempted > 0 }
                    .groupBy { it.dateKey }
                    .toSortedMap()
                    .entries
                    .toList()
                    .takeLast(7)
                    .map { (dateKey, items) ->
                        val att = items.sumOf { it.attempted }
                        val cor = items.sumOf { it.correct }
                        dateKey to if (att > 0) (cor * 100f) / att else 0f
                    }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricStatCard(
                            label = if (strings.isBn) "মোট অনুশীলন" else "Attempted",
                            value = "$totalAttempted",
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            label = if (strings.isBn) "সঠিকতা" else "Accuracy",
                            value = "$accuracyPct%",
                            icon = Icons.Default.TrendingUp,
                            tint = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            label = if (strings.isBn) "সেরা নম্বর" else "Best Score",
                            value = "${userProfile?.bestScore?.toInt() ?: 0}",
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (subjectBars.isEmpty() && dailyPoints.isEmpty()) {
                    item {
                        BengaliEmptyState(
                            icon = Icons.Default.TrendingUp,
                            message = if (strings.isBn)
                                "পরিসংখ্যান দেখতে অনুশীলন বা পরীক্ষায় অংশগ্রহণ করুন।"
                            else
                                "Complete practice sessions or exams to view analytics."
                        )
                    }
                } else {
                    if (subjectBars.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = if (strings.isBn) "বিষয়ভিত্তিক দক্ষতা" else "Subject Performance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    SubjectPerformanceBarChart(items = subjectBars)
                                }
                            }
                        }
                    }

                    if (dailyPoints.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = if (strings.isBn) "দৈনিক অনুশীলন অগ্রগতি" else "Daily Practice Accuracy",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    WeeklyProgressLineChart(points = dailyPoints)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
