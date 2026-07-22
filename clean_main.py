import os

# Let's inspect MainActivity.kt lines around ConfettiEffect to clean them up
path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Clean out bad top imports
top_imports_bad = """package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random"""

content = text.replace(top_imports_bad, "package com.example")

# Clean out any leftover ConfettiEffect if inserted
if "data class ConfettiParticle" in content:
    idx_start = content.find("data class ConfettiParticle")
    idx_end = content.find("fun PremiumCard(")
    if idx_start != -1 and idx_end != -1:
        content = content[:idx_start] + content[idx_end:]

# Clean out any leftover confettiTrigger references in MainActivity
content = content.replace("var confettiTrigger by remember { mutableIntStateOf(0) }\n", "")
content = content.replace("ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize() )\n", "")
content = content.replace("if (!chore.isCompleted) confettiTrigger++; ", "")
content = content.replace("if (!item.isBought) confettiTrigger++; ", "")
content = content.replace("if (!goal.isCompleted) confettiTrigger++; ", "")
content = content.replace("if (!note.isCompleted) confettiTrigger++; ", "")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("CLEANED MAINACTIVITY.KT ENTIRELY!")
