package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val role: String = "student",
    val preferredLanguage: String = "bn",
    val fcmToken: String = "",
    val totalExams: Long = 0L,
    val totalAttempted: Long = 0L,
    val totalCorrect: Long = 0L,
    val totalIncorrect: Long = 0L,
    val bestScore: Double = 0.0,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val permissions: List<String> = emptyList(),
    val assignedBy: String = "",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val description: String = "",
    val iconName: String = "menu_book",
    val displayOrder: Int = 0,
    val questionCount: Int = 0,
    val organizationId: String = "default",
    val status: String = "published",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayName(isBn: Boolean): String =
        if (isBn) nameBn.ifBlank { nameEn } else nameEn.ifBlank { nameBn }
}

@Keep
@IgnoreExtraProperties
data class Chapter(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val displayOrder: Int = 0,
    val organizationId: String = "default",
    val status: String = "published",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayName(isBn: Boolean): String =
        if (isBn) nameBn.ifBlank { nameEn } else nameEn.ifBlank { nameBn }
}

@Keep
@IgnoreExtraProperties
data class Topic(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val organizationId: String = "default",
    val status: String = "published",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PdfDocument(
    @DocumentId val id: String = "",
    val fileName: String = "",
    val storagePath: String = "",
    val fileUrl: String = "",
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 0,
    val fileType: String = "application/pdf",
    val processingStatus: String = "uploaded",
    val extractedCount: Int = 0,
    val subjectId: String = "",
    val chapterId: String = "",
    val organizationId: String = "default",
    val status: String = "active",
    val uploadedBy: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PdfProcessingJob(
    @DocumentId val id: String = "",
    val pdfId: String = "",
    val pdfFileName: String = "",
    val mode: String = "hybrid",
    val processedPages: Int = 0,
    val totalPages: Int = 0,
    val extractedQuestionsCount: Int = 0,
    val unverifiedCount: Int = 0,
    val errorMessage: String = "",
    val status: String = "queued",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionPublic(
    @DocumentId val id: String = "",
    val questionText: String = "",
    val originalPdfText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val questionSetId: String = "",
    val difficulty: String = "medium",
    val language: String = "bn",
    val source: String = "",
    val sourcePdfId: String = "",
    val pageNumber: Int = 0,
    val questionNumber: Int = 0,
    val confidence: Double = 1.0,
    val verified: Boolean = true,
    val needsReview: Boolean = false,
    val hasMath: Boolean = false,
    val searchTokens: List<String> = emptyList(),
    val organizationId: String = "default",
    val status: String = "unpublished",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionAnswerPrivate(
    @DocumentId val id: String = "",
    val questionId: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val verified: Boolean = false,
    val confidence: Double = 0.0,
    val status: String = "unverified",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AdminQuestionWithAnswer(
    val publicQuestion: QuestionPublic,
    val privateAnswer: QuestionAnswerPrivate?
)

@Keep
@IgnoreExtraProperties
data class QuestionSet(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val topicId: String = "",
    val questionCount: Int = 0,
    val attemptCount: Int = 0,
    val language: String = "bn",
    val sourcePdfId: String = "",
    val organizationId: String = "default",
    val status: String = "published",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Examination(
    @DocumentId val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val questionSetId: String = "",
    val questionIds: List<String> = emptyList(),
    val totalQuestions: Int = 0,
    val totalMarks: Double = 0.0,
    val marksPerQuestion: Double = 1.0,
    val negativeMarking: Double = 0.0,
    val timeLimitMinutes: Int = 30,
    val examMode: String = "timed",
    val randomQuestionOrder: Boolean = false,
    val randomOptionOrder: Boolean = false,
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L,
    val organizationId: String = "default",
    val status: String = "published",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamSession(
    @DocumentId val id: String = "",
    val examId: String = "",
    val userId: String = "",
    val answers: Map<String, String> = emptyMap(),
    val startedAtMillis: Long = 0L,
    val status: String = "in_progress",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExaminationResult(
    @DocumentId val id: String = "",
    val examId: String = "",
    val examTitle: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val totalQuestions: Int = 0,
    val answeredQuestions: Int = 0,
    val unansweredQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val totalMarks: Double = 0.0,
    val obtainedMarks: Double = 0.0,
    val negativeMarks: Double = 0.0,
    val percentage: Double = 0.0,
    val accuracy: Double = 0.0,
    val timeTakenSeconds: Int = 0,
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PracticeSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val chapterId: String = "",
    val topicId: String = "",
    val questionSetId: String = "",
    val title: String = "",
    val questionIds: List<String> = emptyList(),
    val selectedAnswers: Map<String, String> = emptyMap(),
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val status: String = "in_progress",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val dateKey: String = "",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WrongAnswer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val questionText: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val selectedOption: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val mistakeCount: Int = 1,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Bookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val questionText: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val collectionName: String = "Saved Questions",
    val reviewLater: Boolean = false,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val targetUid: String = "all",
    val title: String = "",
    val body: String = "",
    val category: String = "announcement",
    val referenceId: String = "",
    val readByUids: List<String> = emptyList(),
    val status: String = "active",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val maintenanceMode: Boolean = false,
    val allowRegistration: Boolean = true,
    val defaultNegativeMarking: Double = 0.5,
    val announcementBanner: String = "",
    val status: String = "active",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val action: String = "",
    val targetPath: String = "",
    val details: String = "",
    val status: String = "logged",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object SearchTokenizer {
    fun buildTokens(vararg fields: String): List<String> {
        return fields
            .flatMap { text ->
                text.lowercase()
                    .split(Regex("[\\s,.;:?!()\\[\\]{}/\\\\\"'<>।]+"))
            }
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(40)
    }

    fun serverTimestamp(): Any = FieldValue.serverTimestamp()
}
