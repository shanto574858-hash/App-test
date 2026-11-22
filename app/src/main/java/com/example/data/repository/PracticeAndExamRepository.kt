package com.example.data.repository

import com.example.data.local.McqDao
import com.example.data.local.PendingPracticeSyncEntity
import com.example.data.model.AppNotification
import com.example.data.model.Bookmark
import com.example.data.model.ExamReviewItem
import com.example.data.model.ExamSession
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.PracticeCheckResult
import com.example.data.model.PracticeSession
import com.example.data.model.QuestionPublic
import com.example.data.model.StudentProgress
import com.example.data.model.WrongAnswer
import com.example.utils.OperationType
import com.example.utils.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PracticeAndExamRepository(
    private val db: FirebaseFirestore,
    private val dao: McqDao,
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")

    fun observePublishedExaminations(): Flow<List<Examination>> =
        db.collection("examinations")
            .whereIn("status", listOf("published", "closed"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Examination::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "examinations") }

    fun observeMyResults(uid: String): Flow<List<ExaminationResult>> =
        db.collection("examinationResults")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ExaminationResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "examinationResults") }

    fun observeMyPracticeSessions(uid: String): Flow<List<PracticeSession>> =
        db.collection("practiceSessions")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PracticeSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "practiceSessions") }

    fun observeMyProgress(uid: String): Flow<List<StudentProgress>> =
        db.collection("studentProgress")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudentProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { handleStreamError(it, OperationType.LIST, "studentProgress") }

    fun observeMyWrongAnswers(uid: String): Flow<List<WrongAnswer>> =
        db.collection("wrongAnswers")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WrongAnswer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "wrongAnswers") }

    fun observeMyBookmarks(uid: String): Flow<List<Bookmark>> =
        db.collection("bookmarks")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Bookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "bookmarks") }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereIn("targetUid", listOf("all", uid))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status == "active" }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { handleStreamError(it, OperationType.LIST, "notifications") }

    fun observePendingOfflineSyncCount(uid: String): Flow<Int> =
        dao.observePendingSync(uid).map { it.size }

    suspend fun createPracticeSession(
        title: String,
        subjectId: String,
        chapterId: String,
        topicId: String,
        questionSetId: String,
        questionIds: List<String>
    ): Result<PracticeSession> {
        val path = "practiceSessions"
        return try {
            val uid = requireUserId()
            val ref = db.collection(path).document()
            val payload = mapOf(
                "userId" to uid,
                "subjectId" to subjectId,
                "chapterId" to chapterId,
                "topicId" to topicId,
                "questionSetId" to questionSetId,
                "title" to title.take(200),
                "questionIds" to questionIds.take(300),
                "selectedAnswers" to emptyMap<String, String>(),
                "currentIndex" to 0,
                "correctCount" to 0,
                "incorrectCount" to 0,
                "status" to "in_progress",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            val snap = ref.get().await()
            val session = snap.toObject(PracticeSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: PracticeSession(id = ref.id, userId = uid, title = title, questionIds = questionIds)
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePracticeProgress(
        sessionId: String,
        currentIndex: Int,
        completed: Boolean = false
    ): Result<Unit> {
        val path = "practiceSessions/$sessionId"
        return try {
            requireUserId()
            db.collection("practiceSessions").document(sessionId).update(
                mapOf(
                    "currentIndex" to currentIndex.coerceAtLeast(0),
                    "status" to if (completed) "completed" else "in_progress",
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

    suspend fun checkPracticeAnswerOnline(
        questionId: String,
        selectedOption: String,
        sessionId: String = "",
        isWrongRetry: Boolean = false
    ): Result<PracticeCheckResult> {
        return try {
            requireUserId()
            val data = hashMapOf(
                "questionId" to questionId,
                "selectedOption" to selectedOption.uppercase(),
                "sessionId" to sessionId,
                "isWrongRetry" to isWrongRetry
            )
            val result = functions
                .getHttpsCallable("checkPracticeAnswer")
                .call(data)
                .await()
            val map = result.getData() as? Map<*, *> ?: emptyMap<String, Any>()
            Result.success(
                PracticeCheckResult(
                    questionId = map["questionId"]?.toString() ?: questionId,
                    isCorrect = map["isCorrect"] as? Boolean ?: false,
                    correctAnswer = map["correctAnswer"]?.toString().orEmpty(),
                    explanation = map["explanation"]?.toString().orEmpty()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queueOfflinePracticeAttempt(
        questionId: String,
        questionSetId: String,
        selectedOption: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            dao.insertPendingSync(
                PendingPracticeSyncEntity(
                    userId = uid,
                    questionId = questionId,
                    questionSetId = questionSetId,
                    selectedOption = selectedOption.uppercase(),
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingOfflinePractice(): Result<Int> {
        return try {
            val uid = requireUserId()
            val pending = dao.getPendingSyncOnce(uid)
            var synced = 0
            for (item in pending) {
                val res = checkPracticeAnswerOnline(
                    questionId = item.questionId,
                    selectedOption = item.selectedOption
                )
                if (res.isSuccess) {
                    dao.deletePendingSync(item.localId)
                    synced++
                }
            }
            Result.success(synced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startExamSession(examId: String): Result<ExamSession> {
        val path = "examSessions"
        return try {
            val uid = requireUserId()
            val ref = db.collection(path).document()
            val startedAt = System.currentTimeMillis()
            val payload = mapOf(
                "examId" to examId,
                "userId" to uid,
                "answers" to emptyMap<String, String>(),
                "startedAtMillis" to startedAt,
                "status" to "in_progress",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(
                ExamSession(
                    id = ref.id,
                    examId = examId,
                    userId = uid,
                    startedAtMillis = startedAt,
                    status = "in_progress"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveExamSessionDraftAnswers(
        sessionId: String,
        answers: Map<String, String>
    ): Result<Unit> {
        val path = "examSessions/$sessionId"
        return try {
            requireUserId()
            db.collection("examSessions").document(sessionId).update(
                mapOf(
                    "answers" to answers,
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

    suspend fun submitExaminationToServer(
        examId: String,
        sessionId: String,
        answers: Map<String, String>,
        timeTakenSeconds: Int
    ): Result<ExamSubmissionResult> {
        return try {
            requireUserId()
            val payload = hashMapOf(
                "examId" to examId,
                "sessionId" to sessionId,
                "answers" to answers,
                "timeTakenSeconds" to timeTakenSeconds
            )
            val response = functions
                .getHttpsCallable("submitExamination")
                .call(payload)
                .await()
            val map = response.getData() as? Map<*, *> ?: emptyMap<String, Any>()
            val rawReview = (map["reviewItems"] as? List<*>)?.mapNotNull { entry ->
                val item = entry as? Map<*, *> ?: return@mapNotNull null
                ExamReviewItem(
                    questionId = item["questionId"]?.toString().orEmpty(),
                    questionText = item["questionText"]?.toString().orEmpty(),
                    optionA = item["optionA"]?.toString().orEmpty(),
                    optionB = item["optionB"]?.toString().orEmpty(),
                    optionC = item["optionC"]?.toString().orEmpty(),
                    optionD = item["optionD"]?.toString().orEmpty(),
                    selectedOption = item["selectedOption"]?.toString().orEmpty(),
                    correctAnswer = item["correctAnswer"]?.toString().orEmpty(),
                    explanation = item["explanation"]?.toString().orEmpty(),
                    isCorrect = item["isCorrect"] as? Boolean ?: false
                )
            } ?: emptyList()

            Result.success(
                ExamSubmissionResult(
                    resultId = map["resultId"]?.toString().orEmpty(),
                    examId = map["examId"]?.toString() ?: examId,
                    examTitle = map["examTitle"]?.toString().orEmpty(),
                    totalQuestions = (map["totalQuestions"] as? Number)?.toInt() ?: 0,
                    answeredQuestions = (map["answeredQuestions"] as? Number)?.toInt() ?: 0,
                    unansweredQuestions = (map["unansweredQuestions"] as? Number)?.toInt() ?: 0,
                    correctAnswers = (map["correctAnswers"] as? Number)?.toInt() ?: 0,
                    incorrectAnswers = (map["incorrectAnswers"] as? Number)?.toInt() ?: 0,
                    totalMarks = (map["totalMarks"] as? Number)?.toDouble() ?: 0.0,
                    obtainedMarks = (map["obtainedMarks"] as? Number)?.toDouble() ?: 0.0,
                    negativeMarks = (map["negativeMarks"] as? Number)?.toDouble() ?: 0.0,
                    percentage = (map["percentage"] as? Number)?.toDouble() ?: 0.0,
                    accuracy = (map["accuracy"] as? Number)?.toDouble() ?: 0.0,
                    timeTakenSeconds = (map["timeTakenSeconds"] as? Number)?.toInt() ?: timeTakenSeconds,
                    reviewItems = rawReview
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchExamReview(resultId: String): Result<List<ExamReviewItem>> {
        return try {
            requireUserId()
            val res = functions.getHttpsCallable("getExamReview")
                .call(hashMapOf("resultId" to resultId))
                .await()
            val map = res.getData() as? Map<*, *> ?: emptyMap<String, Any>()
            val items = (map["reviewItems"] as? List<*>)?.mapNotNull { entry ->
                val item = entry as? Map<*, *> ?: return@mapNotNull null
                ExamReviewItem(
                    questionId = item["questionId"]?.toString().orEmpty(),
                    questionText = item["questionText"]?.toString().orEmpty(),
                    optionA = item["optionA"]?.toString().orEmpty(),
                    optionB = item["optionB"]?.toString().orEmpty(),
                    optionC = item["optionC"]?.toString().orEmpty(),
                    optionD = item["optionD"]?.toString().orEmpty(),
                    selectedOption = item["selectedOption"]?.toString().orEmpty(),
                    correctAnswer = item["correctAnswer"]?.toString().orEmpty(),
                    explanation = item["explanation"]?.toString().orEmpty(),
                    isCorrect = item["isCorrect"] as? Boolean ?: false
                )
            } ?: emptyList()
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(
        question: QuestionPublic,
        collectionName: String = "Saved Questions",
        reviewLater: Boolean = false
    ): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "${uid}_${question.id}"
        val path = "bookmarks/$docId"
        return try {
            val docRef = db.collection("bookmarks").document(docId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.delete().await()
                Result.success(false)
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "questionId" to question.id,
                        "questionText" to question.questionText.take(4000),
                        "subjectId" to question.subjectId,
                        "subjectName" to question.subjectName,
                        "chapterId" to question.chapterId,
                        "collectionName" to collectionName.ifBlank { "Saved Questions" }.take(80),
                        "reviewLater" to reviewLater,
                        "status" to "active",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookmarkCollection(
        bookmarkId: String,
        collectionName: String,
        reviewLater: Boolean
    ): Result<Unit> {
        val path = "bookmarks/$bookmarkId"
        return try {
            requireUserId()
            db.collection("bookmarks").document(bookmarkId).update(
                mapOf(
                    "collectionName" to collectionName.take(80),
                    "reviewLater" to reviewLater,
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

    suspend fun removeWrongAnswer(wrongAnswerId: String): Result<Unit> {
        val path = "wrongAnswers/$wrongAnswerId"
        return try {
            requireUserId()
            db.collection("wrongAnswers").document(wrongAnswerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            val uid = requireUserId()
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "readByUids" to FieldValue.arrayUnion(uid),
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
