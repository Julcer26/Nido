import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Clean top bad imports
bad_top = "import androidx.compose.foundation.Canvas\nimport androidx.compose.ui.geometry.Size\nimport androidx.compose.ui.graphics.drawscope.withTransform\nimport kotlin.random.Random\n"
if content.startswith(bad_top):
    content = content[len(bad_top):]

# Insert imports after package com.example
good_imports = "package com.example\n\nimport androidx.compose.foundation.Canvas\nimport androidx.compose.ui.geometry.Size\nimport androidx.compose.ui.graphics.drawscope.withTransform\nimport kotlin.random.Random"
content = content.replace("package com.example", good_imports)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("CLEANED IMPORTS IN MAINACTIVITY.KT!")
