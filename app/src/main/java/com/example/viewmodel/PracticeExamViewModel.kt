package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExamReviewItem
import com.example.data.model.ExamSession
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.Examination
import com.example.data.model.PracticeCheckResult
import com.example.data.model.PracticeSession
import com.example.data.model.QuestionPublic
import com.example.data.repository.EducationRepository
import com.example.data.repository.PracticeAndExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivePracticeUiState(
    val isLoading: Boolean = false,
    val isActive: Boolean = false,
    val isOfflineSession: Boolean = false,
    val isWrongRetryMode: Boolean = false,
    val session: PracticeSession? = null,
    val questions: List<QuestionPublic> = emptyList(),
    val currentIndex: Int = 0,
    val pendingSelection: String? = null,
    val checkedResults: Map<String, PracticeCheckResult> = emptyMap(),
    val offlineSelections: Map<String, String> = emptyMap(),
    val allowChangeBeforeFinalize: Boolean = true,
    val isCheckingAnswer: Boolean = false,
    val errorMessage: String? = null,
    val finishedSummary: Boolean = false
)

data class ActiveExamUiState(
    val isLoading: Boolean = false,
    val isActive: Boolean = false,
    val exam: Examination? = null,
    val session: ExamSession? = null,
    val questions: List<QuestionPublic> = emptyList(),
    val optionOrders: Map<String, List<String>> = emptyMap(),
    val selectedAnswers: Map<String, String> = emptyMap(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val totalElapsedSeconds: Int = 0,
    val isSubmitting: Boolean = false,
    val submissionResult: ExamSubmissionResult? = null,
    val reviewItems: List<ExamReviewItem> = emptyList(),
    val errorMessage: String? = null
)

class PracticeExamViewModel(
    private val educationRepo: EducationRepository,
    private val practiceExamRepo: PracticeAndExamRepository
) : ViewModel() {

    private val _practiceState = MutableStateFlow(ActivePracticeUiState())
    val practiceState: StateFlow<ActivePracticeUiState> = _practiceState.asStateFlow()

    private val _examState = MutableStateFlow(ActiveExamUiState())
    val examState: StateFlow<ActiveExamUiState> = _examState.asStateFlow()

    private var timerJob: Job? = null

    // --- PRACTICE MODE ---

    fun startOnlinePractice(
        title: String,
        subjectId: String?,
        chapterId: String?,
        topicId: String?,
        questionSetId: String?,
        count: Int,
        allowChangeBeforeFinalize: Boolean = true
    ) {
        viewModelScope.launch {
            _practiceState.value = ActivePracticeUiState(isLoading = true)
            val res = educationRepo.fetchPublishedQuestionsForPractice(
                subjectId = subjectId,
                chapterId = chapterId,
                topicId = topicId,
                questionSetId = questionSetId,
                limit = count
            )
            if (res.isFailure) {
                _practiceState.value = ActivePracticeUiState(
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "প্রশ্ন লোড করতে ব্যর্থ হয়েছে।"
                )
                return@launch
            }
            val questions = res.getOrThrow()
            if (questions.isEmpty()) {
                _practiceState.value = ActivePracticeUiState(
                    errorMessage = "এখনো কোনো MCQ যোগ করা হয়নি।"
                )
                return@launch
            }
            val sessionRes = practiceExamRepo.createPracticeSession(
                title = title,
                subjectId = subjectId.orEmpty(),
                chapterId = chapterId.orEmpty(),
                topicId = topicId.orEmpty(),
                questionSetId = questionSetId.orEmpty(),
                questionIds = questions.map { it.id }
            )
            _practiceState.value = ActivePracticeUiState(
                isActive = true,
                session = sessionRes.getOrNull(),
                questions = questions,
                allowChangeBeforeFinalize = allowChangeBeforeFinalize
            )
        }
    }

    fun startWrongAnswerPractice(questionIds: List<String>) {
        viewModelScope.launch {
            _practiceState.value = ActivePracticeUiState(isLoading = true)
            val res = educationRepo.fetchQuestionsByIds(questionIds)
            if (res.isFailure || res.getOrNull().isNullOrEmpty()) {
                _practiceState.value = ActivePracticeUiState(
                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "ভুল উত্তর প্রশ্ন লোড হয়নি।"
                )
                return@launch
            }
            _practiceState.value = ActivePracticeUiState(
                isActive = true,
                isWrongRetryMode = true,
                questions = res.getOrThrow(),
                allowChangeBeforeFinalize = true
            )
        }
    }

    fun resumePracticeSession(session: PracticeSession) {
        viewModelScope.launch {
            _practiceState.value = ActivePracticeUiState(isLoading = true)
            val res = educationRepo.fetchQuestionsByIds(session.questionIds)
            if (res.isFailure || res.getOrNull().isNullOrEmpty()) {
                _practiceState.value = ActivePracticeUiState(
                    errorMessage = "সেশন পুনরুদ্ধার করা যায়নি।"
                )
                return@launch
            }
            _practiceState.value = ActivePracticeUiState(
                isActive = true,
                session = session,
                questions = res.getOrThrow(),
                currentIndex = session.currentIndex.coerceIn(0, res.getOrThrow().lastIndex)
            )
        }
    }

    fun startOfflinePracticeSet(setId: String) {
        viewModelScope.launch {
            _practiceState.value = ActivePracticeUiState(isLoading = true)
            val cached = educationRepo.getOfflineQuestionsForSet(setId)
            if (cached.isEmpty()) {
                _practiceState.value = ActivePracticeUiState(errorMessage = "অফলাইন সেটে কোনো প্রশ্ন নেই।")
                return@launch
            }
            _practiceState.value = ActivePracticeUiState(
                isActive = true,
                isOfflineSession = true,
                questions = cached,
                allowChangeBeforeFinalize = true
            )
        }
    }

    fun selectPracticeOption(optionKey: String) {
        val state = _practiceState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        if (state.checkedResults.containsKey(currentQ.id)) return

        if (state.allowChangeBeforeFinalize) {
            _practiceState.update { it.copy(pendingSelection = optionKey) }
        } else {
            _practiceState.update { it.copy(pendingSelection = optionKey) }
            finalizeCurrentPracticeAnswer()
        }
    }

    fun finalizeCurrentPracticeAnswer() {
        val state = _practiceState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val selected = state.pendingSelection ?: return
        if (state.checkedResults.containsKey(currentQ.id)) return

        if (state.isOfflineSession) {
            viewModelScope.launch {
                practiceExamRepo.queueOfflinePracticeAttempt(
                    questionId = currentQ.id,
                    questionSetId = currentQ.questionSetId,
                    selectedOption = selected
                )
                _practiceState.update {
                    it.copy(
                        offlineSelections = it.offlineSelections + (currentQ.id to selected),
                        pendingSelection = null
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _practiceState.update { it.copy(isCheckingAnswer = true, errorMessage = null) }
            val res = practiceExamRepo.checkPracticeAnswerOnline(
                questionId = currentQ.id,
                selectedOption = selected,
                sessionId = state.session?.id.orEmpty(),
                isWrongRetry = state.isWrongRetryMode
            )
            if (res.isSuccess) {
                val check = res.getOrThrow()
                _practiceState.update {
                    it.copy(
                        isCheckingAnswer = false,
                        checkedResults = it.checkedResults + (currentQ.id to check),
                        pendingSelection = selected
                    )
                }
            } else {
                _practiceState.update {
                    it.copy(
                        isCheckingAnswer = false,
                        errorMessage = res.exceptionOrNull()?.localizedMessage
                            ?: "উত্তর যাচাই ব্যর্থ হয়েছে (Cloud Function checkPracticeAnswer সক্রিয় আছে কিনা দেখুন)।"
                    )
                }
            }
        }
    }

    fun movePracticeIndex(newIndex: Int) {
        val state = _practiceState.value
        if (newIndex !in state.questions.indices) return
        val nextQ = state.questions[newIndex]
        _practiceState.update {
            it.copy(
                currentIndex = newIndex,
                pendingSelection = it.offlineSelections[nextQ.id]
            )
        }
        state.session?.id?.let { sid ->
            viewModelScope.launch {
                practiceExamRepo.updatePracticeProgress(sid, newIndex, completed = false)
            }
        }
    }

    fun finishPracticeSession() {
        val state = _practiceState.value
        state.session?.id?.let { sid ->
            viewModelScope.launch {
                practiceExamRepo.updatePracticeProgress(sid, state.currentIndex, completed = true)
            }
        }
        _practiceState.update { it.copy(finishedSummary = true) }
    }

    fun exitPractice() {
        _practiceState.value = ActivePracticeUiState()
    }

    // --- EXAMINATION MODE ---

    fun startExamination(exam: Examination) {
        timerJob?.cancel()
        viewModelScope.launch {
            _examState.value = ActiveExamUiState(isLoading = true, exam = exam)
            val qRes = educationRepo.fetchQuestionsByIds(exam.questionIds)
            if (qRes.isFailure) {
                _examState.value = ActiveExamUiState(
                    errorMessage = qRes.exceptionOrNull()?.localizedMessage ?: "পরীক্ষার প্রশ্ন লোড করতে ব্যর্থ হয়েছে।"
                )
                return@launch
            }
            val fetched = qRes.getOrThrow()
            if (fetched.isEmpty()) {
                _examState.value = ActiveExamUiState(errorMessage = "এই পরীক্ষায় এখনো কোনো প্রকাশিত প্রশ্ন নেই।")
                return@launch
            }
            val orderedQuestions = if (exam.randomQuestionOrder) fetched.shuffled() else fetched
            val optionOrders = orderedQuestions.associate { q ->
                val base = listOf("A", "B", "C", "D")
                q.id to if (exam.randomOptionOrder) base.shuffled() else base
            }
            val sessionRes = practiceExamRepo.startExamSession(exam.id)
            if (sessionRes.isFailure) {
                _examState.value = ActiveExamUiState(
                    errorMessage = sessionRes.exceptionOrNull()?.localizedMessage ?: "পরীক্ষার সেশন শুরু করা যায়নি।"
                )
                return@launch
            }
            val totalSecs = if (exam.examMode == "untimed" || exam.timeLimitMinutes <= 0) 0 else exam.timeLimitMinutes * 60
            _examState.value = ActiveExamUiState(
                isActive = true,
                exam = exam,
                session = sessionRes.getOrThrow(),
                questions = orderedQuestions,
                optionOrders = optionOrders,
                remainingSeconds = totalSecs
            )
            startExamTimer(timed = totalSecs > 0)
        }
    }

    private fun startExamTimer(timed: Boolean) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_examState.value.isActive && _examState.value.submissionResult == null) {
                delay(1000L)
                val current = _examState.value
                val nextElapsed = current.totalElapsedSeconds + 1
                if (timed) {
                    val nextRem = (current.remainingSeconds - 1).coerceAtLeast(0)
                    _examState.update {
                        it.copy(remainingSeconds = nextRem, totalElapsedSeconds = nextElapsed)
                    }
                    if (nextRem == 0) {
                        submitExamination()
                        break
                    }
                } else {
                    _examState.update { it.copy(totalElapsedSeconds = nextElapsed) }
                }
            }
        }
    }

    fun selectExamAnswer(questionId: String, optionKey: String) {
        val state = _examState.value
        if (!state.isActive || state.isSubmitting) return
        val updated = state.selectedAnswers + (questionId to optionKey)
        _examState.update { it.copy(selectedAnswers = updated) }
        state.session?.id?.let { sid ->
            viewModelScope.launch {
                practiceExamRepo.saveExamSessionDraftAnswers(sid, updated)
            }
        }
    }

    fun navigateExamQuestion(index: Int) {
        val state = _examState.value
        if (index in state.questions.indices) {
            _examState.update { it.copy(currentIndex = index) }
        }
    }

    fun submitExamination() {
        val state = _examState.value
        val exam = state.exam ?: return
        val session = state.session ?: return
        if (state.isSubmitting) return

        timerJob?.cancel()
        viewModelScope.launch {
            _examState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val res = practiceExamRepo.submitExaminationToServer(
                examId = exam.id,
                sessionId = session.id,
                answers = state.selectedAnswers,
                timeTakenSeconds = state.totalElapsedSeconds
            )
            if (res.isSuccess) {
                val result = res.getOrThrow()
                _examState.update {
                    it.copy(
                        isSubmitting = false,
                        isActive = false,
                        submissionResult = result,
                        reviewItems = result.reviewItems
                    )
                }
            } else {
                _examState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = res.exceptionOrNull()?.localizedMessage
                            ?: "পরীক্ষা জমা দিতে ব্যর্থ হয়েছে। ইন্টারনেট এবং Cloud Function যাচাই করুন।"
                    )
                }
            }
        }
    }

    fun loadSavedResultReview(resultId: String) {
        viewModelScope.launch {
            _examState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = practiceExamRepo.fetchExamReview(resultId)
            _examState.update {
                if (res.isSuccess) {
                    it.copy(isLoading = false, reviewItems = res.getOrThrow())
                } else {
                    it.copy(
                        isLoading = false,
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "রিভিউ লোড করা যায়নি।"
                    )
                }
            }
        }
    }

    fun exitExam() {
        timerJob?.cancel()
        _examState.value = ActiveExamUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
