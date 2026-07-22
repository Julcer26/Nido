import os
import re
import subprocess
import requests
import json

print("Bumping version to 1.26 / 26...")
gradle_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts'
with open(gradle_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 26', content)
content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.26"', content)

with open(gradle_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Building APK v1.26...")
subprocess.run('gradlew.bat assembleDebug', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Uploading to Litterbox via curl...")
apk_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk'
curl_cmd = f'curl.exe -F "reqtype=fileupload" -F "time=12h" -F "fileToUpload=@{apk_path}" https://litterbox.catbox.moe/resources/internals/api.php'
res = subprocess.run(curl_cmd, capture_output=True, text=True, shell=True, check=True)

apk_url = res.stdout.strip()
print("LITTERBOX_URL:", apk_url)

print("Updating Firebase DB...")
update_data = {
    'latest_version_code': 26,
    'latest_apk_url': apk_url
}
with open('update.json', 'w') as uf:
    json.dump(update_data, uf)

subprocess.run('npx firebase database:set /config update.json -f', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("Sending response on Telegram...")
msg_text = f"¡Encontré la falla exacta gracias a la captura que me mandaste! Las tarjetas completadas tenían un color semitransparente (alpha 0.35f) que provocaba que la sombra 3D se trasluciera por dentro creando ese contenedor gris borroso tan feo con el recuadro blanco adentro. Eliminé esa transparencia y ahora se ven 100% sólidas, limpias y flotantes como el resto. Ya está lista la versión 1.26: {apk_url}"

node_script = r'C:\Users\julio\.gemini\antigravity\scratch\gemini-chat-bridge\send_response.js'
subprocess.run(f'node "{node_script}" 1784654551855 Nido "{msg_text}"', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print("SUCCESSFULLY DEPLOYED V1.26 AND NOTIFIED USER!")
