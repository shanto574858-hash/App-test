package com.example.ui.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Chapter
import com.example.data.model.PdfDocument
import com.example.data.model.PdfProcessingJob
import com.example.data.model.Subject
import com.example.data.model.UserSessionRole
import com.example.ui.components.BengaliEmptyState
import com.example.utils.AppStrings
import com.example.viewmodel.PdfPreviewUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfManagerScreen(
    strings: AppStrings,
    sessionRole: UserSessionRole,
    pdfs: List<PdfDocument>,
    pdfJobs: List<PdfProcessingJob>,
    subjects: List<Subject>,
    chapters: List<Chapter>,
    pdfPreview: PdfPreviewUiState,
    ocrPackReady: Boolean,
    isBusy: Boolean,
    onDownloadOcrPacks: () -> Unit,
    onPreparePdfPreview: (Uri, String) -> Unit,
    onLoadPreviewPage: (Int) -> Unit,
    onClearPdfPreview: () -> Unit,
    onUploadAndExtract: (subjectId: String, subjectName: String, chapterId: String, chapterName: String, extractNow: Boolean, forceOcr: Boolean) -> Unit,
    onExtractExistingPdf: (PdfDocument, subjectName: String, chapterName: String, forceOcr: Boolean) -> Unit,
    onRenamePdf: (pdfId: String, newName: String) -> Unit,
    onReplacePdf: (PdfDocument, Uri, String) -> Unit,
    onDeletePdf: (PdfDocument) -> Unit,
    onOpenReviewScreen: () -> Unit
) {
    val context = LocalContext.current
    var pdfToReplace by remember { mutableStateOf<PdfDocument?>(null) }
    var pdfToRename by remember { mutableStateOf<PdfDocument?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var forceOcr by remember { mutableStateOf(false) }
    var extractImmediately by remember { mutableStateOf(true) }
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var chapterMenuOpen by remember { mutableStateOf(false) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = resolveFileName(context, uri)
            onPreparePdfPreview(uri, fileName)
        }
    }

    val replacePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pdfToReplace
        if (uri != null && target != null) {
            val fileName = resolveFileName(context, uri)
            onReplacePdf(target, uri, fileName)
        }
        pdfToReplace = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (strings.isBn) "PDF ও OCR ম্যানেজার" else "PDF & OCR Manager",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (strings.isBn)
                            "টেক্সট এবং স্ক্যান করা বাংলা/ইংরেজি PDF থেকে MCQ এক্সট্র্যাক্ট করুন"
                        else
                            "Extract MCQs from text or scanned Bengali/English PDFs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onOpenReviewScreen) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (strings.isBn) "রিভিউ" else "Review")
                }
            }
        }

        // OCR Traineddata pack card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
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
                            text = if (strings.isBn) "বাংলা ও ইংরেজি OCR ইঞ্জিন" else "Bengali & English OCR Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (ocrPackReady) {
                                if (strings.isBn) "অফলাইন OCR মডেল প্রস্তুত (ben + eng)" else "OCR packs ready (ben + eng)"
                            } else {
                                if (strings.isBn) "স্ক্যান করা PDF পড়তে OCR ডেটা ডাউনলোড করুন" else "Download OCR packs for scanned PDFs"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!ocrPackReady) {
                        Button(onClick = onDownloadOcrPacks, enabled = !isBusy) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (strings.isBn) "ডাউনলোড" else "Download")
                        }
                    }
                }
            }
        }

        // Select PDF via Storage Access Framework
        if (sessionRole.hasPermission("pdf_upload")) {
            item {
                Button(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("select_pdf_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (strings.isBn) "ডিভাইস থেকে PDF নির্বাচন করুন" else "Select PDF from Storage")
                }
            }
        }

        // Selected PDF Preview + Upload + Extract Card
        if (pdfPreview.file != null || pdfPreview.isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pdfPreview.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearPdfPreview) {
                                Text(strings.cancel)
                            }
                        }

                        if (pdfPreview.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else if (pdfPreview.currentBitmap != null) {
                            Image(
                                bitmap = pdfPreview.currentBitmap.asImageBitmap(),
                                contentDescription = "PDF Page Preview",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onLoadPreviewPage(pdfPreview.currentPageIndex - 1) },
                                    enabled = pdfPreview.currentPageIndex > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous page")
                                }
                                Text(
                                    text = if (strings.isBn)
                                        "পৃষ্ঠা ${pdfPreview.currentPageIndex + 1} / ${pdfPreview.pageCount}"
                                    else
                                        "Page ${pdfPreview.currentPageIndex + 1} of ${pdfPreview.pageCount}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                IconButton(
                                    onClick = { onLoadPreviewPage(pdfPreview.currentPageIndex + 1) },
                                    enabled = pdfPreview.currentPageIndex < pdfPreview.pageCount - 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next page")
                                }
                            }
                        }

                        // Target Subject & Chapter
                        ExposedDropdownMenuBox(
                            expanded = subjectMenuOpen,
                            onExpandedChange = { subjectMenuOpen = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSubject?.displayName(strings.isBn)
                                    ?: if (strings.isBn) "বিষয় নির্বাচন (ঐচ্ছিক)" else "Select Subject (Optional)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (strings.isBn) "বিষয়" else "Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuOpen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = subjectMenuOpen,
                                onDismissRequest = { subjectMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (strings.isBn) "নির্ধারিত নয়" else "None") },
                                    onClick = {
                                        selectedSubject = null
                                        selectedChapter = null
                                        subjectMenuOpen = false
                                    }
                                )
                                subjects.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.displayName(strings.isBn)) },
                                        onClick = {
                                            selectedSubject = s
                                            selectedChapter = null
                                            subjectMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        val subChapters = chapters.filter { it.subjectId == selectedSubject?.id }
                        if (selectedSubject != null && subChapters.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = chapterMenuOpen,
                                onExpandedChange = { chapterMenuOpen = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedChapter?.displayName(strings.isBn)
                                        ?: if (strings.isBn) "অধ্যায় নির্বাচন" else "Select Chapter",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (strings.isBn) "অধ্যায়" else "Chapter") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterMenuOpen) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = chapterMenuOpen,
                                    onDismissRequest = { chapterMenuOpen = false }
                                ) {
                                    subChapters.forEach { ch ->
                                        DropdownMenuItem(
                                            text = { Text(ch.displayName(strings.isBn)) },
                                            onClick = {
                                                selectedChapter = ch
                                                chapterMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isBn) "আপলোডের পর স্বয়ংক্রিয় MCQ এক্সট্র্যাক্ট করুন" else "Extract MCQs after upload")
                            Switch(checked = extractImmediately, onCheckedChange = { extractImmediately = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isBn) "স্ক্যান করা PDF (OCR বাধ্যতামূলক করুন)" else "Force OCR for scanned pages")
                            Switch(checked = forceOcr, onCheckedChange = { forceOcr = it })
                        }

                        Button(
                            onClick = {
                                onUploadAndExtract(
                                    selectedSubject?.id.orEmpty(),
                                    selectedSubject?.displayName(strings.isBn).orEmpty(),
                                    selectedChapter?.id.orEmpty(),
                                    selectedChapter?.displayName(strings.isBn).orEmpty(),
                                    extractImmediately,
                                    forceOcr
                                )
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_extract_pdf_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    if (extractImmediately) {
                                        if (strings.isBn) "আপলোড ও MCQ এক্সট্র্যাক্ট করুন" else "Upload & Extract MCQs"
                                    } else {
                                        if (strings.isBn) "শুধু PDF আপলোড করুন" else "Upload PDF Only"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active / Recent Extraction Jobs
        if (pdfJobs.isNotEmpty()) {
            item {
                Text(
                    text = if (strings.isBn) "প্রসেসিং ও এক্সট্র্যাকশন ইতিহাস" else "Extraction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(pdfJobs.take(4), key = { it.id }) { job ->
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(job.pdfFileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (job.totalPages > 0 && job.status == "processing") {
                            LinearProgressIndicator(
                                progress = { job.processedPages.toFloat() / job.totalPages.coerceAtLeast(1) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = "${job.status.uppercase()} • ${job.processedPages}/${job.totalPages} পৃষ্ঠা • ${job.extractedQuestionsCount} MCQ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Uploaded PDFs List
        item {
            Text(
                text = if (strings.isBn) "আপলোড করা PDF তালিকা (${pdfs.size})" else "Uploaded PDFs (${pdfs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (pdfs.isEmpty()) {
            item {
                BengaliEmptyState(
                    icon = Icons.Default.PictureAsPdf,
                    message = strings.emptyPdfs
                )
            }
        } else {
            items(pdfs, key = { it.id }) { pdf ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pdf.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${pdf.fileSizeBytes / 1024} KB • ${pdf.pageCount} পৃষ্ঠা • ${pdf.processingStatus} • ${pdf.extractedCount} MCQ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val subName = subjects.firstOrNull { it.id == pdf.subjectId }?.nameBn.orEmpty()
                                    val chapName = chapters.firstOrNull { it.id == pdf.chapterId }?.nameBn.orEmpty()
                                    onExtractExistingPdf(pdf, subName, chapName, forceOcr)
                                },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (pdf.extractedCount > 0) Icons.Default.Refresh else Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (pdf.extractedCount > 0) {
                                        if (strings.isBn) "পুনরায় এক্সট্র্যাক্ট" else "Reprocess"
                                    } else {
                                        if (strings.isBn) "এক্সট্র্যাক্ট" else "Extract"
                                    },
                                    maxLines = 1
                                )
                            }

                            IconButton(onClick = { pdfToRename = pdf }) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename PDF")
                            }
                            if (sessionRole.hasPermission("pdf_upload")) {
                                IconButton(onClick = {
                                    pdfToReplace = pdf
                                    replacePdfLauncher.launch(arrayOf("application/pdf"))
                                }) {
                                    Icon(Icons.Default.FindReplace, contentDescription = "Replace PDF")
                                }
                            }
                            if (sessionRole.hasPermission("pdf_delete")) {
                                IconButton(onClick = { onDeletePdf(pdf) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete PDF", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pdfToRename?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.fileName) }
        AlertDialog(
            onDismissRequest = { pdfToRename = null },
            title = { Text(if (strings.isBn) "PDF নাম পরিবর্তন" else "Rename PDF") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(if (strings.isBn) "নতুন নাম" else "New File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onRenamePdf(target.id, newName.trim())
                    }
                    pdfToRename = null
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToRename = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    var result = "document_${System.currentTimeMillis()}.pdf"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            result = cursor.getString(nameIndex) ?: result
        }
    }
    return result
}
