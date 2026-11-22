package com.example.ui.pdf

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.UserSessionRole
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.MathText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReviewScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    allQuestions: List<AdminQuestionWithAnswer>,
    isBusy: Boolean,
    onApproveAndPublish: (questionId: String, hasAnswer: Boolean) -> Unit,
    onRejectQuestion: (String) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onPublishBulk: (List<String>) -> Unit,
    onSaveEditedQuestion: (
        questionId: String,
        questionText: String,
        optA: String,
        optB: String,
        optC: String,
        optD: String,
        correctAnswer: String,
        explanation: String,
        subjectId: String,
        subjectName: String,
        chapterId: String,
        chapterName: String,
        topicId: String,
        topicName: String,
        questionSetId: String,
        difficulty: String,
        language: String,
        source: String,
        verified: Boolean,
        publishImmediately: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var filterKey by remember { mutableStateOf("NEEDS_REVIEW") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var editingItem by remember { mutableStateOf<AdminQuestionWithAnswer?>(null) }

    val pdfExtractedOrFiltered = remember(allQuestions, filterKey) {
        when (filterKey) {
            "ALL" -> allQuestions
            "VERIFIED" -> allQuestions.filter { it.publicQuestion.verified }
            "UNVERIFIED" -> allQuestions.filter { !it.publicQuestion.verified }
            "NEEDS_REVIEW" -> allQuestions.filter {
                it.publicQuestion.needsReview || !it.publicQuestion.verified || it.publicQuestion.status == "unverified"
            }
            "PUBLISHED" -> allQuestions.filter { it.publicQuestion.status == "published" }
            "UNPUBLISHED" -> allQuestions.filter { it.publicQuestion.status != "published" }
            else -> allQuestions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isBn) "PDF প্রশ্ন পর্যালোচনা" else "Extracted MCQ Review") },
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
            item {
                val filterChips = listOf(
                    "NEEDS_REVIEW" to if (strings.isBn) "পর্যালোচনা বাকি" else "Needs Review",
                    "UNVERIFIED" to if (strings.isBn) "অযাচাইকৃত" else "Unverified",
                    "VERIFIED" to if (strings.isBn) "যাচাইকৃত" else "Verified",
                    "PUBLISHED" to if (strings.isBn) "প্রকাশিত" else "Published",
                    "UNPUBLISHED" to if (strings.isBn) "অপ্রকাশিত" else "Unpublished",
                    "ALL" to if (strings.isBn) "সব (${allQuestions.size})" else "All (${allQuestions.size})"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterChips) { (key, label) ->
                        FilterChip(
                            selected = filterKey == key,
                            onClick = { filterKey = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (sessionRole.hasPermission("question_publish") && pdfExtractedOrFiltered.isNotEmpty()) {
                item {
                    val publishableTargets = if (selectedIds.isNotEmpty()) {
                        pdfExtractedOrFiltered.filter {
                            it.publicQuestion.id in selectedIds && !it.privateAnswer?.correctAnswer.isNullOrBlank()
                        }.map { it.publicQuestion.id }
                    } else {
                        pdfExtractedOrFiltered.filter {
                            it.publicQuestion.verified &&
                                it.publicQuestion.status != "published" &&
                                !it.privateAnswer?.correctAnswer.isNullOrBlank()
                        }.map { it.publicQuestion.id }
                    }
                    Button(
                        onClick = {
                            onPublishBulk(publishableTargets)
                            selectedIds = emptySet()
                        },
                        enabled = !isBusy && publishableTargets.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selectedIds.isNotEmpty()) {
                                if (strings.isBn) "নির্বাচিত প্রশ্ন প্রকাশ করুন (${publishableTargets.size})"
                                else "Publish Selected (${publishableTargets.size})"
                            } else {
                                if (strings.isBn) "যাচাইকৃত সব প্রশ্ন প্রকাশ করুন (${publishableTargets.size})"
                                else "Publish All Verified (${publishableTargets.size})"
                            }
                        )
                    }
                }
            }

            if (pdfExtractedOrFiltered.isEmpty()) {
                item {
                    BengaliEmptyState(
                        icon = Icons.Default.FactCheck,
                        message = strings.emptyMcqs
                    )
                }
            } else {
                items(pdfExtractedOrFiltered, key = { it.publicQuestion.id }) { item ->
                    val q = item.publicQuestion
                    val ans = item.privateAnswer?.correctAnswer.orEmpty()
                    val exp = item.privateAnswer?.explanation.orEmpty()
                    val isSelected = q.id in selectedIds

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            1.dp,
                            if (ans.isBlank() || q.needsReview) WarningAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + q.id else selectedIds - q.id
                                        }
                                    )
                                    Surface(
                                        color = if (q.verified) SuccessGreenContainer else WarningAmberContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${q.status} • ${(q.confidence * 100).toInt()}%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (q.verified) SuccessGreen else WarningAmber
                                        )
                                    }
                                    if (q.pageNumber > 0) {
                                        Text(
                                            text = "P.${q.pageNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { editingItem = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    if (sessionRole.hasPermission("question_delete")) {
                                        IconButton(onClick = { onDeleteQuestion(q.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                        }
                                    }
                                }
                            }

                            MathText(
                                text = q.questionText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ক) ${q.optionA}", style = MaterialTheme.typography.bodySmall)
                                Text("খ) ${q.optionB}", style = MaterialTheme.typography.bodySmall)
                                if (q.optionC.isNotBlank()) Text("গ) ${q.optionC}", style = MaterialTheme.typography.bodySmall)
                                if (q.optionD.isNotBlank()) Text("ঘ) ${q.optionD}", style = MaterialTheme.typography.bodySmall)
                            }

                            Surface(
                                color = if (ans.isNotBlank()) SuccessGreenContainer else ErrorRedContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = if (ans.isNotBlank()) {
                                        if (strings.isBn) "শনাক্তকৃত উত্তর: $ans" else "Detected Answer: $ans"
                                    } else {
                                        if (strings.isBn) "সঠিক উত্তর অনুপস্থিত — সম্পাদনা করে উত্তর দিন" else "Missing Answer — Edit to assign"
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (ans.isNotBlank()) SuccessGreen else ErrorRed
                                )
                            }

                            if (q.originalPdfText.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "PDF Text: ${q.originalPdfText.take(260)}",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (exp.isNotBlank()) {
                                Text("ব্যাখ্যা: $exp", style = MaterialTheme.typography.bodySmall)
                            }

                            if (sessionRole.hasPermission("question_publish")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onApproveAndPublish(q.id, ans.isNotBlank()) },
                                        enabled = ans.isNotBlank() && !isBusy,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (strings.isBn) "অনুমোদন ও প্রকাশ" else "Approve & Publish")
                                    }
                                    OutlinedButton(
                                        onClick = { onRejectQuestion(q.id) },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (strings.isBn) "বাতিল" else "Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        val q = item.publicQuestion
        var qText by remember(q.id) { mutableStateOf(q.questionText) }
        var optA by remember(q.id) { mutableStateOf(q.optionA) }
        var optB by remember(q.id) { mutableStateOf(q.optionB) }
        var optC by remember(q.id) { mutableStateOf(q.optionC) }
        var optD by remember(q.id) { mutableStateOf(q.optionD) }
        var correctAns by remember(q.id) { mutableStateOf(item.privateAnswer?.correctAnswer.orEmpty()) }
        var explanation by remember(q.id) { mutableStateOf(item.privateAnswer?.explanation.orEmpty()) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text(if (strings.isBn) "প্রশ্ন ও উত্তর সম্পাদনা" else "Edit Extracted MCQ") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("প্রশ্ন") })
                    OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("অপশন A (ক)") })
                    OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("অপশন B (খ)") })
                    OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("অপশন C (গ)") })
                    OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("অপশন D (ঘ)") })
                    Text(if (strings.isBn) "সঠিক উত্তর:" else "Correct Answer:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEach { key ->
                            FilterChip(
                                selected = correctAns.equals(key, ignoreCase = true),
                                onClick = { correctAns = key },
                                label = { Text(key) }
                            )
                        }
                    }
                    OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("ব্যাখ্যা") })
                }
            },
            confirmButton = {
                Button(
                    enabled = qText.isNotBlank() && correctAns in listOf("A", "B", "C", "D"),
                    onClick = {
                        onSaveEditedQuestion(
                            q.id, qText, optA, optB, optC, optD, correctAns, explanation,
                            q.subjectId, q.subjectName, q.chapterId, q.chapterName,
                            q.topicId, q.topicName, q.questionSetId, q.difficulty, q.language, q.source,
                            true, false
                        )
                        editingItem = null
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text(strings.cancel) }
            }
        )
    }
}
