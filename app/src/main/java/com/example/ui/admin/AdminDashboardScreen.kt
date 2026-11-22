package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.Examination
import com.example.data.model.PdfDocument
import com.example.data.model.PdfProcessingJob
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.example.data.model.UserSessionRole
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.utils.AppStrings
import com.example.viewmodel.AppPanelMode

@Composable
fun AdminDashboardScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    subjects: List<Subject>,
    pdfs: List<PdfDocument>,
    pdfJobs: List<PdfProcessingJob>,
    questions: List<AdminQuestionWithAnswer>,
    examinations: List<Examination>,
    students: List<UserProfile>,
    onOpenPdfManager: () -> Unit,
    onOpenPdfReview: () -> Unit,
    onOpenMcqManager: () -> Unit,
    onOpenExamsManager: () -> Unit,
    onSwitchPanel: (AppPanelMode) -> Unit
) {
    val unverifiedCount = questions.count { !it.publicQuestion.verified || it.publicQuestion.status == "unverified" }
    val publishedCount = questions.count { it.publicQuestion.status == "published" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (strings.isBn) "অ্যাডমিন ড্যাশবোর্ড" else "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (strings.isBn) "প্রশ্ন, PDF এবং পরীক্ষা পরিচালনা" else "Manage PDFs, MCQs & Exams",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { onSwitchPanel(AppPanelMode.STUDENT) }) {
                    Text(if (strings.isBn) "শিক্ষার্থী ভিউ" else "Student View")
                }
            }
        }

        // Real Firestore KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricStatCard(
                        label = if (strings.isBn) "মোট MCQ" else "Total MCQs",
                        value = "${questions.size}",
                        icon = Icons.Default.Quiz,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = if (strings.isBn) "প্রকাশিত" else "Published",
                        value = "$publishedCount",
                        icon = Icons.Default.FactCheck,
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricStatCard(
                        label = if (strings.isBn) "পর্যালোচনা বাকি" else "Needs Review",
                        value = "$unverifiedCount",
                        icon = Icons.Default.Assessment,
                        tint = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = if (strings.isBn) "মোট PDF" else "PDF Files",
                        value = "${pdfs.size}",
                        icon = Icons.Default.PictureAsPdf,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricStatCard(
                        label = if (strings.isBn) "বিষয়" else "Subjects",
                        value = "${subjects.size}",
                        icon = Icons.Default.MenuBook,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = if (strings.isBn) "শিক্ষার্থী" else "Students",
                        value = "${students.size}",
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (strings.isBn) "দ্রুত কার্যক্রম" else "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (sessionRole.hasPermission("pdf_upload")) {
                        Button(
                            onClick = onOpenPdfManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_quick_upload_pdf")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (strings.isBn) "PDF আপলোড ও MCQ এক্সট্র্যাক্ট" else "Upload PDF & Extract MCQs")
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenPdfReview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_quick_review_mcqs")
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (strings.isBn)
                                "এক্সট্র্যাক্টেড MCQ পর্যালোচনা ($unverifiedCount)"
                            else
                                "Review Extracted MCQs ($unverifiedCount)"
                        )
                    }

                    if (sessionRole.hasPermission("question_create")) {
                        OutlinedButton(
                            onClick = onOpenMcqManager,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (strings.isBn) "নতুন MCQ, বিষয় ও অধ্যায় পরিচালনা" else "Manage MCQs & Subjects")
                        }
                    }

                    if (sessionRole.hasPermission("exam_manage")) {
                        OutlinedButton(
                            onClick = onOpenExamsManager,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (strings.isBn) "পরীক্ষা তৈরি ও পরিচালনা (${examinations.size})" else "Manage Exams (${examinations.size})")
                        }
                    }
                }
            }
        }

        if (pdfJobs.isNotEmpty()) {
            item {
                Text(
                    text = if (strings.isBn) "সাম্প্রতিক PDF প্রসেসিং ইতিহাস" else "Recent PDF Extraction Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(pdfJobs.take(5), key = { it.id }) { job ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPdfReview() },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(job.pdfFileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (strings.isBn)
                                "অবস্থা: ${job.status} • পৃষ্ঠা: ${job.processedPages}/${job.totalPages} • প্রাপ্ত প্রশ্ন: ${job.extractedQuestionsCount}"
                            else
                                "Status: ${job.status} • Pages: ${job.processedPages}/${job.totalPages} • Extracted: ${job.extractedQuestionsCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (job.errorMessage.isNotBlank()) {
                            Text(job.errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
