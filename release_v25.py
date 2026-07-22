import os
import re
import subprocess
import requests
import json
from datetime import datetime

print("Bumping version to 1.25 / 25...")
gradle_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts'
with open(gradle_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 25', content)
content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.25"', content)

with open(gradle_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Building APK...")
subprocess.run('gradlew.bat assembleDebug', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Uploading to Litterbox...")
apk_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk'
with open(apk_path, 'rb') as f:
    res = requests.post(
        'https://litter.catbox.moe/resources/internals/api.php',
        data={'reqtype': 'fileupload', 'time': '12h'},
        files={'fileToUpload': f}
    )

apk_url = res.text.strip()
print("LITTERBOX_URL:", apk_url)

print("Updating Firebase DB...")
update_data = {
    'latest_version_code': 25,
    'latest_apk_url': apk_url
}
with open('update.json', 'w') as uf:
    json.dump(update_data, uf)

subprocess.run('npx firebase database:set /config update.json -f', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Sending response on Telegram...")
msg_text = f"¡Descubrí el problema exacto! En las listas de compras el código usaba una caja plana anterior que se me había pasado por alto. Ya lo corregí por completo y compilé la nueva versión 1.25. Ya puedes actualizar la app directamente desde el menú o usando este enlace: {apk_url}"

subprocess.run([
    'node',
    r'C:\Users\julio\.gemini\antigravity\scratch\gemini-chat-bridge\send_response.js',
    '1784653510981',
    'Nido',
    msg_text
], shell=True, check=True)

print("ALL DONE SUCCESS!")
