package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_subjects")
data class CachedSubjectEntity(
    @PrimaryKey val id: String,
    val nameBn: String,
    val nameEn: String,
    val description: String,
    val iconName: String,
    val displayOrder: Int,
    val questionCount: Int,
    val updatedAtMillis: Long
)

@Entity(tableName = "cached_chapters")
data class CachedChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val nameBn: String,
    val nameEn: String,
    val displayOrder: Int,
    val updatedAtMillis: Long
)

@Entity(tableName = "cached_question_sets")
data class CachedQuestionSetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterName: String,
    val questionCount: Int,
    val language: String,
    val downloadedAtMillis: Long
)

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val questionSetId: String,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterName: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val difficulty: String,
    val language: String,
    val questionNumber: Int,
    val hasMath: Boolean
)

@Entity(tableName = "pending_practice_sync")
data class PendingPracticeSyncEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val userId: String,
    val questionId: String,
    val questionSetId: String,
    val selectedOption: String,
    val createdAtMillis: Long
)
