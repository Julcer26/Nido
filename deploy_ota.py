import os
import re
import subprocess
import shutil
import time

def run_cmd(cmd):
    print(f"Running: {cmd}")
    subprocess.run(cmd, shell=True, check=True)

def main():
    # 1. Increment version code
    build_file = r"h:\Mi unidad\Nido\hogar-finanzas\app\build.gradle.kts"
    with open(build_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    match = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not match:
        print("Could not find versionCode")
        return
    
    old_version = int(match.group(1))
    new_version = old_version + 1
    
    content = content.replace(f"versionCode = {old_version}", f"versionCode = {new_version}")
    with open(build_file, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print(f"Version updated to {new_version}")

    # 2. Build APK
    print("Compiling APK...")
    run_cmd("gradlew.bat assembleDebug")

    # 3. Copy to public folder
    public_dir = r"h:\Mi unidad\Nido\hogar-finanzas\public"
    if not os.path.exists(public_dir):
        os.makedirs(public_dir)
        
    apk_path = r"h:\Mi unidad\Nido\hogar-finanzas\app\build\outputs\apk\debug\app-debug.apk"
    dest_path = os.path.join(public_dir, "app-update.apk")
    
    shutil.copy2(apk_path, dest_path)
    print("APK copied to public folder")

    # 4. Deploy to Firebase Hosting and Database Rules
    print("Deploying to Firebase Hosting...")
    run_cmd("npx firebase deploy --only hosting,database")

    # 5. Update Realtime DB with new version info
    print("Updating Firebase Realtime Database...")
    run_cmd(f"npx firebase database:set /config/latest_version_code --data {new_version} -f")
    run_cmd(f"npx firebase database:set /config/latest_apk_url --data '\"https://hogar-finanzas-2026.web.app/app-update.apk\"' -f")

    print(f"✅ Update v{new_version} deployed successfully via OTA!")

if __name__ == "__main__":
    main()
