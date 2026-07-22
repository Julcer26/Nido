import os

path = r'C:\Users\julio\.gemini\antigravity\brain\a8558a92-fb67-4d9e-a129-0c485d0e6649\scratch\release_update.py'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'npx firebase database:set /config/' in line:
        lines[i] = '# ' + line
    if 'print("Sending message' in line:
        lines.insert(i, """
    import json
    update_data = {
        "latest_version_code": vc,
        "latest_apk_url": catbox_url
    }
    with open("update.json", "w") as f:
        json.dump(update_data, f)
    run_cmd("npx firebase database:set /config update.json -f")
""")
        break

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
