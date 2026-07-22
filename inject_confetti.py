import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Insert imports
imports_to_add = "import androidx.compose.foundation.Canvas\nimport androidx.compose.ui.geometry.Size\nimport androidx.compose.ui.graphics.drawscope.withTransform\nimport kotlin.random.Random\n"
content = imports_to_add + content

# 2. Add ConfettiEffect definition before PremiumCard
confetti_code = """
data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRot: Float,
    val color: Color,
    val size: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    if (trigger <= 0) return

    var particles by remember(trigger) {
        val colors = listOf(
            Color(0xFF10B981),
            Color(0xFF6366F1),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFF14B8A6)
        )
        val list = List(75) {
            ConfettiParticle(
                x = 0.5f + (Random.nextFloat() - 0.5f) * 0.45f,
                y = 0.35f + (Random.nextFloat() - 0.5f) * 0.25f,
                vx = (Random.nextFloat() - 0.5f) * 0.025f,
                vy = -0.022f - Random.nextFloat() * 0.025f,
                rotation = Random.nextFloat() * 360f,
                vRot = (Random.nextFloat() - 0.5f) * 14f,
                color = colors.random(),
                size = 10f + Random.nextFloat() * 14f,
                isCircle = Random.nextBoolean()
            )
        }
        mutableStateOf(list)
    }

    var progress by remember(trigger) { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 1_800_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            val fraction = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            progress = fraction

            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.0009f,
                    rotation = p.rotation + p.vRot
                )
            }

            if (fraction >= 1f) break
        }
    }

    if (progress < 1f) {
        val alpha = (1f - progress).coerceIn(0f, 1f)
        androidx.compose.foundation.Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            particles.forEach { p ->
                val px = p.x * canvasWidth
                val py = p.y * canvasHeight

                drawContext.transform.withTransform({
                    translate(left = px, top = py)
                    rotate(p.rotation)
                }) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.size / 2f
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(-p.size / 2f, -p.size / 4f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                        )
                    }
                }
            }
        }
    }
}

"""

content = content.replace('fun PremiumCard(', confetti_code + 'fun PremiumCard(')

# 3. Add confettiTrigger in MainScreen
content = content.replace(
    'var currentTab by remember { mutableStateOf("inicio") }',
    'var currentTab by remember { mutableStateOf("inicio") }\n    var confettiTrigger by remember { mutableIntStateOf(0) }'
)

# 4. Add ConfettiEffect composable to root Box
content = content.replace(
    'Box(modifier = Modifier.fillMaxSize() ){\n            Scaffold(',
    'Box(modifier = Modifier.fillMaxSize() ){\n            ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize() )\n            Scaffold('
)

# 5. Add trigger increment to completion toggles
content = content.replace(
    '.clickable { viewModel.toggleChoreCompletion(chore) }',
    '.clickable { if (!chore.isCompleted) confettiTrigger++; viewModel.toggleChoreCompletion(chore) }'
)

content = content.replace(
    '.clickable { viewModel.toggleShoppingItemBought(item) }',
    '.clickable { if (!item.isBought) confettiTrigger++; viewModel.toggleShoppingItemBought(item) }'
)

content = content.replace(
    '.clickable { viewModel.editGoal(goal.copy(isCompleted = !goal.isCompleted)) }',
    '.clickable { if (!goal.isCompleted) confettiTrigger++; viewModel.editGoal(goal.copy(isCompleted = !goal.isCompleted)) }'
)

content = content.replace(
    '.clickable { viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) }',
    '.clickable { if (!note.isCompleted) confettiTrigger++; viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) }'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print('SUCCESSFULLY INJECTED CONFETTI EFFECT!')
