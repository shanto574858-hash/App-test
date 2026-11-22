package com.example.ui.examination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamReviewItem
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.Examination
import com.example.data.model.UiState
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MathText
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.utils.AppStrings
import com.example.viewmodel.ActiveExamUiState
import java.util.Locale

@Composable
fun ExaminationScreen(
    strings: AppStrings,
    isOnline: Boolean,
    examinationsState: UiState<List<Examination>>,
    examState: ActiveExamUiState,
    onStartExam: (Examination) -> Unit,
    onSelectAnswer: (questionId: String, optionKey: String) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmitExam: () -> Unit,
    onExitExam: () -> Unit
) {
    if (examState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingCard(if (strings.isBn) "পরীক্ষা লোড হচ্ছে..." else "Loading examination...")
        }
        return
    }

    if (examState.submissionResult != null) {
        ExamSubmissionResultView(
            strings = strings,
            result = examState.submissionResult,
            reviewItems = examState.reviewItems,
            onRetryExam = { examState.exam?.let(onStartExam) },
            onExit = onExitExam
        )
        return
    }

    if (examState.isActive && examState.exam != null && examState.questions.isNotEmpty()) {
        LiveExaminationRunner(
            strings = strings,
            examState = examState,
            onSelectAnswer = onSelectAnswer,
            onNavigateQuestion = onNavigateQuestion,
            onSubmitExam = onSubmitExam,
            onExitExam = onExitExam
        )
        return
    }

    // Exam List View
    var modeFilter by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (strings.isBn) "অনলাইন পরীক্ষাসমূহ" else "Online Examinations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (strings.isBn)
                    "সময়ভিত্তিক, সময়সীমাবিহীন ও কাস্টম পরীক্ষায় অংশ নিন। জমা দেওয়ার পর বিস্তারিত ফলাফল দেখানো হবে।"
                else
                    "Take timed, untimed, or custom examinations with server-verified scoring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isOnline) {
            item {
                ErrorBannerCard(
                    message = if (strings.isBn)
                        "পরীক্ষায় অংশগ্রহণ এবং ফলাফল মূল্যায়নের জন্য ইন্টারনেট সংযোগ প্রয়োজন।"
                    else
                        "Internet connection is required to start and submit online examinations."
                )
            }
        }

        if (!examState.errorMessage.isNullOrBlank()) {
            item {
                ErrorBannerCard(message = examState.errorMessage)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "ALL" to if (strings.isBn) "সব" else "All",
                    "timed" to if (strings.isBn) "সময়ভিত্তিক" else "Timed",
                    "untimed" to if (strings.isBn) "সময়সীমাবিহীন" else "Untimed",
                    "custom" to if (strings.isBn) "কাস্টম" else "Custom"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = modeFilter == key,
                        onClick = { modeFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        when (examinationsState) {
            is UiState.Loading -> item { LoadingCard() }
            is UiState.Error -> item { ErrorBannerCard(message = examinationsState.message) }
            is UiState.Success -> {
                val filtered = examinationsState.data
                    .filter { it.status == "published" }
                    .filter { modeFilter == "ALL" || it.examMode.equals(modeFilter, ignoreCase = true) }

                if (filtered.isEmpty()) {
                    item {
                        BengaliEmptyState(
                            icon = Icons.Default.Timer,
                            message = strings.emptyExams
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { exam ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exam.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = when (exam.examMode) {
                                                "untimed" -> if (strings.isBn) "সময়সীমাবিহীন" else "Untimed"
                                                "custom" -> if (strings.isBn) "কাস্টম" else "Custom"
                                                else -> "${exam.timeLimitMinutes} মি."
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                if (exam.subjectName.isNotBlank()) {
                                    Text(
                                        text = exam.subjectName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (strings.isBn)
                                        "প্রশ্ন: ${exam.totalQuestions} • মোট নম্বর: ${exam.totalMarks} • নেগেটিভ মার্কিং: ${exam.negativeMarking}"
                                    else
                                        "Questions: ${exam.totalQuestions} • Total Marks: ${exam.totalMarks} • Negative: ${exam.negativeMarking}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { onStartExam(exam) },
                                    enabled = isOnline,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("start_exam_${exam.id}")
                                ) {
                                    Text(if (strings.isBn) "পরীক্ষা শুরু করুন" else "Start Examination")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveExaminationRunner(
    strings: AppStrings,
    examState: ActiveExamUiState,
    onSelectAnswer: (questionId: String, optionKey: String) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmitExam: () -> Unit,
    onExitExam: () -> Unit
) {
    var showSubmitConfirm by remember { mutableStateOf(false) }
    var showNavigatorGrid by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler { showExitConfirm = true }

    val exam = examState.exam!!
    val total = examState.questions.size
    val idx = examState.currentIndex.coerceIn(0, total - 1)
    val question = examState.questions[idx]
    val answeredCount = examState.selectedAnswers.size
    val unansweredCount = (total - answeredCount).coerceAtLeast(0)
    val selectedOption = examState.selectedAnswers[question.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Timer & Question Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (strings.isBn)
                        "প্রশ্ন ${idx + 1} / $total • উত্তর: $answeredCount • বাকি: $unansweredCount"
                    else
                        "Q ${idx + 1}/$total • Answered: $answeredCount • Left: $unansweredCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (examState.remainingSeconds > 0) {
                    val mins = examState.remainingSeconds / 60
                    val secs = examState.remainingSeconds % 60
                    val isLowTime = examState.remainingSeconds <= 60
                    Surface(
                        color = if (isLowTime) ErrorRedContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = if (isLowTime) ErrorRed else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = { showNavigatorGrid = true }) {
                    Icon(Icons.Default.GridView, contentDescription = "Question Grid")
                }
            }
        }

        LinearProgressIndicator(
            progress = { (idx + 1).toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )

        if (!examState.errorMessage.isNullOrBlank()) {
            ErrorBannerCard(message = examState.errorMessage)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    MathText(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            val orderKeys = examState.optionOrders[question.id] ?: listOf("A", "B", "C", "D")
            val optionTextByKey = mapOf(
                "A" to question.optionA,
                "B" to question.optionB,
                "C" to question.optionC,
                "D" to question.optionD
            )
            val displayLabels = if (strings.isBn) listOf("ক", "খ", "গ", "ঘ") else listOf("A", "B", "C", "D")

            orderKeys.forEachIndexed { displayIdx, originalKey ->
                val text = optionTextByKey[originalKey].orEmpty()
                if (text.isNotBlank()) {
                    val isSelected = selectedOption == originalKey
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !examState.isSubmitting) {
                                onSelectAnswer(question.id, originalKey)
                            }
                            .testTag("exam_option_$originalKey"),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = displayLabels.getOrElse(displayIdx) { originalKey },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            MathText(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onNavigateQuestion(idx - 1) },
                enabled = idx > 0 && !examState.isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.previous)
            }

            if (idx < total - 1) {
                Button(
                    onClick = { onNavigateQuestion(idx + 1) },
                    enabled = !examState.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.next)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Button(
                onClick = { showSubmitConfirm = true },
                enabled = !examState.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_exam_button")
            ) {
                if (examState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(strings.submit)
                }
            }
        }
    }

    if (showNavigatorGrid) {
        AlertDialog(
            onDismissRequest = { showNavigatorGrid = false },
            title = { Text(if (strings.isBn) "প্রশ্ন নেভিগেটর" else "Question Navigator") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    examState.questions.forEachIndexed { qIdx, q ->
                        val answered = examState.selectedAnswers.containsKey(q.id)
                        val isCurrent = qIdx == idx
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        answered -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable {
                                    onNavigateQuestion(qIdx)
                                    showNavigatorGrid = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${qIdx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                                    answered -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNavigatorGrid = false }) {
                    Text(if (strings.isBn) "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text(if (strings.isBn) "পরীক্ষা জমা দেবেন?" else "Submit Examination?") },
            text = {
                Text(
                    if (strings.isBn)
                        "মোট প্রশ্ন: $total\nউত্তর দিয়েছেন: $answeredCount\nউত্তর দেননি: $unansweredCount\n\nজমা দেওয়ার পর আর পরিবর্তন করা যাবে না।"
                    else
                        "Total: $total\nAnswered: $answeredCount\nUnanswered: $unansweredCount\n\nYou cannot modify answers after submission."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitConfirm = false
                    onSubmitExam()
                }) {
                    Text(strings.submit)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(if (strings.isBn) "পরীক্ষা থেকে বের হবেন?" else "Leave Exam?") },
            text = {
                Text(
                    if (strings.isBn)
                        "জমা না দিয়ে বের হলে বর্তমান পরীক্ষা মূল্যায়ন হবে না।"
                    else
                        "Leaving without submitting will discard your active attempt."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onExitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(if (strings.isBn) "বের হন" else "Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun ExamSubmissionResultView(
    strings: AppStrings,
    result: ExamSubmissionResult,
    reviewItems: List<ExamReviewItem>,
    onRetryExam: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }
    var wrongOnly by remember { mutableStateOf(false) }

    val visibleReview = remember(reviewItems, wrongOnly) {
        if (wrongOnly) reviewItems.filter { !it.isCorrect } else reviewItems
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.examTitle.ifBlank { if (strings.isBn) "পরীক্ষার ফলাফল" else "Examination Result" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", result.obtainedMarks)} / ${result.totalMarks} (${result.percentage.toInt()}%)",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (strings.isBn)
                            "ব্যয়িত সময়: ${result.timeTakenSeconds / 60} মি. ${result.timeTakenSeconds % 60} সে. • সঠিকতা: ${result.accuracy.toInt()}%"
                        else
                            "Time: ${result.timeTakenSeconds / 60}m ${result.timeTakenSeconds % 60}s • Accuracy: ${result.accuracy.toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricStatCard(
                    label = if (strings.isBn) "সঠিক" else "Correct",
                    value = "${result.correctAnswers}",
                    icon = Icons.Default.CheckCircle,
                    tint = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = if (strings.isBn) "ভুল" else "Wrong",
                    value = "${result.incorrectAnswers}",
                    icon = Icons.Default.Close,
                    tint = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    label = if (strings.isBn) "নেগেটিভ" else "Negative",
                    value = "-${result.negativeMarks}",
                    icon = Icons.Default.Timer,
                    tint = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetryExam, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (strings.isBn) "পুনরায় পরীক্ষা" else "Retry")
                }
                Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                    Text(if (strings.isBn) "তালিকায় ফিরুন" else "Back to List")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !wrongOnly,
                    onClick = { wrongOnly = false },
                    label = { Text(if (strings.isBn) "সব প্রশ্ন (${reviewItems.size})" else "All (${reviewItems.size})") }
                )
                FilterChip(
                    selected = wrongOnly,
                    onClick = { wrongOnly = true },
                    label = { Text(if (strings.isBn) "শুধু ভুল উত্তর" else "Wrong Only") }
                )
            }
        }

        items(visibleReview, key = { it.questionId }) { item ->
            ExamReviewItemCard(strings = strings, item = item)
        }
    }
}

@Composable
fun ExamReviewItemCard(strings: AppStrings, item: ExamReviewItem) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (item.isCorrect) SuccessGreen.copy(alpha = 0.5f) else ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MathText(
                text = item.questionText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            val optMap = mapOf(
                "A" to item.optionA,
                "B" to item.optionB,
                "C" to item.optionC,
                "D" to item.optionD
            )
            Text(
                text = if (strings.isBn)
                    "আপনার উত্তর: ${item.selectedOption.ifBlank { "উত্তর দেননি" }} ${optMap[item.selectedOption]?.let { "($it)" } ?: ""}"
                else
                    "Your Answer: ${item.selectedOption.ifBlank { "Skipped" }}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isCorrect) SuccessGreen else ErrorRed
            )
            Text(
                text = if (strings.isBn)
                    "সঠিক উত্তর: ${item.correctAnswer} ${optMap[item.correctAnswer]?.let { "($it)" } ?: ""}"
                else
                    "Correct Answer: ${item.correctAnswer}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
            if (item.explanation.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    MathText(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
