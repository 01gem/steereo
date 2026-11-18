# Steereo 🎵

Steereo is a modern, feature-rich offline music player for Android, built with **Jetpack Compose** and **Jetpack Media3**. It provides a smooth, responsive audio playback experience with robust local file scanning, playlist management, and a clean Material 3 user interface.

---

## ✨ Key Features

- **Local Music Library & Scanner**: Automatically discovers and indexes audio files from device storage with robust error handling and file filtering.
- **Advanced Audio Playback**: Powered by **Jetpack Media3 ExoPlayer**, supporting background playback, lock screen controls, and media notifications.
- **Playlists & Queue Management**: Create custom playlists, manage playback queues, and organize your favorite tracks.
- **Modern Material 3 UI**: Clean, intuitive interface crafted entirely with Jetpack Compose, supporting responsive layouts and smooth navigation.
- **Settings & Preferences**: Persistent settings management using Preferences DataStore.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Playback**: [Jetpack Media3 (ExoPlayer)](https://developer.android.com/media/media3)
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room) & DataStore
- **Concurrency**: Kotlin Coroutines & Flow
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern

---

## 📱 Project Structure

```
app/src/main/java/com/gem/steereo/
├── data/               # Room database, DAOs, entities, repositories, and media scanner
├── playback/           # Media3 playback service and player connection manager
├── ui/                 # Jetpack Compose screens (Library, Player, Playlists) and ViewModels
└── MainActivity.kt     # Entry point and navigation host
```

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/01gem/steereo.git
   ```
2. Open the project in **Android Studio** (Giraffe or newer recommended).
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device (API 26+).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
