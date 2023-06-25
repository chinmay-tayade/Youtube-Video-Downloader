import yt_dlp
import os

def download_video(video_url, download_destination):
    ydl_opts = {
        'outtmpl': os.path.join(download_destination, '%(title)s.%(ext)s'),
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        ydl.download([video_url])
