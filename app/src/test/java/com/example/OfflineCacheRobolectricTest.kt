package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.CachedQuestionSetEntity
import com.example.data.local.PendingPracticeSyncEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineCacheRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `offline question set and pending practice sync persist in Room`() = runTest {
        val dao = db.mcqDao()
        dao.insertQuestionSet(
            CachedQuestionSetEntity(
                id = "set_bcs_1",
                title = "৪৫তম বিসিএস প্রস্তুতি",
                description = "বাংলা সাহিত্য ও ব্যাকরণ",
                subjectId = "sub_1",
                subjectName = "বাংলা",
                chapterId = "ch_1",
                chapterName = "সাহিত্য",
                questionCount = 1,
                language = "bn",
                downloadedAtMillis = 1000L
            )
        )
        dao.insertQuestions(
            listOf(
                CachedQuestionEntity(
                    id = "q_1",
                    questionSetId = "set_bcs_1",
                    subjectId = "sub_1",
                    subjectName = "বাংলা",
                    chapterId = "ch_1",
                    chapterName = "সাহিত্য",
                    questionText = "চর্যাপদ কোন যুগের নিদর্শন?",
                    optionA = "প্রাচীন যুগ",
                    optionB = "মধ্যযুগ",
                    optionC = "আধুনিক যুগ",
                    optionD = "কোনোটিই নয়",
                    difficulty = "easy",
                    language = "bn",
                    hasMath = false,
                    questionNumber = 1
                )
            )
        )
        dao.insertPendingSync(
            PendingPracticeSyncEntity(
                userId = "user_1",
                questionId = "q_1",
                questionSetId = "set_bcs_1",
                selectedOption = "A",
                createdAtMillis = 2000L
            )
        )

        val cachedQs = dao.getQuestionsForSet("set_bcs_1")
        assertEquals(1, cachedQs.size)
        assertEquals("চর্যাপদ কোন যুগের নিদর্শন?", cachedQs.first().questionText)

        val pending = dao.getPendingSyncOnce("user_1")
        assertEquals(1, pending.size)
        assertEquals("A", pending.first().selectedOption)
    }
}
