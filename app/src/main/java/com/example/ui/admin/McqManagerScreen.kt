package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.Chapter
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UserSessionRole
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.MathText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqManagerScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    questions: List<AdminQuestionWithAnswer>,
    subjects: List<Subject>,
    chapters: List<Chapter>,
    topics: List<Topic>,
    questionSets: List<QuestionSet>,
    exportedJson: String?,
    isBusy: Boolean,
    onSaveQuestion: (
        questionId: String?,
        questionText: String,
        optionA: String,
        optionB: String,
        optionC: String,
        optionD: String,
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
    onDeleteQuestion: (String) -> Unit,
    onDuplicateQuestion: (AdminQuestionWithAnswer) -> Unit,
    onPublishQuestion: (questionId: String, hasAnswer: Boolean) -> Unit,
    onUnpublishQuestion: (String) -> Unit,
    onBulkImportJson: (json: String, subjectId: String, subjectName: String) -> Unit,
    onExportQuestionsJson: (List<AdminQuestionWithAnswer>) -> Unit,
    onClearExportedJson: () -> Unit,
    onSaveSubject: (id: String?, nameBn: String, nameEn: String, desc: String, icon: String, order: Int) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onAddSuggestedCategories: () -> Unit,
    onSaveChapter: (id: String?, subjectId: String, nameBn: String, nameEn: String, order: Int) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onSaveTopic: (subjectId: String, chapterId: String, nameBn: String, nameEn: String) -> Unit,
    onCreateQuestionSet: (
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        chapterId: String,
        chapterName: String,
        questionIds: List<String>,
        language: String
    ) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    var editingQuestion by remember { mutableStateOf<AdminQuestionWithAnswer?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf<Subject?>(null) }
    var showTopicDialog by remember { mutableStateOf<Chapter?>(null) }
    var showSetDialog by remember { mutableStateOf(false) }

    val filteredQuestions = remember(questions, searchQuery, statusFilter) {
        questions.filter { item ->
            val q = item.publicQuestion
            val matchesStatus = statusFilter == "ALL" || q.status == statusFilter
            val matchesQuery = searchQuery.isBlank() ||
                q.questionText.contains(searchQuery, ignoreCase = true) ||
                q.subjectName.contains(searchQuery, ignoreCase = true) ||
                q.source.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (strings.isBn) "প্রশ্ন ব্যাংক (${questions.size})" else "MCQs (${questions.size})") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(if (strings.isBn) "বিষয় ও অধ্যায়" else "Subjects & Chapters") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(if (strings.isBn) "প্রশ্ন সেট" else "Sets") }
            )
        }

        when (activeTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text(if (strings.isBn) "প্রশ্ন, বিষয় বা উৎস খুঁজুন..." else "Search MCQs...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "published", "unpublished", "unverified").forEach { st ->
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = st },
                                    label = { Text(st) }
                                )
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sessionRole.hasPermission("question_create")) {
                                Button(
                                    onClick = { showAddQuestionDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_add_mcq_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (strings.isBn) "নতুন MCQ" else "Add MCQ")
                                }
                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("JSON")
                                }
                            }
                            OutlinedButton(
                                onClick = { onExportQuestionsJson(filteredQuestions) },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Export")
                            }
                        }
                    }

                    if (filteredQuestions.isEmpty()) {
                        item {
                            BengaliEmptyState(icon = Icons.Default.Quiz, message = strings.emptyMcqs)
                        }
                    } else {
                        items(filteredQuestions, key = { it.publicQuestion.id }) { item ->
                            val q = item.publicQuestion
                            val ans = item.privateAnswer?.correctAnswer.orEmpty()
                            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = if (q.status == "published") SuccessGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${q.status} • উত্তর: ${ans.ifBlank { "?" }}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (q.status == "published") SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            if (sessionRole.hasPermission("question_edit")) {
                                                IconButton(onClick = { editingQuestion = item }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                                }
                                            }
                                            if (sessionRole.hasPermission("question_create")) {
                                                IconButton(onClick = { onDuplicateQuestion(item) }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                                                }
                                            }
                                            if (sessionRole.hasPermission("question_publish")) {
                                                IconButton(onClick = {
                                                    if (q.status == "published") onUnpublishQuestion(q.id)
                                                    else onPublishQuestion(q.id, ans.isNotBlank())
                                                }) {
                                                    Icon(
                                                        Icons.Default.Publish,
                                                        contentDescription = if (q.status == "published") "Unpublish" else "Publish",
                                                        tint = if (q.status == "published") SuccessGreen else MaterialTheme.colorScheme.primary
                                                    )
                                                }
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${q.subjectName} ${if (q.chapterName.isNotBlank()) "• ${q.chapterName}" else ""} • ${q.difficulty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sessionRole.hasPermission("subject_manage")) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showSubjectDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (strings.isBn) "নতুন বিষয়" else "Add Subject")
                                }
                                OutlinedButton(
                                    onClick = onAddSuggestedCategories,
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text(if (strings.isBn) "প্রস্তাবিত বিষয় যোগ" else "Add Suggested")
                                }
                            }
                        }
                    }

                    if (subjects.isEmpty()) {
                        item {
                            BengaliEmptyState(icon = Icons.Default.Quiz, message = strings.emptySubjects)
                        }
                    } else {
                        items(subjects, key = { it.id }) { subject ->
                            val subChapters = chapters.filter { it.subjectId == subject.id }
                            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(subject.nameBn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            if (subject.nameEn.isNotBlank()) {
                                                Text(subject.nameEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (sessionRole.hasPermission("subject_manage")) {
                                            Row {
                                                TextButton(onClick = { showChapterDialog = subject }) {
                                                    Text(if (strings.isBn) "+ অধ্যায়" else "+ Chapter")
                                                }
                                                IconButton(onClick = { onDeleteSubject(subject.id) }) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete subject", tint = ErrorRed)
                                                }
                                            }
                                        }
                                    }
                                    subChapters.forEach { chap ->
                                        val chapTopics = topics.filter { it.chapterId == chap.id }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("• ${chap.nameBn}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                                if (chapTopics.isNotEmpty()) {
                                                    Text(
                                                        text = "  টপিক: ${chapTopics.joinToString { it.nameBn }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (sessionRole.hasPermission("subject_manage")) {
                                                Row {
                                                    TextButton(onClick = { showTopicDialog = chap }) {
                                                        Text("+ টপিক", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    IconButton(onClick = { onDeleteChapter(chap.id) }) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete chapter", modifier = Modifier.size(18.dp))
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
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sessionRole.hasPermission("question_create")) {
                        item {
                            Button(
                                onClick = { showSetDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isBn) "নতুন MCQ সেট তৈরি করুন" else "Create Question Set")
                            }
                        }
                    }
                    items(questionSets, key = { it.id }) { qSet ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(qSet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${qSet.subjectName} • ${qSet.questionCount} MCQ",
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

    if (showAddQuestionDialog || editingQuestion != null) {
        QuestionEditorDialog(
            strings = strings,
            initial = editingQuestion,
            subjects = subjects,
            chapters = chapters,
            topics = topics,
            questionSets = questionSets,
            canPublish = sessionRole.hasPermission("question_publish"),
            onDismiss = {
                showAddQuestionDialog = false
                editingQuestion = null
            },
            onSave = { qId, qText, oA, oB, oC, oD, ans, exp, sId, sName, cId, cName, tId, tName, setId, diff, lang, src, ver, pub ->
                onSaveQuestion(qId, qText, oA, oB, oC, oD, ans, exp, sId, sName, cId, cName, tId, tName, setId, diff, lang, src, ver, pub)
                showAddQuestionDialog = false
                editingQuestion = null
            }
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        var importSubject by remember { mutableStateOf(subjects.firstOrNull()) }
        var subDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(if (strings.isBn) "বাল্ক JSON আমদানি" else "Bulk JSON Import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = subDropdown, onExpandedChange = { subDropdown = it }) {
                        OutlinedTextField(
                            value = importSubject?.nameBn ?: "Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (strings.isBn) "বিষয়" else "Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = subDropdown, onDismissRequest = { subDropdown = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.nameBn) },
                                    onClick = { importSubject = s; subDropdown = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = { Text("[{\"questionText\":\"...\",\"optionA\":\"...\",\"optionB\":\"...\",\"correctAnswer\":\"A\"}]") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = jsonInput.isNotBlank(),
                    onClick = {
                        onBulkImportJson(jsonInput, importSubject?.id.orEmpty(), importSubject?.nameBn.orEmpty())
                        showImportDialog = false
                    }
                ) {
                    Text(if (strings.isBn) "আমদানি করুন" else "Import")
                }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (!exportedJson.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onClearExportedJson,
            title = { Text("Exported MCQ JSON") },
            text = {
                OutlinedTextField(
                    value = exportedJson,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = onClearExportedJson) { Text("OK") }
            }
        )
    }

    if (showSubjectDialog) {
        var nameBn by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text(if (strings.isBn) "নতুন বিষয় যোগ করুন" else "Add Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("বিষয়ের নাম (বাংলা)") })
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Subject Name (English)") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("বিবরণ") })
                }
            },
            confirmButton = {
                Button(enabled = nameBn.isNotBlank(), onClick = {
                    onSaveSubject(null, nameBn, nameEn, desc, "menu_book", subjects.size + 1)
                    showSubjectDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showSubjectDialog = false }) { Text(strings.cancel) } }
        )
    }

    showChapterDialog?.let { subject ->
        var nameBn by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChapterDialog = null },
            title = { Text("${subject.nameBn} - নতুন অধ্যায়") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("অধ্যায়ের নাম (বাংলা)") })
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Chapter Name (English)") })
                }
            },
            confirmButton = {
                Button(enabled = nameBn.isNotBlank(), onClick = {
                    onSaveChapter(null, subject.id, nameBn, nameEn, 1)
                    showChapterDialog = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showChapterDialog = null }) { Text(strings.cancel) } }
        )
    }

    showTopicDialog?.let { chap ->
        var nameBn by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTopicDialog = null },
            title = { Text("${chap.nameBn} - নতুন টপিক") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("টপিকের নাম (বাংলা)") })
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Topic Name (English)") })
                }
            },
            confirmButton = {
                Button(enabled = nameBn.isNotBlank(), onClick = {
                    onSaveTopic(chap.subjectId, chap.id, nameBn, nameEn)
                    showTopicDialog = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showTopicDialog = null }) { Text(strings.cancel) } }
        )
    }

    if (showSetDialog) {
        var title by remember { mutableStateOf("") }
        var setSub by remember { mutableStateOf(subjects.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { showSetDialog = false },
            title = { Text(if (strings.isBn) "নতুন প্রশ্ন সেট" else "Create Question Set") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("সেটের শিরোনাম") })
                    Text(if (strings.isBn) "বিষয়: ${setSub?.nameBn ?: "সব"}" else "Subject: ${setSub?.nameEn ?: "All"}")
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    val qIds = questions
                        .filter { setSub == null || it.publicQuestion.subjectId == setSub?.id }
                        .filter { it.publicQuestion.status == "published" }
                        .take(30)
                        .map { it.publicQuestion.id }
                    onCreateQuestionSet(
                        title, "", setSub?.id.orEmpty(), setSub?.nameBn.orEmpty(),
                        "", "", qIds, "bn"
                    )
                    showSetDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showSetDialog = false }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditorDialog(
    strings: AppStrings,
    initial: AdminQuestionWithAnswer?,
    subjects: List<Subject>,
    chapters: List<Chapter>,
    topics: List<Topic>,
    questionSets: List<QuestionSet>,
    canPublish: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        questionId: String?,
        questionText: String,
        optionA: String,
        optionB: String,
        optionC: String,
        optionD: String,
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
    ) -> Unit
) {
    val q = initial?.publicQuestion
    var questionText by remember { mutableStateOf(q?.questionText.orEmpty()) }
    var optA by remember { mutableStateOf(q?.optionA.orEmpty()) }
    var optB by remember { mutableStateOf(q?.optionB.orEmpty()) }
    var optC by remember { mutableStateOf(q?.optionC.orEmpty()) }
    var optD by remember { mutableStateOf(q?.optionD.orEmpty()) }
    var correctAnswer by remember { mutableStateOf(initial?.privateAnswer?.correctAnswer?.ifBlank { "A" } ?: "A") }
    var explanation by remember { mutableStateOf(initial?.privateAnswer?.explanation.orEmpty()) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull { it.id == q?.subjectId }) }
    var selectedChapter by remember { mutableStateOf(chapters.firstOrNull { it.id == q?.chapterId }) }
    var selectedTopic by remember { mutableStateOf(topics.firstOrNull { it.id == q?.topicId }) }
    var selectedSet by remember { mutableStateOf(questionSets.firstOrNull { it.id == q?.questionSetId }) }
    var difficulty by remember { mutableStateOf(q?.difficulty?.ifBlank { "medium" } ?: "medium") }
    var source by remember { mutableStateOf(q?.source.orEmpty()) }
    var publishNow by remember { mutableStateOf(q?.status == "published" || (initial == null && canPublish)) }
    var subMenu by remember { mutableStateOf(false) }
    var chapMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (strings.isBn) "নতুন MCQ যোগ করুন" else "Add New MCQ"
                } else {
                    if (strings.isBn) "MCQ সম্পাদনা" else "Edit MCQ"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = subMenu, onExpandedChange = { subMenu = it }) {
                    OutlinedTextField(
                        value = selectedSubject?.nameBn ?: if (strings.isBn) "বিষয় নির্বাচন" else "Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (strings.isBn) "বিষয়" else "Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = subMenu, onDismissRequest = { subMenu = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(text = { Text(s.nameBn) }, onClick = {
                                selectedSubject = s; selectedChapter = null; selectedTopic = null; subMenu = false
                            })
                        }
                    }
                }

                val subChaps = chapters.filter { it.subjectId == selectedSubject?.id }
                if (subChaps.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = chapMenu, onExpandedChange = { chapMenu = it }) {
                        OutlinedTextField(
                            value = selectedChapter?.nameBn ?: if (strings.isBn) "অধ্যায়" else "Chapter",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (strings.isBn) "অধ্যায়" else "Chapter") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = chapMenu, onDismissRequest = { chapMenu = false }) {
                            subChaps.forEach { c ->
                                DropdownMenuItem(text = { Text(c.nameBn) }, onClick = {
                                    selectedChapter = c; selectedTopic = null; chapMenu = false
                                })
                            }
                        }
                    }
                }

                OutlinedTextField(value = questionText, onValueChange = { questionText = it }, label = { Text("প্রশ্ন *") }, minLines = 2)
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("অপশন ক (A) *") })
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("অপশন খ (B) *") })
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("অপশন গ (C)") })
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("অপশন ঘ (D)") })

                Text(if (strings.isBn) "সঠিক উত্তর:" else "Correct Answer:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("A", "B", "C", "D").forEach { key ->
                        FilterChip(selected = correctAnswer == key, onClick = { correctAnswer = key }, label = { Text(key) })
                    }
                }

                Text(if (strings.isBn) "কঠিনতা:" else "Difficulty:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("easy", "medium", "hard").forEach { d ->
                        FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d) })
                    }
                }

                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("ব্যাখ্যা (Explanation)") }, minLines = 2)
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("উৎস (যেমন: ৪৫তম বিসিএস)") })

                if (canPublish) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (strings.isBn) "সরাসরি প্রকাশ করুন" else "Publish immediately")
                        Switch(checked = publishNow, onCheckedChange = { publishNow = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank(),
                onClick = {
                    onSave(
                        q?.id,
                        questionText, optA, optB, optC, optD,
                        correctAnswer, explanation,
                        selectedSubject?.id.orEmpty(),
                        selectedSubject?.nameBn.orEmpty(),
                        selectedChapter?.id.orEmpty(),
                        selectedChapter?.nameBn.orEmpty(),
                        selectedTopic?.id.orEmpty(),
                        selectedTopic?.nameBn.orEmpty(),
                        selectedSet?.id.orEmpty(),
                        difficulty, "bn", source,
                        true,
                        publishNow && canPublish
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
