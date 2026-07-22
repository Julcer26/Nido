import shutil, subprocess, os

apk_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk'
dest_path = r'h:\Mi unidad\Nido\hogar-finanzas\public\app-update.bin'

# Clean public folder of .apk
for f in os.listdir(r'h:\Mi unidad\Nido\hogar-finanzas\public'):
    if f.endswith('.apk'):
        os.remove(os.path.join(r'h:\Mi unidad\Nido\hogar-finanzas\public', f))

shutil.copy2(apk_path, dest_path)
print('Copied APK as .bin to public folder.')

print('Deploying to Firebase Hosting...')
subprocess.run(['npx.cmd', 'firebase', 'deploy', '--only', 'hosting'], check=True)

hosting_url = "https://hogar-finanzas-2026.web.app/app-update.bin"
print('HOSTING_URL:', hosting_url)

print('Updating Firebase DB...')
update_data = {
    'latest_version_code': 30,
    'latest_apk_url': hosting_url
}
import json
with open('update.json', 'w') as uf:
    json.dump(update_data, uf)

subprocess.run(['npx.cmd', 'firebase', 'database:set', '/config', 'update.json', '-f'], check=True)

msg_text = f"✨ ¡Animación de confeti mejorada y suavizada en v1.30! Extendí la duración a 3.8 segundos con físicas de bamboleo lateral estilo cinta/papel real, reduje la velocidad de caída y agregué más variedad de formas y colores para que se sienta súper fluida y bonita. Descarga la v1.30 aquí: {hosting_url}"

node_script = r'C:\Users\julio\.gemini\antigravity\scratch\gemini-chat-bridge\send_response.js'
subprocess.run(f'node "{node_script}" 1784664453384 Nido "{msg_text}"', cwd=r'h:\Mi unidad\Nido\hogar-finanzas', shell=True, check=True)

print('SUCCESSFULLY DEPLOYED V1.30 ON FIREBASE HOSTING AND NOTIFIED USER!')
