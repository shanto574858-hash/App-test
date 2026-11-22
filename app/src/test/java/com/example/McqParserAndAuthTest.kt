package com.example

import com.example.data.model.SearchTokenizer
import com.example.data.model.UserRole
import com.example.data.model.UserSessionRole
import com.example.domain.pdf.McqParserEngine
import com.example.domain.pdf.PageTextInput
import com.example.utils.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McqParserAndAuthTest {

    @Test
    fun `parse bengali mcq with inline answer and explanation`() {
        val page = PageTextInput(
            pageNumber = 1,
            text = """
                ১. বাংলাদেশের সংবিধান কবে গৃহীত হয়?
                ক) ৪ নভেম্বর ১৯৭২
                খ) ১৬ ডিসেম্বর ১৯৭২
                গ) ২৬ মার্চ ১৯৭১
                ঘ) ১৭ এপ্রিল ১৯৭১
                সঠিক উত্তর: ক
                ব্যাখ্যা: ১৯৭২ সালের ৪ নভেম্বর গণপরিষদে সংবিধান গৃহীত হয়।
            """.trimIndent()
        )
        val parsed = McqParserEngine.parsePages(listOf(page))
        assertEquals(1, parsed.size)
        val q = parsed.first()
        assertEquals(1, q.questionNumber)
        assertEquals("বাংলাদেশের সংবিধান কবে গৃহীত হয়?", q.questionText)
        assertEquals("৪ নভেম্বর ১৯৭২", q.optionA)
        assertEquals("১৬ ডিসেম্বর ১৯৭২", q.optionB)
        assertEquals("২৬ মার্চ ১৯৭১", q.optionC)
        assertEquals("১৭ এপ্রিল ১৯৭১", q.optionD)
        assertEquals("A", q.correctAnswer)
        assertEquals("bn", q.language)
        assertTrue(q.explanation.contains("৪ নভেম্বর"))
    }

    @Test
    fun `parse multi page english mcq and match answer key from final page`() {
        val page1 = PageTextInput(
            pageNumber = 1,
            text = """
                1. What is the value of \sqrt{144} + 3^2?
                A) 21
                B) 18
                C) 24
                D) 15

                2. Which river flows through Dhaka?
                (A) Padma
                (B) Buriganga
                (C) Jamuna
                (D) Surma
            """.trimIndent()
        )
        val page2 = PageTextInput(
            pageNumber = 2,
            text = """
                Answer Key
                1. A
                2. B
            """.trimIndent()
        )
        val parsed = McqParserEngine.parsePages(listOf(page1, page2))
        assertEquals(2, parsed.size)
        assertEquals("A", parsed[0].correctAnswer)
        assertTrue(parsed[0].hasMath)
        assertEquals("B", parsed[1].correctAnswer)
    }

    @Test
    fun `missing answer is never guessed and stays unverified`() {
        val page = PageTextInput(
            pageNumber = 1,
            text = """
                ৩. পদ্মা সেতুর দৈর্ঘ্য কত কিলোমিটার?
                ক) ৬.১৫ কিমি
                খ) ৫.৫০ কিমি
                গ) ৭.০০ কিমি
                ঘ) ৪.৮০ কিমি
            """.trimIndent()
        )
        val parsed = McqParserEngine.parsePages(listOf(page))
        assertEquals(1, parsed.size)
        val q = parsed.first()
        assertNull(q.correctAnswer)
        assertFalse(q.verified)
        assertTrue(q.needsReview)
    }

    @Test
    fun `super admin uid and role permissions enforce least privilege`() {
        assertEquals("aaWqMj9EgnWbZbX3skVUMOgpgRq2", AppConstants.SUPER_ADMIN_UID)

        val studentRole = UserSessionRole(uid = "student_1", role = UserRole.STUDENT)
        assertFalse(studentRole.isAdmin)
        assertFalse(studentRole.isSuperAdmin)
        assertFalse(studentRole.hasPermission("pdf_upload"))

        val adminRole = UserSessionRole(
            uid = "admin_1",
            role = UserRole.ADMIN,
            permissions = setOf("pdf_upload", "question_create")
        )
        assertTrue(adminRole.isAdmin)
        assertFalse(adminRole.isSuperAdmin)
        assertTrue(adminRole.hasPermission("pdf_upload"))
        assertFalse(adminRole.hasPermission("pdf_delete"))

        val superRole = UserSessionRole(
            uid = AppConstants.SUPER_ADMIN_UID,
            role = UserRole.SUPER_ADMIN,
            permissions = emptySet()
        )
        assertTrue(superRole.isAdmin)
        assertTrue(superRole.isSuperAdmin)
        assertTrue(superRole.hasPermission("pdf_delete"))
    }

    @Test
    fun `search tokenizer extracts bengali and english keywords`() {
        val tokens = SearchTokenizer.buildTokens("বাংলাদেশের সংবিধান ১৯৭২", "BCS Preparation")
        assertTrue(tokens.contains("বাংলাদেশের"))
        assertTrue(tokens.contains("সংবিধান"))
        assertTrue(tokens.contains("bcs"))
    }
}
