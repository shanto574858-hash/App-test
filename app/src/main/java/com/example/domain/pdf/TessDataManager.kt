package com.example.domain.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TessDataManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun getTessDataParentDir(): File = context.filesDir

    fun getTessDataDir(): File = File(context.filesDir, "tessdata").apply {
        if (!exists()) mkdirs()
    }

    fun isLanguageAvailable(langCode: String): Boolean {
        val file = File(getTessDataDir(), "$langCode.traineddata")
        return file.exists() && file.length() > 100_000L
    }

    fun areBengaliAndEnglishReady(): Boolean =
        isLanguageAvailable("ben") && isLanguageAvailable("eng")

    suspend fun ensureTrainedData(
        languages: List<String> = listOf("ben", "eng"),
        onProgress: (String) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = getTessDataDir()
            for (lang in languages) {
                val target = File(dir, "$lang.traineddata")
                if (target.exists() && target.length() > 100_000L) continue

                onProgress("Downloading OCR language pack: $lang...")
                val url = "https://github.com/tesseract-ocr/tessdata_fast/raw/main/$lang.traineddata"
                val request = Request.Builder().url(url).build()
                val tmp = File(dir, "$lang.traineddata.tmp")

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("OCR traineddata download failed ($lang): HTTP ${response.code}")
                        )
                    }
                    val body = response.body ?: return@withContext Result.failure(
                        IllegalStateException("Empty OCR response body for $lang")
                    )
                    body.byteStream().use { input ->
                        FileOutputStream(tmp).use { output ->
                            input.copyTo(output, bufferSize = 16 * 1024)
                        }
                    }
                }
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) {
                    return@withContext Result.failure(
                        IllegalStateException("Failed to save $lang.traineddata")
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
