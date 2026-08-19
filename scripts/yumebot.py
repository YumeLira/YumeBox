import requests
import os
import glob
import re
import shutil
import subprocess  # nosec B404 - only runs fixed git commands from CI env
import html
import json
from contextlib import ExitStack
from pathlib import Path

BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = os.environ.get("CHAT_ID")
TITLE = os.environ.get("TITLE")
BRANCH = os.environ.get("BRANCH")
WORKFLOW_NAME = os.environ.get("WORKFLOW_NAME", "")
EVENT_NAME = os.environ.get("EVENT_NAME", "")
RUN_NUMBER = os.environ.get("RUN_NUMBER", "")
COMMIT_SHA = os.environ.get("COMMIT_SHA", "")
REPOSITORY = os.environ.get("REPOSITORY", "")
RUN_ID = os.environ.get("RUN_ID", "")
SERVER_URL = os.environ.get("SERVER_URL", "https://github.com")
VERSION_NAME = os.environ.get("VERSION_NAME", "")
VERSION_CODE = os.environ.get("VERSION_CODE", "")
RELEASE_URL = os.environ.get("RELEASE_URL", "")
META_URL = os.environ.get("META_URL", "")
PUBLISH_DIR = os.environ.get("PUBLISH_DIR", "")
LOGO_PATH = os.environ.get("LOGO_PATH", "website/images/brand/logo.webp")
LOGO_URL = os.environ.get("LOGO_URL", "https://yumebox.gal.tf/images/brand/logo.webp")
COMMIT_MESSAGE = os.environ.get("COMMIT_MESSAGE", "")
BOT_API_BASE_URL = os.environ.get("BOT_API_BASE_URL", "https://api.telegram.org").rstrip("/")

MAX_LOGO_BYTES = 10 * 1024 * 1024


def get_commit_message():
    msg = (COMMIT_MESSAGE or "").strip()
    if not msg and COMMIT_SHA:
        if not re.fullmatch(r"[0-9a-fA-F]{7,64}", COMMIT_SHA):
            print("[-] COMMIT_SHA is not a valid git sha, skipping git log")
            return msg
        git = shutil.which("git") or "git"
        try:
            # nosemgrep: python.lang.security.audit.dangerous-subprocess-use-audit.dangerous-subprocess-use-audit, python.lang.security.audit.dangerous-subprocess-use-tainted-env-args.dangerous-subprocess-use-tainted-env-args
            result = subprocess.run(  # noqa: S603 # nosec B603 - fixed git argv, sha validated above
                [git, "log", "-1", "--format=%B", COMMIT_SHA],
                cwd=os.environ.get("GITHUB_WORKSPACE") or ".",
                capture_output=True, text=True, timeout=15,
            )
            msg = (result.stdout or "").strip()
        except Exception as e:
            print(f"[-] git log failed: {e}")
    return msg


def html_escape(text):
    return html.escape(text or "")


def _workflow_label():
    workflow_name_lower = WORKFLOW_NAME.lower()
    if "test" in workflow_name_lower:
        return "Test"
    return "Normal"


def _trigger_label():
    return {"push": "Push", "schedule": "Nightly"}.get(EVENT_NAME, "Manual")


def _version_lines():
    if VERSION_NAME and VERSION_CODE:
        version = f"{html_escape(VERSION_NAME)} ({html_escape(VERSION_CODE)})"
        return [f"<b>• Version</b>: <code>{version}</code>"]
    if VERSION_NAME:
        return [f"<b>• Version</b>: <code>{html_escape(VERSION_NAME)}</code>"]
    return []


def _link_lines():
    lines = []
    if REPOSITORY and RUN_ID:
        action_url = f"{SERVER_URL}/{REPOSITORY}/actions/runs/{RUN_ID}"
        lines.append(f'<b>• Download</b>: <a href="{action_url}">workpiece</a>')
    if RELEASE_URL:
        lines.append(f'<b>• Release</b>: <a href="{RELEASE_URL}">open</a>')
    if META_URL:
        lines.append(f'<b>• Meta</b>: <a href="{META_URL}">json</a>')
    return lines


def _commit_lines():
    commit_short = COMMIT_SHA[:7] if COMMIT_SHA else "unknown"
    if REPOSITORY and COMMIT_SHA:
        commit_url = f"{SERVER_URL}/{REPOSITORY}/commit/{COMMIT_SHA}"
        link = f'<a href="{commit_url}">{html_escape(commit_short)}</a>'
        lines = [f"<b>• Commit</b>: {link}"]
    else:
        lines = [f"<b>• Commit</b>: {html_escape(commit_short)}"]

    commit_message = get_commit_message()
    if commit_message:
        body = commit_message.strip()
        if len(body) > 700:
            body = body[:700].rstrip() + "…"
        lines.append(f"<blockquote>{html_escape(body)}</blockquote>")
    return lines


def get_caption():
    display_title = TITLE
    if _workflow_label() == "Test" and "test" not in TITLE.lower():
        display_title = f"{TITLE} Test"

    lines = [
        f"<b>{html_escape(display_title)}</b>",
        f"<b>• Trigger</b>: {html_escape(_trigger_label())}",
        f"<b>• Branch</b>: {html_escape(BRANCH)}",
    ]
    lines.extend(_version_lines())
    lines.extend(_link_lines())
    lines.extend(_commit_lines())
    return "\n".join(lines)


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
    if PUBLISH_DIR:
        pattern = os.path.join(PUBLISH_DIR, "*.apk")
        found = sorted(glob.glob(pattern))
        if found:
            print(f"[+] Found {len(found)} files in {pattern}")
            return found

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

    files = sorted(set(files))

    if not files:
        print("[-] No APK files found!")
        exit(1)

    print(f"[+] Total files to upload: {len(files)}")
    for f in files:
        print(f"    - {f}")

    return files


def _validate_logo(data, source):
    if not data:
        raise ValueError(f"Logo is empty: {source}")
    if len(data) > MAX_LOGO_BYTES:
        raise ValueError(f"Logo exceeds {MAX_LOGO_BYTES} bytes: {source}")
    return data


def load_logo():
    if LOGO_PATH:
        path = Path(LOGO_PATH)
        if path.is_file():
            data = _validate_logo(path.read_bytes(), path)
            content_type = "image/webp" if path.suffix.lower() == ".webp" else "application/octet-stream"
            print(f"[+] Loaded current checkout logo: {path} ({len(data)} bytes)")
            return path.name, data, content_type

    if not LOGO_URL:
        raise FileNotFoundError(f"Logo not found: {LOGO_PATH}")

    cache_key = RUN_ID or COMMIT_SHA or RUN_NUMBER
    params = {"ci_run": cache_key} if cache_key else None
    with requests.get(
        LOGO_URL,
        params=params,
        headers={"Cache-Control": "no-cache"},
        stream=True,
        timeout=(15, 60),
    ) as response:
        response.raise_for_status()
        content_type = response.headers.get("Content-Type", "image/webp").split(";", 1)[0]
        if not content_type.startswith("image/"):
            raise ValueError(f"Logo URL returned unexpected content type: {content_type}")

        data = bytearray()
        for chunk in response.iter_content(chunk_size=64 * 1024):
            data.extend(chunk)
            if len(data) > MAX_LOGO_BYTES:
                raise ValueError(f"Logo exceeds {MAX_LOGO_BYTES} bytes: {LOGO_URL}")

    logo = _validate_logo(bytes(data), LOGO_URL)
    print(f"[+] Downloaded fresh logo: {LOGO_URL} ({len(logo)} bytes)")
    return "og.webp", logo, content_type


def send_files_via_bot_api():
    print("[+] Starting Telegram upload")
    check_environ()

    files = find_apk_files()

    # Bot API URL
    bot_url = f"{BOT_API_BASE_URL}/bot{BOT_TOKEN}"

    caption = get_caption()
    print("[+] Caption:", caption)

    caption_sent = False
    try:
        photo_name, photo, photo_content_type = load_logo()
        photo_data = {
            'chat_id': CHAT_ID,
            'caption': caption,
            'parse_mode': 'HTML',
        }

        photo_resp = requests.post(
            f"{bot_url}/sendPhoto",
            data=photo_data,
            files={'photo': (photo_name, photo, photo_content_type)},
            timeout=60,
        )

        print(f"[+] Photo+caption: {photo_resp.status_code}")

        if photo_resp.status_code != 200:
            print(f"[-] Photo send failed: {photo_resp.text}")
        else:
            caption_sent = True

    except Exception as e:
        print(f"[-] Photo send failed: {e}")

    if not caption_sent:
        try:
            text_resp = requests.post(
                f"{bot_url}/sendMessage",
                data={
                    "chat_id": CHAT_ID,
                    "text": caption,
                    "parse_mode": "HTML",
                },
                timeout=60,
            )
            if text_resp.status_code == 200:
                caption_sent = True
                print("[+] Text caption sent without a logo")
            else:
                print(f"[-] Text caption send failed: {text_resp.text}")
        except Exception as e:
            print(f"[-] Text caption send failed: {e}")


    # Telegram renders a document media group as one album-style chat item.
    all_uploaded = caption_sent
    try:
        media = []
        upload_files = {}
        with ExitStack() as stack:
            for index, file_path in enumerate(files):
                field_name = f"document{index}"
                media.append({"type": "document", "media": f"attach://{field_name}"})
                upload_files[field_name] = (
                    os.path.basename(file_path),
                    stack.enter_context(open(file_path, "rb")),
                    "application/vnd.android.package-archive",
                )
                print(f"[+] Queued {file_path} for media group")

            response = requests.post(
                f"{bot_url}/sendMediaGroup",
                data={"chat_id": CHAT_ID, "media": json.dumps(media)},
                files=upload_files,
                timeout=(30, 600),
            )

        if response.status_code == 200:
            print(f"[+] Uploaded {len(files)} APKs in one media group")
        else:
            print(f"[-] Failed to upload APK media group: {response.text}")
            all_uploaded = False
    except Exception as e:
        print(f"[-] Failed to upload APK media group: {e}")
        all_uploaded = False

    return all_uploaded


if __name__ == "__main__":
    try:
        if not send_files_via_bot_api():
            raise SystemExit(1)
    except Exception as e:
        print(f"[-] Error: {e}")
        raise SystemExit(1)
