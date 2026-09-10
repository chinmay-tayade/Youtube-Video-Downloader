"""Bridge between the Android app and yt-dlp.

Every public function returns a JSON *string* so the Kotlin side never has to
deal with Python objects, ``None`` or malformed tuples. Nothing here raises:
failures are reported as ``{"ok": false, "error": ...}``.
"""

import json

import yt_dlp


# --------------------------------------------------------------------------- #
# helpers
# --------------------------------------------------------------------------- #
def _safe_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def _base_opts():
    return {
        "no_warnings": True,
        "quiet": True,
        "noprogress": True,
        "nocheckcertificate": True,
        "retries": 3,
        "fragment_retries": 3,
        "socket_timeout": 30,
        # We never invoke ffmpeg on device, so make that explicit and let yt-dlp
        # fall back to a single progressive stream instead of erroring out.
        "prefer_ffmpeg": False,
        # The mobile clients still expose muxed progressive streams (itag 18/22)
        # and don't need the JS "n" challenge solved, which keeps downloads
        # working from data-centre IPs and on the pinned yt-dlp build.
        "extractor_args": {
            "youtube": {"player_client": ["android", "ios", "web"]}
        },
    }


def _dumps(payload):
    return json.dumps(payload, ensure_ascii=False)


def _thumb(info):
    direct = info.get("thumbnail")
    if direct:
        return str(direct)
    thumbs = info.get("thumbnails") or []
    for t in reversed(thumbs):
        url = t.get("url") if isinstance(t, dict) else None
        if url:
            return str(url)
    return ""


# --------------------------------------------------------------------------- #
# metadata
# --------------------------------------------------------------------------- #
def get_video_info(video_url):
    """Return lightweight metadata for a single video."""
    result = {
        "ok": False,
        "id": "",
        "title": "",
        "thumbnail": "",
        "uploader": "",
        "webpage_url": "",
        "views": 0,
        "likes": 0,
        "duration": 0,
        "error": "",
    }

    if not video_url:
        result["error"] = "No URL provided"
        return _dumps(result)

    opts = _base_opts()
    opts.update({"skip_download": True})

    try:
        # process=False skips yt-dlp's format-selection step, which would
        # otherwise try a bestvideo+bestaudio merge and fail with no ffmpeg.
        # The metadata we need is already in the raw info dict.
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(video_url, download=False, process=False) or {}
        if info.get("_type") == "playlist":
            entries = list(info.get("entries") or [])
            info = entries[0] if entries else {}

        result["ok"] = True
        result["id"] = str(info.get("id") or "")
        result["title"] = str(info.get("title") or "Unknown title")
        result["thumbnail"] = _thumb(info)
        result["uploader"] = str(info.get("uploader") or info.get("channel") or "")
        result["webpage_url"] = str(info.get("webpage_url") or video_url)
        result["views"] = _safe_int(info.get("view_count"))
        result["likes"] = _safe_int(info.get("like_count"))
        result["duration"] = _safe_int(info.get("duration"))
    except Exception as e:  # noqa: BLE001 - surface every yt-dlp failure to the UI
        result["error"] = str(e)

    return _dumps(result)


# --------------------------------------------------------------------------- #
# format discovery
# --------------------------------------------------------------------------- #
def probe_formats(video_url):
    """Report which downloadable formats are available *without ffmpeg*.

    Only two kinds of stream can be saved directly on device:

    * progressive video  – has both a video and an audio codec (max ~720p on
      YouTube), and
    * audio-only          – has an audio codec and no video codec.

    DASH video-only streams need an ffmpeg merge step and are excluded.
    """
    result = {
        "ok": False,
        "id": "",
        "title": "",
        "thumbnail": "",
        "uploader": "",
        "webpage_url": "",
        "views": 0,
        "likes": 0,
        "duration": 0,
        "heights": [],
        "audio": [],
        "error": "",
    }

    if not video_url:
        result["error"] = "No URL provided"
        return _dumps(result)

    opts = _base_opts()
    opts.update({"skip_download": True})

    try:
        # process=False keeps yt-dlp from running format selection (which would
        # attempt a merge and fail with no ffmpeg on device). The raw info dict
        # still carries the full `formats` list and all the metadata we show.
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(video_url, download=False, process=False) or {}
        if info.get("_type") == "playlist":
            entries = list(info.get("entries") or [])
            info = entries[0] if entries else {}

        result["ok"] = True
        result["id"] = str(info.get("id") or "")
        result["title"] = str(info.get("title") or "Unknown title")
        result["thumbnail"] = _thumb(info)
        result["uploader"] = str(info.get("uploader") or info.get("channel") or "")
        result["webpage_url"] = str(info.get("webpage_url") or video_url)
        result["views"] = _safe_int(info.get("view_count"))
        result["likes"] = _safe_int(info.get("like_count"))
        result["duration"] = _safe_int(info.get("duration"))

        heights = set()
        audio_exts = set()
        for fmt in info.get("formats") or []:
            vcodec = fmt.get("vcodec") or "none"
            acodec = fmt.get("acodec") or "none"
            has_video = vcodec != "none"
            has_audio = acodec != "none"
            if has_video and has_audio:
                h = _safe_int(fmt.get("height"))
                if h > 0:
                    heights.add(h)
            elif has_audio and not has_video:
                ext = fmt.get("ext")
                if ext:
                    audio_exts.add(str(ext))

        result["heights"] = sorted(heights, reverse=True)
        # Keep the ones we actually build selectors for, best first.
        ordered = [e for e in ("m4a", "mp3", "opus", "webm") if e in audio_exts]
        result["audio"] = ordered or (["m4a"] if info.get("formats") else [])
    except Exception as e:  # noqa: BLE001
        result["ok"] = False
        result["error"] = str(e)

    return _dumps(result)


# --------------------------------------------------------------------------- #
# download
# --------------------------------------------------------------------------- #
def download(video_url, destination_dir, format_selector, callback):
    """Download ``video_url`` into ``destination_dir`` using ``format_selector``.

    ``callback`` is a Kotlin object exposing:

    * ``onProgress(json)`` – called repeatedly with a small progress payload, and
    * ``isCancelled()``    – polled between chunks; when true the download aborts.

    Returns a JSON string: ``{ok, message, file_path, ext, filesize}``.
    """
    result = {"ok": False, "message": "", "file_path": "", "ext": "", "filesize": 0}

    if not video_url:
        result["message"] = "No URL provided"
        return _dumps(result)
    if not destination_dir:
        result["message"] = "No download destination provided"
        return _dumps(result)

    import os

    try:
        os.makedirs(destination_dir, exist_ok=True)
    except OSError as e:
        result["message"] = "Cannot create destination folder: {}".format(e)
        return _dumps(result)

    def _hook(d):
        if callback is not None and callback.isCancelled():
            raise yt_dlp.utils.DownloadCancelled()
        if callback is None:
            return
        status = d.get("status")
        payload = {
            "status": status,
            "downloaded_bytes": _safe_int(d.get("downloaded_bytes")),
            "total_bytes": _safe_int(
                d.get("total_bytes") or d.get("total_bytes_estimate")
            ),
            "speed": _safe_int(d.get("speed")),
            "eta": _safe_int(d.get("eta")),
            "fragment_index": _safe_int(d.get("fragment_index")),
            "fragment_count": _safe_int(d.get("fragment_count")),
        }
        try:
            callback.onProgress(_dumps(payload))
        except Exception:  # noqa: BLE001 - never let a UI hiccup kill the download
            pass

    opts = _base_opts()
    opts.update(
        {
            "outtmpl": os.path.join(destination_dir, "%(title).150B [%(id)s].%(ext)s"),
            "format": format_selector or "best",
            "progress_hooks": [_hook],
            "restrictfilenames": False,
            "windowsfilenames": True,
            "overwrites": True,
        }
    )

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(video_url, download=True) or {}
            if info.get("_type") == "playlist":
                entries = info.get("entries") or []
                info = entries[0] if entries else {}
            file_path = ""
            requested = info.get("requested_downloads") or []
            if requested:
                file_path = requested[0].get("filepath") or ""
            if not file_path:
                file_path = ydl.prepare_filename(info)

        result["ok"] = True
        result["message"] = "Download complete"
        result["file_path"] = file_path or ""
        result["ext"] = str(info.get("ext") or "")
        try:
            result["filesize"] = os.path.getsize(file_path) if file_path else 0
        except OSError:
            result["filesize"] = 0
    except yt_dlp.utils.DownloadCancelled:
        result["ok"] = False
        result["message"] = "Cancelled"
    except Exception as e:  # noqa: BLE001
        result["ok"] = False
        result["message"] = str(e)

    return _dumps(result)


# Backwards-compatible alias for the previous entry point.
def download_video(video_url, download_destination):
    return download(video_url, download_destination, "best", None)
