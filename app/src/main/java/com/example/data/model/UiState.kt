package com.example.data.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class PracticeCheckResult(
    val questionId: String = "",
    val isCorrect: Boolean = false,
    val correctAnswer: String = "",
    val explanation: String = ""
)

data class ExamReviewItem(
    val questionId: String = "",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val selectedOption: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val isCorrect: Boolean = false
)

data class ExamSubmissionResult(
    val resultId: String = "",
    val examId: String = "",
    val examTitle: String = "",
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
    val reviewItems: List<ExamReviewItem> = emptyList()
)

enum class UserRole(val key: String) {
    STUDENT("student"),
    ADMIN("admin"),
    SUPER_ADMIN("super_admin");

    companion object {
        fun fromKey(raw: String?): UserRole =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: STUDENT
    }
}

data class UserSessionRole(
    val uid: String = "",
    val role: UserRole = UserRole.STUDENT,
    val permissions: Set<String> = emptySet()
) {
    val isSuperAdmin: Boolean get() = role == UserRole.SUPER_ADMIN
    val isAdmin: Boolean get() = role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN
    fun hasPermission(permission: String): Boolean = isSuperAdmin || (role == UserRole.ADMIN && permission in permissions)
}
