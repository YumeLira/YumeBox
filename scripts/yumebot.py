import requests
import os
import glob
import json

BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = os.environ.get("CHAT_ID")
MESSAGE_THREAD_ID = os.environ.get("MESSAGE_THREAD_ID")
COMMIT_MESSAGE = os.environ.get("COMMIT_MESSAGE")
TITLE = os.environ.get("TITLE")
BRANCH = os.environ.get("BRANCH")


def get_caption():
    msg = f"""**{TITLE}**
Branch: {BRANCH}
```
{COMMIT_MESSAGE}
```"""
    return msg


def check_environ():
    if BOT_TOKEN is None:
        print("[-] Invalid BOT_TOKEN")
        exit(1)
    if CHAT_ID is None:
        print("[-] Invalid CHAT_ID")
        exit(1)
    if TITLE is None:
        print("[-] Invalid TITLE")
        exit(1)
    if BRANCH is None:
        print("[-] Invalid BRANCH")
        exit(1)


def find_apk_files():
    patterns = [
        "./app/build/outputs/apk/release/*arm64-v8a*.apk",
        "app/build/outputs/apk/release/*arm64-v8a*.apk",
        "/github/workspace/app/build/outputs/apk/release/*arm64-v8a*.apk"
    ]

    files = []
    for pattern in patterns:
        found = glob.glob(pattern)
        if found:
            files.extend(found)
            print(f"[+] Found {len(found)} files in {pattern}")

    files = list(set(files))

    if not files:
        print("[-] No arm64-v8a APK files found!")
        exit(1)

    print(f"[+] Total files to upload: {len(files)}")
    for f in files:
        print(f"    - {f}")

    return files


def send_files_via_bot_api():
    print("[+] Starting Telegram upload")
    check_environ()

    files = find_apk_files()

    # Bot API URL
    bot_url = f"https://api.telegram.org/bot{BOT_TOKEN}"

    caption = get_caption()
    print("[+] Caption:", caption)

    file_path = files[0]
    print(f"[+] Uploading {file_path}...")

    with open(file_path, 'rb') as f:
        data = {
            'chat_id': CHAT_ID,
            'caption': caption,
            'parse_mode': 'markdown'
        }

        if MESSAGE_THREAD_ID:
            data['message_thread_id'] = MESSAGE_THREAD_ID

        files_data = {
            'document': f
        }

        response = requests.post(
            f"{bot_url}/sendDocument",
            data=data,
            files=files_data,
            timeout=60
        )

    if response.status_code == 200:
        print(f"[+] {file_path} uploaded successfully!")
        return True
    else:
        print(f"[-] Failed to upload {file_path}: {response.text}")
        return False


if __name__ == "__main__":
    try:
        send_files_via_bot_api()
    except Exception as e:
        print(f"[-] Error: {e}")
        exit(1)