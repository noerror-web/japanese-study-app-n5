import subprocess
import json
import os
import urllib.request

GIT_PATH = r"C:\Program Files\Git\cmd\git.exe"

def get_git_token():
    try:
        proc = subprocess.run(
            [GIT_PATH, "credential", "fill"],
            input="protocol=https\nhost=github.com\n\n",
            text=True,
            capture_output=True
        )
        for line in proc.stdout.splitlines():
            if line.startswith("password="):
                return line.split("=", 1)[1]
    except Exception as e:
        print(f"Error getting git token: {e}")
    return None

token = get_git_token()
print(f"Found git token: {bool(token)}")

if token:
    repo = "noerror-web/japanese-study-app-n5"
    tag = "v2.0"
    
    # Check if release exists
    get_release_url = f"https://api.github.com/repos/{repo}/releases/tags/{tag}"
    req = urllib.request.Request(
        get_release_url,
        headers={
            "Authorization": f"token {token}",
            "Accept": "application/vnd.github.v3+json"
        }
    )
    
    try:
        res = urllib.request.urlopen(req)
        release_info = json.loads(res.read().decode("utf-8"))
        print(f"[FOUND] Existing GitHub Release for {tag}: {release_info['html_url']}")
        
        upload_url = release_info["upload_url"].split("{")[0]
        assets = release_info.get("assets", [])
        
        # Delete old asset if exists
        for asset in assets:
            if asset.get("name") == "JapaneseStudyApp-v2.0-release.apk":
                del_url = asset.get("url")
                print(f"Deleting old release asset ({asset.get('id')})...")
                del_req = urllib.request.Request(
                    del_url,
                    headers={
                        "Authorization": f"token {token}",
                        "Accept": "application/vnd.github.v3+json"
                    },
                    method="DELETE"
                )
                urllib.request.urlopen(del_req)
                print("Deleted old asset successfully!")
                
        # Copy compiled APK from Gradle output if present
        os.makedirs("release", exist_ok=True)
        gradle_apk = os.path.abspath("app/build/outputs/apk/release/app-release.apk")
        apk_path = os.path.abspath("release/JapaneseStudyApp-v2.0-release.apk")
        if os.path.exists(gradle_apk):
            import shutil
            shutil.copy2(gradle_apk, apk_path)
            print(f"Copied built release APK to target release folder: {apk_path}")

        if os.path.exists(apk_path):
            print(f"Uploading updated release asset ({round(os.path.getsize(apk_path)/(1024*1024), 2)} MB)...")
            with open(apk_path, "rb") as f:
                apk_bytes = f.read()
                
            upload_target = f"{upload_url}?name=JapaneseStudyApp-v2.0-release.apk"
            upload_req = urllib.request.Request(
                upload_target,
                data=apk_bytes,
                headers={
                    "Authorization": f"token {token}",
                    "Content-Type": "application/vnd.android.package-archive",
                    "Accept": "application/vnd.github.v3+json"
                },
                method="POST"
            )
            up_res = urllib.request.urlopen(upload_req)
            up_info = json.loads(up_res.read().decode("utf-8"))
            print(f"[SUCCESS] Uploaded Updated Release APK Asset: {up_info['browser_download_url']}")
            
    except urllib.error.HTTPError as e:
        err_text = e.read().decode("utf-8")
        print(f"[HTTP ERROR {e.code}]: {err_text}")
    except Exception as ex:
        print(f"[ERROR]: {ex}")
