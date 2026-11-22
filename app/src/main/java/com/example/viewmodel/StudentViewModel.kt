package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedQuestionSetEntity
import com.example.data.model.AppNotification
import com.example.data.model.Bookmark
import com.example.data.model.Chapter
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.PracticeSession
import com.example.data.model.QuestionPublic
import com.example.data.model.QuestionSet
import com.example.data.model.StudentProgress
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UiState
import com.example.data.model.WrongAnswer
import com.example.data.repository.EducationRepository
import com.example.data.repository.PracticeAndExamRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudentViewModel(
    private val educationRepo: EducationRepository,
    private val practiceExamRepo: PracticeAndExamRepository,
    private val currentUserId: String
) : ViewModel() {

    val subjectsState: StateFlow<UiState<List<Subject>>> =
        educationRepo.observePublishedSubjects()
            .onEach { list ->
                if (list.isNotEmpty()) educationRepo.cacheSubjectsLocally(list)
            }
            .toUiStateFlow("বিষয় লোড করতে ব্যর্থ হয়েছে")

    val cachedSubjects: StateFlow<List<Subject>> =
        educationRepo.observeCachedSubjects()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    private val _selectedChapterId = MutableStateFlow<String?>(null)
    val selectedChapterId: StateFlow<String?> = _selectedChapterId.asStateFlow()

    val chaptersForSelectedSubject: StateFlow<List<Chapter>> =
        _selectedSubjectId.flatMapLatest { subId ->
            if (subId.isNullOrBlank()) flowOf(emptyList())
            else educationRepo.observePublishedChapters(subId).catch {
                if (it is CancellationException) throw it
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topicsForSelectedChapter: StateFlow<List<Topic>> =
        _selectedChapterId.flatMapLatest { chapId ->
            if (chapId.isNullOrBlank()) flowOf(emptyList())
            else educationRepo.observePublishedTopics(chapId).catch {
                if (it is CancellationException) throw it
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionSetsState: StateFlow<UiState<List<QuestionSet>>> =
        educationRepo.observePublishedQuestionSets(30)
            .toUiStateFlow("প্রশ্ন সেট লোড করতে ব্যর্থ হয়েছে")

    val downloadedSets: StateFlow<List<CachedQuestionSetEntity>> =
        educationRepo.observeDownloadedSets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examinationsState: StateFlow<UiState<List<Examination>>> =
        practiceExamRepo.observePublishedExaminations()
            .toUiStateFlow("পরীক্ষা লোড করতে ব্যর্থ হয়েছে")

    val resultsState: StateFlow<UiState<List<ExaminationResult>>> =
        practiceExamRepo.observeMyResults(currentUserId)
            .toUiStateFlow("ফলাফল লোড করতে ব্যর্থ হয়েছে")

    val practiceSessions: StateFlow<List<PracticeSession>> =
        practiceExamRepo.observeMyPracticeSessions(currentUserId)
            .catch { if (it is CancellationException) throw it; emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressList: StateFlow<List<StudentProgress>> =
        practiceExamRepo.observeMyProgress(currentUserId)
            .catch { if (it is CancellationException) throw it; emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wrongAnswers: StateFlow<List<WrongAnswer>> =
        practiceExamRepo.observeMyWrongAnswers(currentUserId)
            .catch { if (it is CancellationException) throw it; emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> =
        practiceExamRepo.observeMyBookmarks(currentUserId)
            .catch { if (it is CancellationException) throw it; emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        practiceExamRepo.observeNotifications(currentUserId)
            .catch { if (it is CancellationException) throw it; emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> =
        practiceExamRepo.observePendingOfflineSyncCount(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchState = MutableStateFlow<UiState<List<QuestionPublic>>>(UiState.Success(emptyList()))
    val searchState: StateFlow<UiState<List<QuestionPublic>>> = _searchState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun selectSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
        _selectedChapterId.value = null
    }

    fun selectChapter(chapterId: String?) {
        _selectedChapterId.value = chapterId
    }

    fun searchQuestions(
        query: String,
        subjectId: String? = null,
        difficulty: String? = null,
        language: String? = null
    ) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            val res = educationRepo.searchPublishedQuestions(
                queryText = query,
                subjectIdFilter = subjectId,
                difficultyFilter = difficulty,
                languageFilter = language
            )
            _searchState.value = if (res.isSuccess) {
                UiState.Success(res.getOrThrow())
            } else {
                UiState.Error(res.exceptionOrNull()?.localizedMessage ?: "অনুসন্ধান ব্যর্থ হয়েছে।")
            }
        }
    }

    fun downloadSetOffline(questionSet: QuestionSet) {
        viewModelScope.launch {
            val res = educationRepo.downloadQuestionSetOffline(questionSet)
            _actionMessage.value = if (res.isSuccess) {
                "অফলাইনের জন্য ${res.getOrThrow()}টি প্রশ্ন ডাউনলোড হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে।"
            }
        }
    }

    fun removeOfflineSet(setId: String) {
        viewModelScope.launch {
            educationRepo.removeOfflineQuestionSet(setId)
            _actionMessage.value = "অফলাইন সেট মুছে ফেলা হয়েছে।"
        }
    }

    fun syncOfflinePractice() {
        viewModelScope.launch {
            val res = practiceExamRepo.syncPendingOfflinePractice()
            _actionMessage.value = if (res.isSuccess) {
                "${res.getOrThrow()}টি অফলাইন অনুশীলন সিঙ্ক হয়েছে।"
            } else {
                "সিঙ্ক ব্যর্থ হয়েছে: ${res.exceptionOrNull()?.localizedMessage.orEmpty()}"
            }
        }
    }

    fun toggleBookmark(question: QuestionPublic, collectionName: String = "Saved Questions", reviewLater: Boolean = false) {
        viewModelScope.launch {
            val res = practiceExamRepo.toggleBookmark(question, collectionName, reviewLater)
            _actionMessage.value = if (res.isSuccess) {
                if (res.getOrThrow()) "প্রশ্নটি বুকমার্ক করা হয়েছে।" else "বুকমার্ক সরানো হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "বুকমার্ক আপডেট ব্যর্থ হয়েছে।"
            }
        }
    }

    fun updateBookmarkCollection(bookmarkId: String, collectionName: String, reviewLater: Boolean) {
        viewModelScope.launch {
            practiceExamRepo.updateBookmarkCollection(bookmarkId, collectionName, reviewLater)
        }
    }

    fun removeWrongAnswer(wrongAnswerId: String) {
        viewModelScope.launch {
            practiceExamRepo.removeWrongAnswer(wrongAnswerId)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            practiceExamRepo.markNotificationRead(notificationId)
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackErr: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.localizedMessage ?: fallbackErr))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
}
