package com.example.videobackup

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class YtDlpRunner(private val context: Context) {
    private val client = OkHttpClient()

    fun download(link: String): File {
        // Prefer bundled yt-dlp if your app ships it.
        val ytDlpPath: String = try {
            getBundledPath()
        } catch (e: Exception) {
            "yt-dlp"
        }

        val outputDir = context.getExternalFilesDir("downloads") ?: context.filesDir
        outputDir.mkdirs()

        val targetFile = File(outputDir, "download_${link.hashCode()}.mp4")

        return try {
            val process = ProcessBuilder(
                ytDlpPath,
                "-o",
                targetFile.absolutePath,
                "--no-playlist",
                "--no-warnings",
                link
            ).redirectErrorStream(true).start()

            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    android.util.Log.d("YtDlpRunner", line ?: "")
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0 && targetFile.exists()) {
                targetFile
            } else {
                throw IllegalStateException("yt-dlp failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            android.util.Log.w("YtDlpRunner", "yt-dlp failed, using HTTP fallback: ${e.message}")
            httpFallback(link, targetFile)
        }
    }

    private fun getBundledPath(): String {
        val destination = File(context.filesDir, "yt-dlp")
        if (!destination.exists()) {
            try {
                context.assets.open("yt-dlp").use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
                destination.setExecutable(true)
            } catch (_: Exception) {
                // Asset not bundled; let the system try the PATH version.
            }
        }

        return if (destination.exists()) destination.absolutePath else "yt-dlp"
    }

    private fun httpFallback(link: String, targetFile: File): File {
        val request = Request.Builder().url(link).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP fallback failed: ${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return targetFile
    }
}
