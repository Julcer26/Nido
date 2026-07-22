import os

path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    code = f.read()

# Make sure zIndex is imported
if 'import androidx.compose.ui.zIndex' not in code:
    code = code.replace("package com.example", "package com.example\n\nimport androidx.compose.ui.zIndex")

# Replace ConfettiEffect definition
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
        val list = List(120) {
            ConfettiParticle(
                x = 0.5f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.7f,
                y = 0.4f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.3f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.035f,
                vy = -0.028f - kotlin.random.Random.nextFloat() * 0.035f,
                rotation = kotlin.random.Random.nextFloat() * 360f,
                vRot = (kotlin.random.Random.nextFloat() - 0.5f) * 18f,
                color = colors.random(),
                size = 14f + kotlin.random.Random.nextFloat() * 18f,
                isCircle = kotlin.random.Random.nextBoolean()
            )
        }
        mutableStateOf(list)
    }

    var frameTick by remember(trigger) { mutableLongStateOf(0L) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 2_500_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            val fraction = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            frameTick = elapsed

            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.0011f,
                    rotation = p.rotation + p.vRot
                )
            }

            if (fraction >= 1f) break
        }
    }

    val progress = (frameTick.toFloat() / 2_500_000_000L).coerceIn(0f, 1f)
    if (frameTick > 0L && progress < 1f) {
        val alpha = (1f - progress).coerceIn(0f, 1f)
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

# Remove old ConfettiEffect if present
if "data class ConfettiParticle" in code:
    idx_start = code.find("data class ConfettiParticle")
    idx_end = code.find("@Composable\nfun MainScreen(")
    if idx_start != -1 and idx_end != -1:
        code = code[:idx_start] + code[idx_end:]

# Insert updated ConfettiEffect
code = code.replace('@Composable\nfun MainScreen(', confetti_composable + '\n@Composable\nfun MainScreen(')

# Remove old misplaced ConfettiEffect call
code = code.replace("ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize() )", "")

# Place ConfettiEffect AFTER TopNotificationBanner (right at line 589 before closing root Box)
target = """                topNotification?.let { msg ->
                    TopNotificationBanner(
                        message = msg,
                        onDismiss = { viewModel.dismissNotification() }
                    )
                }"""

replacement = target + """
                
                ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize().zIndex(9999f))"""

code = code.replace(target, replacement)

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)

print("SUCCESSFULLY UPDATED CONFETTI OVERLAY WITH Z-INDEX AND CORRECT PLACEMENT!")
