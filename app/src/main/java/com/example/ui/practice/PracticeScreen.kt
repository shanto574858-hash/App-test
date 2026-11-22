package com.example.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.model.Bookmark
import com.example.data.model.Chapter
import com.example.data.model.PracticeSession
import com.example.data.model.QuestionPublic
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MathText
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.utils.AppStrings
import com.example.viewmodel.ActivePracticeUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    strings: AppStrings,
    isOnline: Boolean,
    subjectsState: UiState<List<Subject>>,
    chapters: List<Chapter>,
    topics: List<Topic>,
    questionSetsState: UiState<List<QuestionSet>>,
    practiceSessions: List<PracticeSession>,
    bookmarks: List<Bookmark>,
    practiceState: ActivePracticeUiState,
    onSelectSubjectFilter: (String?) -> Unit,
    onSelectChapterFilter: (String?) -> Unit,
    onStartPractice: (
        title: String,
        subjectId: String?,
        chapterId: String?,
        topicId: String?,
        questionSetId: String?,
        count: Int,
        allowChange: Boolean
    ) -> Unit,
    onResumeSession: (PracticeSession) -> Unit,
    onSelectOption: (String) -> Unit,
    onFinalizeAnswer: () -> Unit,
    onMoveIndex: (Int) -> Unit,
    onFinishSession: () -> Unit,
    onExitPractice: () -> Unit,
    onToggleBookmark: (QuestionPublic) -> Unit,
    onOpenWrongAnswers: () -> Unit
) {
    if (practiceState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingCard(if (strings.isBn) "প্রশ্ন প্রস্তুত হচ্ছে..." else "Preparing practice...")
        }
        return
    }

    if (practiceState.isActive && practiceState.finishedSummary) {
        PracticeSummaryView(
            strings = strings,
            practiceState = practiceState,
            onDone = onExitPractice
        )
        return
    }

    if (practiceState.isActive && practiceState.questions.isNotEmpty()) {
        ActivePracticeRunner(
            strings = strings,
            practiceState = practiceState,
            bookmarks = bookmarks,
            onSelectOption = onSelectOption,
            onFinalizeAnswer = onFinalizeAnswer,
            onMoveIndex = onMoveIndex,
            onFinishSession = onFinishSession,
            onExitPractice = onExitPractice,
            onToggleBookmark = onToggleBookmark
        )
        return
    }

    // Setup View
    val subjects = (subjectsState as? UiState.Success)?.data ?: emptyList()
    val sets = (questionSetsState as? UiState.Success)?.data ?: emptyList()

    var chosenSubject by remember { mutableStateOf<Subject?>(null) }
    var chosenChapter by remember { mutableStateOf<Chapter?>(null) }
    var chosenTopic by remember { mutableStateOf<Topic?>(null) }
    var chosenSet by remember { mutableStateOf<QuestionSet?>(null) }
    var questionCount by remember { mutableIntStateOf(20) }
    var allowChangeBeforeCheck by remember { mutableStateOf(true) }

    var subjectDropdownOpen by remember { mutableStateOf(false) }
    var chapterDropdownOpen by remember { mutableStateOf(false) }
    var topicDropdownOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (strings.isBn) "MCQ অনুশীলন মোড" else "MCQ Practice Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (strings.isBn)
                    "বিষয়, অধ্যায় বা প্রশ্ন সেট বেছে নিয়ে ব্যাখ্যাসহ অনুশীলন শুরু করুন।"
                else
                    "Select a subject, chapter, or question set to practice with explanations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!practiceState.errorMessage.isNullOrBlank()) {
            item {
                ErrorBannerCard(message = practiceState.errorMessage)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (strings.isBn) "অনুশীলন সেটআপ" else "Practice Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Subject dropdown
                    ExposedDropdownMenuBox(
                        expanded = subjectDropdownOpen,
                        onExpandedChange = { subjectDropdownOpen = it }
                    ) {
                        OutlinedTextField(
                            value = chosenSubject?.displayName(strings.isBn)
                                ?: if (strings.isBn) "সব বিষয়" else "All Subjects",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (strings.isBn) "বিষয় নির্বাচন" else "Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = subjectDropdownOpen,
                            onDismissRequest = { subjectDropdownOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (strings.isBn) "সব বিষয়" else "All Subjects") },
                                onClick = {
                                    chosenSubject = null
                                    chosenChapter = null
                                    chosenTopic = null
                                    onSelectSubjectFilter(null)
                                    subjectDropdownOpen = false
                                }
                            )
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.displayName(strings.isBn)) },
                                    onClick = {
                                        chosenSubject = subject
                                        chosenChapter = null
                                        chosenTopic = null
                                        chosenSet = null
                                        onSelectSubjectFilter(subject.id)
                                        subjectDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Chapter dropdown (when subject selected)
                    if (chosenSubject != null && chapters.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = chapterDropdownOpen,
                            onExpandedChange = { chapterDropdownOpen = it }
                        ) {
                            OutlinedTextField(
                                value = chosenChapter?.displayName(strings.isBn)
                                    ?: if (strings.isBn) "সব অধ্যায়" else "All Chapters",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (strings.isBn) "অধ্যায় নির্বাচন" else "Chapter") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterDropdownOpen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = chapterDropdownOpen,
                                onDismissRequest = { chapterDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (strings.isBn) "সব অধ্যায়" else "All Chapters") },
                                    onClick = {
                                        chosenChapter = null
                                        chosenTopic = null
                                        onSelectChapterFilter(null)
                                        chapterDropdownOpen = false
                                    }
                                )
                                chapters.forEach { chap ->
                                    DropdownMenuItem(
                                        text = { Text(chap.displayName(strings.isBn)) },
                                        onClick = {
                                            chosenChapter = chap
                                            chosenTopic = null
                                            onSelectChapterFilter(chap.id)
                                            chapterDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Topic dropdown (when chapter selected)
                    if (chosenChapter != null && topics.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = topicDropdownOpen,
                            onExpandedChange = { topicDropdownOpen = it }
                        ) {
                            OutlinedTextField(
                                value = chosenTopic?.let { if (strings.isBn) it.nameBn else it.nameEn.ifBlank { it.nameBn } }
                                    ?: if (strings.isBn) "সব টপিক" else "All Topics",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (strings.isBn) "টপিক" else "Topic") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownOpen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = topicDropdownOpen,
                                onDismissRequest = { topicDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (strings.isBn) "সব টপিক" else "All Topics") },
                                    onClick = {
                                        chosenTopic = null
                                        topicDropdownOpen = false
                                    }
                                )
                                topics.forEach { topic ->
                                    DropdownMenuItem(
                                        text = { Text(if (strings.isBn) topic.nameBn else topic.nameEn.ifBlank { topic.nameBn }) },
                                        onClick = {
                                            chosenTopic = topic
                                            topicDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (strings.isBn) "প্রশ্নের সংখ্যা" else "Question Count",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 50).forEach { cnt ->
                            FilterChip(
                                selected = questionCount == cnt,
                                onClick = { questionCount = cnt },
                                label = { Text("$cnt") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (strings.isBn)
                                "উত্তর যাচাইয়ের আগে অপশন পরিবর্তনের সুযোগ"
                            else
                                "Allow changing answer before checking",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = allowChangeBeforeCheck,
                            onCheckedChange = { allowChangeBeforeCheck = it }
                        )
                    }

                    Button(
                        onClick = {
                            val title = chosenSet?.title
                                ?: chosenChapter?.displayName(strings.isBn)
                                ?: chosenSubject?.displayName(strings.isBn)
                                ?: if (strings.isBn) "মিশ্র MCQ অনুশীলন" else "Mixed MCQ Practice"
                            onStartPractice(
                                title,
                                chosenSubject?.id,
                                chosenChapter?.id,
                                chosenTopic?.id,
                                chosenSet?.id,
                                questionCount,
                                allowChangeBeforeCheck
                            )
                        },
                        enabled = isOnline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_practice_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isBn) "অনুশীলন শুরু করুন" else "Start Practice")
                    }

                    OutlinedButton(
                        onClick = onOpenWrongAnswers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isBn) "ভুল উত্তর অনুশীলন খুলুন" else "Open Wrong Answer Practice")
                    }
                }
            }
        }

        // Question sets quick start
        if (sets.isNotEmpty()) {
            item {
                Text(
                    text = if (strings.isBn) "প্রস্তুত MCQ সেট" else "Ready MCQ Sets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            sets.forEach { qSet ->
                item(key = qSet.id) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isOnline) {
                                onStartPractice(
                                    qSet.title,
                                    qSet.subjectId.ifBlank { null },
                                    qSet.chapterId.ifBlank { null },
                                    null,
                                    qSet.id,
                                    qSet.questionCount.coerceAtLeast(20),
                                    allowChangeBeforeCheck
                                )
                            },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(qSet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${qSet.subjectName} • ${qSet.questionCount} MCQ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                enabled = isOnline,
                                onClick = {
                                    onStartPractice(
                                        qSet.title,
                                        qSet.subjectId.ifBlank { null },
                                        qSet.chapterId.ifBlank { null },
                                        null,
                                        qSet.id,
                                        qSet.questionCount.coerceAtLeast(20),
                                        allowChangeBeforeCheck
                                    )
                                }
                            ) {
                                Text(if (strings.isBn) "শুরু করুন" else "Start")
                            }
                        }
                    }
                }
            }
        }

        // Resume Sessions
        val resumable = practiceSessions.filter { it.status == "in_progress" && it.questionIds.isNotEmpty() }
        if (resumable.isNotEmpty()) {
            item {
                Text(
                    text = if (strings.isBn) "অসমাপ্ত সেশন" else "Saved Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            resumable.forEach { session ->
                item(key = session.id) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isOnline) { onResumeSession(session) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.title.ifBlank { "Practice" }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${session.currentIndex + 1}/${session.questionIds.size} প্রশ্ন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(enabled = isOnline, onClick = { onResumeSession(session) }) {
                                Text(if (strings.isBn) "চালিয়ে যান" else "Resume")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePracticeRunner(
    strings: AppStrings,
    practiceState: ActivePracticeUiState,
    bookmarks: List<Bookmark>,
    onSelectOption: (String) -> Unit,
    onFinalizeAnswer: () -> Unit,
    onMoveIndex: (Int) -> Unit,
    onFinishSession: () -> Unit,
    onExitPractice: () -> Unit,
    onToggleBookmark: (QuestionPublic) -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    val total = practiceState.questions.size
    val idx = practiceState.currentIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
    val question = practiceState.questions[idx]
    val checkedResult = practiceState.checkedResults[question.id]
    val offlineSavedOption = practiceState.offlineSelections[question.id]
    val selectedKey = if (checkedResult != null) {
        practiceState.pendingSelection ?: offlineSavedOption
    } else {
        practiceState.pendingSelection ?: offlineSavedOption
    }
    val isBookmarked = bookmarks.any { it.questionId == question.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Default.Close, contentDescription = if (strings.isBn) "বের হন" else "Exit")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (strings.isBn) "প্রশ্ন ${idx + 1} / $total" else "Question ${idx + 1} of $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (practiceState.isOfflineSession) {
                    Text(
                        text = if (strings.isBn) "অফলাইন অনুশীলন" else "Offline Practice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = { onToggleBookmark(question) }) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (strings.isBn) "বুকমার্ক" else "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LinearProgressIndicator(
            progress = { (idx + 1).toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )

        if (!practiceState.errorMessage.isNullOrBlank()) {
            ErrorBannerCard(message = practiceState.errorMessage)
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
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (question.subjectName.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = question.subjectName,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (question.source.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = question.source,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    MathText(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            val options = listOf(
                Triple("A", if (strings.isBn) "ক" else "A", question.optionA),
                Triple("B", if (strings.isBn) "খ" else "B", question.optionB),
                Triple("C", if (strings.isBn) "গ" else "C", question.optionC),
                Triple("D", if (strings.isBn) "ঘ" else "D", question.optionD)
            ).filter { it.third.isNotBlank() }

            options.forEach { (key, label, optText) ->
                val isSelected = selectedKey.equals(key, ignoreCase = true)
                val isCorrectOption = checkedResult?.correctAnswer.equals(key, ignoreCase = true)
                val isWrongSelected = checkedResult != null && !checkedResult.isCorrect && isSelected

                val borderColor = when {
                    checkedResult != null && isCorrectOption -> SuccessGreen
                    isWrongSelected -> ErrorRed
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }
                val bgColor = when {
                    checkedResult != null && isCorrectOption -> SuccessGreenContainer.copy(alpha = 0.65f)
                    isWrongSelected -> ErrorRedContainer.copy(alpha = 0.65f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surface
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = checkedResult == null && !practiceState.isCheckingAnswer) {
                            onSelectOption(key)
                        }
                        .testTag("practice_option_$key"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(if (isSelected || isCorrectOption) 2.dp else 1.dp, borderColor)
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
                            color = when {
                                checkedResult != null && isCorrectOption -> SuccessGreen
                                isWrongSelected -> ErrorRed
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected || isCorrectOption || isWrongSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        MathText(
                            text = optText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (checkedResult != null && isCorrectOption) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen)
                        } else if (isWrongSelected) {
                            Icon(Icons.Default.Close, contentDescription = "Wrong", tint = ErrorRed)
                        }
                    }
                }
            }

            if (checkedResult == null && offlineSavedOption != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (strings.isBn)
                            "অফলাইন উত্তর ($offlineSavedOption) সংরক্ষিত হয়েছে — অনলাইনে সিঙ্ক করলে যাচাই হবে।"
                        else
                            "Offline selection ($offlineSavedOption) saved — will verify once synced online.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (checkedResult != null) {
                val isCorrect = checkedResult.isCorrect
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) SuccessGreenContainer else ErrorRedContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCorrect) strings.correctAnswerText else strings.wrongAnswerText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) SuccessGreen else ErrorRed
                        )
                        if (!isCorrect) {
                            Text(
                                text = if (strings.isBn)
                                    "সঠিক উত্তর: ${checkedResult.correctAnswer}"
                                else
                                    "Correct Answer: ${checkedResult.correctAnswer}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (checkedResult.explanation.isNotBlank()) {
                            Text(
                                text = if (strings.isBn) "ব্যাখ্যা:" else "Explanation:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            MathText(
                                text = checkedResult.explanation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onMoveIndex(idx - 1) },
                enabled = idx > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.previous)
            }

            if (checkedResult == null && practiceState.allowChangeBeforeFinalize && selectedKey != null && offlineSavedOption == null) {
                Button(
                    onClick = onFinalizeAnswer,
                    enabled = !practiceState.isCheckingAnswer,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("practice_check_answer_button")
                ) {
                    if (practiceState.isCheckingAnswer) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (strings.isBn) "উত্তর যাচাই করুন" else "Check Answer")
                    }
                }
            } else if (idx < total - 1) {
                Button(
                    onClick = { onMoveIndex(idx + 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.next)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = onFinishSession,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (strings.isBn) "সমাপ্ত করুন" else "Finish")
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(if (strings.isBn) "অনুশীলন শেষ করবেন?" else "Exit Practice?") },
            text = {
                Text(
                    if (strings.isBn) "আপনার অগ্রগতি সংরক্ষিত আছে। পরবর্তীতে পুনরায় চালু করতে পারবেন।"
                    else "Your progress is saved and you can resume later."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    onExitPractice()
                }) {
                    Text(if (strings.isBn) "বের হন" else "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun PracticeSummaryView(
    strings: AppStrings,
    practiceState: ActivePracticeUiState,
    onDone: () -> Unit
) {
    val total = practiceState.questions.size
    val checked = practiceState.checkedResults.values
    val correct = checked.count { it.isCorrect }
    val incorrect = checked.count { !it.isCorrect }
    val accuracy = if (checked.isNotEmpty()) ((correct * 100.0) / checked.size).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Quiz,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (strings.isBn) "অনুশীলন সম্পন্ন হয়েছে!" else "Practice Completed!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricStatCard(
                label = if (strings.isBn) "মোট প্রশ্ন" else "Total",
                value = "$total",
                icon = Icons.Default.Quiz,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                label = if (strings.isBn) "সঠিক" else "Correct",
                value = "$correct",
                icon = Icons.Default.CheckCircle,
                tint = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricStatCard(
                label = if (strings.isBn) "ভুল" else "Wrong",
                value = "$incorrect",
                icon = Icons.Default.Close,
                tint = ErrorRed,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                label = if (strings.isBn) "সঠিকতা" else "Accuracy",
                value = "$accuracy%",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (strings.isBn) "সম্পন্ন" else "Done")
        }
    }
}
