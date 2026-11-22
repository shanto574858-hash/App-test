package com.example.ui.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.Subject
import com.example.data.model.UserSessionRole
import com.example.ui.components.BengaliEmptyState
import com.example.ui.theme.ErrorRed
import com.example.utils.AppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamsScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    examinations: List<Examination>,
    allResults: List<ExaminationResult>,
    subjects: List<Subject>,
    questions: List<AdminQuestionWithAnswer>,
    isBusy: Boolean,
    onSaveExamination: (
        examId: String?,
        title: String,
        subjectId: String,
        subjectName: String,
        chapterId: String,
        questionIds: List<String>,
        marksPerQuestion: Double,
        negativeMarking: Double,
        timeLimitMinutes: Int,
        examMode: String,
        randomQuestionOrder: Boolean,
        randomOptionOrder: Boolean,
        status: String
    ) -> Unit,
    onDeleteExamination: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var editingExam by remember { mutableStateOf<Examination?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (strings.isBn) "পরীক্ষা (${examinations.size})" else "Exams (${examinations.size})") }
            )
            if (sessionRole.hasPermission("result_view")) {
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text(if (strings.isBn) "শিক্ষার্থী ফলাফল (${allResults.size})" else "Results (${allResults.size})") }
                )
            }
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sessionRole.hasPermission("exam_manage")) {
                    item {
                        Button(
                            onClick = { showCreateDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isBn) "নতুন পরীক্ষা তৈরি করুন" else "Create Examination")
                        }
                    }
                }

                if (examinations.isEmpty()) {
                    item {
                        BengaliEmptyState(icon = Icons.Default.Timer, message = strings.emptyExams)
                    }
                } else {
                    items(examinations, key = { it.id }) { exam ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(exam.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${exam.subjectName} • ${exam.totalQuestions} প্রশ্ন • ${exam.timeLimitMinutes} মি. • ${exam.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (sessionRole.hasPermission("exam_manage")) {
                                    Row {
                                        IconButton(onClick = { editingExam = exam }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { onDeleteExamination(exam.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allResults.isEmpty()) {
                    item {
                        BengaliEmptyState(icon = Icons.Default.Timer, message = strings.emptyResults)
                    }
                } else {
                    items(allResults, key = { it.id }) { res ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(res.examTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "UID: ${res.userId.take(10)}... • প্রাপ্ত নম্বর: ${String.format(Locale.US, "%.2f", res.obtainedMarks)}/${res.totalMarks} (${res.percentage.toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingExam != null) {
        val initial = editingExam
        var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
        var selectedSubject by remember(initial?.id) { mutableStateOf(subjects.firstOrNull { it.id == initial?.subjectId }) }
        var marksPerQ by remember(initial?.id) { mutableStateOf((initial?.marksPerQuestion ?: 1.0).toString()) }
        var negMarks by remember(initial?.id) { mutableStateOf((initial?.negativeMarking ?: 0.25).toString()) }
        var timeMins by remember(initial?.id) { mutableStateOf((initial?.timeLimitMinutes ?: 30).toString()) }
        var examMode by remember(initial?.id) { mutableStateOf(initial?.examMode ?: "timed") }
        var randQ by remember(initial?.id) { mutableStateOf(initial?.randomQuestionOrder ?: false) }
        var randOpt by remember(initial?.id) { mutableStateOf(initial?.randomOptionOrder ?: false) }
        var published by remember(initial?.id) { mutableStateOf((initial?.status ?: "published") == "published") }
        var selectedQIds by remember(initial?.id) {
            mutableStateOf(
                initial?.questionIds?.toSet()
                    ?: questions.filter { it.publicQuestion.status == "published" }.take(20).map { it.publicQuestion.id }.toSet()
            )
        }
        var subOpen by remember { mutableStateOf(false) }

        val publishedQuestions = remember(questions, selectedSubject) {
            questions
                .map { it.publicQuestion }
                .filter { it.status == "published" }
                .filter { selectedSubject == null || it.subjectId == selectedSubject?.id }
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingExam = null
            },
            title = { Text(if (initial == null) "নতুন পরীক্ষা তৈরি" else "পরীক্ষা সম্পাদনা") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("পরীক্ষার শিরোনাম *") })

                    ExposedDropdownMenuBox(expanded = subOpen, onExpandedChange = { subOpen = it }) {
                        OutlinedTextField(
                            value = selectedSubject?.nameBn ?: "সব বিষয়",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("বিষয়") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = subOpen, onDismissRequest = { subOpen = false }) {
                            DropdownMenuItem(text = { Text("সব বিষয়") }, onClick = { selectedSubject = null; subOpen = false })
                            subjects.forEach { s ->
                                DropdownMenuItem(text = { Text(s.nameBn) }, onClick = { selectedSubject = s; subOpen = false })
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("timed", "untimed", "custom").forEach { mode ->
                            FilterChip(selected = examMode == mode, onClick = { examMode = mode }, label = { Text(mode) })
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = marksPerQ, onValueChange = { marksPerQ = it }, label = { Text("নম্বর/প্রশ্ন") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = negMarks, onValueChange = { negMarks = it }, label = { Text("নেগেটিভ") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = timeMins, onValueChange = { timeMins = it }, label = { Text("সময় (মি.)") }, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("এলোমেলো প্রশ্ন ক্রম")
                        Switch(checked = randQ, onCheckedChange = { randQ = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("এলোমেলো অপশন ক্রম")
                        Switch(checked = randOpt, onCheckedChange = { randOpt = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("প্রকাশিত (Published)")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }

                    Text("প্রশ্ন নির্বাচন (${selectedQIds.size}টি):", style = MaterialTheme.typography.labelLarge)
                    publishedQuestions.take(30).forEach { q ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = q.id in selectedQIds,
                                onCheckedChange = { c ->
                                    selectedQIds = if (c) selectedQIds + q.id else selectedQIds - q.id
                                }
                            )
                            Text(q.questionText.take(65), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && selectedQIds.isNotEmpty(),
                    onClick = {
                        onSaveExamination(
                            initial?.id,
                            title,
                            selectedSubject?.id.orEmpty(),
                            selectedSubject?.nameBn.orEmpty(),
                            "",
                            selectedQIds.toList(),
                            marksPerQ.toDoubleOrNull() ?: 1.0,
                            negMarks.toDoubleOrNull() ?: 0.0,
                            timeMins.toIntOrNull() ?: 30,
                            examMode,
                            randQ,
                            randOpt,
                            if (published) "published" else "draft"
                        )
                        showCreateDialog = false
                        editingExam = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; editingExam = null }) { Text(strings.cancel) }
            }
        )
    }
}
