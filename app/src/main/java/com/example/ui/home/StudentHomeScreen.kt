package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.Bookmark
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.PracticeSession
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WrongAnswer
import com.example.ui.components.AppLogoBadge
import com.example.ui.components.BengaliEmptyState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.OnlineStatusChip
import com.example.ui.components.subjectIconForName
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.utils.AppStrings

@Composable
fun StudentHomeScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    currentUid: String,
    isOnline: Boolean,
    subjectsState: UiState<List<Subject>>,
    cachedSubjects: List<Subject>,
    questionSetsState: UiState<List<QuestionSet>>,
    practiceSessions: List<PracticeSession>,
    examinationsState: UiState<List<Examination>>,
    resultsState: UiState<List<ExaminationResult>>,
    bookmarks: List<Bookmark>,
    wrongAnswers: List<WrongAnswer>,
    notifications: List<AppNotification>,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSubject: (Subject) -> Unit,
    onStartSetPractice: (QuestionSet) -> Unit,
    onResumePractice: (PracticeSession) -> Unit,
    onStartExam: (Examination) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenWrongAnswers: () -> Unit,
    onOpenOfflineSets: () -> Unit
) {
    val unreadNotifications = notifications.count { currentUid !in it.readByUids }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header: App Logo, App Name, Online/Offline, Notification, Profile
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AppLogoBadge()
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OnlineStatusChip(
                            isOnline = isOnline,
                            onlineText = strings.onlineLabel,
                            offlineText = strings.offlineLabel
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text("$unreadNotifications") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = if (strings.isBn) "বিজ্ঞপ্তি" else "Notifications"
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("home_profile_button")
                    ) {
                        val photo = userProfile?.photoUrl.orEmpty()
                        if (photo.isNotBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = if (strings.isBn) "প্রোফাইল" else "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = if (strings.isBn) "প্রোফাইল" else "Profile",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }

        // Welcome Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${strings.welcomeBack}, ${userProfile?.name?.ifBlank { null } ?: if (strings.isBn) "শিক্ষার্থী" else "Student"}!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (strings.isBn)
                                "প্রতিদিন নিয়মিত অনুশীলন এবং পরীক্ষার মাধ্যমে আপনার প্রস্তুতি আরও দৃঢ় করুন।"
                            else
                                "Strengthen your preparation with daily MCQ practice and online exams.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(onClick = onOpenSearch)
                                .testTag("home_search_bar"),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = strings.searchMcqHint,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.searchMcqHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personal Performance & Quick Access Shortcuts (Bookmarks, Wrong Answers, Offline Sets)
        item {
            val totalAttempted = userProfile?.totalAttempted ?: 0L
            val totalCorrect = userProfile?.totalCorrect ?: 0L
            val accuracyPct = if (totalAttempted > 0L) ((totalCorrect * 100.0) / totalAttempted).toInt() else 0

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(
                    title = if (strings.isBn) "আপনার পরিসংখ্যান ও দ্রুত মেনু" else "Performance & Quick Tools"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        label = if (strings.isBn) "সঠিকতা" else "Accuracy",
                        value = "$accuracyPct%",
                        icon = Icons.Default.TrendingUp,
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = if (strings.isBn) "মোট পরীক্ষা" else "Total Exams",
                        value = "${userProfile?.totalExams ?: 0L}",
                        icon = Icons.Default.Timer,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickShortcutCard(
                        title = if (strings.isBn) "বুকমার্ক (${bookmarks.size})" else "Bookmarks (${bookmarks.size})",
                        icon = Icons.Default.Bookmark,
                        tint = WarningAmber,
                        onClick = onOpenBookmarks,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_bookmarks_card")
                    )
                    QuickShortcutCard(
                        title = if (strings.isBn) "ভুল উত্তর (${wrongAnswers.size})" else "Wrong (${wrongAnswers.size})",
                        icon = Icons.Default.ErrorOutline,
                        tint = ErrorRed,
                        onClick = onOpenWrongAnswers,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_wrong_answers_card")
                    )
                    QuickShortcutCard(
                        title = if (strings.isBn) "অফলাইন সেট" else "Offline",
                        icon = Icons.Default.CloudDownload,
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenOfflineSets,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_offline_card")
                    )
                }
            }
        }

        // Continue Practice (Interrupted Sessions)
        val resumable = practiceSessions.filter { it.status == "in_progress" && it.questionIds.isNotEmpty() }
        if (resumable.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(title = if (strings.isBn) "অনুশীলন চালিয়ে যান" else "Continue Practice")
                    resumable.take(2).forEach { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResumePractice(session) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title.ifBlank { if (strings.isBn) "অসমাপ্ত অনুশীলন" else "Ongoing Practice" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (strings.isBn)
                                            "প্রশ্ন ${session.currentIndex + 1} / ${session.questionIds.size}"
                                        else
                                            "Question ${session.currentIndex + 1} of ${session.questionIds.size}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(onClick = { onResumePractice(session) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (strings.isBn) "চালু করুন" else "Resume")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subject Categories
        item {
            SectionTitle(title = if (strings.isBn) "বিষয়সমূহ" else "Subject Categories")
        }
        item {
            val subjectList = when (subjectsState) {
                is UiState.Success -> subjectsState.data
                else -> if (!isOnline) cachedSubjects else emptyList()
            }
            when {
                subjectsState is UiState.Loading && isOnline -> LoadingCard()
                subjectsState is UiState.Error && subjectList.isEmpty() -> ErrorBannerCard(message = subjectsState.message)
                subjectList.isEmpty() -> BengaliEmptyState(
                    icon = Icons.Default.Quiz,
                    message = strings.emptySubjects
                )
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(subjectList, key = { it.id }) { subject ->
                            OutlinedCard(
                                modifier = Modifier
                                    .width(148.dp)
                                    .clickable { onOpenSubject(subject) }
                                    .testTag("subject_card_${subject.id}"),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = subjectIconForName(subject.iconName),
                                            contentDescription = subject.displayName(strings.isBn),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = subject.displayName(strings.isBn),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent & Popular MCQ Sets
        item {
            SectionTitle(title = if (strings.isBn) "সাম্প্রতিক ও জনপ্রিয় MCQ সেট" else "Recent & Popular MCQ Sets")
        }
        when (questionSetsState) {
            is UiState.Loading -> item { LoadingCard() }
            is UiState.Error -> item { ErrorBannerCard(message = questionSetsState.message) }
            is UiState.Success -> {
                val sets = questionSetsState.data
                if (sets.isEmpty()) {
                    item {
                        BengaliEmptyState(
                            icon = Icons.Default.Quiz,
                            message = strings.emptyMcqs
                        )
                    }
                } else {
                    items(sets.take(6), key = { it.id }) { qSet ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartSetPractice(qSet) },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = qSet.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${qSet.subjectName} • ${qSet.questionCount} MCQ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { onStartSetPractice(qSet) }) {
                                    Text(if (strings.isBn) "অনুশীলন" else "Practice")
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Available & Recently Attempted Examinations
        item {
            SectionTitle(title = if (strings.isBn) "চলমান ও সাম্প্রতিক পরীক্ষা" else "Examinations")
        }
        when (examinationsState) {
            is UiState.Loading -> item { LoadingCard() }
            is UiState.Error -> item { ErrorBannerCard(message = examinationsState.message) }
            is UiState.Success -> {
                val exams = examinationsState.data.filter { it.status == "published" }
                if (exams.isEmpty()) {
                    item {
                        BengaliEmptyState(
                            icon = Icons.Default.Timer,
                            message = strings.emptyExams
                        )
                    }
                } else {
                    items(exams.take(4), key = { it.id }) { exam ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = exam.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${exam.subjectName} • ${exam.totalQuestions} প্রশ্ন • ${exam.timeLimitMinutes} মি.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onStartExam(exam) },
                                    enabled = isOnline
                                ) {
                                    Text(if (strings.isBn) "অংশ নিন" else "Start")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (resultsState is UiState.Success && resultsState.data.isNotEmpty()) {
            item {
                SectionTitle(title = if (strings.isBn) "সাম্প্রতিক পরীক্ষার ফলাফল" else "Recent Exam Results")
            }
            items(resultsState.data.take(3), key = { it.id }) { res ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(res.examTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${res.correctAnswers}/${res.totalQuestions} সঠিক • প্রাপ্ত নম্বর: ${res.obtainedMarks}",
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun QuickShortcutCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = tint)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
