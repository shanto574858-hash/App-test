package com.example.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class PdfExtractionSummary(
    val totalPages: Int,
    val ocrPagesCount: Int,
    val pageTexts: List<PageTextInput>,
    val modeUsed: String
)

class PdfTextAndOcrExtractor(
    private val context: Context,
    private val tessDataManager: TessDataManager = TessDataManager(context)
) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun getPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.pageCount
            }
        }
    }

    suspend fun renderPreviewPage(
        pdfFile: File,
        pageIndex: Int,
        maxWidthPx: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                renderer.openPage(pageIndex).use { page ->
                    val scale = (maxWidthPx.toFloat() / page.width.coerceAtLeast(1)).coerceIn(1f, 3f)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }

    suspend fun extractTextAndOcr(
        pdfFile: File,
        forceOcr: Boolean = false,
        onPageProgress: suspend (currentPage: Int, totalPages: Int, statusMsg: String) -> Unit = { _, _, _ -> }
    ): Result<PdfExtractionSummary> = withContext(Dispatchers.IO) {
        var tessApi: TessBaseAPI? = null
        var ocrCount = 0
        val pages = mutableListOf<PageTextInput>()

        try {
            PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly()).use { pdDoc ->
                val totalPages = pdDoc.numberOfPages
                val stripper = PDFTextStripper()

                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { pdfRenderer ->
                        for (pageIdx in 0 until totalPages) {
                            coroutineContext.ensureActive()
                            val pageNumber = pageIdx + 1
                            onPageProgress(pageNumber, totalPages, "পৃষ্ঠা $pageNumber/$totalPages প্রসেস হচ্ছে...")

                            var extractedText = ""
                            if (!forceOcr) {
                                stripper.startPage = pageNumber
                                stripper.endPage = pageNumber
                                extractedText = stripper.getText(pdDoc)?.trim().orEmpty()
                            }

                            val needsOcr = forceOcr || extractedText.length < 40
                            if (needsOcr) {
                                if (tessApi == null) {
                                    if (!tessDataManager.areBengaliAndEnglishReady()) {
                                        val downloadRes = tessDataManager.ensureTrainedData(
                                            languages = listOf("ben", "eng")
                                        ) { msg ->
                                            // Logged or surfaced if needed
                                        }
                                        if (downloadRes.isFailure) {
                                            return@withContext Result.failure(
                                                downloadRes.exceptionOrNull()
                                                    ?: IllegalStateException("OCR language data (ben/eng) unavailable.")
                                            )
                                        }
                                    }
                                    val api = TessBaseAPI()
                                    val initialized = api.init(
                                        tessDataManager.getTessDataParentDir().absolutePath,
                                        "ben+eng"
                                    )
                                    if (!initialized) {
                                        api.recycle()
                                        return@withContext Result.failure(
                                            IllegalStateException("Tesseract OCR initialization failed for ben+eng.")
                                        )
                                    }
                                    tessApi = api
                                }

                                val bitmap = renderBitmapForOcr(pdfRenderer, pageIdx)
                                try {
                                    tessApi!!.setImage(bitmap)
                                    val ocrText = tessApi!!.utF8Text?.trim().orEmpty()
                                    extractedText = if (ocrText.isNotEmpty()) ocrText else extractedText
                                    ocrCount++
                                    pages.add(PageTextInput(pageNumber, extractedText, fromOcr = true))
                                } finally {
                                    bitmap.recycle()
                                }
                            } else {
                                pages.add(PageTextInput(pageNumber, extractedText, fromOcr = false))
                            }
                        }
                    }
                }

                val modeUsed = when {
                    ocrCount == 0 -> "text"
                    ocrCount == totalPages -> "ocr"
                    else -> "hybrid"
                }

                Result.success(
                    PdfExtractionSummary(
                        totalPages = totalPages,
                        ocrPagesCount = ocrCount,
                        pageTexts = pages,
                        modeUsed = modeUsed
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                tessApi?.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun renderBitmapForOcr(renderer: PdfRenderer, pageIndex: Int): Bitmap {
        renderer.openPage(pageIndex).use { page ->
            val maxDimension = 2048f
            val baseScale = 2.5f
            val targetWidth = (page.width * baseScale).coerceAtMost(maxDimension)
            val scale = targetWidth / page.width.coerceAtLeast(1)
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            return bitmap
        }
    }
}
