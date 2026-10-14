package com.example.videobackup

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

class TelegramUploader(private val settings: SettingsRepository) {
    private val client = OkHttpClient()

    private val officialLimit = 50L * 1024 * 1024
    private val customLimit = 2000L * 1024 * 1024

    fun uploadFile(file: File) {
        val token = settings.botToken ?: throw IllegalStateException("Bot token is required")
        val chatId = settings.chatId ?: throw IllegalStateException("Chat ID is required")
        val apiBase = settings.apiUrl?.trim()?.ifEmpty { null } ?: "https://api.telegram.org"
        val limit = if (settings.apiUrl.isNullOrBlank()) officialLimit else customLimit

        if (file.length() > limit) {
            val parts = FileSplitter.splitFile(file, limit)
            parts.forEachIndexed { index, part ->
                uploadDocument(apiBase, token, chatId, part, "Part ${index + 1}/${parts.size}")
            }
            return
        }

        val extension = file.extension.lowercase(Locale.getDefault())
        val isVideo = extension in setOf("mp4", "mkv", "webm", "mov", "avi")

        if (isVideo) {
            uploadVideo(apiBase, token, chatId, file)
        } else {
            uploadDocument(apiBase, token, chatId, file, null)
        }
    }

    private fun uploadVideo(apiBase: String, token: String, chatId: String, file: File) {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("video", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("$apiBase/bot$token/sendVideo")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Telegram sendVideo failed: ${response.code}")
            }
        }
    }

    private fun uploadDocument(apiBase: String, token: String, chatId: String, file: File, caption: String?) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))

        if (!caption.isNullOrBlank()) {
            builder.addFormDataPart("caption", caption)
        }

        val request = Request.Builder()
            .url("$apiBase/bot$token/sendDocument")
            .post(builder.build())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Telegram sendDocument failed: ${response.code}")
            }
        }
    }
}
