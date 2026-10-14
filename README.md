# Video Backup

This repository is a starter Android project for a "Video Backup" app that downloads video links and uploads them to Telegram.

What is included
- Android app scaffold with UI and service structure
- Settings persistence for Telegram bot token, chat ID, and optional custom Bot API server URL
- Foreground service skeleton for batch processing
- yt-dlp runner + HTTP fallback placeholder
- Telegram uploader skeleton with file-splitting logic for oversized files
- Share-intent and batch-URL text-file hints

Important notes
- This repository is a foundation, not a complete production-ready app.
- A real deployment needs a bundled yt-dlp binary for Android (or Chaquopy/embedded Python).
- Telegram Bot API limits and permissions must be validated on device.
- The app should be used only for content that the user owns or has permission to save/upload.

Project structure
- `app/` — Android application module
- `README.md` — project overview

Next steps
1. Open this project in Android Studio.
2. Sync Gradle.
3. Add a compatible Android yt-dlp binary or use a Python-based implementation.
4. Add UX polish, log UI, and background processing details for production use.

See source code in `app/src/main/java/com/example/videobackup` for the starter implementation.
