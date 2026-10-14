package com.example.videobackup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var linkInput: EditText
    private lateinit var startButton: Button
    private lateinit var pickTxtButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val pickTxt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { startBatchFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        linkInput = findViewById(R.id.linkInput)
        startButton = findViewById(R.id.startButton)
        pickTxtButton = findViewById(R.id.pickTxtButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        handleSharedIntent(intent)

        startButton.setOnClickListener {
            val link = linkInput.text.toString().trim()
            if (link.isNotEmpty()) {
                val serviceIntent = Intent(this, DownloadUploadService::class.java).apply {
                    action = DownloadUploadService.ACTION_START_SINGLE
                    putExtra(DownloadUploadService.EXTRA_LINK, link)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }

        pickTxtButton.setOnClickListener {
            pickTxt.launch(arrayOf("text/plain"))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                linkInput.setText(sharedText.trim())
            }
        }
    }

    private fun startBatchFromUri(uri: Uri) {
        val serviceIntent = Intent(this, DownloadUploadService::class.java).apply {
            action = DownloadUploadService.ACTION_START_BATCH
            putExtra(DownloadUploadService.EXTRA_TEXT_URI, uri.toString())
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
