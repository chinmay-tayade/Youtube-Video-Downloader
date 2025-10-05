# YouTube Video Downloader

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**A high-performance media downloader for Android built with Kotlin**

[Report Bug](../../issues) • [Request Feature](../../issues)

</div>

---

## About

YouTube Video Downloader is a powerful Android application that enables users to download and manage video content efficiently. Built with modern Android development practices using Kotlin Coroutines for asynchronous operations, the app provides a seamless downloading experience with support for multiple formats, quality options, and background processing.

### Key Features

- **Native Kotlin** implementation for optimal performance
- **Coroutines-based** async operations for smooth UX
- **Background Downloads** with notification progress
- **Multi-format Support** for various media formats
- **Quality Selection** for different video resolutions
- **Download Manager** for organized media library

---

## Features

### Download Management
- Download videos in various formats (MP4, MKV, WEBM)
- Multiple quality options (4K, 1080p, 720p, 480p, 360p)
- Audio-only downloads (MP3, M4A, AAC)
- Thumbnail preview before downloading
- Video information display (duration, size, format)
- Resume interrupted downloads
- Pause and cancel active downloads

### Background Processing
- Download videos while using other apps
- Persistent foreground service for reliability
- Progress notifications with real-time updates
- Batch download queue management
- Automatic retry on network failure
- Smart bandwidth management

### Media Library
- Built-in media player for downloaded content
- Organized file management system
- Search and filter downloaded videos
- Sort by date, name, or size
- Delete or share downloaded files
- Storage location selection
- Internal/external storage support

### User Interface
- Material Design following Android guidelines
- Clean and intuitive download interface
- Real-time download progress tracking
- Quick download from clipboard URL
- History of downloaded videos
- Dark mode support

### Performance
- Efficient memory management
- Optimized network requests
- Low battery consumption
- Minimal storage overhead
- Fast download speeds with parallel connections
- Smart caching mechanism

---

## Tech Stack

### Core Technologies
- **Language:** Kotlin 1.9+
- **Architecture:** MVVM (Model-View-ViewModel)
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

### Asynchronous Programming
- **Kotlin Coroutines** - Structured concurrency
- **Flow** - Reactive data streams
- **StateFlow** - State management
- **Dispatchers** - Thread management

### Networking
- **OkHttp** - HTTP client for downloads
- **Retrofit** - REST API integration (for metadata)
- **Download Manager API** - Android's native download manager
- **Network Security** - SSL/TLS support

### Background Processing
- **Foreground Service** - Persistent background downloads
- **WorkManager** - Scheduled and deferred tasks
- **Notification Manager** - Download progress notifications
- **Broadcast Receivers** - Download completion handling

### Storage & File Management
- **File I/O** - Efficient file operations
- **MediaStore API** - Media file management
- **Storage Access Framework** - User-selected locations
- **Scoped Storage** - Android 10+ compatibility

### UI Components
- **RecyclerView** - Efficient list display
- **ViewBinding** - Type-safe view access
- **Material Components** - Modern UI elements
- **ConstraintLayout** - Responsive layouts
- **Animations** - Smooth transitions

### Dependency Injection
- **Hilt** - Dependency injection framework

### Database
- **Room Database** - Download history and metadata storage
- **SQLite** - Local data persistence

### Testing
- **JUnit** - Unit testing
- **MockK** - Mocking framework
- **Coroutines Test** - Testing async code
- **Espresso** - UI testing

---

## Architecture

The application follows **MVVM architecture** with clean separation:

```
app/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAOs
│   │   ├── entity/           # Database entities
│   │   └── database/         # Database instance
│   ├── repository/           # Repository implementations
│   └── model/                # Data models
│
├── domain/
│   ├── usecase/              # Business logic
│   │   ├── DownloadVideoUseCase.kt
│   │   ├── GetVideoInfoUseCase.kt
│   │   └── ManageDownloadsUseCase.kt
│   └── repository/           # Repository interfaces
│
├── presentation/
│   ├── ui/
│   │   ├── main/             # Main screen
│   │   ├── downloads/        # Downloads list
│   │   ├── settings/         # Settings screen
│   │   └── player/           # Media player
│   ├── viewmodel/            # ViewModels
│   └── adapter/              # RecyclerView adapters
│
├── service/
│   ├── DownloadService.kt    # Foreground download service
│   └── NotificationHelper.kt # Notification management
│
├── worker/
│   └── CleanupWorker.kt      # Periodic cleanup tasks
│
├── di/                       # Dependency injection
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
│
└── util/
    ├── FileUtils.kt          # File operations
    ├── NetworkUtils.kt       # Network helpers
    └── Extensions.kt         # Kotlin extensions
```

### Data Flow
```
UI Layer (Activities/Fragments)
    ↕
ViewModel (State Management)
    ↕
UseCase (Business Logic)
    ↕
Repository (Data Abstraction)
    ↕
Data Sources (Network + Local DB)
    ↕
Download Service (Background Operations)
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or higher
- Android SDK (API 24+)
- Kotlin 1.9+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Chinmay-tayade/Youtube-Video-Downloader.git
cd Youtube-Video-Downloader
```

2. **Open in Android Studio**
   - Launch Android Studio
   - File > Open
   - Select the project directory

3. **Sync Gradle**
   - Wait for Gradle to download dependencies

4. **Build the project**
```bash
./gradlew build
```

5. **Run the application**
   - Connect an Android device or start emulator
   - Run > Run 'app'

---

## Permissions

The app requires the following Android permissions:

```xml
<!-- Internet access for downloading -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Storage access for saving files -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Foreground service for background downloads -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Network state for connectivity checks -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## Usage

### Basic Download Flow

1. **Enter or paste video URL**
2. **Fetch video information** (title, thumbnail, formats)
3. **Select quality and format**
4. **Choose download location**
5. **Start download**
6. **Monitor progress** via notification
7. **Access downloaded file** in library

### Advanced Features

**Batch Downloads**
```kotlin
// Queue multiple videos
downloadManager.addToQueue(listOf(url1, url2, url3))
```

**Custom Download Location**
```kotlin
// Select custom storage location
val customPath = "/storage/emulated/0/MyVideos"
downloadManager.setDownloadPath(customPath)
```

**Download with Specific Quality**
```kotlin
// Download specific quality
downloadManager.download(url, quality = VideoQuality.HD_1080P)
```

---

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Build Variants
- `debug` - Debug build with logging
- `release` - Optimized release build
- `benchmark` - Build for performance testing

---

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew createDebugCoverageReport
```

---

## Performance Optimization

### Download Speed
- Parallel connection support
- Resume capability for interrupted downloads
- Smart chunk-based downloading
- Connection pooling with OkHttp

### Memory Management
- Efficient buffer management
- Stream-based file writing
- Automatic resource cleanup
- Memory leak prevention

### Battery Optimization
- Intelligent batching
- WiFi-only download option
- Battery-aware scheduling
- Foreground service optimization

---

## Code Quality

### Static Analysis
```bash
# Android Lint
./gradlew lint

# Kotlin lint
./gradlew ktlintCheck

# Detekt
./gradlew detekt
```

### Code Formatting
```bash
./gradlew ktlintFormat
```

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/NewFeature`)
3. Write tests for new functionality
4. Ensure code passes all checks
5. Commit with clear messages (`git commit -m 'Add NewFeature'`)
6. Push to branch (`git push origin feature/NewFeature`)
7. Open a Pull Request

---

## Roadmap

- [ ] Playlist download support
- [ ] Video format conversion
- [ ] Built-in subtitle downloader
- [ ] Channel subscription for auto-downloads
- [ ] Advanced filtering options
- [ ] Cloud backup integration
- [ ] Multi-language support
- [ ] Picture-in-Picture mode
- [ ] Chromecast support

---

## Legal Notice

This application is for educational purposes only. Users are responsible for ensuring they have the right to download any content. Respect copyright laws and terms of service of content platforms.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Author

**Chinmay Tayade**

- GitHub: [@Chinmay-tayade](https://github.com/Chinmay-tayade)
- LinkedIn: [chinmaytayade](https://linkedin.com/in/chinmaytayade)
- Email: chinmaytayade@outlook.com

---

## Acknowledgments

- Android development community for resources
- OkHttp team for robust networking library
- Kotlin team for excellent coroutines support
- All open-source contributors

---

<div align="center">

**Built with Kotlin and Coroutines**

Made by Chinmay Tayade

</div>
