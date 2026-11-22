package com.example.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminQuestionWithAnswer
import com.example.data.model.Chapter
import com.example.data.model.Examination
import com.example.data.model.ExaminationResult
import com.example.data.model.PdfDocument
import com.example.data.model.PdfProcessingJob
import com.example.data.model.QuestionSet
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.domain.pdf.PdfTextAndOcrExtractor
import com.example.domain.pdf.TessDataManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class PdfPreviewUiState(
    val file: File? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val currentPageIndex: Int = 0,
    val currentBitmap: Bitmap? = null,
    val isLoading: Boolean = false
)

class AdminViewModel(
    private val adminRepo: AdminRepository,
    private val pdfExtractor: PdfTextAndOcrExtractor,
    private val tessDataManager: TessDataManager
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = adminRepo.observeAllSubjects()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapters: StateFlow<List<Chapter>> = adminRepo.observeAllChapters()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<Topic>> = adminRepo.observeAllTopics()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pdfDocuments: StateFlow<List<PdfDocument>> = adminRepo.observePdfDocuments()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pdfJobs: StateFlow<List<PdfProcessingJob>> = adminRepo.observePdfProcessingJobs()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<AdminQuestionWithAnswer>> = adminRepo.observeAdminQuestionsWithAnswers()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionSets: StateFlow<List<QuestionSet>> = adminRepo.observeAllQuestionSets()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examinations: StateFlow<List<Examination>> = adminRepo.observeAllExaminations()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<UserProfile>> = adminRepo.observeAllStudents()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResults: StateFlow<List<ExaminationResult>> = adminRepo.observeAllResultsAdmin()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pdfPreview = MutableStateFlow(PdfPreviewUiState())
    val pdfPreview: StateFlow<PdfPreviewUiState> = _pdfPreview.asStateFlow()

    private val _ocrPackReady = MutableStateFlow(tessDataManager.areBengaliAndEnglishReady())
    val ocrPackReady: StateFlow<Boolean> = _ocrPackReady.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    fun downloadOcrPacks() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = tessDataManager.ensureTrainedData(listOf("ben", "eng")) { msg ->
                _statusMessage.value = msg
            }
            _isBusy.value = false
            _ocrPackReady.value = tessDataManager.areBengaliAndEnglishReady()
            _statusMessage.value = if (res.isSuccess) {
                "বাংলা ও ইংরেজি OCR ডেটা সফলভাবে ডাউনলোড হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "OCR ডেটা ডাউনলোড ব্যর্থ হয়েছে।"
            }
        }
    }

    fun preparePdfPreview(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _pdfPreview.value = PdfPreviewUiState(isLoading = true, fileName = fileName)
            val copyRes = adminRepo.copyPdfToCache(uri, fileName)
            if (copyRes.isFailure) {
                _pdfPreview.value = PdfPreviewUiState()
                _statusMessage.value = copyRes.exceptionOrNull()?.localizedMessage ?: "PDF পড়া যায়নি।"
                return@launch
            }
            val file = copyRes.getOrThrow()
            val count = try {
                pdfExtractor.getPageCount(file)
            } catch (_: Exception) {
                0
            }
            val bmp = if (count > 0) pdfExtractor.renderPreviewPage(file, 0) else null
            _pdfPreview.value = PdfPreviewUiState(
                file = file,
                fileName = fileName,
                pageCount = count,
                currentPageIndex = 0,
                currentBitmap = bmp,
                isLoading = false
            )
        }
    }

    fun loadPreviewPage(pageIndex: Int) {
        val current = _pdfPreview.value
        val file = current.file ?: return
        if (pageIndex !in 0 until current.pageCount) return
        viewModelScope.launch {
            _pdfPreview.value = current.copy(isLoading = true)
            val bmp = pdfExtractor.renderPreviewPage(file, pageIndex)
            _pdfPreview.value = current.copy(
                currentPageIndex = pageIndex,
                currentBitmap = bmp,
                isLoading = false
            )
        }
    }

    fun clearPdfPreview() {
        _pdfPreview.value = PdfPreviewUiState()
    }

    fun uploadAndOptionallyExtract(
        subjectId: String,
        subjectName: String,
        chapterId: String,
        chapterName: String,
        extractNow: Boolean,
        forceOcr: Boolean
    ) {
        val preview = _pdfPreview.value
        val file = preview.file ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val uploadRes = adminRepo.uploadPdfDocument(file, preview.fileName, subjectId, chapterId)
            if (uploadRes.isFailure) {
                _isBusy.value = false
                _statusMessage.value = uploadRes.exceptionOrNull()?.localizedMessage ?: "PDF আপলোড ব্যর্থ হয়েছে।"
                return@launch
            }
            val doc = uploadRes.getOrThrow()
            if (extractNow) {
                adminRepo.enqueuePdfExtraction(doc, file.absolutePath, subjectName, chapterName, forceOcr)
                _statusMessage.value = "PDF আপলোড এবং ব্যাকগ্রাউন্ড এক্সট্র্যাকশন শুরু হয়েছে।"
            } else {
                _statusMessage.value = "PDF সফলভাবে আপলোড হয়েছে।"
            }
            _isBusy.value = false
            clearPdfPreview()
        }
    }

    fun extractExistingPdf(
        pdf: PdfDocument,
        subjectName: String,
        chapterName: String,
        forceOcr: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.enqueuePdfExtraction(pdf, "", subjectName, chapterName, forceOcr)
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) {
                "প্রশ্ন এক্সট্র্যাকশন কাজ শুরু হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "এক্সট্র্যাকশন শুরু ব্যর্থ হয়েছে।"
            }
        }
    }

    fun renamePdf(pdfId: String, newName: String) {
        viewModelScope.launch {
            val res = adminRepo.renamePdfDocument(pdfId, newName)
            _statusMessage.value = if (res.isSuccess) "PDF নাম পরিবর্তিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun replacePdf(pdf: PdfDocument, uri: Uri, newName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val copy = adminRepo.copyPdfToCache(uri, newName)
            if (copy.isSuccess) {
                val res = adminRepo.replacePdfDocument(pdf, copy.getOrThrow(), newName)
                _statusMessage.value = if (res.isSuccess) "PDF প্রতিস্থাপিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
            } else {
                _statusMessage.value = copy.exceptionOrNull()?.localizedMessage
            }
            _isBusy.value = false
        }
    }

    fun deletePdf(pdf: PdfDocument) {
        viewModelScope.launch {
            val res = adminRepo.deletePdfDocument(pdf)
            _statusMessage.value = if (res.isSuccess) "PDF মুছে ফেলা হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveQuestion(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.saveQuestionWithAnswer(
                questionId, questionText, optionA, optionB, optionC, optionD,
                correctAnswer, explanation, subjectId, subjectName, chapterId, chapterName,
                topicId, topicName, questionSetId, difficulty, language, source,
                verified, publishImmediately
            )
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন সংরক্ষিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun approveAndPublishQuestion(questionId: String, hasAnswer: Boolean) {
        if (!hasAnswer) {
            _statusMessage.value = "সঠিক উত্তর নির্ধারণ না করে প্রশ্ন প্রকাশ করা যাবে না।"
            return
        }
        viewModelScope.launch {
            val res = adminRepo.setQuestionStatus(questionId, status = "published", verified = true)
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন অনুমোদিত ও প্রকাশিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun rejectQuestion(questionId: String) {
        viewModelScope.launch {
            val res = adminRepo.setQuestionStatus(questionId, status = "rejected", verified = false)
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন প্রত্যাখ্যান করা হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun unpublishQuestion(questionId: String) {
        viewModelScope.launch {
            adminRepo.setQuestionStatus(questionId, status = "unpublished", verified = true)
        }
    }

    fun publishSelectedOrAllVerified(ids: List<String>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.publishQuestionsBulk(ids)
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) {
                "${res.getOrThrow()}টি যাচাইকৃত প্রশ্ন প্রকাশিত হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            val res = adminRepo.deleteQuestion(questionId)
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন মুছে ফেলা হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun duplicateQuestion(item: AdminQuestionWithAnswer) {
        viewModelScope.launch {
            val res = adminRepo.duplicateQuestion(item)
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন কপি করা হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun bulkImportJson(json: String, subjectId: String, subjectName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.bulkImportJson(json, subjectId, subjectName)
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) {
                "${res.getOrThrow()}টি প্রশ্ন রিভিউ তালিকায় আমদানি হয়েছে।"
            } else {
                "JSON ত্রুটি: ${res.exceptionOrNull()?.localizedMessage.orEmpty()}"
            }
        }
    }

    fun exportQuestionsJson(list: List<AdminQuestionWithAnswer>) {
        _exportedJson.value = adminRepo.exportQuestionsToJson(list)
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    fun saveSubject(id: String?, nameBn: String, nameEn: String, desc: String, icon: String, order: Int) {
        viewModelScope.launch {
            val res = adminRepo.saveSubject(id, nameBn, nameEn, desc, icon, order)
            _statusMessage.value = if (res.isSuccess) "বিষয় সংরক্ষিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch { adminRepo.deleteSubject(id) }
    }

    fun addSuggestedCategories() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.createSuggestedSubjectCategories()
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) {
                "${res.getOrThrow()}টি প্রস্তাবিত বিষয় যোগ করা হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun saveChapter(id: String?, subjectId: String, nameBn: String, nameEn: String, order: Int) {
        viewModelScope.launch {
            val res = adminRepo.saveChapter(id, subjectId, nameBn, nameEn, order)
            _statusMessage.value = if (res.isSuccess) "অধ্যায় সংরক্ষিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteChapter(id: String) {
        viewModelScope.launch { adminRepo.deleteChapter(id) }
    }

    fun saveTopic(subjectId: String, chapterId: String, nameBn: String, nameEn: String) {
        viewModelScope.launch {
            val res = adminRepo.saveTopic(subjectId, chapterId, nameBn, nameEn)
            _statusMessage.value = if (res.isSuccess) "টপিক যোগ হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun createQuestionSet(
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        chapterId: String,
        chapterName: String,
        questionIds: List<String>,
        language: String
    ) {
        viewModelScope.launch {
            val res = adminRepo.createQuestionSet(title, description, subjectId, subjectName, chapterId, chapterName, questionIds, language)
            _statusMessage.value = if (res.isSuccess) "প্রশ্ন সেট তৈরি হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveExamination(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepo.saveExamination(
                examId, title, subjectId, subjectName, chapterId, questionIds,
                marksPerQuestion, negativeMarking, timeLimitMinutes, examMode,
                randomQuestionOrder, randomOptionOrder, status
            )
            _isBusy.value = false
            _statusMessage.value = if (res.isSuccess) "পরীক্ষা সংরক্ষিত হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteExamination(examId: String) {
        viewModelScope.launch { adminRepo.deleteExamination(examId) }
    }

    fun broadcastNotification(title: String, body: String, category: String) {
        viewModelScope.launch {
            val res = adminRepo.sendNotification(title, body, category)
            _statusMessage.value = if (res.isSuccess) "বিজ্ঞপ্তি পাঠানো হয়েছে।" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
