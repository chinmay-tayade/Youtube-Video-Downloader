import json
import os

import yt_dlp


def _safe_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def get_video_info(video_url):
    """Return a JSON string with video metadata.

    Always returns a JSON object so the Android side never has to deal with
    None / malformed tuples. On failure `ok` is False and `error` is set.
    """
    result = {
        "ok": False,
        "title": "",
        "thumbnail": "",
        "views": 0,
        "likes": 0,
        "duration": 0,
        "error": "",
    }

    if not video_url:
        result["error"] = "No URL provided"
        return json.dumps(result)

    ydl_opts = {
        "skip_download": True,
        "no_warnings": True,
        "quiet": True,
        # Progressive stream: no ffmpeg merge step required on device.
        "format": "best",
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(video_url, download=False) or {}

        result["ok"] = True
        result["title"] = str(info.get("title") or "Unknown title")
        result["thumbnail"] = str(info.get("thumbnail") or "")
        result["views"] = _safe_int(info.get("view_count"))
        result["likes"] = _safe_int(info.get("like_count"))
        result["duration"] = _safe_int(info.get("duration"))
    except Exception as e:  # noqa: BLE001 - surface any yt-dlp failure to the UI
        result["ok"] = False
        result["error"] = str(e)

    return json.dumps(result)


def download_video(video_url, download_destination):
    """Download a video. Returns a JSON string describing the outcome."""
    result = {"ok": False, "message": "", "file_path": ""}

    if not video_url:
        result["message"] = "No URL provided"
        return json.dumps(result)

    if not download_destination:
        result["message"] = "No download destination provided"
        return json.dumps(result)

    try:
        os.makedirs(download_destination, exist_ok=True)
    except OSError as e:
        result["message"] = "Cannot create destination folder: {}".format(e)
        return json.dumps(result)

    ydl_opts = {
        "outtmpl": os.path.join(download_destination, "%(title)s.%(ext)s"),
        "no_warnings": True,
        "quiet": True,
        "noprogress": True,
        # Progressive stream: no ffmpeg merge step required on device.
        "format": "best",
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(video_url, download=True) or {}
            file_path = ydl.prepare_filename(info)

        result["ok"] = True
        result["message"] = "Video downloaded successfully."
        result["file_path"] = file_path or ""
    except Exception as e:  # noqa: BLE001
        result["ok"] = False
        result["message"] = str(e)

    return json.dumps(result)
