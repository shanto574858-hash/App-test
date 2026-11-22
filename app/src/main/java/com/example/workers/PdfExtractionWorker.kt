package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.model.SearchTokenizer
import com.example.domain.pdf.McqParserConfig
import com.example.domain.pdf.McqParserEngine
import com.example.domain.pdf.PdfTextAndOcrExtractor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class PdfExtractionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pdfId = inputData.getString(KEY_PDF_ID).orEmpty()
        val jobId = inputData.getString(KEY_JOB_ID).orEmpty()
        val pdfName = inputData.getString(KEY_PDF_NAME).orEmpty()
        val storagePath = inputData.getString(KEY_STORAGE_PATH).orEmpty()
        val localFilePath = inputData.getString(KEY_LOCAL_FILE_PATH).orEmpty()
        val subjectId = inputData.getString(KEY_SUBJECT_ID).orEmpty()
        val subjectName = inputData.getString(KEY_SUBJECT_NAME).orEmpty()
        val chapterId = inputData.getString(KEY_CHAPTER_ID).orEmpty()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME).orEmpty()
        val forceOcr = inputData.getBoolean(KEY_FORCE_OCR, false)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(workDataOf(KEY_ERROR to "Authentication required"))

        val databaseId = applicationContext.getString(R.string.firestore_database_id)
        val db = if (databaseId.isBlank() || databaseId == "(default)") {
            FirebaseFirestore.getInstance()
        } else {
            FirebaseFirestore.getInstance(databaseId)
        }
        val jobRef = db.collection("pdfProcessingJobs").document(jobId)
        val pdfRef = db.collection("pdfDocuments").document(pdfId)

        val tempFile = if (localFilePath.isNotBlank() && File(localFilePath).exists()) {
            File(localFilePath)
        } else {
            File(applicationContext.cacheDir, "extract_${pdfId}.pdf")
        }

        return try {
            jobRef.update(
                mapOf(
                    "status" to "running",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            pdfRef.update(
                mapOf(
                    "processingStatus" to "processing",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!tempFile.exists() && storagePath.isNotBlank()) {
                FirebaseStorage.getInstance().reference.child(storagePath)
                    .getFile(tempFile)
                    .await()
            }

            val extractor = PdfTextAndOcrExtractor(applicationContext)
            val extractionResult = extractor.extractTextAndOcr(
                pdfFile = tempFile,
                forceOcr = forceOcr
            ) { currentPage, totalPages, _ ->
                setProgress(workDataOf(KEY_PROGRESS_PAGE to currentPage, KEY_TOTAL_PAGES to totalPages))
                jobRef.update(
                    mapOf(
                        "processedPages" to currentPage,
                        "totalPages" to totalPages,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (extractionResult.isFailure) {
                val errMsg = extractionResult.exceptionOrNull()?.localizedMessage ?: "PDF extraction failed"
                markFailed(jobRef, pdfRef, errMsg)
                return Result.failure(workDataOf(KEY_ERROR to errMsg))
            }

            val summary = extractionResult.getOrThrow()
            val parsedMcqs = McqParserEngine.parsePages(
                pages = summary.pageTexts,
                config = McqParserConfig(autoVerifyHighConfidence = false)
            )

            // Write extracted questions to Admin Review queue (NEVER auto-publish unverified!)
            var unverifiedCount = 0
            val chunked = parsedMcqs.chunked(150)
            for (chunk in chunked) {
                val batch = db.batch()
                for (mcq in chunk) {
                    if (!mcq.verified) unverifiedCount++
                    val qRef = db.collection("questions_public").document()
                    val qId = qRef.id
                    val now = FieldValue.serverTimestamp()

                    val pubPayload = mapOf(
                        "questionText" to mcq.questionText,
                        "originalPdfText" to mcq.originalPdfText,
                        "optionA" to mcq.optionA,
                        "optionB" to mcq.optionB,
                        "optionC" to mcq.optionC,
                        "optionD" to mcq.optionD,
                        "subjectId" to subjectId.ifBlank { "general" },
                        "subjectName" to subjectName,
                        "chapterId" to chapterId,
                        "chapterName" to chapterName,
                        "topicId" to "",
                        "topicName" to "",
                        "questionSetId" to "",
                        "difficulty" to "medium",
                        "language" to mcq.language,
                        "source" to pdfName,
                        "sourcePdfId" to pdfId,
                        "pageNumber" to mcq.pageNumber,
                        "questionNumber" to mcq.questionNumber,
                        "confidence" to mcq.confidence,
                        "verified" to mcq.verified,
                        "needsReview" to mcq.needsReview,
                        "hasMath" to mcq.hasMath,
                        "searchTokens" to SearchTokenizer.buildTokens(
                            mcq.questionText, subjectName, chapterName, pdfName, mcq.questionNumber.toString()
                        ),
                        "organizationId" to "default",
                        "status" to "unpublished",
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                    batch.set(qRef, pubPayload)

                    val privRef = db.collection("questionAnswers_private").document(qId)
                    val privPayload = mapOf(
                        "questionId" to qId,
                        "correctAnswer" to (mcq.correctAnswer ?: ""),
                        "explanation" to mcq.explanation,
                        "verified" to mcq.verified,
                        "confidence" to mcq.confidence,
                        "status" to if (mcq.verified) "active" else "unverified",
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                    batch.set(privRef, privPayload)
                }
                batch.commit().await()
            }

            jobRef.update(
                mapOf(
                    "mode" to summary.modeUsed,
                    "processedPages" to summary.totalPages,
                    "totalPages" to summary.totalPages,
                    "extractedQuestionsCount" to parsedMcqs.size,
                    "unverifiedCount" to unverifiedCount,
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            pdfRef.update(
                mapOf(
                    "pageCount" to summary.totalPages,
                    "processingStatus" to "completed",
                    "extractedCount" to parsedMcqs.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                workDataOf(
                    KEY_EXTRACTED_COUNT to parsedMcqs.size,
                    KEY_UNVERIFIED_COUNT to unverifiedCount
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "PdfExtractionWorker failed", e)
            val errMsg = e.localizedMessage ?: "Extraction failed"
            try {
                markFailed(jobRef, pdfRef, errMsg)
            } catch (_: Exception) {
            }
            Result.failure(workDataOf(KEY_ERROR to errMsg))
        }
    }

    private suspend fun markFailed(
        jobRef: com.google.firebase.firestore.DocumentReference,
        pdfRef: com.google.firebase.firestore.DocumentReference,
        errMsg: String
    ) {
        jobRef.update(
            mapOf(
                "status" to "failed",
                "errorMessage" to errMsg.take(900),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        pdfRef.update(
            mapOf(
                "processingStatus" to "failed",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    companion object {
        private const val TAG = "PdfExtractionWorker"
        const val KEY_PDF_ID = "pdf_id"
        const val KEY_JOB_ID = "job_id"
        const val KEY_PDF_NAME = "pdf_name"
        const val KEY_STORAGE_PATH = "storage_path"
        const val KEY_LOCAL_FILE_PATH = "local_file_path"
        const val KEY_SUBJECT_ID = "subject_id"
        const val KEY_SUBJECT_NAME = "subject_name"
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_CHAPTER_NAME = "chapter_name"
        const val KEY_FORCE_OCR = "force_ocr"
        const val KEY_PROGRESS_PAGE = "progress_page"
        const val KEY_TOTAL_PAGES = "total_pages"
        const val KEY_EXTRACTED_COUNT = "extracted_count"
        const val KEY_UNVERIFIED_COUNT = "unverified_count"
        const val KEY_ERROR = "error"
    }
}
