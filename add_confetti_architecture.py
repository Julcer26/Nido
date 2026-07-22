import os
import re

# 1. Update FamilyViewModel.kt
vm_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\viewmodel\FamilyViewModel.kt'
with open(vm_path, 'r', encoding='utf-8') as f:
    vm_code = f.read()

if '_confettiTrigger' not in vm_code:
    # InsertStateFlow
    target = 'val topNotification: StateFlow<String?> = _topNotification.asStateFlow()'
    replacement = target + '\n\n    private val _confettiTrigger = MutableStateFlow(0)\n    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()\n    fun triggerConfetti() { _confettiTrigger.value += 1 }'
    vm_code = vm_code.replace(target, replacement)

    # Add triggerConfetti calls
    vm_code = vm_code.replace(
        'fun toggleChoreCompletion(chore: Chore) { viewModelScope.launch {',
        'fun toggleChoreCompletion(chore: Chore) { if (!chore.isCompleted) triggerConfetti()\n        viewModelScope.launch {'
    )
    vm_code = vm_code.replace(
        'fun toggleShoppingItemBought(item: ShoppingItem) { viewModelScope.launch {',
        'fun toggleShoppingItemBought(item: ShoppingItem) { if (!item.isBought) triggerConfetti()\n        viewModelScope.launch {'
    )
    vm_code = vm_code.replace(
        'fun editGoal(goal: Goal) { viewModelScope.launch {',
        'fun editGoal(goal: Goal) { if (goal.isCompleted) triggerConfetti()\n        viewModelScope.launch {'
    )
    vm_code = vm_code.replace(
        'fun editNote(note: ShoppingNote) { viewModelScope.launch {',
        'fun editNote(note: ShoppingNote) { if (note.isCompleted) triggerConfetti()\n        viewModelScope.launch {'
    )

    with open(vm_path, 'w', encoding='utf-8') as f:
        f.write(vm_code)
    print("UPDATED FAMILYVIEWMODEL.KT!")

# 2. Update MainActivity.kt
main_path = r'h:\Mi unidad\Nido\hogar-finanzas\app\src\main\java\com\example\MainActivity.kt'
with open(main_path, 'r', encoding='utf-8') as f:
    main_code = f.read()

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
        val list = List(75) {
            ConfettiParticle(
                x = 0.5f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.45f,
                y = 0.35f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.25f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.025f,
                vy = -0.022f - kotlin.random.Random.nextFloat() * 0.025f,
                rotation = kotlin.random.Random.nextFloat() * 360f,
                vRot = (kotlin.random.Random.nextFloat() - 0.5f) * 14f,
                color = colors.random(),
                size = 10f + kotlin.random.Random.nextFloat() * 14f,
                isCircle = kotlin.random.Random.nextBoolean()
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

if 'ConfettiEffect(' not in main_code:
    main_code = main_code.replace('@Composable\nfun MainScreen(', confetti_composable + '\n@Composable\nfun MainScreen(')

if 'confettiTrigger by viewModel.confettiTrigger' not in main_code:
    main_code = main_code.replace(
        'val topNotification by viewModel.topNotification.collectAsStateWithLifecycle()',
        'val topNotification by viewModel.topNotification.collectAsStateWithLifecycle()\n    val confettiTrigger by viewModel.confettiTrigger.collectAsStateWithLifecycle()'
    )

if 'ConfettiEffect(trigger = confettiTrigger' not in main_code:
    main_code = main_code.replace(
        'Box(modifier = Modifier.fillMaxSize() ){\n            Scaffold(',
        'Box(modifier = Modifier.fillMaxSize() ){\n            ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize() )\n            Scaffold('
    )

with open(main_path, 'w', encoding='utf-8') as f:
    f.write(main_code)

print("UPDATED MAINACTIVITY.KT ENTIRELY!")
