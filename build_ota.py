import os
import re
import subprocess
import shutil

def run_cmd(cmd):
    print(f"Running: {cmd}")
    subprocess.run(cmd, shell=True, check=True)

def main():
    # 1. Increment version code and version name
    build_file = r"h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts"
    with open(build_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    match_code = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not match_code:
        print("Could not find versionCode")
        return
    
    old_version = int(match_code.group(1))
    new_version = old_version + 1
    
    match_name = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    if match_name:
        old_name = match_name.group(1)
        # Assuming format like "1.x", bump the minor version to match new_version
        new_name = f"1.{new_version}"
        content = content.replace(f'versionName = "{old_name}"', f'versionName = "{new_name}"')
    else:
        new_name = f"1.{new_version}"
        
    content = content.replace(f"versionCode = {old_version}", f"versionCode = {new_version}")
    
    with open(build_file, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print(f"Version updated to Code: {new_version}, Name: {new_name}")

    # 2. Build APK
    print("Compiling APK...")
    run_cmd("gradlew.bat clean assembleDebug")

    apk_path = r"h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk"
    print("\n" + "="*50)
    print("COMPILACION EXITOSA!")
    print(f"La nueva version de la app (v{new_name}) ha sido generada en:")
    print(apk_path)
    print("\nPara subir esta actualizacion a los telefonos (OTA):")
    print("1. Abre https://hogar-finanzas-2026.web.app en tu navegador.")
    print("2. Inicia sesion con tu cuenta de Nido.")
    print(f"3. Ingresa la version ({new_version}).")
    print("4. Selecciona el archivo APK generado y dale a 'Subir'.")
    print("="*50 + "\n")

if __name__ == "__main__":
    main()
