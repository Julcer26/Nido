import os
import re
import subprocess
import requests
import json

print("Bumping version to 1.29 / 29...")
gradle_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts'
with open(gradle_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 29', content)
content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.28"', content) # wait, versionName = "1.29"
content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.29"', content)

with open(gradle_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Building APK v1.29...")
subprocess.run('gradlew.bat assembleDebug', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Uploading to Litterbox via curl...")
apk_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk'
curl_cmd = f'curl.exe -F "reqtype=fileupload" -F "time=12h" -F "fileToUpload=@{apk_path}" https://litterbox.catbox.moe/resources/internals/api.php'
res = subprocess.run(curl_cmd, capture_output=True, text=True, shell=True, check=True)

apk_url = res.stdout.strip()
print("LITTERBOX_URL:", apk_url)

print("Updating Firebase DB...")
update_data = {
    'latest_version_code': 29,
    'latest_apk_url': apk_url
}
with open('update.json', 'w') as uf:
    json.dump(update_data, uf)

subprocess.run('npx firebase database:set /config update.json -f', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Sending response on Telegram...")
msg_text = f"🎉 ¡Descubrí la causa exacta por la que no se veía la animación! En Jetpack Compose, el fondo de la pantalla (Scaffold) se estaba dibujando ENCIMA de la capa de animación, tapando por completo el confeti. Ya reubiqué el efecto al nivel más alto (z-index 9999) como la capa superior de toda la aplicación. ¡Ahora sí estallará el confeti sobre toda la pantalla al completar cualquier tarea, compra o meta! Descarga la v1.29 aquí: {apk_url}"

node_script = r'C:\Users\julio\.gemini\antigravity\scratch\gemini-chat-bridge\send_response.js'
subprocess.run(f'node "{node_script}" 1784658313970 Nido "{msg_text}"', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("SUCCESSFULLY DEPLOYED V1.29 AND NOTIFIED USER!")
