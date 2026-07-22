import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    code = f.read()

new_confetti_composable = """
data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRot: Float,
    val swayPhase: Float,
    val swaySpeed: Float,
    val color: Color,
    val size: Float,
    val shapeType: Int
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
            Color(0xFF14B8A6),
            Color(0xFFF43F5E),
            Color(0xFFFFD700)
        )
        val list = List(140) {
            ConfettiParticle(
                x = 0.5f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.7f,
                y = 0.3f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.2f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.015f,
                vy = -0.012f - kotlin.random.Random.nextFloat() * 0.014f,
                rotation = kotlin.random.Random.nextFloat() * 360f,
                vRot = (kotlin.random.Random.nextFloat() - 0.5f) * 6f,
                swayPhase = kotlin.random.Random.nextFloat() * 6.28f,
                swaySpeed = 2f + kotlin.random.Random.nextFloat() * 3f,
                color = colors.random(),
                size = 10f + kotlin.random.Random.nextFloat() * 14f,
                shapeType = kotlin.random.Random.nextInt(3)
            )
        }
        mutableStateOf(list)
    }

    var frameTick by remember(trigger) { mutableLongStateOf(0L) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 3_800_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            val fraction = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            frameTick = elapsed

            val elapsedSec = elapsed / 1_000_000_000f
            particles = particles.map { p ->
                val sway = kotlin.math.sin(elapsedSec * p.swaySpeed + p.swayPhase) * 0.0012f
                p.copy(
                    x = p.x + p.vx + sway,
                    y = p.y + p.vy,
                    vy = p.vy + 0.00035f,
                    rotation = p.rotation + p.vRot
                )
            }

            if (fraction >= 1f) break
        }
    }

    val progress = (frameTick.toFloat() / 3_800_000_000L).coerceIn(0f, 1f)
    if (frameTick > 0L && progress < 1f) {
        val alpha = if (progress > 0.75f) {
            ((1f - progress) / 0.25f).coerceIn(0f, 1f)
        } else {
            1f
        }

        androidx.compose.foundation.Canvas(
            modifier = modifier
                .fillMaxSize()
                .zIndex(9999f)
                .graphicsLayer { this.alpha = alpha }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            particles.forEach { p ->
                val px = p.x * canvasWidth
                val py = p.y * canvasHeight

                rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                    when (p.shapeType) {
                        1 -> drawCircle(
                            color = p.color,
                            center = Offset(px, py),
                            radius = p.size / 2.2f
                        )
                        2 -> drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 3f, py - p.size * 0.75f),
                            size = androidx.compose.ui.geometry.Size(p.size * 0.6f, p.size * 1.5f)
                        )
                        else -> drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2f, py - p.size / 3f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.65f)
                        )
                    }
                }
            }
        }
    }
}
"""

# Remove old ConfettiEffect definition
if "data class ConfettiParticle" in code:
    idx_start = code.find("data class ConfettiParticle")
    idx_end = code.find("@Composable\nfun MainScreen(")
    if idx_start != -1 and idx_end != -1:
        code = code[:idx_start] + code[idx_end:]

# Insert updated ConfettiEffect
code = code.replace('@Composable\nfun MainScreen(', new_confetti_composable + '\n@Composable\nfun MainScreen(')

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)

print("SUCCESSFULLY UPGRADED CONFETTI PHYSICS & SMOOTHNESS!")
