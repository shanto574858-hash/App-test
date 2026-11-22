package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface McqDao {
    @Query("SELECT * FROM cached_subjects ORDER BY displayOrder ASC")
    fun observeSubjects(): Flow<List<CachedSubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<CachedSubjectEntity>)

    @Query("SELECT * FROM cached_chapters WHERE subjectId = :subjectId ORDER BY displayOrder ASC")
    fun observeChapters(subjectId: String): Flow<List<CachedChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<CachedChapterEntity>)

    @Query("SELECT * FROM cached_question_sets ORDER BY downloadedAtMillis DESC")
    fun observeDownloadedSets(): Flow<List<CachedQuestionSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionSet(questionSet: CachedQuestionSetEntity)

    @Query("DELETE FROM cached_question_sets WHERE id = :setId")
    suspend fun deleteQuestionSet(setId: String)

    @Query("SELECT * FROM cached_questions WHERE questionSetId = :setId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForSet(setId: String): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_questions WHERE subjectId = :subjectId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForSubject(subjectId: String): List<CachedQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE questionSetId = :setId")
    suspend fun deleteQuestionsForSet(setId: String)

    @Query("SELECT * FROM pending_practice_sync WHERE userId = :userId ORDER BY createdAtMillis ASC")
    fun observePendingSync(userId: String): Flow<List<PendingPracticeSyncEntity>>

    @Query("SELECT * FROM pending_practice_sync WHERE userId = :userId ORDER BY createdAtMillis ASC")
    suspend fun getPendingSyncOnce(userId: String): List<PendingPracticeSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSync(entry: PendingPracticeSyncEntity)

    @Query("DELETE FROM pending_practice_sync WHERE localId = :localId")
    suspend fun deletePendingSync(localId: Long)
}
