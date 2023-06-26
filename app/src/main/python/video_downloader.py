import yt_dlp
import os

def download_video(video_url, download_destination):
    try:
        ydl_opts = {
            'outtmpl': os.path.join(download_destination, '%(title)s.%(ext)s'),
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(video_url, download=False)
            ydl.prepare_filename(info)

            # Download the video
            ydl.download([video_url])

            # Get the downloaded file path
            file_path = ydl.prepare_filename(info)
            return 1, "Video downloaded successfully.", file_path
    except Exception as e:
        return 0, str(e), None

def convert_to_mp3(video_file, mp3_destination):
    try:
        # Perform the conversion to MP3
        # Code to convert video_file to MP3 format
        return 2, "Video converted to MP3 successfully."
    except Exception as e:
        return 0, str(e)

def save_to_destination(mp3_file, destination_path):
    try:
        # Save the MP3 file to the specified destination path
        new_file_path = os.path.join(destination_path, os.path.basename(mp3_file))
        os.rename(mp3_file, new_file_path)
        return 3, "MP3 saved to destination folder successfully."
    except Exception as e:
        return 0, str(e)

def handle_errors(error_list):
    if error_list:
        return 0, ", ".join(error_list)
    else:
        return 1, "No errors occurred."

def get_video_info(video_url):
    ydl_opts = {
        'skip_download': True,
        'no_warnings': True,
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        video_info = ydl.extract_info(video_url, download=False)

    title = video_info.get('title')
    thumbnail = video_info.get('thumbnail')
    views = video_info.get('view_count')
    likes = video_info.get('like_count')

    return title, thumbnail, views, likes
