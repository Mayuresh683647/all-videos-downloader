package com.example.videobackup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileSplitter {
    fun splitFile(file: File, maxPartSizeBytes: Long): List<File> {
        val partFiles = mutableListOf<File>()
        val bufferSize = 4 * 1024 * 1024
        val input = FileInputStream(file)
        var partIndex = 1
        var partOutput: FileOutputStream? = null
        var currentPartFile: File? = null

        try {
            val buffer = ByteArray(bufferSize)
            var read: Int
            var bytesThisPart = 0L

            while (input.read(buffer).also { read = it } != -1) {
                if (partOutput == null || bytesThisPart >= maxPartSizeBytes) {
                    partOutput?.close()
                    currentPartFile = File(file.parentFile, "${file.name}.part${String.format("%03d", partIndex++)}")
                    partFiles.add(currentPartFile)
                    partOutput = FileOutputStream(currentPartFile)
                    bytesThisPart = 0L
                }

                var offset = 0
                while (offset < read) {
                    val remaining = maxPartSizeBytes - bytesThisPart
                    val chunkLen = minOf(read - offset, remaining.toInt())
                    partOutput!!.write(buffer, offset, chunkLen)
                    offset += chunkLen
                    bytesThisPart += chunkLen.toLong()
                    if (bytesThisPart >= maxPartSizeBytes) break
                }
            }
        } finally {
            partOutput?.close()
            input.close()
        }

        return partFiles
    }
}
