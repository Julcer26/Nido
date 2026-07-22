import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    code = f.read()

# Make sure ConfettiEffect definition is present
confetti_composable = """
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
        val list = List(100) {
            ConfettiParticle(
                x = 0.5f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.6f,
                y = 0.4f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.3f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.03f,
                vy = -0.025f - kotlin.random.Random.nextFloat() * 0.03f,
                rotation = kotlin.random.Random.nextFloat() * 360f,
                vRot = (kotlin.random.Random.nextFloat() - 0.5f) * 16f,
                color = colors.random(),
                size = 12f + kotlin.random.Random.nextFloat() * 16f,
                isCircle = kotlin.random.Random.nextBoolean()
            )
        }
        mutableStateOf(list)
    }

    var progress by remember(trigger) { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 2_200_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            val fraction = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            progress = fraction

            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.001f,
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

                rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            center = Offset(px, py),
                            radius = p.size / 2f
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2f, py - p.size / 4f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
"""

if 'ConfettiEffect(' not in code:
    code = code.replace('@Composable\nfun MainScreen(', confetti_composable + '\n@Composable\nfun MainScreen(')

# Ensure confettiTrigger state is collected
if 'confettiTrigger by viewModel.confettiTrigger' not in code:
    code = code.replace(
        'val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()',
        'val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()\n        val confettiTrigger by viewModel.confettiTrigger.collectAsStateWithLifecycle()'
    )

# Place ConfettiEffect inside root Box overlay
target_box = 'Box(modifier = Modifier.fillMaxSize() ){\n            Scaffold('
replacement_box = 'Box(modifier = Modifier.fillMaxSize() ){\n            Scaffold('

if 'ConfettiEffect(trigger = confettiTrigger' not in code:
    # Look for Box(modifier = Modifier.fillMaxSize() ){ right before Scaffold
    idx = code.find('Box(modifier = Modifier.fillMaxSize() ){')
    if idx != -1:
        scaffold_idx = code.find('Scaffold(', idx)
        if scaffold_idx != -1:
            code = code[:scaffold_idx] + 'ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize() )\n            ' + code[scaffold_idx:]

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)

print("FIXED CONFETTI PLACEMENT IN MAINACTIVITY.KT!")
