package com.example.utils

object AppConstants {
    const val SUPER_ADMIN_UID = "aaWqMj9EgnWbZbX3skVUMOgpgRq2"

    val ALL_ADMIN_PERMISSIONS = listOf(
        "pdf_upload",
        "pdf_delete",
        "question_create",
        "question_edit",
        "question_delete",
        "question_publish",
        "subject_manage",
        "exam_manage",
        "student_manage",
        "result_view"
    )

    val SUGGESTED_SUBJECT_TEMPLATES = listOf(
        SubjectTemplate("বাংলা", "Bangla", "menu_book"),
        SubjectTemplate("English", "English", "translate"),
        SubjectTemplate("গণিত", "Mathematics", "calculate"),
        SubjectTemplate("সাধারণ জ্ঞান", "General Knowledge", "public"),
        SubjectTemplate("বাংলাদেশ বিষয়াবলি", "Bangladesh Affairs", "flag"),
        SubjectTemplate("আন্তর্জাতিক বিষয়াবলি", "International Affairs", "language"),
        SubjectTemplate("সাধারণ বিজ্ঞান", "General Science", "science"),
        SubjectTemplate("পদার্থবিজ্ঞান", "Physics", "bolt"),
        SubjectTemplate("রসায়ন", "Chemistry", "biotech"),
        SubjectTemplate("জীববিজ্ঞান", "Biology", "eco"),
        SubjectTemplate("ICT", "ICT", "computer"),
        SubjectTemplate("ভূগোল", "Geography", "terrain"),
        SubjectTemplate("ইতিহাস", "History", "history_edu"),
        SubjectTemplate("মানসিক দক্ষতা", "Mental Ability", "psychology"),
        SubjectTemplate("চাকরির প্রস্তুতি", "Job Preparation", "work")
    )
}

data class SubjectTemplate(
    val nameBn: String,
    val nameEn: String,
    val iconName: String
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val code: String, val displayLabel: String) {
    BENGALI("bn", "বাংলা"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: BENGALI
    }
}
