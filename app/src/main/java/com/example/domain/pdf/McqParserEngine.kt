package com.example.domain.pdf

data class McqParserConfig(
    val allowBengaliOptions: Boolean = true,
    val allowEnglishOptions: Boolean = true,
    val allowNumericOptions: Boolean = true,
    val autoVerifyHighConfidence: Boolean = false,
    val minQuestionLength: Int = 5
)

data class ParsedMcq(
    val questionNumber: Int,
    val questionText: String,
    val originalPdfText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String?,
    val explanation: String,
    val language: String,
    val pageNumber: Int,
    val confidence: Double,
    val verified: Boolean,
    val needsReview: Boolean,
    val hasMath: Boolean
)

data class PageTextInput(
    val pageNumber: Int,
    val text: String,
    val fromOcr: Boolean = false
)

object McqParserEngine {

    private val BENGALI_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val OPTION_MAP = mapOf(
        "ক" to "A", "খ" to "B", "গ" to "C", "ঘ" to "D",
        "a" to "A", "b" to "B", "c" to "C", "d" to "D",
        "A" to "A", "B" to "B", "C" to "C", "D" to "D",
        "1" to "A", "2" to "B", "3" to "C", "4" to "D",
        "১" to "A", "২" to "B", "৩" to "C", "৪" to "D"
    )

    private val QUESTION_START_REGEX = Regex(
        """^(?:(?:প্রশ্ন|Q|Question)?\s*)?([0-9০-৯]{1,4})\s*[.)।:\-]\s*(.+)$"""
    )

    private val OPTION_START_REGEX = Regex(
        """^\(?([কখগঘA-Da-d1-4১-৪])\s*[).:\-]\s*(.+)$"""
    )

    private val INLINE_OPTION_SPLIT_REGEX = Regex(
        """(?:(?<=^)|(?<=\s))(?=\(?[কখগঘA-Da-d]\s*[).:\-]\s+)"""
    )

    private val ANSWER_LINE_REGEX = Regex(
        """^(?:সঠিক\s*)?(?:উত্তর|Answer|Ans|Correct\s*Answer)\s*[:ঃ\-]?\s*\(?([কখগঘA-Da-d1-4১-৪])\)?(?:[).:\s].*)?$""",
        RegexOption.IGNORE_CASE
    )

    private val EXPLANATION_LINE_REGEX = Regex(
        """^(?:ব্যাখ্যা|সমাধান|Explanation|Solution|Note)\s*[:ঃ\-]?\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val ANSWER_KEY_HEADER_REGEX = Regex(
        """(?:উত্তরমালা|উত্তরপত্র|Answer\s*Key|Answers\s*List)""",
        RegexOption.IGNORE_CASE
    )

    private val ANSWER_KEY_PAIR_REGEX = Regex(
        """([0-9০-৯]{1,4})\s*[.\-:)=\s]+\s*\(?([কখগঘA-Da-d])\)?"""
    )

    private val MATH_SYMBOL_REGEX = Regex(
        """(\$.*?\$|\\frac|\\sqrt|\\sum|\\int|[√∑∫πθ±×÷≤≥≠°²³⁴⁵⁶⁷⁸⁹⁰₀₁₂₃₄₅₆₇₈₉])"""
    )

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(BENGALI_DIGITS[ch] ?: ch)
        }
        return sb.toString()
    }

    fun detectLanguage(text: String): String {
        val bengaliCount = text.count { it in '\u0980'..'\u09FF' }
        val latinCount = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        return if (bengaliCount >= latinCount / 3 && bengaliCount > 0) "bn" else "en"
    }

    fun containsMathSymbols(text: String): Boolean = MATH_SYMBOL_REGEX.containsMatchIn(text)

    fun mapOptionSymbol(rawSymbol: String): String? {
        val cleaned = rawSymbol.trim()
        return OPTION_MAP[cleaned] ?: OPTION_MAP[cleaned.uppercase()]
    }

    /**
     * Parses multi-page PDF text into structured MCQs, associating end-of-document / separate-page
     * answer keys with the matching question number. Missing or uncertain answers are never guessed.
     */
    fun parsePages(
        pages: List<PageTextInput>,
        config: McqParserConfig = McqParserConfig()
    ): List<ParsedMcq> {
        val answerKeyMap = extractGlobalAnswerKey(pages)
        val parsedList = mutableListOf<ParsedMcq>()

        var currentQNum = 0
        var currentPage = 1
        var currentFromOcr = false
        val currentRawLines = mutableListOf<String>()
        val stemLines = mutableListOf<String>()
        val optionsMap = linkedMapOf<String, String>()
        var currentAnswer: String? = null
        val explanationLines = mutableListOf<String>()
        var lastSection = "" // "stem", "optA", "optB", "optC", "optD", "exp"
        var inAnswerKeySection = false

        fun flushCurrentQuestion() {
            if (currentQNum <= 0 || stemLines.isEmpty()) return
            val stem = stemLines.joinToString(" ").replace(Regex("\\s+"), " ").trim()
            if (stem.length < config.minQuestionLength) return

            val optA = optionsMap["A"].orEmpty().trim()
            val optB = optionsMap["B"].orEmpty().trim()
            val optC = optionsMap["C"].orEmpty().trim()
            val optD = optionsMap["D"].orEmpty().trim()

            // Require at least 2 non-empty options to qualify as an MCQ
            val nonEmptyOptions = listOf(optA, optB, optC, optD).count { it.isNotBlank() }
            if (nonEmptyOptions < 2) return

            val resolvedAnswer = currentAnswer ?: answerKeyMap[currentQNum]
            val explanation = explanationLines.joinToString(" ").replace(Regex("\\s+"), " ").trim()
            val rawText = currentRawLines.joinToString("\n")
            val combinedText = "$stem $optA $optB $optC $optD $explanation"

            var confidence = 0.45
            if (nonEmptyOptions == 4) confidence += 0.25 else if (nonEmptyOptions == 3) confidence += 0.12
            if (resolvedAnswer in listOf("A", "B", "C", "D")) confidence += 0.25
            if (stem.length >= 15) confidence += 0.05
            if (currentFromOcr) confidence -= 0.08
            val boundedConfidence = confidence.coerceIn(0.1, 0.99)

            val hasValidAnswer = resolvedAnswer in listOf("A", "B", "C", "D")
            val isVerified = hasValidAnswer && nonEmptyOptions == 4 && config.autoVerifyHighConfidence && boundedConfidence >= 0.85

            parsedList.add(
                ParsedMcq(
                    questionNumber = currentQNum,
                    questionText = stem,
                    originalPdfText = rawText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctAnswer = if (hasValidAnswer) resolvedAnswer else null,
                    explanation = explanation,
                    language = detectLanguage(combinedText),
                    pageNumber = currentPage,
                    confidence = boundedConfidence,
                    verified = isVerified,
                    needsReview = !isVerified,
                    hasMath = containsMathSymbols(combinedText)
                )
            )
        }

        fun resetCurrent() {
            currentQNum = 0
            currentRawLines.clear()
            stemLines.clear()
            optionsMap.clear()
            currentAnswer = null
            explanationLines.clear()
            lastSection = ""
        }

        for (page in pages) {
            val rawLines = page.text.lines()
            inAnswerKeySection = false

            for (rawLine in rawLines) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) continue

                if (ANSWER_KEY_HEADER_REGEX.containsMatchIn(trimmed) && trimmed.length < 60) {
                    flushCurrentQuestion()
                    resetCurrent()
                    inAnswerKeySection = true
                    continue
                }

                // Skip standalone answer-key lines when inside উত্তরমালা section
                if (inAnswerKeySection) {
                    val qMatch = QUESTION_START_REGEX.find(trimmed)
                    if (qMatch != null && qMatch.groupValues[2].length > 20) {
                        inAnswerKeySection = false
                    } else {
                        continue
                    }
                }

                // Check explanation line
                val expMatch = EXPLANATION_LINE_REGEX.find(trimmed)
                if (expMatch != null && currentQNum > 0) {
                    currentRawLines.add(trimmed)
                    explanationLines.add(expMatch.groupValues[1].trim())
                    lastSection = "exp"
                    continue
                }

                // Check inline answer line
                val ansMatch = ANSWER_LINE_REGEX.find(trimmed)
                if (ansMatch != null && currentQNum > 0) {
                    currentRawLines.add(trimmed)
                    currentAnswer = mapOptionSymbol(ansMatch.groupValues[1])
                    lastSection = "ans"
                    continue
                }

                // Check if line starts a new numbered question
                val qStart = QUESTION_START_REGEX.find(trimmed)
                if (qStart != null) {
                    val numStr = normalizeDigits(qStart.groupValues[1])
                    val qNum = numStr.toIntOrNull()
                    val body = qStart.groupValues[2].trim()
                    val firstTokenLooksLikeNumericOption =
                        qNum != null && qNum in 1..4 && optionsMap.isNotEmpty() && !optionsMap.containsKey(
                            when (qNum) {
                                1 -> "A"; 2 -> "B"; 3 -> "C"; else -> "D"
                            }
                        ) && config.allowNumericOptions && currentQNum > 0

                    if (!firstTokenLooksLikeNumericOption && qNum != null && qNum > 0) {
                        flushCurrentQuestion()
                        resetCurrent()
                        currentQNum = qNum
                        currentPage = page.pageNumber
                        currentFromOcr = page.fromOcr
                        currentRawLines.add(trimmed)

                        // Handle case where options appear inline on the same line as the question
                        val splitInline = body.split(INLINE_OPTION_SPLIT_REGEX)
                        stemLines.add(splitInline.first().trim())
                        lastSection = "stem"
                        for (part in splitInline.drop(1)) {
                            val optMatch = OPTION_START_REGEX.find(part.trim())
                            if (optMatch != null) {
                                val mapped = mapOptionSymbol(optMatch.groupValues[1])
                                if (mapped != null && isOptionAllowed(optMatch.groupValues[1], config)) {
                                    optionsMap[mapped] = optMatch.groupValues[2].trim()
                                    lastSection = "opt$mapped"
                                }
                            }
                        }
                        continue
                    }
                }

                // Check option line (possibly multiple options on one line e.g. "ক) ঢাকা খ) খুলনা")
                if (currentQNum > 0) {
                    val inlineParts = trimmed.split(INLINE_OPTION_SPLIT_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
                    val firstOptMatch = OPTION_START_REGEX.find(inlineParts.firstOrNull().orEmpty())
                    if (firstOptMatch != null && isOptionAllowed(firstOptMatch.groupValues[1], config)) {
                        currentRawLines.add(trimmed)
                        for (part in inlineParts) {
                            val m = OPTION_START_REGEX.find(part)
                            if (m != null && isOptionAllowed(m.groupValues[1], config)) {
                                val key = mapOptionSymbol(m.groupValues[1])
                                if (key != null) {
                                    optionsMap[key] = m.groupValues[2].trim()
                                    lastSection = "opt$key"
                                }
                            }
                        }
                        continue
                    }

                    // Continuation line for stem, option, or explanation
                    currentRawLines.add(trimmed)
                    when (lastSection) {
                        "stem" -> stemLines.add(trimmed)
                        "optA" -> optionsMap["A"] = "${optionsMap["A"].orEmpty()} $trimmed".trim()
                        "optB" -> optionsMap["B"] = "${optionsMap["B"].orEmpty()} $trimmed".trim()
                        "optC" -> optionsMap["C"] = "${optionsMap["C"].orEmpty()} $trimmed".trim()
                        "optD" -> optionsMap["D"] = "${optionsMap["D"].orEmpty()} $trimmed".trim()
                        "exp" -> explanationLines.add(trimmed)
                    }
                }
            }
        }

        flushCurrentQuestion()
        return parsedList
    }

    private fun isOptionAllowed(symbol: String, config: McqParserConfig): Boolean {
        return when {
            symbol in listOf("ক", "খ", "গ", "ঘ") -> config.allowBengaliOptions
            symbol.uppercase() in listOf("A", "B", "C", "D") -> config.allowEnglishOptions
            symbol in listOf("1", "2", "3", "4", "১", "২", "৩", "৪") -> config.allowNumericOptions
            else -> false
        }
    }

    /**
     * Scans separate answer key sections across all pages (e.g., "উত্তরমালা: ১. খ ২. ক")
     * and maps question numbers to A/B/C/D.
     */
    fun extractGlobalAnswerKey(pages: List<PageTextInput>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        var collecting = false

        for (page in pages) {
            for (rawLine in page.text.lines()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                if (ANSWER_KEY_HEADER_REGEX.containsMatchIn(line)) {
                    collecting = true
                }
                if (collecting) {
                    for (match in ANSWER_KEY_PAIR_REGEX.findAll(line)) {
                        val qNum = normalizeDigits(match.groupValues[1]).toIntOrNull()
                        val opt = mapOptionSymbol(match.groupValues[2])
                        if (qNum != null && qNum > 0 && opt != null) {
                            result[qNum] = opt
                        }
                    }
                }
            }
        }
        return result
    }
}
