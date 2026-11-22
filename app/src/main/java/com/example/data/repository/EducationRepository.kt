package com.example.data.repository

import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.CachedQuestionSetEntity
import com.example.data.local.CachedSubjectEntity
import com.example.data.local.McqDao
import com.example.data.model.Chapter
import com.example.data.model.QuestionPublic
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.utils.AppPreferences
import com.example.utils.OperationType
import com.example.utils.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EducationRepository(
    private val db: FirebaseFirestore,
    private val dao: McqDao,
    private val preferences: AppPreferences,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun observePublishedSubjects(): Flow<List<Subject>> =
        db.collection("subjects")
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, "subjects")
            }

    fun observeCachedSubjects(): Flow<List<Subject>> =
        dao.observeSubjects().map { list ->
            list.map {
                Subject(
                    id = it.id,
                    nameBn = it.nameBn,
                    nameEn = it.nameEn,
                    description = it.description,
                    iconName = it.iconName,
                    displayOrder = it.displayOrder,
                    questionCount = it.questionCount,
                    status = "published"
                )
            }
        }

    suspend fun cacheSubjectsLocally(subjects: List<Subject>) {
        val now = System.currentTimeMillis()
        dao.insertSubjects(
            subjects.map {
                CachedSubjectEntity(
                    id = it.id,
                    nameBn = it.nameBn,
                    nameEn = it.nameEn,
                    description = it.description,
                    iconName = it.iconName,
                    displayOrder = it.displayOrder,
                    questionCount = it.questionCount,
                    updatedAtMillis = now
                )
            }
        )
    }

    fun observePublishedChapters(subjectId: String): Flow<List<Chapter>> =
        db.collection("chapters")
            .whereEqualTo("subjectId", subjectId)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Chapter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, "chapters")
            }

    fun observePublishedTopics(chapterId: String): Flow<List<Topic>> =
        db.collection("topics")
            .whereEqualTo("chapterId", chapterId)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Topic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, "topics")
            }

    fun observePublishedQuestionSets(limit: Long = 20): Flow<List<QuestionSet>> =
        db.collection("questionSets")
            .whereEqualTo("status", "published")
            .limit(limit)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuestionSet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                handleStreamError(error, OperationType.LIST, "questionSets")
            }

    fun observeDownloadedSets(): Flow<List<CachedQuestionSetEntity>> = dao.observeDownloadedSets()

    suspend fun fetchPublishedQuestionsForPractice(
        subjectId: String? = null,
        chapterId: String? = null,
        topicId: String? = null,
        questionSetId: String? = null,
        limit: Int = 25
    ): Result<List<QuestionPublic>> {
        val path = "questions_public"
        return try {
            var query: Query = db.collection(path).whereEqualTo("status", "published")
            if (!questionSetId.isNullOrBlank()) {
                query = query.whereEqualTo("questionSetId", questionSetId)
            } else {
                if (!subjectId.isNullOrBlank()) query = query.whereEqualTo("subjectId", subjectId)
                if (!chapterId.isNullOrBlank()) query = query.whereEqualTo("chapterId", chapterId)
                if (!topicId.isNullOrBlank()) query = query.whereEqualTo("topicId", topicId)
            }
            val snapshot = query.limit(limit.toLong()).get().await()
            val items = snapshot.toObjects(
                QuestionPublic::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchQuestionsByIds(ids: List<String>): Result<List<QuestionPublic>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val allQuestions = mutableListOf<QuestionPublic>()
            for (chunk in ids.chunked(10)) {
                val snap = db.collection("questions_public")
                    .whereEqualTo("status", "published")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                allQuestions.addAll(
                    snap.toObjects(QuestionPublic::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            val mapById = allQuestions.associateBy { it.id }
            Result.success(ids.mapNotNull { mapById[it] })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "questions_public")
            Result.failure(e)
        }
    }

    suspend fun searchPublishedQuestions(
        queryText: String,
        subjectIdFilter: String? = null,
        difficultyFilter: String? = null,
        languageFilter: String? = null,
        limit: Int = 40
    ): Result<List<QuestionPublic>> {
        val trimmed = queryText.trim().lowercase()
        val path = "questions_public"
        return try {
            val token = trimmed.split(Regex("\\s+")).firstOrNull { it.length >= 2 }
            var query: Query = db.collection(path).whereEqualTo("status", "published")
            if (!subjectIdFilter.isNullOrBlank()) {
                query = query.whereEqualTo("subjectId", subjectIdFilter)
            }
            if (token != null) {
                query = query.whereArrayContains("searchTokens", token)
            }
            val snapshot = query.limit(80).get().await()
            val list = snapshot.toObjects(
                QuestionPublic::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { q ->
                val matchesText = trimmed.isBlank() ||
                    q.questionText.lowercase().contains(trimmed) ||
                    q.subjectName.lowercase().contains(trimmed) ||
                    q.chapterName.lowercase().contains(trimmed) ||
                    q.topicName.lowercase().contains(trimmed) ||
                    q.source.lowercase().contains(trimmed) ||
                    q.questionNumber.toString() == trimmed
                val matchesDiff = difficultyFilter.isNullOrBlank() || q.difficulty == difficultyFilter
                val matchesLang = languageFilter.isNullOrBlank() || q.language == languageFilter
                matchesText && matchesDiff && matchesLang
            }.take(limit)
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun downloadQuestionSetOffline(questionSet: QuestionSet): Result<Int> {
        return try {
            val questionsRes = fetchPublishedQuestionsForPractice(
                questionSetId = questionSet.id,
                subjectId = questionSet.subjectId.takeIf { questionSet.id.isBlank() },
                limit = 200
            )
            val questions = questionsRes.getOrThrow()
            dao.insertQuestionSet(
                CachedQuestionSetEntity(
                    id = questionSet.id,
                    title = questionSet.title,
                    description = questionSet.description,
                    subjectId = questionSet.subjectId,
                    subjectName = questionSet.subjectName,
                    chapterId = questionSet.chapterId,
                    chapterName = questionSet.chapterName,
                    questionCount = questions.size,
                    language = questionSet.language,
                    downloadedAtMillis = System.currentTimeMillis()
                )
            )
            dao.insertQuestions(
                questions.map {
                    CachedQuestionEntity(
                        id = it.id,
                        questionSetId = questionSet.id,
                        subjectId = it.subjectId,
                        subjectName = it.subjectName,
                        chapterId = it.chapterId,
                        chapterName = it.chapterName,
                        questionText = it.questionText,
                        optionA = it.optionA,
                        optionB = it.optionB,
                        optionC = it.optionC,
                        optionD = it.optionD,
                        difficulty = it.difficulty,
                        language = it.language,
                        questionNumber = it.questionNumber,
                        hasMath = it.hasMath
                    )
                }
            )
            preferences.markSetDownloaded(questionSet.id)
            Result.success(questions.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "questions_public")
            Result.failure(e)
        }
    }

    suspend fun getOfflineQuestionsForSet(setId: String): List<QuestionPublic> =
        dao.getQuestionsForSet(setId).map {
            QuestionPublic(
                id = it.id,
                questionSetId = it.questionSetId,
                subjectId = it.subjectId,
                subjectName = it.subjectName,
                chapterId = it.chapterId,
                chapterName = it.chapterName,
                questionText = it.questionText,
                optionA = it.optionA,
                optionB = it.optionB,
                optionC = it.optionC,
                optionD = it.optionD,
                difficulty = it.difficulty,
                language = it.language,
                questionNumber = it.questionNumber,
                hasMath = it.hasMath,
                status = "published"
            )
        }

    suspend fun removeOfflineQuestionSet(setId: String) {
        dao.deleteQuestionsForSet(setId)
        dao.deleteQuestionSet(setId)
        preferences.removeDownloadedSet(setId)
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
