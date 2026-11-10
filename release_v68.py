import os, re, subprocess, json, urllib.request, ssl, shutil

def run_cmd(cmd):
    print(f"Running: {cmd}")
    subprocess.run(cmd, shell=True, check=True)

def main():
    print("Bumping version to 1.68 / 68...")
    gradle_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts'
    with open(gradle_path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 68', content)
    content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.68"', content)

    with open(gradle_path, 'w', encoding='utf-8') as f:
        f.write(content)

    print("Building APK v1.68...")
    run_cmd(r'.\gradlew.bat assembleDebug')

    apk_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk'
    dest_path = r'h:\Mi unidad\Nido\hogar-finanzas\public\app-update.bin'
    
    shutil.copy2(apk_path, dest_path)
    print("Copied APK to public/app-update.bin.")

    print("Deploying to Firebase Hosting...")
    run_cmd('npx.cmd firebase deploy --only hosting --project hogar-finanzas-2026')

    hosting_url = "https://hogar-finanzas-2026.web.app/app-update.bin"
    print("HOSTING_URL:", hosting_url)

    print("Updating Firebase RTDB...")
    config_path = os.path.expanduser('~/.config/configstore/firebase-tools.json')
    with open(config_path, 'r') as f:
        data = json.load(f)

    token = data.get('tokens', {}).get('access_token')

    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    url = f'https://hogar-finanzas-2026-default-rtdb.firebaseio.com/config.json?access_token={token}'
    payload = json.dumps({
        'latest_version_code': 68,
        'latest_apk_url': hosting_url
    }).encode('utf-8')

    req = urllib.request.Request(url, data=payload, headers={'Content-Type': 'application/json'}, method='PUT')
    with urllib.request.urlopen(req, context=ctx) as resp:
        print("Firebase RTDB updated:", resp.read().decode('utf-8'))

    print("SUCCESSFULLY DEPLOYED V1.68!")

if __name__ == "__main__":
    main()
