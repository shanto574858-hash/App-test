package com.example.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.EducationRepository
import com.example.data.repository.PracticeAndExamRepository
import com.example.data.repository.authStateFlow
import com.example.domain.pdf.PdfTextAndOcrExtractor
import com.example.domain.pdf.TessDataManager
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminExamsScreen
import com.example.ui.admin.AdminSettingsScreen
import com.example.ui.admin.McqManagerScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.examination.ExaminationScreen
import com.example.ui.home.GlobalSearchScreen
import com.example.ui.home.NotificationsScreen
import com.example.ui.home.StudentHomeScreen
import com.example.ui.pdf.PdfManagerScreen
import com.example.ui.pdf.PdfReviewScreen
import com.example.ui.practice.BookmarksScreen
import com.example.ui.practice.PracticeScreen
import com.example.ui.practice.WrongAnswersScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.results.ResultsAndAnalyticsScreen
import com.example.ui.subjects.OfflineSetsScreen
import com.example.ui.subjects.SubjectsScreen
import com.example.ui.superadmin.SuperAdminScreen
import com.example.utils.AppConstants
import com.example.utils.AppLanguage
import com.example.utils.AppPreferences
import com.example.utils.AppStrings
import com.example.utils.ConnectivityObserver
import com.example.utils.ThemeMode
import com.example.ui.components.ErrorBannerCard
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AppPanelMode
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.PracticeExamViewModel
import com.example.viewmodel.StudentViewModel
import com.example.viewmodel.SuperAdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object SubjectsRoute
@Serializable object PracticeRoute
@Serializable object ExamsRoute
@Serializable object ResultsRoute
@Serializable object ProfileRoute
@Serializable object NotificationsRoute
@Serializable object GlobalSearchRoute
@Serializable object BookmarksRoute
@Serializable object WrongAnswersRoute
@Serializable object OfflineSetsRoute

@Serializable object AdminDashboardRoute
@Serializable object AdminPdfManagerRoute
@Serializable object AdminPdfReviewRoute
@Serializable object AdminMcqManagerRoute
@Serializable object AdminExamsRoute
@Serializable object AdminSettingsRoute

@Serializable object SuperAdminRoute

private data class NavTabItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun McqAppEntry(
    preferences: AppPreferences,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val language by preferences.language.collectAsStateWithLifecycle(initialValue = AppLanguage.BENGALI)

    LaunchedEffect(themeMode) {
        onThemeChanged(themeMode)
    }

    val strings = remember(language) { AppStrings(isBn = language == AppLanguage.BENGALI) }
    val firebaseReady = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty() ||
            runCatching { FirebaseApp.initializeApp(context) }.getOrNull() != null
    }

    if (!firebaseReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBannerCard(
                    message = if (strings.isBn) {
                        "Firebase কনফিগারেশন (app/google-services.json) এই APK ফাইলে যুক্ত হয়নি। GitHub-এ app/google-services.json বা GOOGLE_SERVICES_JSON সিক্রেট যোগ করে পুনরায় APK বিল্ড করুন।"
                    } else {
                        "Firebase configuration (app/google-services.json) is missing in this APK build. Add app/google-services.json or GOOGLE_SERVICES_JSON secret on GitHub and rebuild."
                    }
                )
            }
        }
        return
    }

    val dbId = remember(context) { context.getString(R.string.firestore_database_id) }
    val firestore = remember(dbId) {
        if (dbId.isBlank() || dbId == "(default)") {
            FirebaseFirestore.getInstance()
        } else {
            FirebaseFirestore.getInstance(dbId)
        }
    }
    val roomDb = remember(context) { AppDatabase.getInstance(context) }
    val connectivityObserver = remember(context) { ConnectivityObserver(context) }

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            strings = strings,
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedShell(
                uid = user.uid,
                strings = strings,
                themeMode = themeMode,
                language = language,
                firestore = firestore,
                roomDb = roomDb,
                preferences = preferences,
                connectivityObserver = connectivityObserver
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AuthenticatedShell(
    uid: String,
    strings: AppStrings,
    themeMode: ThemeMode,
    language: AppLanguage,
    firestore: FirebaseFirestore,
    roomDb: AppDatabase,
    preferences: AppPreferences,
    connectivityObserver: ConnectivityObserver
) {
    val context = LocalContext.current
    val authRepo = remember(firestore) { AuthRepository(db = firestore) }
    val educationRepo = remember(firestore, roomDb, preferences) {
        EducationRepository(db = firestore, dao = roomDb.mcqDao(), preferences = preferences)
    }
    val practiceExamRepo = remember(firestore, roomDb) {
        PracticeAndExamRepository(db = firestore, dao = roomDb.mcqDao())
    }

    val appVm: AppViewModel = viewModel(key = "app_$uid") {
        AppViewModel(authRepo, preferences, connectivityObserver, uid)
    }
    val studentVm: StudentViewModel = viewModel(key = "student_$uid") {
        StudentViewModel(educationRepo, practiceExamRepo, uid)
    }
    val practiceExamVm: PracticeExamViewModel = viewModel(key = "pe_$uid") {
        PracticeExamViewModel(educationRepo, practiceExamRepo)
    }

    val userProfile by appVm.userProfile.collectAsStateWithLifecycle()
    val sessionRole by appVm.sessionRole.collectAsStateWithLifecycle()
    val panelMode by appVm.panelMode.collectAsStateWithLifecycle()
    val isOnline by appVm.isOnline.collectAsStateWithLifecycle()
    val appStatusMsg by appVm.statusMessage.collectAsStateWithLifecycle()
    val studentActionMsg by studentVm.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(appStatusMsg) {
        appStatusMsg?.let {
            snackbarHostState.showSnackbar(it)
            appVm.clearStatusMessage()
        }
    }
    LaunchedEffect(studentActionMsg) {
        studentActionMsg?.let {
            snackbarHostState.showSnackbar(it)
            studentVm.clearActionMessage()
        }
    }

    if (panelMode == AppPanelMode.SUPER_ADMIN && sessionRole.isSuperAdmin && uid == AppConstants.SUPER_ADMIN_UID) {
        val adminRepo = remember(context, firestore) { AdminRepository(context.applicationContext, firestore) }
        val tessDataManager = remember(context) { TessDataManager(context.applicationContext) }
        val pdfExtractor = remember(context, tessDataManager) { PdfTextAndOcrExtractor(context.applicationContext, tessDataManager) }
        val adminVm: AdminViewModel = viewModel(key = "admin_$uid") {
            AdminViewModel(adminRepo, pdfExtractor, tessDataManager)
        }
        val superAdminVm: SuperAdminViewModel = viewModel(key = "super_$uid") {
            SuperAdminViewModel(adminRepo)
        }

        val admins by superAdminVm.admins.collectAsStateWithLifecycle()
        val students by adminVm.students.collectAsStateWithLifecycle()
        val appSettings by superAdminVm.appSettings.collectAsStateWithLifecycle()
        val auditLogs by superAdminVm.auditLogs.collectAsStateWithLifecycle()
        val superMsg by superAdminVm.statusMessage.collectAsStateWithLifecycle()

        LaunchedEffect(superMsg) {
            superMsg?.let {
                snackbarHostState.showSnackbar(it)
                superAdminVm.clearStatusMessage()
            }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                SuperAdminScreen(
                    strings = strings,
                    currentUid = uid,
                    admins = admins,
                    students = students,
                    appSettings = appSettings,
                    auditLogs = auditLogs,
                    onUpsertAdmin = superAdminVm::upsertAdmin,
                    onRevokeAdmin = superAdminVm::revokeAdmin,
                    onUpdateStudentStatus = superAdminVm::updateStudentStatus,
                    onUpdateAppSettings = superAdminVm::updateAppSettings,
                    onSwitchPanel = appVm::switchPanel
                )
            }
        }
        return
    }

    if (panelMode == AppPanelMode.ADMIN && sessionRole.isAdmin) {
        val adminRepo = remember(context, firestore) { AdminRepository(context.applicationContext, firestore) }
        val tessDataManager = remember(context) { TessDataManager(context.applicationContext) }
        val pdfExtractor = remember(context, tessDataManager) { PdfTextAndOcrExtractor(context.applicationContext, tessDataManager) }
        val adminVm: AdminViewModel = viewModel(key = "admin_$uid") {
            AdminViewModel(adminRepo, pdfExtractor, tessDataManager)
        }

        val subjects by adminVm.subjects.collectAsStateWithLifecycle()
        val chapters by adminVm.chapters.collectAsStateWithLifecycle()
        val topics by adminVm.topics.collectAsStateWithLifecycle()
        val pdfs by adminVm.pdfDocuments.collectAsStateWithLifecycle()
        val pdfJobs by adminVm.pdfJobs.collectAsStateWithLifecycle()
        val allQuestions by adminVm.allQuestions.collectAsStateWithLifecycle()
        val questionSets by adminVm.questionSets.collectAsStateWithLifecycle()
        val examinations by adminVm.examinations.collectAsStateWithLifecycle()
        val students by adminVm.students.collectAsStateWithLifecycle()
        val allResults by adminVm.allResults.collectAsStateWithLifecycle()
        val pdfPreview by adminVm.pdfPreview.collectAsStateWithLifecycle()
        val ocrPackReady by adminVm.ocrPackReady.collectAsStateWithLifecycle()
        val isBusy by adminVm.isBusy.collectAsStateWithLifecycle()
        val exportedJson by adminVm.exportedJson.collectAsStateWithLifecycle()
        val adminStatus by adminVm.statusMessage.collectAsStateWithLifecycle()

        LaunchedEffect(adminStatus) {
            adminStatus?.let {
                snackbarHostState.showSnackbar(it)
                adminVm.clearStatusMessage()
            }
        }

        val adminNavController = rememberNavController()
        val navBackStack by adminNavController.currentBackStackEntryAsState()
        val currentDest = navBackStack?.destination

        val adminTabs = remember(strings) {
            listOf(
                NavTabItem(AdminDashboardRoute, strings.adminDashboard, Icons.Default.Dashboard, "admin_tab_dashboard"),
                NavTabItem(AdminPdfManagerRoute, strings.adminPdfManager, Icons.Default.PictureAsPdf, "admin_tab_pdf"),
                NavTabItem(AdminMcqManagerRoute, strings.adminMcqManager, Icons.Default.Quiz, "admin_tab_mcq"),
                NavTabItem(AdminExamsRoute, strings.adminExams, Icons.Default.Timer, "admin_tab_exams"),
                NavTabItem(AdminSettingsRoute, strings.adminSettings, Icons.Default.Settings, "admin_tab_settings")
            )
        }
        val showAdminBars = currentDest?.hasRoute(AdminPdfReviewRoute::class) != true

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail && showAdminBars) {
                        NavigationBar {
                            adminTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        adminNavController.navigate(tab.route) {
                                            popUpTo(adminNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (useRail && showAdminBars) {
                        NavigationRail {
                            adminTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        adminNavController.navigate(tab.route) {
                                            popUpTo(adminNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        NavHost(
                            navController = adminNavController,
                            startDestination = AdminDashboardRoute,
                            modifier = Modifier.widthIn(max = 920.dp).fillMaxSize()
                        ) {
                            composable<AdminDashboardRoute> {
                                AdminDashboardScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    subjects = subjects,
                                    pdfs = pdfs,
                                    pdfJobs = pdfJobs,
                                    questions = allQuestions,
                                    examinations = examinations,
                                    students = students,
                                    onOpenPdfManager = { adminNavController.navigate(AdminPdfManagerRoute) },
                                    onOpenPdfReview = { adminNavController.navigate(AdminPdfReviewRoute) },
                                    onOpenMcqManager = { adminNavController.navigate(AdminMcqManagerRoute) },
                                    onOpenExamsManager = { adminNavController.navigate(AdminExamsRoute) },
                                    onSwitchPanel = appVm::switchPanel
                                )
                            }
                            composable<AdminPdfManagerRoute> {
                                PdfManagerScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    pdfs = pdfs,
                                    pdfJobs = pdfJobs,
                                    subjects = subjects,
                                    chapters = chapters,
                                    pdfPreview = pdfPreview,
                                    ocrPackReady = ocrPackReady,
                                    isBusy = isBusy,
                                    onDownloadOcrPacks = adminVm::downloadOcrPacks,
                                    onPreparePdfPreview = adminVm::preparePdfPreview,
                                    onLoadPreviewPage = adminVm::loadPreviewPage,
                                    onClearPdfPreview = adminVm::clearPdfPreview,
                                    onUploadAndExtract = adminVm::uploadAndOptionallyExtract,
                                    onExtractExistingPdf = adminVm::extractExistingPdf,
                                    onRenamePdf = adminVm::renamePdf,
                                    onReplacePdf = adminVm::replacePdf,
                                    onDeletePdf = adminVm::deletePdf,
                                    onOpenReviewScreen = { adminNavController.navigate(AdminPdfReviewRoute) }
                                )
                            }
                            composable<AdminPdfReviewRoute> {
                                PdfReviewScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    allQuestions = allQuestions,
                                    isBusy = isBusy,
                                    onApproveAndPublish = adminVm::approveAndPublishQuestion,
                                    onRejectQuestion = adminVm::rejectQuestion,
                                    onDeleteQuestion = adminVm::deleteQuestion,
                                    onPublishBulk = adminVm::publishSelectedOrAllVerified,
                                    onSaveEditedQuestion = { qId, qT, oA, oB, oC, oD, ans, exp, sId, sName, cId, cName, tId, tName, setId, diff, lang, src, ver, pub ->
                                        adminVm.saveQuestion(qId, qT, oA, oB, oC, oD, ans, exp, sId, sName, cId, cName, tId, tName, setId, diff, lang, src, ver, pub)
                                    },
                                    onBack = { adminNavController.popBackStack() }
                                )
                            }
                            composable<AdminMcqManagerRoute> {
                                McqManagerScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    questions = allQuestions,
                                    subjects = subjects,
                                    chapters = chapters,
                                    topics = topics,
                                    questionSets = questionSets,
                                    exportedJson = exportedJson,
                                    isBusy = isBusy,
                                    onSaveQuestion = adminVm::saveQuestion,
                                    onDeleteQuestion = adminVm::deleteQuestion,
                                    onDuplicateQuestion = adminVm::duplicateQuestion,
                                    onPublishQuestion = adminVm::approveAndPublishQuestion,
                                    onUnpublishQuestion = adminVm::unpublishQuestion,
                                    onBulkImportJson = adminVm::bulkImportJson,
                                    onExportQuestionsJson = adminVm::exportQuestionsJson,
                                    onClearExportedJson = adminVm::clearExportedJson,
                                    onSaveSubject = adminVm::saveSubject,
                                    onDeleteSubject = adminVm::deleteSubject,
                                    onAddSuggestedCategories = adminVm::addSuggestedCategories,
                                    onSaveChapter = adminVm::saveChapter,
                                    onDeleteChapter = adminVm::deleteChapter,
                                    onSaveTopic = adminVm::saveTopic,
                                    onCreateQuestionSet = adminVm::createQuestionSet
                                )
                            }
                            composable<AdminExamsRoute> {
                                AdminExamsScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    examinations = examinations,
                                    allResults = allResults,
                                    subjects = subjects,
                                    questions = allQuestions,
                                    isBusy = isBusy,
                                    onSaveExamination = adminVm::saveExamination,
                                    onDeleteExamination = adminVm::deleteExamination
                                )
                            }
                            composable<AdminSettingsRoute> {
                                AdminSettingsScreen(
                                    strings = strings,
                                    sessionRole = sessionRole,
                                    students = students,
                                    onBroadcastNotification = adminVm::broadcastNotification,
                                    onSwitchPanel = appVm::switchPanel
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // --- STUDENT PANEL ---
    val subjectsState by studentVm.subjectsState.collectAsStateWithLifecycle()
    val cachedSubjects by studentVm.cachedSubjects.collectAsStateWithLifecycle()
    val selectedSubjectId by studentVm.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedChapterId by studentVm.selectedChapterId.collectAsStateWithLifecycle()
    val chapters by studentVm.chaptersForSelectedSubject.collectAsStateWithLifecycle()
    val topics by studentVm.topicsForSelectedChapter.collectAsStateWithLifecycle()
    val questionSetsState by studentVm.questionSetsState.collectAsStateWithLifecycle()
    val downloadedSets by studentVm.downloadedSets.collectAsStateWithLifecycle()
    val examinationsState by studentVm.examinationsState.collectAsStateWithLifecycle()
    val resultsState by studentVm.resultsState.collectAsStateWithLifecycle()
    val practiceSessions by studentVm.practiceSessions.collectAsStateWithLifecycle()
    val progressList by studentVm.progressList.collectAsStateWithLifecycle()
    val wrongAnswers by studentVm.wrongAnswers.collectAsStateWithLifecycle()
    val bookmarks by studentVm.bookmarks.collectAsStateWithLifecycle()
    val notifications by studentVm.notifications.collectAsStateWithLifecycle()
    val pendingSyncCount by studentVm.pendingSyncCount.collectAsStateWithLifecycle()
    val searchState by studentVm.searchState.collectAsStateWithLifecycle()

    val practiceState by practiceExamVm.practiceState.collectAsStateWithLifecycle()
    val examState by practiceExamVm.examState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest = navBackStack?.destination

    val studentTabs = remember(strings) {
        listOf(
            NavTabItem(HomeRoute, strings.navHome, Icons.Default.Home, "tab_home"),
            NavTabItem(SubjectsRoute, strings.navSubjects, Icons.Default.MenuBook, "tab_subjects"),
            NavTabItem(PracticeRoute, strings.navPractice, Icons.Default.Quiz, "tab_practice"),
            NavTabItem(ResultsRoute, strings.navResults, Icons.Default.Assessment, "tab_results"),
            NavTabItem(ProfileRoute, strings.navProfile, Icons.Default.Person, "tab_profile")
        )
    }

    val isFullScreenActive = practiceState.isActive ||
        examState.isActive ||
        examState.submissionResult != null ||
        currentDest?.hasRoute(NotificationsRoute::class) == true ||
        currentDest?.hasRoute(GlobalSearchRoute::class) == true ||
        currentDest?.hasRoute(BookmarksRoute::class) == true ||
        currentDest?.hasRoute(WrongAnswersRoute::class) == true ||
        currentDest?.hasRoute(OfflineSetsRoute::class) == true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail && !isFullScreenActive) {
                    NavigationBar {
                        studentTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true ||
                                (tab.route == PracticeRoute && currentDest?.hasRoute(ExamsRoute::class) == true)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (useRail && !isFullScreenActive) {
                    NavigationRail {
                        studentTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.widthIn(max = 920.dp).fillMaxSize()
                    ) {
                        composable<HomeRoute> {
                            StudentHomeScreen(
                                strings = strings,
                                userProfile = userProfile,
                                currentUid = uid,
                                isOnline = isOnline,
                                subjectsState = subjectsState,
                                cachedSubjects = cachedSubjects,
                                questionSetsState = questionSetsState,
                                practiceSessions = practiceSessions,
                                examinationsState = examinationsState,
                                resultsState = resultsState,
                                bookmarks = bookmarks,
                                wrongAnswers = wrongAnswers,
                                notifications = notifications,
                                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                                onOpenProfile = { navigateTopTab(ProfileRoute) },
                                onOpenSearch = { navController.navigate(GlobalSearchRoute) },
                                onOpenSubject = { subject ->
                                    studentVm.selectSubject(subject.id)
                                    navigateTopTab(SubjectsRoute)
                                },
                                onStartSetPractice = { qSet ->
                                    practiceExamVm.startOnlinePractice(
                                        title = qSet.title,
                                        subjectId = qSet.subjectId.ifBlank { null },
                                        chapterId = qSet.chapterId.ifBlank { null },
                                        topicId = null,
                                        questionSetId = qSet.id,
                                        count = qSet.questionCount.coerceAtLeast(20)
                                    )
                                    navigateTopTab(PracticeRoute)
                                },
                                onResumePractice = { session ->
                                    practiceExamVm.resumePracticeSession(session)
                                    navigateTopTab(PracticeRoute)
                                },
                                onStartExam = { exam ->
                                    practiceExamVm.startExamination(exam)
                                    navController.navigate(ExamsRoute)
                                },
                                onOpenBookmarks = { navController.navigate(BookmarksRoute) },
                                onOpenWrongAnswers = { navController.navigate(WrongAnswersRoute) },
                                onOpenOfflineSets = { navController.navigate(OfflineSetsRoute) }
                            )
                        }

                        composable<SubjectsRoute> {
                            SubjectsScreen(
                                strings = strings,
                                isOnline = isOnline,
                                subjectsState = subjectsState,
                                cachedSubjects = cachedSubjects,
                                selectedSubjectId = selectedSubjectId,
                                chapters = chapters,
                                selectedChapterId = selectedChapterId,
                                topics = topics,
                                questionSetsState = questionSetsState,
                                downloadedSets = downloadedSets,
                                onSelectSubject = studentVm::selectSubject,
                                onSelectChapter = studentVm::selectChapter,
                                onPracticeSubject = { sub ->
                                    practiceExamVm.startOnlinePractice(
                                        title = sub.displayName(strings.isBn),
                                        subjectId = sub.id,
                                        chapterId = null,
                                        topicId = null,
                                        questionSetId = null,
                                        count = 20
                                    )
                                    navigateTopTab(PracticeRoute)
                                },
                                onPracticeChapter = { sub, chap ->
                                    practiceExamVm.startOnlinePractice(
                                        title = "${sub.displayName(strings.isBn)} - ${chap.displayName(strings.isBn)}",
                                        subjectId = sub.id,
                                        chapterId = chap.id,
                                        topicId = null,
                                        questionSetId = null,
                                        count = 20
                                    )
                                    navigateTopTab(PracticeRoute)
                                },
                                onPracticeSet = { qSet ->
                                    practiceExamVm.startOnlinePractice(
                                        title = qSet.title,
                                        subjectId = qSet.subjectId.ifBlank { null },
                                        chapterId = qSet.chapterId.ifBlank { null },
                                        topicId = null,
                                        questionSetId = qSet.id,
                                        count = qSet.questionCount.coerceAtLeast(20)
                                    )
                                    navigateTopTab(PracticeRoute)
                                },
                                onDownloadSetOffline = studentVm::downloadSetOffline
                            )
                        }

                        composable<PracticeRoute> {
                            var subTab by remember { mutableIntStateOf(0) }
                            val hideSubTabBar = practiceState.isActive || examState.isActive || examState.submissionResult != null
                            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                                if (!hideSubTabBar) {
                                    TabRow(selectedTabIndex = subTab) {
                                        Tab(
                                            selected = subTab == 0,
                                            onClick = { subTab = 0 },
                                            text = { Text(if (strings.isBn) "MCQ অনুশীলন" else "Practice") }
                                        )
                                        Tab(
                                            selected = subTab == 1,
                                            onClick = { subTab = 1 },
                                            text = { Text(if (strings.isBn) "অনলাইন পরীক্ষা" else "Examinations") }
                                        )
                                    }
                                }
                                if (subTab == 0 || practiceState.isActive) {
                                    PracticeScreen(
                                        strings = strings,
                                        isOnline = isOnline,
                                        subjectsState = subjectsState,
                                        chapters = chapters,
                                        topics = topics,
                                        questionSetsState = questionSetsState,
                                        practiceSessions = practiceSessions,
                                        bookmarks = bookmarks,
                                        practiceState = practiceState,
                                        onSelectSubjectFilter = studentVm::selectSubject,
                                        onSelectChapterFilter = studentVm::selectChapter,
                                        onStartPractice = practiceExamVm::startOnlinePractice,
                                        onResumeSession = practiceExamVm::resumePracticeSession,
                                        onSelectOption = practiceExamVm::selectPracticeOption,
                                        onFinalizeAnswer = practiceExamVm::finalizeCurrentPracticeAnswer,
                                        onMoveIndex = practiceExamVm::movePracticeIndex,
                                        onFinishSession = practiceExamVm::finishPracticeSession,
                                        onExitPractice = practiceExamVm::exitPractice,
                                        onToggleBookmark = { q -> studentVm.toggleBookmark(q) },
                                        onOpenWrongAnswers = { navController.navigate(WrongAnswersRoute) }
                                    )
                                } else {
                                    ExaminationScreen(
                                        strings = strings,
                                        isOnline = isOnline,
                                        examinationsState = examinationsState,
                                        examState = examState,
                                        onStartExam = practiceExamVm::startExamination,
                                        onSelectAnswer = practiceExamVm::selectExamAnswer,
                                        onNavigateQuestion = practiceExamVm::navigateExamQuestion,
                                        onSubmitExam = practiceExamVm::submitExamination,
                                        onExitExam = practiceExamVm::exitExam
                                    )
                                }
                            }
                        }

                        composable<ExamsRoute> {
                            ExaminationScreen(
                                strings = strings,
                                isOnline = isOnline,
                                examinationsState = examinationsState,
                                examState = examState,
                                onStartExam = practiceExamVm::startExamination,
                                onSelectAnswer = practiceExamVm::selectExamAnswer,
                                onNavigateQuestion = practiceExamVm::navigateExamQuestion,
                                onSubmitExam = practiceExamVm::submitExamination,
                                onExitExam = {
                                    practiceExamVm.exitExam()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<ResultsRoute> {
                            ResultsAndAnalyticsScreen(
                                strings = strings,
                                userProfile = userProfile,
                                resultsState = resultsState,
                                progressList = progressList,
                                loadedReviewItems = examState.reviewItems,
                                isLoadingReview = examState.isLoading,
                                onOpenResultReview = practiceExamVm::loadSavedResultReview
                            )
                        }

                        composable<ProfileRoute> {
                            ProfileScreen(
                                strings = strings,
                                userProfile = userProfile,
                                sessionRole = sessionRole,
                                currentThemeMode = themeMode,
                                currentLanguage = language,
                                onUpdateProfile = appVm::updateProfile,
                                onUploadPhoto = appVm::uploadProfilePhoto,
                                onSetThemeMode = appVm::setThemeMode,
                                onSetLanguage = appVm::setLanguage,
                                onSwitchPanel = appVm::switchPanel,
                                onOpenBookmarks = { navController.navigate(BookmarksRoute) },
                                onOpenWrongAnswers = { navController.navigate(WrongAnswersRoute) },
                                onOpenOfflineSets = { navController.navigate(OfflineSetsRoute) }
                            )
                        }

                        composable<NotificationsRoute> {
                            NotificationsScreen(
                                strings = strings,
                                currentUid = uid,
                                notifications = notifications,
                                onMarkRead = studentVm::markNotificationRead,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<GlobalSearchRoute> {
                            val subList = (subjectsState as? UiState.Success)?.data ?: cachedSubjects
                            GlobalSearchScreen(
                                strings = strings,
                                subjects = subList,
                                searchState = searchState,
                                bookmarks = bookmarks,
                                onSearch = studentVm::searchQuestions,
                                onToggleBookmark = { q -> studentVm.toggleBookmark(q) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<BookmarksRoute> {
                            BookmarksScreen(
                                strings = strings,
                                bookmarks = bookmarks,
                                onUpdateCollection = studentVm::updateBookmarkCollection,
                                onRemoveBookmark = { q -> studentVm.toggleBookmark(q) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<WrongAnswersRoute> {
                            WrongAnswersScreen(
                                strings = strings,
                                wrongAnswers = wrongAnswers,
                                onPracticeWrongQuestions = { ids ->
                                    practiceExamVm.startWrongAnswerPractice(ids)
                                    navController.popBackStack()
                                    navigateTopTab(PracticeRoute)
                                },
                                onRemoveWrongAnswer = studentVm::removeWrongAnswer,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<OfflineSetsRoute> {
                            OfflineSetsScreen(
                                strings = strings,
                                isOnline = isOnline,
                                downloadedSets = downloadedSets,
                                pendingSyncCount = pendingSyncCount,
                                onSyncNow = studentVm::syncOfflinePractice,
                                onPracticeOfflineSet = { setEntity ->
                                    practiceExamVm.startOfflinePracticeSet(setEntity.id)
                                    navController.popBackStack()
                                    navigateTopTab(PracticeRoute)
                                },
                                onDeleteOfflineSet = studentVm::removeOfflineSet,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
