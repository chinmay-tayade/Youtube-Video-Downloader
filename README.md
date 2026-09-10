# YouTube Video Downloader

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**An Android video / audio downloader — single-Activity Jetpack Compose, a
foreground download service with live progress, a Room-backed download library,
and a `yt-dlp` engine embedded through Chaquopy.**

</div>

---

## Contents

- [About](#about)
- [Screens & flow](#screens--flow)
- [Features](#features)
- [Architecture](#architecture)
- [How a download works](#how-a-download-works-end-to-end)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Design decisions & trade-offs](#design-decisions--trade-offs)
- [Known limitations](#known-limitations)
- [Building](#building)
- [Testing](#testing)
- [Permissions](#permissions)
- [Roadmap](#roadmap)
- [Legal](#legal) · [License](#license) · [Author](#author)

---

## About

Paste (or **share**) a link, pick a quality, and the app downloads it in the
background with a live progress notification. Finished files land in a folder of
your choosing and appear in Files / Gallery. Every download is recorded in an
on-device library where you can open, share, retry or delete it.

The download engine is [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) running in an
embedded **Python 3.12** runtime ([Chaquopy](https://chaquo.com/chaquopy/)). The
app never ships `ffmpeg`, so it only offers formats that save **without a merge
or conversion step**:

- **progressive video** — video + audio already muxed (up to ~720p on YouTube), and
- **audio-only** streams — M4A / Opus / WebM.

When a site exposes no standalone audio stream to an unauthenticated client, the
audio option transparently falls back to the muxed progressive stream so the
download still succeeds.

---

## Screens & flow

```
 ┌────────────┐  Fetch details   ┌──────────────┐  Download   ┌───────────────┐
 │    Home     │ ───────────────▶ │   Formats     │ ──────────▶ │   Downloads    │
 │ URL + folder │ ◀─────────────── │ quality/audio │            │ progress +     │
 └────────────┘      back          └──────────────┘            │ library        │
        │                                                      └───────────────┘
        └── top bar ──▶ Downloads · Settings
```

| Screen | What it does |
|--------|--------------|
| **Home** | URL field (paste / clipboard / share-sheet), destination folder picker, storage-permission prompt, **Fetch details**. |
| **Formats** | Video info card (thumbnail, title, channel, views, likes, duration) + a chip picker of the *real* progressive heights and audio containers `yt-dlp` reports. |
| **Downloads** | *In progress* (progress bar, speed / ETA, cancel) and *Completed / Failed* (size, date, tap-to-open, overflow → share / retry / delete). Clear-finished. Empty state. |
| **Settings** | Default download folder, light / dark / system theme (applied live), preferred audio format. |

Back navigation is a genuine Navigation-Compose back stack: back on Home leaves
the app, back on any other screen pops to the previous one, and a running
download is unaffected by any of it.

---

## Features

- **Format & quality picker** driven by the formats `yt-dlp` actually reports —
  no hard-coded resolution list.
- **Foreground download service** — downloads keep running when the app is
  backgrounded, the Activity is recreated, or the user presses back. One job at a
  time; the rest queue. Interrupted rows are recovered on next launch.
- **Live progress** — real downloaded-bytes / total / speed / ETA from `yt-dlp`
  progress hooks, throttled and surfaced in both the notification and the
  Downloads screen.
- **Cancel** from the notification action or the list; partial files are removed.
- **Downloads library** (Room) — status, size, date; open & share via
  `FileProvider`, retry failed jobs, delete (with file), clear finished.
- **Share-sheet intake** — share a link from YouTube or a browser straight into
  the app; `https://youtu.be/…` / `youtube.com` links open it too.
- **Single-Activity Navigation Compose** with predictive-back support.
- **Settings** persisted with DataStore; theme switch applies without a restart.
- Runtime permission handling for `POST_NOTIFICATIONS` (13+) and all-files access.
- Finished files are `MediaScanner`-indexed so they show up in Gallery / Files
  immediately.

---

## Architecture

Single-Activity **MVVM** with unidirectional state. Every ViewModel exposes a
`StateFlow`; the UI is a pure function of it.

```
MainActivity ──hosts──▶ AppNavHost (Navigation Compose)
                          ├─ HomeScreen        ── HomeViewModel
                          ├─ FormatsScreen     ── FormatsViewModel
                          ├─ DownloadsScreen   ── DownloadsViewModel
                          └─ SettingsScreen    ── SettingsViewModel
                                    │
                                    ▼
                        DownloadRepository  (process singleton — single source of truth)
                          ├─ Room  (AppDatabase / DownloadDao)     durable records
                          ├─ StateFlow<Map<id, DownloadProgress>>  live byte progress
                          ├─ ConcurrentHashMap<id>                 cancellation flags
                          └─ start / cancel ──▶ DownloadService (foreground, serial queue)
                                                      │
                                                      ▼
                             PythonDownloader ──▶ video_downloader.py ──▶ yt-dlp
```

- **`DownloadRepository`** is the only thing that talks to the service. It merges
  the durable Room rows with the transient in-memory progress into one
  `Flow<List<DownloadRecord>>` the UI collects — the screens never touch Room or
  the service directly.
- **`DownloadService`** is a `LifecycleService` that drains the queue one job at
  a time on a service-scoped coroutine, keeps the foreground notification in
  sync, and stops itself (`stopSelf(startId)`) when the queue is empty.
- **`PythonDownloader`** is an exception-safe bridge: every Python call returns a
  fully-formed result object, and the Python side always emits JSON.

---

## How a download works (end to end)

1. **Home** validates the URL and navigates to `formats/{base64(url)}`.
2. **`FormatsViewModel.load`** calls `PythonDownloader.probeFormats`, which runs
   `yt_dlp` with `process=False` — this skips yt-dlp's format-selection step (it
   would otherwise attempt a `bestvideo+bestaudio` merge and fail with no
   ffmpeg) while still returning the full `formats` list and metadata.
3. The user picks a `MediaSelection`; **`util/FormatSpec.kt`** (pure, unit-tested)
   turns it into a yt-dlp selector string, e.g.
   `best[height<=720][ext=mp4][vcodec!=none][acodec!=none]/…/best/worst`.
4. **`DownloadRepository.enqueue`** inserts a `QUEUED` Room row and calls
   `ContextCompat.startForegroundService`.
5. **`DownloadService`** promotes itself to the foreground (typed `dataSync`
   service on API 34), then runs `PythonDownloader.download(url, dir, selector,
   sink)` on `Dispatchers.IO`.
6. `sink` is a Kotlin object passed into Python. yt-dlp's `progress_hooks` call
   `sink.onProgress(json)` on every chunk and `sink.isCancelled()` between
   chunks; the service throttles those to a `StateFlow` update + a notification
   update (~600 ms) and a persisted percent.
7. On completion **`DownloadRepository.finish`** writes the terminal row
   (`COMPLETED` / `FAILED` / `CANCELLED`), the service scans the file into
   MediaStore and posts a result notification, then drains the next job or stops.

---

## Project structure

```
app/src/main/
├── java/com/chinmay/tayade/mp3downloader/
│   ├── MainActivity.kt                     single Activity, intent parsing, theme
│   ├── App.kt                              Python + Room + notification-channel init
│   ├── ui/
│   │   ├── navigation/                     AppNavHost, Destinations
│   │   ├── screens/                        Home / Formats / Downloads / Settings
│   │   ├── components/                     AppTopBar, VideoInfoCard, EmptyState, …
│   │   └── theme/                          Material 3 colour scheme + typography
│   ├── viewmodel/                          one AndroidViewModel per screen
│   ├── data/
│   │   ├── model/Models.kt                 VideoInfo, FormatOptions, MediaSelection, …
│   │   ├── local/                          Room: AppDatabase, DownloadDao, DownloadEntity
│   │   ├── DownloadRepository.kt           single source of truth
│   │   ├── PythonDownloader.kt             Chaquopy ⇄ Kotlin bridge
│   │   ├── SettingsStore.kt                DataStore preferences
│   │   └── service/                        DownloadService, NotificationHelper
│   └── util/                               FormatSpec, StoragePaths, Permissions,
│                                           FileActions, Format (byte/time helpers)
├── python/video_downloader.py              get_video_info · probe_formats · download
└── res/                                    strings, themes, file_paths, icons
```

---

## Tech stack

| Area          | Choice |
|---------------|--------|
| Language      | Kotlin 1.9, Coroutines + Flow |
| UI            | Jetpack Compose (BOM 2024.06), Material 3, Navigation Compose 2.7 |
| Architecture  | MVVM, single Activity, `AndroidViewModel` + repository, unidirectional state |
| Persistence   | Room 2.6 (KSP), DataStore Preferences (settings) |
| Background    | Foreground `LifecycleService` + `NotificationCompat` |
| Download core | current `yt-dlp` via Chaquopy 15 / embedded Python 3.12 |
| Images        | Coil |
| Build tools   | AGP 8.5, Gradle 8.7, KSP, JDK 17 |
| Min / target  | SDK 24 (Android 7) / SDK 34 (Android 14) |

---

## Design decisions & trade-offs

| Decision | Why | Trade-off |
|----------|-----|-----------|
| **Repository owns the service, screens own nothing** | One source of truth; a screen can be destroyed and re-created mid-download with zero state loss. | One process-singleton to reason about. |
| **Foreground service, not `WorkManager`** | The job is user-initiated, needs a live progress UI *now*, and must not be deferred by Doze. | We manage the queue and lifecycle ourselves. |
| **No ffmpeg** | `FFmpegKit` is retired and pulled from Maven Central; a static binary adds ~30 MB and licensing weight. | 1080p+/4K need a merge, so only progressive heights are offered; "audio only" can fall back to a small muxed file. |
| **`yt-dlp` selector strings, not exact format IDs** | IDs change per request and per client; selectors like `best[height<=720]…/best/worst` degrade gracefully. | Slightly less precise than pinning an itag. |
| **Chaquopy / embedded Python** | `yt-dlp` is the only extractor that keeps up with YouTube; re-implementing it in Kotlin is not realistic. | ~40 MB Python runtime in the APK; must track Python/`yt-dlp` versions. |
| **`process=False` for the metadata probe** | Avoids yt-dlp running format selection (and failing on the missing merge) just to read titles/formats. | Relies on the extractor populating `formats` before processing (it does for YouTube). |
| **Manual DI (Application-scoped singletons)** | Keeps the module graph obvious for a small app; no extra annotation processor. | Would move to Hilt as the surface grows. |

---

## Known limitations

- **YouTube ≥ 1080p / true MP3** need an ffmpeg merge/transcode — out of scope (see roadmap).
- **Standalone audio** is increasingly gated behind an attestation token; from
  data-centre IPs (incl. the Android emulator) the app falls back to the muxed
  progressive stream, so an "audio" download can arrive as a small `.mp4`.
- **Playlists** download only the first entry.
- `MANAGE_EXTERNAL_STORAGE` is used so `yt-dlp` can write to an arbitrary
  user-picked folder via a real path; a Play-Store build would switch to
  `MediaStore` + a fixed app directory.

---

## Building

Requires **JDK 17** and a local **Python 3.12** on `PATH` — Chaquopy uses it to
resolve the `yt-dlp` wheel (`Requires-Python >=3.10`). On macOS:
`brew install python@3.12`. Recent Android Studio ships a compatible JDK.

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # R8 + resource shrinking (keep rules in proguard-rules.pro)
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # Android lint
./gradlew :app:installDebug         # build + install on a connected device/emulator
```

The first build downloads the Python 3.12 runtime and the current `yt-dlp`
wheel via Chaquopy, so it takes a few minutes.

---

## Testing

Pure logic is unit-tested off-device (`app/src/test`):

- **`FormatSpecTest`** — every `MediaSelection` → the expected yt-dlp selector,
  including the height cap and the progressive fallback.
- **`FormatUtilTest`** — `formatBytes` / `formatDuration` / `formatCountAbbreviated`
  / `formatSpeed`, plus framework-free URL parsing (`hostOf`, `isYouTubeUrl`) for
  `youtu.be`, `m.` / `music.` hosts, and userinfo/port URLs.

```bash
./gradlew :app:testDebugUnitTest
```

Verified end-to-end on a Pixel 9a emulator (API 34): fetch → 360p video download
and audio-fallback download both complete, appear in the library, and open in the
system player; foreground notification, cancel, share-intake and back navigation
all behave.

---

## Permissions

| Permission | Why |
|------------|-----|
| `INTERNET`, `ACCESS_NETWORK_STATE` | download |
| `POST_NOTIFICATIONS` (13+) | progress + result notifications |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | background downloads (typed FGS on API 34) |
| `MANAGE_EXTERNAL_STORAGE` | `yt-dlp` writes through a plain filesystem path, so the app needs write access to the folder the user picks |

---

## Roadmap

- [ ] Bundle `ffmpeg` for 1080p / 4K merge and true MP3 extraction
- [ ] Playlist / batch downloads
- [ ] Subtitle download
- [ ] Configurable parallel downloads
- [ ] Picture-in-Picture playback of finished files
- [ ] Hilt + a `:core` / `:feature` module split

---

## Legal

For personal use only. You are responsible for complying with copyright law and
the terms of service of the sites you download from.

## License

MIT — see [LICENSE](LICENSE).

## Author

**Chinmay Tayade** · [@Chinmay-tayade](https://github.com/Chinmay-tayade)
