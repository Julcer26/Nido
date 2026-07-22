import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = "package com.example"
replacement = "package com.example\n\nimport androidx.compose.ui.graphics.drawscope.rotate"

if "import androidx.compose.ui.graphics.drawscope.rotate" not in content:
    content = content.replace(target, replacement)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("ADDED ROTATE IMPORT TO MAINACTIVITY.KT!")
