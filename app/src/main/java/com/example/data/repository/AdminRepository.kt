package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.AdminRecord
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Chapter
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.PdfDocument
import com.example.data.model.PdfProcessingJob
import com.example.data.model.QuestionAnswerPrivate
import com.example.data.model.QuestionPublic
import com.example.data.model.QuestionSet
import com.example.data.model.SearchTokenizer
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UserProfile
import com.example.domain.pdf.PdfTextAndOcrExtractor
import com.example.utils.AppConstants
import com.example.utils.OperationType
import com.example.utils.handleFirestoreError
import com.example.workers.PdfExtractionWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AdminRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Admin authentication required.")

    fun observeAllSubjects(): Flow<List<Subject>> =
        db.collection("subjects")
            .snapshots()
            .map { snap ->
                snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { handleStreamError(it, OperationType.LIST, "subjects") }

    fun observeAllChapters(): Flow<List<Chapter>> =
        db.collection("chapters")
            .snapshots()
            .map { snap ->
                snap.toObjects(Chapter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { handleStreamError(it, OperationType.LIST, "chapters") }

    fun observeAllTopics(): Flow<List<Topic>> =
        db.collection("topics")
            .snapshots()
            .map { snap ->
                snap.toObjects(Topic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { handleStreamError(it, OperationType.LIST, "topics") }

    fun observePdfDocuments(): Flow<List<PdfDocument>> =
        db.collection("pdfDocuments")
            .snapshots()
            .map { snap ->
                snap.toObjects(PdfDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "pdfDocuments") }

    fun observePdfProcessingJobs(): Flow<List<PdfProcessingJob>> =
        db.collection("pdfProcessingJobs")
            .snapshots()
            .map { snap ->
                snap.toObjects(PdfProcessingJob::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "pdfProcessingJobs") }

    fun observeAdminQuestionsWithAnswers(): Flow<List<AdminQuestionWithAnswer>> {
        val pubFlow = db.collection("questions_public").snapshots()
            .map { it.toObjects(QuestionPublic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val privFlow = db.collection("questionAnswers_private").snapshots()
            .map { snap ->
                snap.toObjects(QuestionAnswerPrivate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .associateBy { it.questionId.ifBlank { it.id } }
            }

        return combine(pubFlow, privFlow) { pubs, privMap ->
            pubs.sortedByDescending { it.createdAt?.seconds ?: 0L }
                .map { pub -> AdminQuestionWithAnswer(pub, privMap[pub.id]) }
        }.catch { handleStreamError(it, OperationType.LIST, "questions_public") }
    }

    fun observeAllQuestionSets(): Flow<List<QuestionSet>> =
        db.collection("questionSets")
            .snapshots()
            .map { snap ->
                snap.toObjects(QuestionSet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "questionSets") }

    fun observeAllExaminations(): Flow<List<Examination>> =
        db.collection("examinations")
            .snapshots()
            .map { snap ->
                snap.toObjects(Examination::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "examinations") }

    fun observeAllStudents(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "users") }

    fun observeAllResultsAdmin(): Flow<List<ExaminationResult>> =
        db.collection("examinationResults")
            .snapshots()
            .map { snap ->
                snap.toObjects(ExaminationResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "examinationResults") }

    fun observeAdmins(): Flow<List<AdminRecord>> =
        db.collection("admins")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { handleStreamError(it, OperationType.LIST, "admins") }

    fun observeAppSettings(): Flow<AppSettings?> =
        db.collection("appSettings").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else null
            }
            .catch { handleStreamError(it, OperationType.GET, "appSettings/global") }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("auditLogs")
            .limit(100)
            .snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "auditLogs") }

    // --- PDF MANAGEMENT ---

    fun copyPdfToCache(uri: Uri, targetName: String): Result<File> {
        return try {
            val safeName = targetName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val cacheFile = File(context.cacheDir, "pdf_${System.currentTimeMillis()}_$safeName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return Result.failure(IllegalStateException("Cannot open selected PDF"))
            Result.success(cacheFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPdfDocument(
        localFile: File,
        displayName: String,
        subjectId: String,
        chapterId: String
    ): Result<PdfDocument> {
        val path = "pdfDocuments"
        return try {
            val uid = requireUserId()
            val pdfRef = db.collection(path).document()
            val pdfId = pdfRef.id
            val pageCount = try {
                PdfTextAndOcrExtractor(context).getPageCount(localFile)
            } catch (_: Exception) {
                0
            }

            val storagePath = "pdfs/$uid/$pdfId/${displayName.replace("/", "_")}"
            val storageRef = storage.reference.child(storagePath)
            storageRef.putFile(Uri.fromFile(localFile)).await()
            val fileUrl = storageRef.downloadUrl.await().toString()

            val payload = mapOf(
                "fileName" to displayName.take(255),
                "storagePath" to storagePath,
                "fileUrl" to fileUrl,
                "fileSizeBytes" to localFile.length(),
                "pageCount" to pageCount,
                "fileType" to "application/pdf",
                "processingStatus" to "uploaded",
                "extractedCount" to 0,
                "subjectId" to subjectId,
                "chapterId" to chapterId,
                "organizationId" to "default",
                "status" to "active",
                "uploadedBy" to uid,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            pdfRef.set(payload).await()
            val saved = pdfRef.get().await().toObject(PdfDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: PdfDocument(id = pdfId, fileName = displayName, storagePath = storagePath, fileUrl = fileUrl, pageCount = pageCount)
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun renamePdfDocument(pdfId: String, newName: String): Result<Unit> {
        val path = "pdfDocuments/$pdfId"
        return try {
            requireUserId()
            require(newName.trim().isNotEmpty()) { "ফাইলের নাম খালি রাখা যাবে না।" }
            db.collection("pdfDocuments").document(pdfId).update(
                mapOf(
                    "fileName" to newName.trim().take(255),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun replacePdfDocument(pdf: PdfDocument, newLocalFile: File, newName: String): Result<Unit> {
        val path = "pdfDocuments/${pdf.id}"
        return try {
            val uid = requireUserId()
            val pageCount = try {
                PdfTextAndOcrExtractor(context).getPageCount(newLocalFile)
            } catch (_: Exception) {
                0
            }
            val newStoragePath = "pdfs/$uid/${pdf.id}/${newName.replace("/", "_")}"
            val ref = storage.reference.child(newStoragePath)
            ref.putFile(Uri.fromFile(newLocalFile)).await()
            val newUrl = ref.downloadUrl.await().toString()
            if (pdf.storagePath.isNotBlank() && pdf.storagePath != newStoragePath) {
                try {
                    storage.reference.child(pdf.storagePath).delete().await()
                } catch (_: Exception) {
                }
            }
            db.collection("pdfDocuments").document(pdf.id).update(
                mapOf(
                    "fileName" to newName.take(255),
                    "storagePath" to newStoragePath,
                    "fileUrl" to newUrl,
                    "fileSizeBytes" to newLocalFile.length(),
                    "pageCount" to pageCount,
                    "processingStatus" to "uploaded",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePdfDocument(pdf: PdfDocument): Result<Unit> {
        val path = "pdfDocuments/${pdf.id}"
        return try {
            requireUserId()
            if (pdf.storagePath.isNotBlank()) {
                try {
                    storage.reference.child(pdf.storagePath).delete().await()
                } catch (_: Exception) {
                }
            }
            db.collection("pdfDocuments").document(pdf.id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun enqueuePdfExtraction(
        pdf: PdfDocument,
        localFilePath: String,
        subjectName: String,
        chapterName: String,
        forceOcr: Boolean
    ): Result<String> {
        val path = "pdfProcessingJobs"
        return try {
            val uid = requireUserId()
            val jobRef = db.collection(path).document()
            val jobId = jobRef.id
            jobRef.set(
                mapOf(
                    "pdfId" to pdf.id,
                    "pdfFileName" to pdf.fileName,
                    "mode" to if (forceOcr) "ocr" else "hybrid",
                    "processedPages" to 0,
                    "totalPages" to pdf.pageCount,
                    "extractedQuestionsCount" to 0,
                    "unverifiedCount" to 0,
                    "errorMessage" to "",
                    "status" to "queued",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val data = Data.Builder()
                .putString(PdfExtractionWorker.KEY_PDF_ID, pdf.id)
                .putString(PdfExtractionWorker.KEY_JOB_ID, jobId)
                .putString(PdfExtractionWorker.KEY_PDF_NAME, pdf.fileName)
                .putString(PdfExtractionWorker.KEY_STORAGE_PATH, pdf.storagePath)
                .putString(PdfExtractionWorker.KEY_LOCAL_FILE_PATH, localFilePath)
                .putString(PdfExtractionWorker.KEY_SUBJECT_ID, pdf.subjectId)
                .putString(PdfExtractionWorker.KEY_SUBJECT_NAME, subjectName)
                .putString(PdfExtractionWorker.KEY_CHAPTER_ID, pdf.chapterId)
                .putString(PdfExtractionWorker.KEY_CHAPTER_NAME, chapterName)
                .putBoolean(PdfExtractionWorker.KEY_FORCE_OCR, forceOcr)
                .build()

            val request = OneTimeWorkRequestBuilder<PdfExtractionWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("pdf_extract_${pdf.id}", ExistingWorkPolicy.REPLACE, request)

            Result.success(jobId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- MCQ & REVIEW CRUD ---

    suspend fun saveQuestionWithAnswer(
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
    ): Result<String> {
        val path = "questions_public"
        return try {
            val uid = requireUserId()
            require(questionText.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank()) {
                "প্রশ্ন এবং ন্যূনতম দুটি বিকল্প আবশ্যক।"
            }
            val qRef = if (questionId.isNullOrBlank()) db.collection(path).document() else db.collection(path).document(questionId)
            val resolvedId = qRef.id
            val privRef = db.collection("questionAnswers_private").document(resolvedId)

            val hasValidAns = correctAnswer in listOf("A", "B", "C", "D")
            val finalVerified = verified && hasValidAns
            val finalStatus = if (publishImmediately && finalVerified) "published" else "unpublished"
            val now = FieldValue.serverTimestamp()
            val tokens = SearchTokenizer.buildTokens(questionText, subjectName, chapterName, topicName, source)

            val batch = db.batch()
            if (questionId.isNullOrBlank()) {
                batch.set(
                    qRef,
                    mapOf(
                        "questionText" to questionText.trim(),
                        "originalPdfText" to "",
                        "optionA" to optionA.trim(),
                        "optionB" to optionB.trim(),
                        "optionC" to optionC.trim(),
                        "optionD" to optionD.trim(),
                        "subjectId" to subjectId.ifBlank { "general" },
                        "subjectName" to subjectName,
                        "chapterId" to chapterId,
                        "chapterName" to chapterName,
                        "topicId" to topicId,
                        "topicName" to topicName,
                        "questionSetId" to questionSetId,
                        "difficulty" to difficulty,
                        "language" to language,
                        "source" to source,
                        "sourcePdfId" to "",
                        "pageNumber" to 0,
                        "questionNumber" to 0,
                        "confidence" to 1.0,
                        "verified" to finalVerified,
                        "needsReview" to !finalVerified,
                        "hasMath" to com.example.domain.pdf.McqParserEngine.containsMathSymbols(questionText),
                        "searchTokens" to tokens,
                        "organizationId" to "default",
                        "status" to finalStatus,
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.set(
                    privRef,
                    mapOf(
                        "questionId" to resolvedId,
                        "correctAnswer" to if (hasValidAns) correctAnswer else "",
                        "explanation" to explanation.trim(),
                        "verified" to finalVerified,
                        "confidence" to 1.0,
                        "status" to if (finalVerified) "active" else "unverified",
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            } else {
                batch.update(
                    qRef,
                    mapOf(
                        "questionText" to questionText.trim(),
                        "optionA" to optionA.trim(),
                        "optionB" to optionB.trim(),
                        "optionC" to optionC.trim(),
                        "optionD" to optionD.trim(),
                        "subjectId" to subjectId.ifBlank { "general" },
                        "subjectName" to subjectName,
                        "chapterId" to chapterId,
                        "chapterName" to chapterName,
                        "topicId" to topicId,
                        "topicName" to topicName,
                        "questionSetId" to questionSetId,
                        "difficulty" to difficulty,
                        "language" to language,
                        "source" to source,
                        "verified" to finalVerified,
                        "needsReview" to !finalVerified,
                        "hasMath" to com.example.domain.pdf.McqParserEngine.containsMathSymbols(questionText),
                        "searchTokens" to tokens,
                        "status" to finalStatus,
                        "updatedAt" to now
                    )
                )
                batch.update(
                    privRef,
                    mapOf(
                        "correctAnswer" to if (hasValidAns) correctAnswer else "",
                        "explanation" to explanation.trim(),
                        "verified" to finalVerified,
                        "status" to if (finalVerified) "active" else "unverified",
                        "updatedAt" to now
                    )
                )
            }
            batch.commit().await()
            Result.success(resolvedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setQuestionStatus(questionId: String, status: String, verified: Boolean): Result<Unit> {
        val path = "questions_public/$questionId"
        return try {
            requireUserId()
            val batch = db.batch()
            val now = FieldValue.serverTimestamp()
            batch.update(
                db.collection("questions_public").document(questionId),
                mapOf(
                    "status" to status,
                    "verified" to verified,
                    "needsReview" to !verified,
                    "updatedAt" to now
                )
            )
            batch.update(
                db.collection("questionAnswers_private").document(questionId),
                mapOf(
                    "verified" to verified,
                    "status" to if (status == "rejected") "rejected" else if (verified) "active" else "unverified",
                    "updatedAt" to now
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishQuestionsBulk(questionIds: List<String>): Result<Int> {
        if (questionIds.isEmpty()) return Result.success(0)
        return try {
            requireUserId()
            val now = FieldValue.serverTimestamp()
            for (chunk in questionIds.chunked(200)) {
                val batch = db.batch()
                for (qId in chunk) {
                    batch.update(
                        db.collection("questions_public").document(qId),
                        mapOf("status" to "published", "verified" to true, "needsReview" to false, "updatedAt" to now)
                    )
                    batch.update(
                        db.collection("questionAnswers_private").document(qId),
                        mapOf("verified" to true, "status" to "active", "updatedAt" to now)
                    )
                }
                batch.commit().await()
            }
            Result.success(questionIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "questions_public")
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> {
        val path = "questions_public/$questionId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.delete(db.collection("questions_public").document(questionId))
            batch.delete(db.collection("questionAnswers_private").document(questionId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun duplicateQuestion(item: AdminQuestionWithAnswer): Result<String> {
        val q = item.publicQuestion
        val a = item.privateAnswer
        return saveQuestionWithAnswer(
            questionId = null,
            questionText = "${q.questionText} (কপি)",
            optionA = q.optionA,
            optionB = q.optionB,
            optionC = q.optionC,
            optionD = q.optionD,
            correctAnswer = a?.correctAnswer.orEmpty(),
            explanation = a?.explanation.orEmpty(),
            subjectId = q.subjectId,
            subjectName = q.subjectName,
            chapterId = q.chapterId,
            chapterName = q.chapterName,
            topicId = q.topicId,
            topicName = q.topicName,
            questionSetId = q.questionSetId,
            difficulty = q.difficulty,
            language = q.language,
            source = q.source,
            verified = q.verified,
            publishImmediately = false
        )
    }

    suspend fun bulkImportJson(jsonContent: String, subjectId: String, subjectName: String): Result<Int> {
        return try {
            val uid = requireUserId()
            val array = JSONArray(jsonContent)
            var imported = 0
            val now = FieldValue.serverTimestamp()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val qText = obj.optString("question").ifBlank { obj.optString("questionText") }.trim()
                val optionsObj = obj.optJSONObject("options")
                val optA = (optionsObj?.optString("A") ?: obj.optString("optionA")).trim()
                val optB = (optionsObj?.optString("B") ?: obj.optString("optionB")).trim()
                val optC = (optionsObj?.optString("C") ?: obj.optString("optionC")).trim()
                val optD = (optionsObj?.optString("D") ?: obj.optString("optionD")).trim()
                val ans = obj.optString("correctAnswer").trim().uppercase()
                val exp = obj.optString("explanation").trim()
                val lang = obj.optString("language", "bn")
                if (qText.isBlank() || optA.isBlank() || optB.isBlank()) continue

                val qRef = db.collection("questions_public").document()
                val verified = ans in listOf("A", "B", "C", "D")
                val batch = db.batch()
                batch.set(
                    qRef,
                    mapOf(
                        "questionText" to qText,
                        "optionA" to optA,
                        "optionB" to optB,
                        "optionC" to optC,
                        "optionD" to optD,
                        "subjectId" to subjectId.ifBlank { "general" },
                        "subjectName" to subjectName,
                        "difficulty" to obj.optString("difficulty", "medium"),
                        "language" to lang,
                        "verified" to verified,
                        "needsReview" to !verified,
                        "searchTokens" to SearchTokenizer.buildTokens(qText, subjectName),
                        "status" to "unpublished",
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.set(
                    db.collection("questionAnswers_private").document(qRef.id),
                    mapOf(
                        "questionId" to qRef.id,
                        "correctAnswer" to if (verified) ans else "",
                        "explanation" to exp,
                        "verified" to verified,
                        "confidence" to 1.0,
                        "status" to if (verified) "active" else "unverified",
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.commit().await()
                imported++
            }
            Result.success(imported)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportQuestionsToJson(questions: List<AdminQuestionWithAnswer>): String {
        val array = JSONArray()
        for (item in questions) {
            val q = item.publicQuestion
            val a = item.privateAnswer
            val options = JSONObject().apply {
                put("A", q.optionA)
                put("B", q.optionB)
                put("C", q.optionC)
                put("D", q.optionD)
            }
            val obj = JSONObject().apply {
                put("id", q.id)
                put("question", q.questionText)
                put("options", options)
                put("correctAnswer", a?.correctAnswer.orEmpty())
                put("explanation", a?.explanation.orEmpty())
                put("subject", q.subjectName)
                put("difficulty", q.difficulty)
                put("language", q.language)
                put("status", q.status)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    // --- SUBJECTS, CHAPTERS, TOPICS, SETS ---

    suspend fun saveSubject(
        subjectId: String?,
        nameBn: String,
        nameEn: String,
        description: String,
        iconName: String,
        displayOrder: Int,
        status: String = "published"
    ): Result<Unit> {
        val path = "subjects"
        return try {
            val uid = requireUserId()
            val ref = if (subjectId.isNullOrBlank()) db.collection(path).document() else db.collection(path).document(subjectId)
            val now = FieldValue.serverTimestamp()
            if (subjectId.isNullOrBlank()) {
                ref.set(
                    mapOf(
                        "nameBn" to nameBn.trim(),
                        "nameEn" to nameEn.trim(),
                        "description" to description.trim(),
                        "iconName" to iconName,
                        "displayOrder" to displayOrder,
                        "questionCount" to 0,
                        "organizationId" to "default",
                        "status" to status,
                        "createdBy" to uid,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "nameBn" to nameBn.trim(),
                        "nameEn" to nameEn.trim(),
                        "description" to description.trim(),
                        "iconName" to iconName,
                        "displayOrder" to displayOrder,
                        "status" to status,
                        "updatedAt" to now
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> = try {
        requireUserId()
        db.collection("subjects").document(subjectId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "subjects/$subjectId")
        Result.failure(e)
    }

    suspend fun createSuggestedSubjectCategories(): Result<Int> {
        return try {
            val uid = requireUserId()
            val existing = db.collection("subjects").get().await()
            val existingNames = existing.documents.mapNotNull { it.getString("nameBn") }.toSet()
            val now = FieldValue.serverTimestamp()
            val batch = db.batch()
            var added = 0
            AppConstants.SUGGESTED_SUBJECT_TEMPLATES.forEachIndexed { index, tpl ->
                if (tpl.nameBn !in existingNames) {
                    val ref = db.collection("subjects").document()
                    batch.set(
                        ref,
                        mapOf(
                            "nameBn" to tpl.nameBn,
                            "nameEn" to tpl.nameEn,
                            "description" to "",
                            "iconName" to tpl.iconName,
                            "displayOrder" to index + 1,
                            "questionCount" to 0,
                            "organizationId" to "default",
                            "status" to "published",
                            "createdBy" to uid,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                    added++
                }
            }
            if (added > 0) batch.commit().await()
            Result.success(added)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "subjects")
            Result.failure(e)
        }
    }

    suspend fun saveChapter(
        chapterId: String?,
        subjectId: String,
        nameBn: String,
        nameEn: String,
        displayOrder: Int
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = if (chapterId.isNullOrBlank()) db.collection("chapters").document() else db.collection("chapters").document(chapterId)
        val now = FieldValue.serverTimestamp()
        if (chapterId.isNullOrBlank()) {
            ref.set(
                mapOf(
                    "subjectId" to subjectId,
                    "nameBn" to nameBn.trim(),
                    "nameEn" to nameEn.trim(),
                    "displayOrder" to displayOrder,
                    "organizationId" to "default",
                    "status" to "published",
                    "createdBy" to uid,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "subjectId" to subjectId,
                    "nameBn" to nameBn.trim(),
                    "nameEn" to nameEn.trim(),
                    "displayOrder" to displayOrder,
                    "updatedAt" to now
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "chapters")
        Result.failure(e)
    }

    suspend fun deleteChapter(chapterId: String): Result<Unit> = try {
        requireUserId()
        db.collection("chapters").document(chapterId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "chapters/$chapterId")
        Result.failure(e)
    }

    suspend fun saveTopic(subjectId: String, chapterId: String, nameBn: String, nameEn: String): Result<Unit> = try {
        val uid = requireUserId()
        val now = FieldValue.serverTimestamp()
        db.collection("topics").document().set(
            mapOf(
                "subjectId" to subjectId,
                "chapterId" to chapterId,
                "nameBn" to nameBn.trim(),
                "nameEn" to nameEn.trim(),
                "organizationId" to "default",
                "status" to "published",
                "createdBy" to uid,
                "createdAt" to now,
                "updatedAt" to now
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "topics")
        Result.failure(e)
    }

    suspend fun createQuestionSet(
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        chapterId: String,
        chapterName: String,
        questionIds: List<String>,
        language: String
    ): Result<String> = try {
        val uid = requireUserId()
        val setRef = db.collection("questionSets").document()
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        batch.set(
            setRef,
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "subjectId" to subjectId,
                "subjectName" to subjectName,
                "chapterId" to chapterId,
                "chapterName" to chapterName,
                "questionCount" to questionIds.size,
                "attemptCount" to 0,
                "language" to language,
                "organizationId" to "default",
                "status" to "published",
                "createdBy" to uid,
                "createdAt" to now,
                "updatedAt" to now
            )
        )
        for (qid in questionIds) {
            batch.update(
                db.collection("questions_public").document(qid),
                mapOf("questionSetId" to setRef.id, "updatedAt" to now)
            )
        }
        batch.commit().await()
        Result.success(setRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "questionSets")
        Result.failure(e)
    }

    // --- EXAMINATIONS ---

    suspend fun saveExamination(
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
    ): Result<Unit> = try {
        val uid = requireUserId()
        require(questionIds.isNotEmpty()) { "পরীক্ষায় অন্তত ১টি প্রশ্ন যুক্ত করুন।" }
        val ref = if (examId.isNullOrBlank()) db.collection("examinations").document() else db.collection("examinations").document(examId)
        val totalMarks = questionIds.size * marksPerQuestion
        val now = FieldValue.serverTimestamp()
        if (examId.isNullOrBlank()) {
            ref.set(
                mapOf(
                    "title" to title.trim(),
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "chapterId" to chapterId,
                    "questionSetId" to "",
                    "questionIds" to questionIds,
                    "totalQuestions" to questionIds.size,
                    "totalMarks" to totalMarks,
                    "marksPerQuestion" to marksPerQuestion,
                    "negativeMarking" to negativeMarking,
                    "timeLimitMinutes" to timeLimitMinutes,
                    "examMode" to examMode,
                    "randomQuestionOrder" to randomQuestionOrder,
                    "randomOptionOrder" to randomOptionOrder,
                    "startAtMillis" to System.currentTimeMillis(),
                    "endAtMillis" to (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000),
                    "organizationId" to "default",
                    "status" to status,
                    "createdBy" to uid,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "title" to title.trim(),
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "chapterId" to chapterId,
                    "questionIds" to questionIds,
                    "totalQuestions" to questionIds.size,
                    "totalMarks" to totalMarks,
                    "marksPerQuestion" to marksPerQuestion,
                    "negativeMarking" to negativeMarking,
                    "timeLimitMinutes" to timeLimitMinutes,
                    "examMode" to examMode,
                    "randomQuestionOrder" to randomQuestionOrder,
                    "randomOptionOrder" to randomOptionOrder,
                    "status" to status,
                    "updatedAt" to now
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "examinations")
        Result.failure(e)
    }

    suspend fun deleteExamination(examId: String): Result<Unit> = try {
        requireUserId()
        db.collection("examinations").document(examId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "examinations/$examId")
        Result.failure(e)
    }

    // --- SUPER ADMIN & NOTIFICATIONS ---

    suspend fun manageAdminRole(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        permissions: List<String>,
        revoke: Boolean
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            check(uid == AppConstants.SUPER_ADMIN_UID) { "Only Super Admin can manage Admin roles." }
            try {
                functions.getHttpsCallable("manageAdminRole").call(
                    hashMapOf(
                        "targetUid" to targetUid,
                        "email" to targetEmail,
                        "name" to targetName,
                        "permissions" to permissions,
                        "action" to if (revoke) "revoke" else "upsert"
                    )
                ).await()
            } catch (_: Exception) {
                // Direct Super Admin Firestore write guarded by isSuperAdmin() in firestore.rules
                val now = FieldValue.serverTimestamp()
                val batch = db.batch()
                val adminRef = db.collection("admins").document(targetUid)
                val adminSnap = adminRef.get().await()
                if (!adminSnap.exists()) {
                    batch.set(
                        adminRef,
                        mapOf(
                            "uid" to targetUid,
                            "email" to targetEmail,
                            "name" to targetName.ifBlank { "Admin" },
                            "permissions" to if (revoke) emptyList() else permissions,
                            "assignedBy" to uid,
                            "status" to if (revoke) "revoked" else "active",
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                } else {
                    batch.update(
                        adminRef,
                        mapOf(
                            "permissions" to if (revoke) emptyList() else permissions,
                            "assignedBy" to uid,
                            "status" to if (revoke) "revoked" else "active",
                            "updatedAt" to now
                        )
                    )
                }
                val userRef = db.collection("users").document(targetUid)
                if (userRef.get().await().exists()) {
                    batch.update(
                        userRef,
                        mapOf(
                            "role" to if (revoke) "student" else "admin",
                            "updatedAt" to now
                        )
                    )
                }
                batch.set(
                    db.collection("auditLogs").document(),
                    mapOf(
                        "actorUid" to uid,
                        "action" to if (revoke) "admin_revoke" else "admin_upsert",
                        "targetPath" to "admins/$targetUid",
                        "details" to permissions.joinToString(","),
                        "status" to "logged",
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admins/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun updateAppSettings(
        maintenanceMode: Boolean,
        allowRegistration: Boolean,
        defaultNegativeMarking: Double,
        announcementBanner: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == AppConstants.SUPER_ADMIN_UID) { "Super Admin only." }
        val ref = db.collection("appSettings").document("global")
        val snap = ref.get().await()
        val now = FieldValue.serverTimestamp()
        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "maintenanceMode" to maintenanceMode,
                    "allowRegistration" to allowRegistration,
                    "defaultNegativeMarking" to defaultNegativeMarking,
                    "announcementBanner" to announcementBanner.take(500),
                    "status" to "active",
                    "updatedBy" to uid,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "maintenanceMode" to maintenanceMode,
                    "allowRegistration" to allowRegistration,
                    "defaultNegativeMarking" to defaultNegativeMarking,
                    "announcementBanner" to announcementBanner.take(500),
                    "updatedBy" to uid,
                    "updatedAt" to now
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "appSettings/global")
        Result.failure(e)
    }

    suspend fun updateStudentStatus(targetUid: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("users").document(targetUid).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
        Result.failure(e)
    }

    suspend fun sendNotification(
        title: String,
        body: String,
        category: String,
        targetUid: String = "all"
    ): Result<Unit> = try {
        val uid = requireUserId()
        try {
            functions.getHttpsCallable("broadcastNotification").call(
                hashMapOf(
                    "title" to title.trim(),
                    "body" to body.trim(),
                    "category" to category,
                    "targetUid" to targetUid
                )
            ).await()
        } catch (_: Exception) {
            val now = FieldValue.serverTimestamp()
            db.collection("notifications").document().set(
                mapOf(
                    "targetUid" to targetUid,
                    "title" to title.trim().take(200),
                    "body" to body.trim().take(1500),
                    "category" to category,
                    "referenceId" to "",
                    "readByUids" to emptyList<String>(),
                    "status" to "active",
                    "createdBy" to uid,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "notifications")
        Result.failure(e)
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
            handleFirestoreError(firestoreError ?: error, op, path)
        }
        throw error
    }
}
