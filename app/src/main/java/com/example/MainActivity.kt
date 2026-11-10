package com.example

import androidx.compose.ui.zIndex

import androidx.compose.ui.graphics.drawscope.rotate

import android.os.Bundle
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import com.example.ui.ModernMonthSelector
import com.example.ui.ModernFinanceBalanceCard
import com.example.ui.ModernFinanceActionBar
import com.example.ui.ModernSegmentedTab
import com.example.ui.ModernCategoryFilterChips
import com.example.ui.ModernTransactionCard
import com.example.ui.ModernRecurringPaymentsSection
import com.example.ui.ModernTabHeader
import com.example.ui.ModernFilterPills
import com.example.ui.ModernListSelectorChips
import com.example.ui.ModernEmptyState
import com.example.ui.FinanceAiHeroCard
import com.example.ui.FinanceAiBottomSheet
import com.example.api.FinanceAiAnalyzer
import com.example.api.FinanceAiReport
import com.example.util.FinanceExportHelper
import com.example.api.GeminiParser
import com.example.api.BcvExchangeRateService
import kotlinx.coroutines.launch
import com.example.database.*
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import com.example.viewmodel.FamilyViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.fragment.app.FragmentActivity
import com.example.OtaUpdateManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.foundation.LocalIndication

class MainActivity : FragmentActivity() {
    var onImagePicked: ((Uri?) -> Unit)? = null
    var onPhotoTaken: ((Boolean) -> Unit)? = null
    var onPermissionResult: ((Boolean) -> Unit)? = null

    val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent() ){ uri ->
        onImagePicked?.invoke(uri)
    }

    val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture() ){ success ->
        onPhotoTaken?.invoke(success)
    }

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission() ){ isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: FamilyViewModel = viewModel(factory = FamilyViewModelFactory(context.applicationContext))
            val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
            val isDarkThemeSetting by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkThemeSetting) {
                MainScreen(viewModel)
            }
        }
    }
}

// Utility extension for hex color support

// --- Modern Design Utilities ---
fun Modifier.scaleClickable(
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = actualInteractionSource,
        indication = LocalIndication.current,
        onClick = onClick
    )
}

@Composable

fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val resolvedBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
    if (onClick != null) {
        Card(
            modifier = modifier.scaleClickable(onClick = onClick),
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = resolvedBorder
        ) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = resolvedBorder
        ) { content() }
    }
}


// --------------------------------

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF6750A4) // Fallback default
    }
}

// Utility to convert Uri to Bitmap safely
fun uriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Error converting Uri to Bitmap", e)
        null
    }
}


@OptIn(ExperimentalMaterial3Api::class)



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

    val particles = remember(trigger) {
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
        List(140) {
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
    }

    var frameTick by remember(trigger) { mutableLongStateOf(0L) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 3_800_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            frameTick = elapsed
            if (elapsed >= durationNanos) break
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
            val elapsedSec = frameTick / 1_000_000_000f
            val frames = elapsedSec * 60f
            val gravity = 0.00035f

            particles.forEach { p ->
                val sway = kotlin.math.sin(elapsedSec * p.swaySpeed + p.swayPhase) * 0.0012f * frames
                val curX = p.x + (p.vx * frames) + sway
                val curY = p.y + (p.vy * frames) + (0.5f * gravity * frames * frames)
                val curRot = p.rotation + (p.vRot * frames)

                val px = curX * canvasWidth
                val py = curY * canvasHeight

                rotate(degrees = curRot, pivot = Offset(px, py)) {
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

@Composable
fun MainScreen(viewModel: FamilyViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val topNotification by viewModel.topNotification.collectAsStateWithLifecycle()
    val confettiTrigger by viewModel.confettiTrigger.collectAsStateWithLifecycle()
    
    // Bottom tab routing selection
    var currentTab by remember { mutableStateOf("inicio") }
    
    var showConfigDialog by remember { mutableStateOf(false) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val currentVersionCode = com.example.BuildConfig.VERSION_CODE
        val info = OtaUpdateManager.checkForUpdates(currentVersionCode)
        if (info.isUpdateAvailable && info.downloadUrl.isNotEmpty() ){
            updateUrl = info.downloadUrl
            showUpdateDialog = true
        }
    }

    // Hoisted camera/gallery launcher state (must be outside AnimatedContent to avoid requestCode overflow)
    val coroutineScopeMain = rememberCoroutineScope()
    var isScanningDirect by remember { mutableStateOf(false) }
    var errorMessageScan by remember { mutableStateOf<String?>(null) }
    var successMessageScan by remember { mutableStateOf<String?>(null) }
    var showConfirmScanDialog by remember { mutableStateOf(false) }
    var scannedAmount by remember { mutableStateOf("") }
    var scannedDesc by remember { mutableStateOf("") }
    var scannedCurrency by remember { mutableStateOf("USD") }
    var scannedCategory by remember { mutableStateOf("") }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val mainSettings by viewModel.settings.collectAsStateWithLifecycle()

    var scannedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var scannedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var saveReceiptPhoto by remember { mutableStateOf(true) }
    var scannedItems by remember { mutableStateOf<List<com.example.api.ParsedReceiptItem>>(emptyList()) }
    var scannedBalanceDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val imagePickerLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent() ){ uri ->
        if (uri != null) {
            coroutineScopeMain.launch {
                isScanningDirect = true
                errorMessageScan = null
                successMessageScan = null
                val bitmap = uriToBitmap(context, uri)
                if (bitmap != null) {
                    val availableCats = categories.map { it.categoryName }
                    val customKey = mainSettings.find { it.key == "gemini_api_key" }?.value
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats, customKey)
                    if (parsedTx != null) {
                        val tx = parsedTx.transaction
                        scannedAmount = tx.originalAmount.toString()
                        scannedDesc = tx.description
                        scannedCurrency = tx.currency
                        scannedCategory = if (tx.category in availableCats) tx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = tx.date
                        scannedBalanceDate = tx.date
                        scannedItems = parsedTx.items
                        scannedBitmap = bitmap
                        showConfirmScanDialog = true
                    } else if (errorStr != null) {
                        errorMessageScan = errorStr
                    }
                } else {
                    errorMessageScan = "No se pudo procesar la imagen seleccionada."
                }
                isScanningDirect = false
            }
        }
    }

    val cameraLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture() ){ success ->
        val uri = tempPhotoUri
        if (success && uri != null) {
            coroutineScopeMain.launch {
                isScanningDirect = true
                errorMessageScan = null
                successMessageScan = null
                val bitmap = uriToBitmap(context, uri)
                if (bitmap != null) {
                    val availableCats = categories.map { it.categoryName }
                    val customKey = mainSettings.find { it.key == "gemini_api_key" }?.value
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats, customKey)
                    if (parsedTx != null) {
                        val tx = parsedTx.transaction
                        scannedAmount = tx.originalAmount.toString()
                        scannedDesc = tx.description
                        scannedCurrency = tx.currency
                        scannedCategory = if (tx.category in availableCats) tx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = tx.date
                        scannedBalanceDate = tx.date
                        scannedItems = parsedTx.items
                        scannedBitmap = bitmap
                        showConfirmScanDialog = true
                    } else if (errorStr != null) {
                        errorMessageScan = errorStr
                    }
                } else {
                    errorMessageScan = "No se pudo cargar la foto tomada."
                }
                isScanningDirect = false
            }
        } else {
            errorMessageScan = "No se tomó ninguna foto o se canceló."
        }
    }

    val launchHighResCamera = {
        try {
            val directory = context.cacheDir
            val file = java.io.File.createTempFile("receipt_photo_", ".jpg", directory).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            tempPhotoUri = uri
            cameraLauncherDirect.launch(uri)
        } catch (e: Exception) {
            errorMessageScan = "No se pudo preparar la foto: ${e.localizedMessage}"
        }
    }

    val permissionLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission() ){ isGranted ->
        if (isGranted) {
            launchHighResCamera()
        } else {
            errorMessageScan = "Permiso de cámara denegado."
        }
    }

    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize() ){
            AuthScreen(viewModel = viewModel)
            
            // Top banner notification system on login
            topNotification?.let { msg ->
                TopNotificationBanner(
                    message = msg,
                    onDismiss = { viewModel.dismissNotification() }
                )
            }
        }
    } else {
        val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize() ){
                        
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    FamilyBottomNavigation(currentTab = currentTab, isDarkTheme = isDarkTheme) { currentTab = it }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 0.dp
                        ) ){
                val tabOrder = listOf("inicio", "cuentas", "tareas", "compras", "metas")
                // Main content based on routing
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val initialIndex = tabOrder.indexOf(initialState)
                        val targetIndex = tabOrder.indexOf(targetState)
                        val isForward = targetIndex > initialIndex
                        
                        val enterAnim = if (isForward) {
                            slideInHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ it / 2 } + fadeIn(animationSpec = tween(220))
                        } else {
                            slideInHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ -it / 2 } + fadeIn(animationSpec = tween(220))
                        }
                        
                        val exitAnim = if (isForward) {
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ -it / 2 } + fadeOut(animationSpec = tween(220))
                        } else {
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ it / 2 } + fadeOut(animationSpec = tween(220))
                        }
                        
                        enterAnim togetherWith exitAnim
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        "inicio" -> DashboardTab(
                            viewModel = viewModel,
                            onOpenConfig = { showConfigDialog = true },
                            isScanningDirect = isScanningDirect,
                            errorMessageScan = errorMessageScan,
                            successMessageScan = successMessageScan,
                            showConfirmScanDialog = showConfirmScanDialog,
                            scannedAmount = scannedAmount,
                            scannedDesc = scannedDesc,
                            scannedCurrency = scannedCurrency,
                            scannedCategory = scannedCategory,
                            scannedDate = scannedDate,
                            onSetIsScanningDirect = { isScanningDirect = it },
                            onSetErrorMessageScan = { errorMessageScan = it },
                            onSetSuccessMessageScan = { successMessageScan = it },
                            onSetShowConfirmScanDialog = { showConfirmScanDialog = it },
                            onSetScannedAmount = { scannedAmount = it },
                            onSetScannedDesc = { scannedDesc = it },
                            onSetScannedCurrency = { scannedCurrency = it },
                            onSetScannedCategory = { scannedCategory = it },
                            onSetScannedDate = { scannedDate = it },
                            scannedBitmap = scannedBitmap,
                            scannedItems = scannedItems,
                            onSetScannedItems = { scannedItems = it },
                            scannedBalanceDate = scannedBalanceDate,
                            onSetScannedBalanceDate = { scannedBalanceDate = it },
                            saveReceiptPhoto = saveReceiptPhoto,
                            onSetSaveReceiptPhoto = { saveReceiptPhoto = it },
                            onLaunchCamera = {
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                try {
                                    if (hasCameraPermission) {
                                        launchHighResCamera()
                                    } else {
                                        permissionLauncherDirect.launch(android.Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    errorMessageScan = "Camera Error: ${e.message}"
                                }
                            },
                            onLaunchGallery = {
                                try {
                                    imagePickerLauncherDirect.launch("image/*")
                                } catch (e: Exception) {
                                    errorMessageScan = "Gallery Error: ${e.message}"
                                }
                            }
                        )
                        "cuentas" -> AccountsTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "tareas" -> TasksTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "compras" -> ComprasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "metas" -> MetasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                    }
                }

                // Global Config Dialog as popup at the top
                if (showConfigDialog) {
                    ConfigDialog(viewModel = viewModel, onDismiss = { showConfigDialog = false })
                }
                
                if (showUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = { Text("Actualización Disponible") },
                        text = { Text("Hay una nueva versión de la aplicación disponible. ¿Deseas descargarla e instalarla ahora?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showUpdateDialog = false
                                try {
                                    val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}"))
                                    context.startActivity(playIntent)
                                } catch (e: Exception) {
                                    try {
                                        val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(if (updateUrl.startsWith("http")) updateUrl else "https://play.google.com/store/apps/details?id=${context.packageName}"))
                                        context.startActivity(webIntent)
                                    } catch (e2: Exception) {
                                        android.widget.Toast.makeText(context, "No se pudo abrir el enlace de actualización.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Text("Actualizar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateDialog = false }) {
                                Text("Más tarde")
                            }
                        }
                    )
                }

                // Top banner notification system
                topNotification?.let { msg ->
                    TopNotificationBanner(
                        message = msg,
                        onDismiss = { viewModel.dismissNotification() }
                    )
                }
                
                ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize().zIndex(9999f))
                }
            }
        }
    }
}

// Custom Floating Pill Bottom Navigation Bar (Redesigned Modern Glassmorphism)
@Composable
fun FamilyBottomNavigation(currentTab: String, isDarkTheme: Boolean, onTabSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    if (isDarkTheme) Color(0xFF141418).copy(alpha = 0.94f)
                    else Color(0xFFFFFFFF).copy(alpha = 0.94f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            val tabs = listOf(
                Triple("inicio", "Inicio", if (currentTab == "inicio") Icons.Filled.Home else Icons.Outlined.Home),
                Triple("cuentas", "Finanzas", if (currentTab == "cuentas") Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet),
                Triple("tareas", "Tareas", if (currentTab == "tareas") Icons.Filled.ListAlt else Icons.Outlined.ListAlt),
                Triple("compras", "Compras", if (currentTab == "compras") Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart),
                Triple("metas", "Metas", if (currentTab == "metas") Icons.Filled.Star else Icons.Outlined.StarBorder)
            )

            val selectedIndex = tabs.indexOfFirst { it.first == currentTab }.coerceAtLeast(0)
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "indicatorOffset"
            )

            // Sleek active tab fluid pill backdrop (Subtle, elegant capsule with soft tint)
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 5.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.20f else 0.14f)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.40f else 0.30f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (tabId, label, icon) ->
                    val selected = currentTab == tabId
                    val iconColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDarkTheme) Color.White.copy(alpha = 0.52f) else Color(0xFF64748B)
                    }
                    val iconSize by animateDpAsState(
                        targetValue = if (selected) 23.dp else 20.dp,
                        label = "tabIconSize"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tabId) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier.size(iconSize)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// Floating notification bar
@Composable
fun BoxScope.TopNotificationBanner(message: String, onDismiss: () -> Unit) {
    // Auto dismiss after 3.5 seconds
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AlertRedBg)
            .border(1.dp, AlertRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onDismiss() }
            .padding(16.dp) ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta",
                tint = AlertRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = AlertRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1.0f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp) ){
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Cerrar",
                    tint = AlertRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// --- TAB 1: INICIO (DASHBOARD) ---
@Composable
fun MonthSelector(
    selectedMonth: java.util.Calendar,
    onMonthChange: (java.util.Calendar) -> Unit
) {
    val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES"))
    val monthStr = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val prev = selectedMonth.clone() as java.util.Calendar
            prev.add(java.util.Calendar.MONTH, -1)
            onMonthChange(prev)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Mes Anterior", tint = MaterialTheme.colorScheme.primary)
        }
        Row(
            modifier = Modifier.clickable {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, _ ->
                        val newCal = selectedMonth.clone() as java.util.Calendar
                        newCal.set(java.util.Calendar.YEAR, year)
                        newCal.set(java.util.Calendar.MONTH, month)
                        newCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        onMonthChange(newCal)
                    },
                    selectedMonth.get(java.util.Calendar.YEAR),
                    selectedMonth.get(java.util.Calendar.MONTH),
                    1
                ).show()
            }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Seleccionar Mes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = monthStr,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onBackground)
        }

        IconButton(onClick = {
            val next = selectedMonth.clone() as java.util.Calendar
            next.add(java.util.Calendar.MONTH, 1)
            onMonthChange(next)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.ArrowForward, contentDescription = "Mes Siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: FamilyViewModel,
    onOpenConfig: () -> Unit,
    isScanningDirect: Boolean,
    errorMessageScan: String?,
    successMessageScan: String?,
    showConfirmScanDialog: Boolean,
    scannedAmount: String,
    scannedDesc: String,
    scannedCurrency: String,
    scannedCategory: String,
    scannedDate: Long,
    onSetIsScanningDirect: (Boolean) -> Unit,
    onSetErrorMessageScan: (String?) -> Unit,
    onSetSuccessMessageScan: (String?) -> Unit,
    onSetShowConfirmScanDialog: (Boolean) -> Unit,
    onSetScannedAmount: (String) -> Unit,
    onSetScannedDesc: (String) -> Unit,
    onSetScannedCurrency: (String) -> Unit,
    onSetScannedCategory: (String) -> Unit,
    onSetScannedDate: (Long) -> Unit,
    scannedBitmap: android.graphics.Bitmap?,
    scannedItems: List<com.example.api.ParsedReceiptItem> = emptyList(),
    onSetScannedItems: (List<com.example.api.ParsedReceiptItem>) -> Unit = {},
    scannedBalanceDate: Long = System.currentTimeMillis(),
    onSetScannedBalanceDate: (Long) -> Unit = {},
    saveReceiptPhoto: Boolean,
    onSetSaveReceiptPhoto: (Boolean) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val isRegisteringNlp by viewModel.isRegisteringNlp.collectAsStateWithLifecycle()
    val nlpError by viewModel.nlpError.collectAsStateWithLifecycle()

    var nlpInputText by remember { mutableStateOf("") }

    // --- AI & Sincronización State variables ---
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var showEmailSyncDialog by remember { mutableStateOf(false) }
    val recurringPayments by viewModel.recurringPayments.collectAsStateWithLifecycle()
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var showPaidDetailsDialog by remember { mutableStateOf(false) }
    var selectedRecurringPayment by remember { mutableStateOf<com.example.database.RecurringPayment?>(null) }
    var recNameInput by remember { mutableStateOf("") }
    var recAmountInput by remember { mutableStateOf("") }
    var recCurrencySelected by remember { mutableStateOf("USD") }
    var recDueDayInput by remember { mutableStateOf("5") }
    var recEmojiInput by remember { mutableStateOf("⚡") }
    // showConfirmScanDialog, isScanningDirect, errorMessageScan, successMessageScan,
    // scannedAmount, scannedDesc, scannedCurrency, scannedCategory are now hoisted to MainScreen
    
    // Email sync flows & simulation
    var isLinkingEmail by remember { mutableStateOf(false) }
    var isEmailLinked by remember { mutableStateOf(false) }
    var isFetchingEmails by remember { mutableStateOf(false) }
    var selectedEmailsToSync by remember { mutableStateOf(setOf<Int>()) }
    val hideBalanceSetting = settings.find { it.key == "hide_balance" }?.value
    var showBalance by remember(hideBalanceSetting) { mutableStateOf(hideBalanceSetting != "true") }

    // imagePickerLauncherDirect, cameraLauncherDirect, launchHighResCamera, permissionLauncher
    // are now hoisted to MainScreen and passed via onLaunchCamera / onLaunchGallery lambdas
    
    // Read live rates & parameters
    val bcvRateStr = settings.find { it.key == "bcv_rate" }?.value ?: "36.85"
    val bcvRate = bcvRateStr.toDoubleOrNull() ?: 36.85
    val totalLimitStr = settings.find { it.key == "total_limit_usd" }?.value ?: "1800.0"
    val totalLimit = totalLimitStr.toDoubleOrNull() ?: 1800.0

    // FAMILIA COMPARTIDA: Todos los miembros ven todas las transacciones
    val userTransactions = transactions.sortedByDescending { if (it.balanceDate > 0) it.balanceDate else it.date }
    val curMonth = selectedMonth.get(java.util.Calendar.MONTH)
    val curYear = selectedMonth.get(java.util.Calendar.YEAR)
    
    val currentMonthTransactions = userTransactions.filter {
        val tCal = java.util.Calendar.getInstance()
        val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
        tCal.timeInMillis = filterDate
        tCal.get(java.util.Calendar.MONTH) == curMonth && tCal.get(java.util.Calendar.YEAR) == curYear
    }

    val totalSpentUsd = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val totalIncomeUsd = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
    val baseLimitUsd = totalLimit + totalIncomeUsd // Configurado en Ajustes + Ingresos
    val disponibleUsd = baseLimitUsd - totalSpentUsd // DISPONIBLE = BASE - CONSUMIDO
    val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(selectedMonth.time)
    val presupuestadoUsd = categories.sumOf { it.limitsPerMonth[monthKey] ?: it.limitAmount } // Suma de categorias
    val remainingBudgetUsd = presupuestadoUsd + totalIncomeUsd - totalSpentUsd
    val libreBudgetUsd = baseLimitUsd - presupuestadoUsd
    val totalAvailableForPercentage = presupuestadoUsd + totalIncomeUsd
    val spentPercentage = if (baseLimitUsd > 0) (totalSpentUsd / baseLimitUsd * 100).roundToInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        // App top header (Branding and Settings)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small premium container for fami Logo
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFF0D9488), Color(0xFF2DD4BF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FamiLogo(modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentUser.isNotBlank()) "¡Hola, $currentUser! 👋" else "Nido",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Text(
                            text = "Tu hogar al día • Finanzas & Convivencia",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SubtitleText
                        )
                    }
                }

                // Configuración gear button at the top right
                IconButton(
                    onClick = onOpenConfig,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape) ){
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            ModernMonthSelector(selectedMonth = selectedMonth, onMonthChange = { viewModel.updateSelectedMonth(it) })
            Spacer(modifier = Modifier.height(10.dp))
        }
        // Modern 3D Premium Credit Card (Vibrant Tone & Correct Math)
        item {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color(0xFF3F51B5).copy(alpha = 0.4f),
                        spotColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3730A3),
                                Color(0xFF4F46E5),
                                Color(0xFF1E1B4B)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 13.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "BALANCE GENERAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            IconToggleButton(
                                checked = showBalance,
                                onCheckedChange = { 
                                    showBalance = it
                                    viewModel.updateValueSetting("hide_balance", if (it) "false" else "true")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Balance",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Surface(
                            onClick = { viewModel.syncLiveBcvRate() },
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(11.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BCV: $bcvRateStr", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("DISPONIBLE DEL MES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 0.8.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (showBalance) "$${String.format(Locale.US, "%,.2f", disponibleUsd)} USD" else "•••••• USD",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        if (showBalance) {
                            Text(
                                text = "≈ Bs. ${String.format(Locale.US, "%,.2f", disponibleUsd * bcvRate)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFC7D2FE),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.24f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BASE + INGRESOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            Text(if (showBalance) "$${String.format(Locale.US, "%,.0f", baseLimitUsd)}" else "••••", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PRESUPUESTADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            Text(if (showBalance) "$${String.format(Locale.US, "%,.0f", presupuestadoUsd)}" else "••••", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CONSUMIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            Text(if (showBalance) "$${String.format(Locale.US, "%,.2f", totalSpentUsd)}" else "••••", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (totalSpentUsd > 0) Color(0xFFFF8A80) else Color(0xFF69F0AE))
                        }
                    }
                        
                    Spacer(modifier = Modifier.height(10.dp))
                        
                    val healthBarColor = when {
                        spentPercentage >= 95 -> Color(0xFFFF5252)
                        spentPercentage >= 75 -> Color(0xFFFFD740)
                        else -> Color(0xFF69F0AE)
                    }
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (if (baseLimitUsd > 0) (totalSpentUsd / baseLimitUsd) else 0.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = healthBarColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                        
                    Spacer(modifier = Modifier.height(8.dp))
                        
                    // Status alert
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (spentPercentage >= 85) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = healthBarColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                spentPercentage > 100 -> "Excediste tu base mensual por $${String.format(Locale.US, "%,.2f", totalSpentUsd - baseLimitUsd)} USD"
                                spentPercentage >= 85 -> "Atención: Has consumido el $spentPercentage% de tu disponible mensual"
                                else -> "Saludable: Has consumido el $spentPercentage% de tu disponible mensual"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                    }
                }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            ModernRecurringPaymentsSection(
                recurringPayments = recurringPayments,
                monthKey = monthKey,
                onPaymentClick = { rec ->
                    selectedRecurringPayment = rec
                    val isPaid = rec.paidMonths[monthKey] == true
                    if (isPaid) {
                        showPaidDetailsDialog = true
                    } else {
                        recAmountInput = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
                        recCurrencySelected = rec.currency
                        showMarkPaidDialog = true
                    }
                },
                onAddPaymentClick = {
                    selectedRecurringPayment = null
                    recNameInput = ""
                    recAmountInput = ""
                    recCurrencySelected = "USD"
                    recDueDayInput = "5"
                    recEmojiInput = "⚡"
                    showAddRecurringDialog = true
                },
                onSeedDefaultPayments = {
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Condominio", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🏢", dueDay = 5))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Seguro Médico / Familiar", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🛡️", dueDay = 10))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Servicio Eléctrico (Luz)", defaultAmount = 0.0, category = "Servicios", iconEmoji = "⚡", dueDay = 15))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Internet / WiFi", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🌐", dueDay = 20))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Seguro Vehicular", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🚗", dueDay = 25))
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Conversational Intelligent NLP block & Receipt Scanner Unificado
        item {
            PremiumCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp) ){
                Column(modifier = Modifier.padding(18.dp) ){
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Input",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REGISTRO INTELIGENTE CON IA ✨",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Voz • Texto • Foto",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Escribe tu gasto en lenguaje natural o escanea la factura para desglosar cada producto automáticamente.",
                        fontSize = 11.5.sp,
                        color = SubtitleText,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Chips rápidos de ejemplo para agilizar escritura
                    val quickPrompts = listOf(
                        "🛒 Mercado 45$" to "Gasté 45$ en Supermercado",
                        "⛽ Gasolina 20$" to "Gasté 20$ en Gasolina",
                        "🥖 Panadería 350 Bs" to "Gasté 350 bs en Panadería",
                        "💊 Farmacia 15$" to "Gasté 15$ en Farmatodo"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickPrompts) { (chipLabel, promptText) ->
                            Surface(
                                onClick = { nlpInputText = promptText },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = chipLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = nlpInputText,
                        onValueChange = { nlpInputText = it },
                        placeholder = { Text("ej: Gasté 450 bs en comida...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            if (isRegisteringNlp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (nlpInputText.isNotEmpty()) {
                                        IconButton(
                                            onClick = { nlpInputText = "" },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpiar",
                                                tint = SubtitleText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        if (nlpInputText.isNotBlank() ){
                                            viewModel.registerFromNaturalLanguage(nlpInputText)
                                            nlpInputText = ""
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Enviar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        ),
                        maxLines = 2
                    )

                    if (nlpError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nlpError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp) ){
                        // Take Photo Button (Camera)
                        Button(
                            onClick = { onLaunchCamera() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                            Icon(Icons.Default.CameraAlt, contentDescription = "Tomar Foto", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear Recibo", fontSize = 11.5.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }

                        // Select Gallery Button (Gallery)
                        FilledTonalButton(
                            onClick = { onLaunchGallery() },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Galería", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desde Galería", fontSize = 11.5.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Scan Loading Indicators (Adaptativos a Modo Claro y Oscuro)
                    if (isScanningDirect) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "IA analizando factura y extrayendo productos...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (errorMessageScan != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Text(
                                    text = errorMessageScan!!,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onSetErrorMessageScan(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SubtitleText, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (successMessageScan != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, "Éxito", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Text(
                                    text = successMessageScan!!,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onSetSuccessMessageScan(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SubtitleText, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // COMPLETE MONTHLY CALENDAR GRID WIDGET
        item {
            var calendarMonth by remember(selectedMonth) { 
                mutableStateOf(Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedMonth.get(Calendar.YEAR))
                    set(Calendar.MONTH, selectedMonth.get(Calendar.MONTH))
                }) 
            }
            var selectedDayCal by remember(selectedMonth) { 
                mutableStateOf(Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedMonth.get(Calendar.YEAR))
                    set(Calendar.MONTH, selectedMonth.get(Calendar.MONTH))
                }) 
            }

            val year = calendarMonth.get(Calendar.YEAR)
            val month = calendarMonth.get(Calendar.MONTH)
            val monthLabel = getMonthNameSpanish(month)

            val firstDayOfMonth = (calendarMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
            val emptyDaysBefore = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
            val maxDaysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

            PremiumCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp) ){
                Column(modifier = Modifier.padding(16.dp) ){
                    // Header controls of calendar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALENDARIO REGISTRO COMPLETO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                        add(Calendar.MONTH, -1)
                                    }
                                },
                                modifier = Modifier.size(28.dp) ){
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$monthLabel $year",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                        add(Calendar.MONTH, 1)
                                    }
                                },
                                modifier = Modifier.size(28.dp) ){
                                Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Column Label Headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do").forEach { wDay ->
                            Text(
                                text = wDay,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SubtitleText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Dynamic days grid cells
                    val totalCells = emptyDaysBefore + maxDaysInMonth
                    val rows = (0 until totalCells).chunked(7)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp) ){
                        rows.forEach { rowIdxs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (0 until 7).forEach { col ->
                                    val cellIndex = rowIdxs.getOrNull(col)
                                    if (cellIndex == null || cellIndex < emptyDaysBefore) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val dayNum = cellIndex - emptyDaysBefore + 1
                                        val cellCal = (calendarMonth.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, dayNum)
                                        }

                                        val isToday = isSameDay(System.currentTimeMillis(), cellCal)
                                        val isSelected = isSameDay(selectedDayCal.timeInMillis, cellCal)

                                        val dayTransactions = userTransactions.filter { 
                                            val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
                                            isSameDay(filterDate, cellCal) 
                                        }

                                        val dayCompletedChores = chores.filter { it.isCompleted && it.completedDate != null && isSameDay(it.completedDate, cellCal) }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                )
                                                .scaleClickable { selectedDayCal = cellCal
                                                }
                                                .border(
                                                    width = if (isToday) 1.5.dp else if (isSelected) 1.dp else 0.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primary else OutlineGrey.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize() ){
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                
                                                // Category dots + chore indicators
                                                if (dayTransactions.isNotEmpty() || dayCompletedChores.isNotEmpty() ){
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(top = 2.dp) ){
                                                        dayTransactions.take(2).forEach { tx ->
                                                            val catColorHex = categories.find { it.categoryName.equals(tx.category, ignoreCase = true) }?.colorHex ?: "#6750A4"
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else catColorHex.toColor())
                                                            )
                                                        }
                                                        if (dayCompletedChores.isNotEmpty() ){
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (isSelected) Color.White else Color(0xFF10B981),
                                                                modifier = Modifier.size(6.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Day detailed records
                    val selectedDateTimeLabel = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es")).format(selectedDayCal.time)
                    val dayTx = userTransactions.filter { 
                        val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
                        isSameDay(filterDate, selectedDayCal) 
                    }
                    val dayLogs = activityLogs.filter { isSameDay(it.timestamp, selectedDayCal) }
                    val dayCompletedChores = chores.filter { it.isCompleted && it.completedDate != null && isSameDay(it.completedDate, selectedDayCal) }

                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.HorizontalDivider(color = OutlineGrey.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                         text = "DETALLE DE ACTIVIDAD: ${selectedDateTimeLabel.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (dayTx.isEmpty() && dayLogs.isEmpty() && dayCompletedChores.isEmpty() ){
                        Text(
                            text = "No hay financiamientos ni actividades registradas en este día.",
                            fontSize = 11.sp,
                            color = SubtitleText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp) ){
                            // Render completed chores first (Adaptativo a Modo Claro / Oscuro)
                            dayCompletedChores.forEach { chore ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.30f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completada",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f) ){
                                                Text(chore.title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                Text("Tarea • Asignado: ${chore.assignedTo} • Prioridad: ${chore.priority}", fontSize = 9.sp, color = SubtitleText, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            }
                                        }
                                        Text(
                                            text = "COMPLETADA",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            dayTx.forEach { tx ->
                                val symbol = if (tx.currency == "USD") "$" else "Bs"
                                val prefix = if (tx.isIncome) "+" else "-"
                                val amountColor = if (tx.isIncome) Color(0xFF2E7D32) else AlertRed
                                val assocColor = if (tx.isIncome) "#2E7D32" else (categories.find { it.categoryName.equals(tx.category, ignoreCase = true) }?.colorHex ?: "#6750A4")

                                PremiumCard(
                                    modifier = Modifier.fillMaxWidth() ){
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(20.dp).clip(CircleShape).background(assocColor.toColor())
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f) ){
                                                Text(tx.description, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                Text("Categoría: ${tx.category} • Por: ${tx.registeredBy}", fontSize = 8.sp, color = SubtitleText, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$prefix ${tx.originalAmount} $symbol",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = amountColor
                                            )
                                            if (tx.currency != "USD") {
                                                Text(
                                                    text = "(${String.format("%.2f", tx.amount)} $)",
                                                    fontSize = 9.sp,
                                                    color = SubtitleText,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            dayLogs.take(3).forEach { log ->
                                PremiumCard(
                                    modifier = Modifier.fillMaxWidth() ){
                                    Row(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp) ){
                                            Text(log.tag, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(log.message, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }


    }

    // --- DIÁLOGOS DE PAGOS RECURRENTES Y ESCÁNER IA (FUERA DE LAZYCOLUMN PARA EVITAR DESCOMPOSICIÓN AL HACER SCROLL) ---
    com.example.ui.ModernRecurringPaymentDialogs(
        monthKey = monthKey,
        selectedMonth = selectedMonth,
        selectedRecurringPayment = selectedRecurringPayment,
        showMarkPaidDialog = showMarkPaidDialog,
        showPaidDetailsDialog = showPaidDetailsDialog,
        showAddRecurringDialog = showAddRecurringDialog,
        recNameInput = recNameInput,
        recAmountInput = recAmountInput,
        recCurrencySelected = recCurrencySelected,
        recDueDayInput = recDueDayInput,
        recEmojiInput = recEmojiInput,
        onNameChange = { recNameInput = it },
        onAmountChange = { recAmountInput = it },
        onCurrencyChange = { recCurrencySelected = it },
        onDueDayChange = { recDueDayInput = it },
        onEmojiChange = { recEmojiInput = it },
        onDismissMarkPaid = { showMarkPaidDialog = false },
        onDismissPaidDetails = { showPaidDetailsDialog = false },
        onDismissAddRecurring = { showAddRecurringDialog = false },
        onSwitchToEdit = { rec ->
            selectedRecurringPayment = rec
            recNameInput = rec.name
            recAmountInput = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
            recCurrencySelected = rec.currency
            recDueDayInput = rec.dueDay.toString()
            recEmojiInput = rec.iconEmoji.ifBlank { "⚡" }
            showAddRecurringDialog = true
        },
        onConfirmMarkPaid = { rec, amt, cur, dateMillis ->
            viewModel.markRecurringPaymentPaid(rec, monthKey, amt, cur, dateMillis)
            showMarkPaidDialog = false
        },
        onConfirmMarkUnpaid = { rec ->
            viewModel.markRecurringPaymentUnpaid(rec, monthKey)
            showPaidDetailsDialog = false
        },
        onSaveRecurring = { item ->
            viewModel.createOrEditRecurringPayment(item)
            showAddRecurringDialog = false
        },
        onDeleteRecurring = { rec ->
            viewModel.deleteRecurringPayment(rec)
            showAddRecurringDialog = false
        }
    )

    if (showConfirmScanDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { onSetShowConfirmScanDialog(false) },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxSize()
                ) {
                    // --- FIXED HEADER ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7C4DFF), Color(0xFF651FFF))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Confirmar IA",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Confirmar Recibo ✨",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extracción Inteligente por IA",
                                    fontSize = 10.sp,
                                    color = SubtitleText
                                )
                            }
                        }
                        IconButton(
                            onClick = { onSetShowConfirmScanDialog(false) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = SubtitleText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Divider(color = OutlineGrey.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // --- SCROLLABLE BODY ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            value = scannedDesc,
                            onValueChange = { onSetScannedDesc(it) },
                            label = { Text("Comercio / Detalle", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = scannedAmount,
                                onValueChange = { onSetScannedAmount(it) },
                                label = { Text("Monto Total", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.3f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853)),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Moneda:", fontSize = 10.sp, color = SubtitleText)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("USD", "VES").forEach { m ->
                                        val isSelected = scannedCurrency == m
                                        Button(
                                            onClick = { onSetScannedCurrency(m) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (m == "USD") "$" else "Bs.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Text("Fechas del Registro:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .scaleClickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = scannedDate }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val newCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                }
                                                onSetScannedDate(newCal.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OutlineGrey.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📄 Fecha Factura", fontSize = 10.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(sdfDate.format(Date(scannedDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .scaleClickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = scannedBalanceDate }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val newCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                }
                                                onSetScannedBalanceDate(newCal.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("⚖️ Fecha Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(sdfDate.format(Date(scannedBalanceDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Text("Categoría Principal:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SubtitleText)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            this@LazyRow.items(categories) { cat ->
                                val isSelected = scannedCategory == cat.categoryName
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetScannedCategory(cat.categoryName) },
                                    label = { Text(cat.categoryName, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        if (scannedItems.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🛒 Desglose de Productos (${scannedItems.size} ítems)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "IVA Incluido",
                                        fontSize = 9.sp,
                                        color = SubtitleText,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                androidx.compose.material3.Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        scannedItems.forEach { item ->
                                            androidx.compose.material3.Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(10.dp),
                                                tonalElevation = 1.dp,
                                                border = BorderStroke(1.dp, OutlineGrey.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (item.category.isNotBlank()) {
                                                            androidx.compose.material3.Surface(
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                                shape = RoundedCornerShape(4.dp),
                                                                modifier = Modifier.padding(top = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = item.category,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "${item.amount} $scannedCurrency",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF00C853)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            androidx.compose.material3.Switch(
                                checked = saveReceiptPhoto,
                                onCheckedChange = { onSetSaveReceiptPhoto(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar foto como respaldo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Divider(color = OutlineGrey.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { onSetShowConfirmScanDialog(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Descartar", color = SubtitleText, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val amt = scannedAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (amt > 0.0 && scannedDesc.isNotBlank()) {
                                    coroutineScope.launch {
                                        var receiptUrl = ""
                                        if (saveReceiptPhoto && scannedBitmap != null) {
                                            receiptUrl = viewModel.uploadReceiptImage(scannedBitmap!!) ?: ""
                                        }
                                        val txItems = scannedItems.map { item ->
                                            val catName = if (categories.any { c -> c.categoryName == item.category }) item.category else (if (scannedCategory.isEmpty()) (categories.firstOrNull()?.categoryName ?: "General") else scannedCategory)
                                            com.example.database.TransactionItem(name = item.name, amount = item.amount, category = catName)
                                        }
                                        viewModel.addTransaction(
                                            originalAmount = amt,
                                            category = if (scannedCategory.isEmpty()) (categories.firstOrNull()?.categoryName ?: "General") else scannedCategory,
                                            description = scannedDesc,
                                            currency = scannedCurrency,
                                            isIncome = false,
                                            date = scannedDate,
                                            balanceDate = scannedBalanceDate,
                                            receiptUrl = receiptUrl,
                                            items = txItems
                                        )
                                        val msg = if (txItems.isNotEmpty()) "Recibo registrado ($amt $scannedCurrency) con ${txItems.size} ítems en $scannedDesc" else "Gasto registrado: $amt $scannedCurrency en $scannedDesc"
                                        onSetSuccessMessageScan(msg)
                                        onSetShowConfirmScanDialog(false)
                                    }
                                } else {
                                    onSetErrorMessageScan("Por favor ingrese un monto y descripción válidos.")
                                    onSetShowConfirmScanDialog(false)
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Registrar Gasto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEmailSyncDialog) {
        var simMailInput by remember { mutableStateOf(currentUser.ifBlank { "julioingdlop@gmail.com" }) }
        val detectedEmailsList = listOf(
            Triple("PedidosYa - Delivery Cena 🍔", 14.50, "USD"),
            Triple("Netflix - Suscripción Familiar 📺", 7.99, "USD"),
            Triple("Farmatodo Sabana Grande 💊", 850.00, "VES"),
            Triple("MercadoLibre Venezuela 📦", 1200.00, "VES")
        )

        AlertDialog(
            onDismissRequest = { showEmailSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Vincular Correo Electrónico ✉️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState() )){

                    if (!isEmailLinked) {
                        Text(
                            "Vincule su correo para que la IA escanee su bandeja en búsqueda de recibos digitales de compras y suscripciones de manera automática.",
                            fontSize = 11.sp,
                            color = SubtitleText,
                            lineHeight = 14.sp
                        )

                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = simMailInput,
                            onValueChange = { simMailInput = it },
                            label = { Text("Correo Electrónico", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            singleLine = true
                        )

                        if (isLinkingEmail) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Solicitando autorización de lectura segura OAUTH2...", fontSize = 10.sp, color = SubtitleText)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLinkingEmail = true
                                        kotlinx.coroutines.delay(1200)
                                        isEmailLinked = true
                                        isLinkingEmail = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                                Text("Otorgar Permiso de Correo e IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                            Icon(Icons.Default.CheckCircle, "Linked", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Text("Bandeja vinculada: $simMailInput", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                        }

                        if (isFetchingEmails) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscando facturas con IA de Gemini...", fontSize = 11.sp, color = SubtitleText)
                            }
                        } else if (selectedEmailsToSync.isEmpty() && !isFetchingEmails) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isFetchingEmails = true
                                        kotlinx.coroutines.delay(1000)
                                        selectedEmailsToSync = setOf(0, 1)
                                        isFetchingEmails = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A) )){
                                Text("Buscar nuevos recibos en bandeja", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedEmailsToSync.isNotEmpty() ){
                            Text("Recibos detectados por IA en tus correos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            detectedEmailsList.forEachIndexed { index, item ->
                                val isChecked = selectedEmailsToSync.contains(index)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .scaleClickable { selectedEmailsToSync = if (isChecked) {
                                                selectedEmailsToSync - index
                                            } else {
                                                selectedEmailsToSync + index
                                            }
                                        }
                                        .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedEmailsToSync = if (checked == true) {
                                                selectedEmailsToSync + index
                                            } else {
                                                selectedEmailsToSync - index
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f) ){
                                        Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("Monto detectado: ${item.second} ${item.third}", fontSize = 9.5.sp, color = SubtitleText)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    var count = 0
                                    selectedEmailsToSync.forEach { idx ->
                                        val entry = detectedEmailsList[idx]
                                        viewModel.addTransaction(
                                            originalAmount = entry.second,
                                            category = "Servicios",
                                            description = entry.first,
                                            currency = entry.third,
                                            isIncome = false
                                        )
                                        count++
                                    }
                                    onSetSuccessMessageScan("Sincronizados con éxito: $count gastos nuevos registrados desde Correo.")
                                    selectedEmailsToSync = emptySet()
                                    showEmailSyncDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp) ){
                                Text("Sincronizar e importar marcados (${selectedEmailsToSync.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = {
                                isEmailLinked = false
                                selectedEmailsToSync = emptySet()
                            },
                            modifier = Modifier.fillMaxWidth() ){
                            Text("Desvincular Correo", color = Color(0xFFEF4444), fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmailSyncDialog = false }) {
                    Text("Cerrar", color = SubtitleText, fontSize = 11.sp)
                }
            }
        )
    }
}

// Helpers for calendar tracking
fun getTodayMidnight(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

fun isSameDay(timestampMillis: Long, targetCal: Calendar): Boolean {
    val txCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return txCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
           txCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
           txCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
}

fun getMonthNameSpanish(monthIndex: Int): String {
    return when (monthIndex) {
        Calendar.JANUARY -> "Enero"
        Calendar.FEBRUARY -> "Febrero"
        Calendar.MARCH -> "Marzo"
        Calendar.APRIL -> "Abril"
        Calendar.MAY -> "Mayo"
        Calendar.JUNE -> "Junio"
        Calendar.JULY -> "Julio"
        Calendar.AUGUST -> "Agosto"
        Calendar.SEPTEMBER -> "Septiembre"
        Calendar.OCTOBER -> "Octubre"
        Calendar.NOVEMBER -> "Noviembre"
        Calendar.DECEMBER -> "Diciembre"
        else -> ""
    }
}



// --- DIALOGO DE ANALISTA FINANCIERO CON IA ---
@Composable
fun FinanceAiAnalysisDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    report: FinanceAiReport?,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    if (!isOpen) return

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.6f),
                            Color(0xFF06B6D4).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Analista Financiero IA",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "INTELIGENTE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Diagnóstico en tiempo real de tus gastos",
                                fontSize = 11.sp,
                                color = SubtitleText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(46.dp),
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analizando consumos y facturas...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Identificando en qué gastas más y oportunidades de ahorro",
                                fontSize = 11.sp,
                                color = SubtitleText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = AlertRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar Análisis")
                        }
                    }
                } else if (report != null) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // HERO CARD: EN QUÉ GASTAS MÁS
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF3730A3),
                                                Color(0xFF4F46E5),
                                                Color(0xFF1E1B4B)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DONDE SE VA MÁS DINERO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            letterSpacing = 1.sp
                                        )
                                        val healthColor = when (report.budgetHealth) {
                                            "Saludable" -> Color(0xFF4ADE80)
                                            "Precaución" -> Color(0xFFFBBF24)
                                            else -> Color(0xFFF87171)
                                        }
                                        Surface(
                                            color = healthColor.copy(alpha = 0.20f),
                                            border = BorderStroke(1.dp, healthColor.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = report.budgetHealth.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = healthColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = report.topSpendingCategory,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${String.format("%.2f", report.topSpendingAmountUsd)} USD consumidos (${report.topSpendingPercentage}% del gasto total)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        // RESUMEN EJECUTIVO
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📝", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Diagnóstico del Mes",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = report.summary,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // PUNTOS CLAVE DE CONSUMO
                        if (report.topExpensesSummary.isNotEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🎯", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Concentración de Gastos",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.topExpensesSummary.forEach { point ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("•", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(point, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // FUGAS DE DINERO Y GASTOS HORMIGA
                        if (report.moneyLeaks.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚠️", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Fugas de Dinero / Alertas",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.moneyLeaks.forEach { leak ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("🔍", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(leak, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CONSEJOS DE AHORRO ACCIONABLES
                        if (report.savingsTips.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💡", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Consejos de Ahorro para tu Hogar",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.savingsTips.forEach { tip ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("✓", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefresh,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("Entendido", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: CUENTAS, CATEGORÍAS & GASTOS (ARQUITECTURA FINTECH MODULAR) ---
@Composable
fun AccountsTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    com.example.ui.ModernAccountsTab(viewModel = viewModel, onOpenConfig = onOpenConfig)
}

// --- TAB 3: TAREAS CON PRIORIDADES ---
@Composable
fun TasksTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var showChoreDialog by remember { mutableStateOf(false) }
    var isEditingChore by remember { mutableStateOf(false) }
    var editingChore by remember { mutableStateOf<Chore?>(null) }

    var taskTitle by remember { mutableStateOf("") }
    var taskCategorySelected by remember { mutableStateOf("General") }
    var taskDesc by remember { mutableStateOf("") }
    var taskAssignee by remember { mutableStateOf("Julio") }
    var taskPrioritySelected by remember { mutableStateOf("Medio") } // Alto, Medio, Bajo
    var taskIsPrivate by remember { mutableStateOf(false) }

    var activeTaskFilter by remember { mutableStateOf("pendientes") } // "pendientes" or "completadas"

    var showListManager by remember { mutableStateOf(false) }
    var listNameToEdit by remember { mutableStateOf<String?>(null) }
    var editListNameInput by remember { mutableStateOf("") }

    val listNames = remember(chores, settings) {
        val dbListsSetting = settings.find { it.key == "task_lists" }?.value
        val dbLists = dbListsSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val itemLists = chores.map { it.category }
        val allButGeneral = (dbLists + itemLists).filter { !it.equals("General", ignoreCase = true) && !it.equals("Todas", ignoreCase = true) }.distinct().sorted()
        listOf("Todas") + allButGeneral
    }
    var selectedListName by remember { mutableStateOf("Todas") }
    var selectedPriorityFilter by remember { mutableStateOf("Todas") }

    val userChores = chores.filter { !it.isPrivate || it.createdBy == currentUser || it.assignedTo == currentUser }
    val pendingChores = userChores.filter { !it.isCompleted }
    val completedChores = userChores.filter { it.isCompleted }
    val shownChores = (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
        (selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)) &&
        (selectedPriorityFilter.equals("Todas", ignoreCase = true) || it.priority.equals(selectedPriorityFilter, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        // App top header of tasks
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Tareas del Hogar",
                subtitle = "Progreso: ${completedChores.size}/${userChores.size} completadas",
                actionText = "Nueva",
                onActionClick = {
                    isEditingChore = false
                    editingChore = null
                    taskTitle = ""
                    taskDesc = ""
                    taskCategorySelected = if (selectedListName == "Todas") "General" else selectedListName
                    taskAssignee = if (profiles.isNotEmpty()) profiles.first().name else "Julio"
                    taskPrioritySelected = "Medio"
                    taskIsPrivate = false
                    showChoreDialog = true
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tareas lists selector row
        item {
            var showAddListDialog by remember { mutableStateOf(false) }
            var newListNameInput by remember { mutableStateOf("") }
            
            if (showAddListDialog) {
                AlertDialog(
                    onDismissRequest = { showAddListDialog = false },
                    title = { Text("Nueva Lista de Tareas", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = newListNameInput,
                            onValueChange = { newListNameInput = it },
                            placeholder = { Text("Ej: Limpieza, Jardín...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            val cleanList = newListNameInput.trim()
                            if (cleanList.isNotEmpty() && cleanList != "General") {
                                val currentSetting = settings.find { it.key == "task_lists" }?.value
                                val newSettingVal = if (currentSetting.isNullOrBlank()) cleanList else "$currentSetting,$cleanList"
                                viewModel.updateValueSetting("task_lists", newSettingVal)
                                selectedListName = cleanList
                            }
                            newListNameInput = ""
                            showAddListDialog = false
                        }) {
                            Text("Crear")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddListDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showListManager) {
                AlertDialog(
                    onDismissRequest = { showListManager = false },
                    title = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Mis Listas de Tareas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showListManager = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                    },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp) ){
                            items(listNames) { listName ->
                                val isEditing = listNameToEdit == listName
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) ){
                                    if (isEditing) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                                                value = editListNameInput,
                                                onValueChange = { editListNameInput = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            IconButton(onClick = {
                                                val cleanList = editListNameInput.trim()
                                                if (cleanList.isNotEmpty() && cleanList != listName && cleanList != "General") {
                                                    // No renameChoreList method yet, so we just add it to settings for now
                                                    val currentSetting = settings.find { it.key == "task_lists" }?.value
                                                    val dbLists = currentSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                                                    if (dbLists.contains(listName) ){
                                                        dbLists[dbLists.indexOf(listName)] = cleanList
                                                        viewModel.updateValueSetting("task_lists", dbLists.joinToString(","))
                                                    }
                                                    if (selectedListName == listName) {
                                                        selectedListName = cleanList
                                                    }
                                                }
                                                listNameToEdit = null
                                            }) {
                                                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                selectedListName = listName
                                                showListManager = false
                                            }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(listName, fontSize = 16.sp, fontWeight = if (selectedListName == listName) FontWeight.Bold else FontWeight.Normal)
                                            if (listName != "General") {
                                                Row {
                                                    IconButton(onClick = {
                                                        listNameToEdit = listName
                                                        editListNameInput = listName
                                                    }, modifier = Modifier.size(32.dp) ){
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                                                    }
                                                    DeleteIconButton(
                                                        modifier = Modifier.size(32.dp),
                                                        iconSize = 20.dp,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        onDeleteConfirm = {
                                                            val currentSetting = settings.find { it.key == "task_lists" }?.value
                                                            val dbLists = currentSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                                                            dbLists.remove(listName)
                                                            viewModel.updateValueSetting("task_lists", dbLists.joinToString(","))
                                                            if (selectedListName == listName) {
                                                                selectedListName = "Todas"
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showAddListDialog = true }, modifier = Modifier.fillMaxWidth() ){
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear Nueva Lista")
                        }
                    }
                )
            }

            ModernListSelectorChips(
                listNames = listNames,
                selectedList = selectedListName,
                onSelectList = { selectedListName = it },
                onAddListClick = { showAddListDialog = true },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter pills (Pendientes vs Completadas)
        item {
            ModernFilterPills(
                options = listOf(
                    "pendientes" to "Pendientes (${pendingChores.size})",
                    "completadas" to "Completadas (${completedChores.size})"
                ),
                selectedId = activeTaskFilter,
                onSelect = { activeTaskFilter = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Priority filter selector row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val priorities = listOf("Todas", "Alto", "Medio", "Bajo")
                priorities.forEach { priority ->
                    val isSelected = selectedPriorityFilter == priority
                    val count = when (priority) {
                        "Todas" -> (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
                            selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)
                        }.size
                        else -> (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
                            (selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)) &&
                            it.priority.equals(priority, ignoreCase = true)
                        }.size
                    }

                    val badgeColor = when (priority) {
                        "Alto" -> Color(0xFFEF5350)  // Soft red
                        "Medio" -> Color(0xFFFFB74D) // Soft orange
                        "Bajo" -> Color(0xFF81C784)  // Soft green
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) badgeColor.copy(alpha = 0.20f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) badgeColor else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedPriorityFilter = priority }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                            if (priority != "Todas") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                            }
                            Text(
                                text = "$priority ($count)",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Homework lists
        if (shownChores.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = if (activeTaskFilter == "pendientes") Icons.Default.CheckCircle else Icons.Default.DoneAll,
                    title = if (activeTaskFilter == "pendientes") "¡Todo listo por ahora!" else "No hay tareas completadas",
                    subtitle = if (activeTaskFilter == "pendientes") "No tienes labores pendientes en esta lista. ¡Excelente trabajo!" else "Completa tareas para ver su registro aquí.",
                    actionText = if (activeTaskFilter == "pendientes") "+ Nueva Tarea" else null,
                    onActionClick = {
                        isEditingChore = false
                        editingChore = null
                        taskTitle = ""
                        taskDesc = ""
                        taskCategorySelected = if (selectedListName == "Todas") "General" else selectedListName
                        taskAssignee = if (profiles.isNotEmpty()) profiles.first().name else "Julio"
                        taskPrioritySelected = "Medio"
                        taskIsPrivate = false
                        showChoreDialog = true
                    }
                )
            }
        } else {
            items(shownChores, key = { it.id.ifEmpty { "${it.title}_${it.createdDate}" } }) { chore ->
                    val priorityColor = when (chore.priority.lowercase() ){
                        "alto" -> AlertRed
                        "medio" -> Color(0xFFF57C00)
                        else -> Color(0xFF00796B)
                    }

                    val cardBgColor = if (chore.isCompleted) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Box {
                        PremiumCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                            onClick = {
                                editingChore = chore
                                isEditingChore = true
                                taskTitle = chore.title
                                taskDesc = chore.description
                                taskAssignee = chore.assignedTo
                                taskPrioritySelected = chore.priority
                                showChoreDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox rounded toggle with soft ring and check mark
                                val checkboxBg = if (chore.isCompleted) Color(0xFF10B981) else Color.Transparent
                                val checkboxBorderColor = if (chore.isCompleted) Color(0xFF10B981) else priorityColor
                                val checkIconTint = Color.White

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(checkboxBg)
                                        .border(2.dp, checkboxBorderColor, CircleShape)
                                        .clickable { viewModel.toggleChoreCompletion(chore) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chore.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Desmarcar",
                                            tint = checkIconTint,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f) ){
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chore.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (chore.isCompleted) SubtitleText.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                            style = androidx.compose.ui.text.TextStyle(
                                                textDecoration = if (chore.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Priority badge tag
                                        val priorityBg = when (chore.priority.lowercase() ){
                                            "alto" -> Color(0xFFFEE2E2)
                                            "medio" -> Color(0xFFFFEDD5)
                                            else -> Color(0xFFCCFBF1)
                                        }
                                        val priorityTextCol = when (chore.priority.lowercase() ){
                                            "alto" -> Color(0xFFEF4444)
                                            "medio" -> Color(0xFFF97316)
                                            else -> Color(0xFF0D9488)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (chore.isCompleted) OutlineGrey.copy(alpha = 0.15f) else priorityBg)
                                                .padding(horizontal = 8.dp, vertical = 4.dp) ){
                                            Text(
                                                text = chore.priority.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (chore.isCompleted) SubtitleText.copy(alpha = 0.6f) else priorityTextCol,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                    if (chore.description.isNotBlank() ){
                                        Text(
                                            text = chore.description,
                                            fontSize = 12.sp,
                                            color = if (chore.isCompleted) SubtitleText.copy(alpha = 0.5f) else SubtitleText.copy(alpha = 0.9f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp),
                                            lineHeight = 16.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                                        // Assignee profile initials bubble
                                        val initialChar = chore.assignedTo.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initialChar,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Asignado a ${chore.assignedTo}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SubtitleText
                                        )
                                        Text(
                                            text = " • ${chore.category}",
                                            fontSize = 11.sp,
                                            color = SubtitleText
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                editingChore = chore
                                                isEditingChore = true
                                                taskTitle = chore.title
                                                taskDesc = chore.description
                                                taskCategorySelected = chore.category
                                                taskAssignee = chore.assignedTo
                                                taskPrioritySelected = chore.priority
                                                taskIsPrivate = chore.isPrivate
                                                showChoreDialog = true
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        DeleteIconButton(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(AlertRed.copy(alpha = 0.10f), CircleShape),
                                            iconSize = 16.dp,
                                            tint = AlertRed,
                                            onDeleteConfirm = { viewModel.deleteChore(chore) }
                                        )
                                    }
                                }
                            }
                        }
                    } // End Box
                }
            }
        }

    // Modal Sheet of Chores Dialog Creator
    if (showChoreDialog) {
        AlertDialog(
            onDismissRequest = { showChoreDialog = false },
            title = {
                Text(
                    text = if (isEditingChore) "Editar Tarea" else "Programar Nueva Tarea",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Título de la tarea") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        )
                    )

                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        ),
                        maxLines = 3
                    )

                    // Lista dropdown selector
                    var expandedListDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth() ){
                        OutlinedTextField(
                            value = taskCategorySelected,
                            onValueChange = {},
                            label = { Text("Lista") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expandedListDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = OutlineGrey
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar lista",
                                    modifier = Modifier.clickable { expandedListDropdown = !expandedListDropdown })
                            }
                        )
                        DropdownMenu(
                            expanded = expandedListDropdown,
                            onDismissRequest = { expandedListDropdown = false }
                        ) {
                            listNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        taskCategorySelected = name
                                        expandedListDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority segments layout
                    Column {
                        Text("Prioridad de Ejecución:", fontSize = 11.sp, color = SubtitleText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                            listOf("Bajo", "Medio", "Alto").forEach { pOption ->
                                val optionColor = when (pOption) {
                                    "Alto" -> AlertRed
                                    "Medio" -> Color(0xFFF57C00)
                                    else -> Color(0xFF00796B)
                                }
                                val isChosen = taskPrioritySelected.equals(pOption, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChosen) optionColor
                                            else optionColor.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = if (isChosen) 2.dp else 1.dp,
                                            color = if (isChosen) optionColor else OutlineGrey,
                                            shape = CircleShape
                                        )
                                        .clickable { taskPrioritySelected = pOption }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pOption.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else optionColor
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(
                            checked = taskIsPrivate,
                            onCheckedChange = { taskIsPrivate = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solo para mí (Privado)", fontSize = 14.sp)
                    }

                    // Assignee dropdown picker list (Represented by profiles capsules)
                    Column {
                        Text("Asignar esta tarea a:", fontSize = 11.sp, color = SubtitleText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                            val activeProfiles = if (profiles.isEmpty() ){
                                listOf("Julio", "Ana", "Ambos")
                            } else {
                                profiles.map { it.name } + "Ambos"
                            }

                            activeProfiles.forEach { name ->
                                val isSelected = taskAssignee == name
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { taskAssignee = name }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else OutlineGrey,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 14.dp, vertical = 6.dp) ){
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank() ){
                            if (isEditingChore && editingChore != null) {
                                val updated = editingChore!!.copy(
                                        title = taskTitle.trim(),
                                        description = taskDesc.trim(),
                                        priority = taskPrioritySelected,
                                        category = taskCategorySelected,
                                        assignedTo = if (taskIsPrivate) currentUser else taskAssignee,
                                        isPrivate = taskIsPrivate
                                )
                                viewModel.editChore(updated)
                            } else {
                                viewModel.addChore(
                                    title = taskTitle.trim(),
                                    assignedTo = if (taskIsPrivate) currentUser else taskAssignee,
                                    description = taskDesc.trim(),
                                    priority = taskPrioritySelected,
                                    category = taskCategorySelected,
                                    isPrivate = taskIsPrivate
                                )
                            }
                            showChoreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChoreDialog = false }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp)
        )
    }
}


// --- TAB 3.5: LISTA DE COMPRAS SEPARADA ---
@Composable
fun ComprasTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var editQty by remember { mutableStateOf("") }

    var itemNameInput by remember { mutableStateOf("") }
    var itemQtyInput by remember { mutableStateOf("") }
    var itemIsPrivate by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    var activeShoppingFilter by remember { mutableStateOf("pendientes") } // "pendientes" or "comprados"
    var selectedListName by remember { mutableStateOf("General") }
    var showListManager by remember { mutableStateOf(false) }
    var listNameToEdit by remember { mutableStateOf<String?>(null) }
    var editListNameInput by remember { mutableStateOf("") }
    val listNames = remember(shoppingItems, settings) {
        val dbListsSetting = settings.find { it.key == "shopping_lists" }?.value
        val dbLists = dbListsSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val itemLists = shoppingItems.map { it.listName }
        (listOf("General") + dbLists + itemLists).distinct()
    }
    var editSelectedList by remember { mutableStateOf("General") }
    var itemSelectedList by remember(selectedListName) { mutableStateOf(selectedListName) }

    val userShoppingItems = shoppingItems.filter { (!it.isPrivate || it.addedBy == currentUser) && (selectedListName == "General" || it.listName == selectedListName) }
    val pendingShopping = userShoppingItems.filter { !it.isBought }
    val boughtShopping = userShoppingItems.filter { it.isBought }
    val shownShopping = if (activeShoppingFilter == "pendientes") pendingShopping else boughtShopping

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp) ){
        // Top header block
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Lista de Compras",
                subtitle = "${boughtShopping.size}/${userShoppingItems.size} artículos listos",
                actionText = "Nuevo",
                onActionClick = {
                    itemNameInput = ""
                    itemQtyInput = ""
                    itemIsPrivate = false
                    showAddItemDialog = true
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Shopping lists selector row
        item {
            var showAddListDialog by remember { mutableStateOf(false) }
            var newListNameInput by remember { mutableStateOf("") }
            
            if (showAddListDialog) {
                AlertDialog(
                    onDismissRequest = { showAddListDialog = false },
                    title = { Text("Nueva Lista de Compras", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = newListNameInput,
                            onValueChange = { newListNameInput = it },
                            placeholder = { Text("Ej: Supermercado, Farmacia...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            val cleanList = newListNameInput.trim()
                            if (cleanList.isNotEmpty() && cleanList != "General") {
                                val currentSetting = settings.find { it.key == "shopping_lists" }?.value
                                val newSettingVal = if (currentSetting.isNullOrBlank()) cleanList else "$currentSetting,$cleanList"
                                viewModel.updateValueSetting("shopping_lists", newSettingVal)
                                selectedListName = cleanList
                            }
                            newListNameInput = ""
                            showAddListDialog = false
                        }) {
                            Text("Crear")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddListDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showListManager) {
                AlertDialog(
                    onDismissRequest = { showListManager = false },
                    title = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Mis Listas de Compras", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showListManager = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                    },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp) ){
                            items(listNames) { listName ->
                                val isEditing = listNameToEdit == listName
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) ){
                                    if (isEditing) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                                                value = editListNameInput,
                                                onValueChange = { editListNameInput = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            IconButton(onClick = {
                                                val cleanList = editListNameInput.trim()
                                                if (cleanList.isNotEmpty() && cleanList != listName && cleanList != "General") {
                                                    viewModel.renameShoppingList(listName, cleanList)
                                                    if (selectedListName == listName) {
                                                        selectedListName = cleanList
                                                    }
                                                }
                                                listNameToEdit = null
                                            }) {
                                                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                selectedListName = listName
                                                showListManager = false
                                            }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(listName, fontSize = 16.sp, fontWeight = if (selectedListName == listName) FontWeight.Bold else FontWeight.Normal)
                                            if (listName != "General") {
                                                Row {
                                                    IconButton(onClick = {
                                                        listNameToEdit = listName
                                                        editListNameInput = listName
                                                    }, modifier = Modifier.size(32.dp) ){
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                                                    }
                                                    DeleteIconButton(
                                                        modifier = Modifier.size(32.dp),
                                                        iconSize = 20.dp,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        onDeleteConfirm = {
                                                            viewModel.deleteShoppingList(listName)
                                                            if (selectedListName == listName) {
                                                                selectedListName = "General"
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showAddListDialog = true }, modifier = Modifier.fillMaxWidth() ){
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear Nueva Lista")
                        }
                    }
                )
            }

            ModernListSelectorChips(
                listNames = listNames,
                selectedList = selectedListName,
                onSelectList = { selectedListName = it },
                onAddListClick = { showAddListDialog = true },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter pills (Por Comprar vs Comprados)
        item {
            ModernFilterPills(
                options = listOf(
                    "pendientes" to "Por Comprar (${pendingShopping.size})",
                    "comprados" to "Comprados (${boughtShopping.size})"
                ),
                selectedId = activeShoppingFilter,
                onSelect = { activeShoppingFilter = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Shopping List tracker items
        if (shownShopping.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.ShoppingCart,
                    title = if (activeShoppingFilter == "pendientes") "¡Despensa al día!" else "No hay compras registradas",
                    subtitle = if (activeShoppingFilter == "pendientes") "No hay artículos pendientes por comprar en esta lista." else "Los artículos marcados como comprados aparecerán aquí.",
                    actionText = if (activeShoppingFilter == "pendientes") "+ Agregar Artículo" else null,
                    onActionClick = {
                        itemNameInput = ""
                        itemQtyInput = ""
                        itemIsPrivate = false
                        showAddItemDialog = true
                    }
                )
            }
        } else {
            items(shownShopping, key = { it.id.ifEmpty { "${it.name}_${it.dateAdded}" } }) { item ->
                val cardBgColor = if (item.isBought) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
                
                val accentColor = if (item.isBought) OutlineGrey.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary

                Box {
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                        onClick = {
                            editingItem = item
                            editName = item.name
                            editQty = item.quantity
                            itemIsPrivate = item.isPrivate
                            showEditDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle Status Bought circle Checkmark
                            val checkboxBg = if (item.isBought) Color(0xFF10B981) else Color.Transparent
                            val checkboxBorderColor = if (item.isBought) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            val checkIconTint = Color.White

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(checkboxBg)
                                    .border(2.dp, checkboxBorderColor, CircleShape)
                                    .clickable { viewModel.toggleShoppingItemBought(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isBought) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Desmarcar",
                                        tint = checkIconTint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f) ){
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.isBought) SubtitleText.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDecoration = if (item.isBought) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                                    // Creator initials avatar bubble
                                    val initialChar = item.addedBy.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initialChar,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "Por ${item.addedBy}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SubtitleText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Controls container Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                                // Unified Quantity Pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, OutlineGrey.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 2.dp, vertical = 1.dp) ){
                                    IconButton(
                                        onClick = {
                                            val currentQtyNum = item.quantity.toIntOrNull() ?: 1
                                            val nextQtyNum = if (currentQtyNum > 1) currentQtyNum - 1 else 1
                                            viewModel.editShoppingItem(item.copy(quantity = nextQtyNum.toString()))
                                        },
                                        modifier = Modifier.size(22.dp) ){
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Disminuir",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Text(
                                        text = item.quantity,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            val currentQtyNum = item.quantity.toIntOrNull() ?: 1
                                            val nextQtyNum = currentQtyNum + 1
                                            viewModel.editShoppingItem(item.copy(quantity = nextQtyNum.toString()))
                                        },
                                        modifier = Modifier.size(22.dp) ){
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Aumentar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingItem = item
                                        editName = item.name
                                        editQty = item.quantity
                                        itemIsPrivate = item.isPrivate
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                }
                                DeleteIconButton(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(AlertRed.copy(alpha = 0.10f), CircleShape),
                                    iconSize = 15.dp,
                                    tint = AlertRed,
                                    onDeleteConfirm = { viewModel.deleteShoppingItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue Sheet editing shopping item
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Artículo", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre del artículo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        )
                    )

                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(
                            checked = itemIsPrivate,
                            onCheckedChange = { itemIsPrivate = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solo para mí (Privado)", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Lista dropdown selector
                    var expandedShopListDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth() ){
                        OutlinedTextField(
                            value = editSelectedList,
                            onValueChange = {},
                            label = { Text("Lista") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expandedShopListDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = OutlineGrey
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar lista",
                                    modifier = Modifier.clickable { expandedShopListDropdown = !expandedShopListDropdown })
                            }
                        )
                        DropdownMenu(
                            expanded = expandedShopListDropdown,
                            onDismissRequest = { expandedShopListDropdown = false }
                        ) {
                            listNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        editSelectedList = name
                                        expandedShopListDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editingItem != null) {
                            val targetQty = editQty.trim().ifBlank { "1" }
                            val updated = editingItem!!.copy(
                                name = editName.trim(),
                                quantity = targetQty,
                                listName = editSelectedList
                            )
                            updated.isPrivate = itemIsPrivate
                            viewModel.editShoppingItem(updated)
                            showEditDialog = false
                            editingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Text("Actualizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    editingItem = null
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Nuevo Artículo", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = itemNameInput,
                        onValueChange = { itemNameInput = it },
                        label = { Text("Nombre del artículo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        )
                    )
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = itemQtyInput,
                        onValueChange = { itemQtyInput = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = OutlineGrey
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Switch(
                            checked = itemIsPrivate,
                            onCheckedChange = { itemIsPrivate = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solo para mí (Privado)", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lista de destino:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                        items(listNames) { list ->
                            val isSelected = itemSelectedList == list
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { itemSelectedList = list }
                                    .padding(horizontal = 12.dp, vertical = 6.dp) ){
                                Text(
                                    text = list,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemNameInput.isNotBlank() ){
                            viewModel.addShoppingItem(
                                itemNameInput.trim(),
                                itemQtyInput.trim().ifBlank { "1" },
                                isPrivate = itemIsPrivate,
                                listName = itemSelectedList
                            )
                            itemNameInput = ""
                            itemQtyInput = ""
                            itemIsPrivate = false
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddItemDialog = false
                    itemNameInput = ""
                    itemQtyInput = ""
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp)
        )
    }
}// --- TAB 4: CONFIGURACIÓN, PERFILES & TEMA ---
@Composable
fun NavigationSidebar(
    categories: List<String>,
    selectedCategory: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) ){
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp) ){
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HorizontalCategoryTabs(
    categories: List<String>,
    selectedCategory: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp) ){
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface 
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ConfigMainContent(
    selectedCategory: Int,
    profiles: List<UserProfile>,
    settings: List<AppSetting>,
    activityLogs: List<ActivityLog>,
    isDarkTheme: Boolean,
    inputRateField: String,
    inputLimitField: String,
    onInputRateChange: (String) -> Unit,
    onInputLimitChange: (String) -> Unit,
    onSyncBcv: () -> Unit,
    onSaveBcv: () -> Unit,
    onSaveLimit: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onAddProfileClick: () -> Unit,
    onEditProfileClick: (UserProfile) -> Unit,
    onDeleteProfileClick: (UserProfile) -> Unit,
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) ){
        if (selectedCategory == 0) {
            // --- GENERAL CATEGORY ---
            // --- THEME SWAP SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) )){
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme state",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f) ){
                                Text("Aspecto Visual del Sistema", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Alterna entre tema claro y tema oscuro", fontSize = 11.sp, color = SubtitleText)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme(it) }
                        )
                    }
                }
            }

            // --- BCV CONVERSION RATE SETTINGS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) )){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tasa Oficial BCV ($ a Bs.)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Button(
                                onClick = onSyncBcv,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp) ){
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sincronizar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Tasa oficial del BCV para cálculos. 'Todo es editable'.", fontSize = 11.sp, color = SubtitleText)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputRateField,
                                onValueChange = onInputRateChange,
                                label = { Text("Tasa en Bs/$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onSaveBcv
                            ) {
                                Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- FAMILY CODE & LOGOUT ---
            item {
                val familyId by viewModel.familyId.collectAsStateWithLifecycle()
                val context = androidx.compose.ui.platform.LocalContext.current
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) )){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Text("Código Familiar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Comparte este código para que otros miembros se unan al nido.", fontSize = 11.sp, color = SubtitleText)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = familyId ?: "CARGANDO...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Button(
                                onClick = {
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Código Familiar", familyId ?: "")
                                    clipboardManager.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Código copiado", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://hogar-finanzas-2026.web.app/privacy.html"))
                                    context.startActivity(browserIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Privacidad", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://hogar-finanzas-2026.web.app/terms.html"))
                                    context.startActivity(browserIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Términos", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth() ){
                            Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar Cuenta", modifier = Modifier.size(14.dp), tint = AlertRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eliminar mi Cuenta y Datos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        }
                    }
                }
            }
            
            // --- GLOBAL BUDGET LIMITS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) )){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Text("Base Presupuestaria Global ($ USD)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Reajusta la base de gastos global de la familia.", fontSize = 11.sp, color = SubtitleText)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputLimitField,
                                onValueChange = onInputLimitChange,
                                label = { Text("Monto en USD") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onSaveLimit
                            ) {
                                Text("Reajustar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        } else if (selectedCategory == 1) {
            // --- SPOUSE PROFILE MANAGEMENT PANEL ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) )){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Miembros del Hogar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Button(
                                onClick = onAddProfileClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp) ){
                                Icon(Icons.Default.Add, contentDescription = "Nuevo perfil", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Perfiles registrados en el hogar para la asignación de responsabilidades.", fontSize = 11.sp, color = SubtitleText)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (profiles.isEmpty() ){
                            Text("No hay perfiles configurados.", fontSize = 11.sp, color = SubtitleText)
                        } else {
                            profiles.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(p.colorHex.toColor()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(p.name.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onEditProfileClick(p) },
                                            modifier = Modifier.size(28.dp) ){
                                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Prevent deleting necessary defaults
                                        if (p.name != "Julio" && p.name != "Ana") {
                                            DeleteIconButton(
                                                modifier = Modifier.size(28.dp),
                                                iconSize = 16.dp,
                                                tint = AlertRed.copy(alpha = 0.8f),
                                                onDeleteConfirm = { onDeleteProfileClick(p) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- HISTORY LOGS ---
            item {
                Text("Historial de Actividades Recientes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (activityLogs.isEmpty() ){
                item {
                    Text("No hay logs de actividad registrados.", fontSize = 11.sp, color = SubtitleText)
                }
            } else {
                items(activityLogs.take(30) ){ log ->
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dateStr = format.format(Date(log.timestamp))
                    
                    val tagColor = when (log.tag.uppercase() ){
                        "FINANZAS" -> MaterialTheme.colorScheme.primary
                        "TAREAS" -> Color(0xFFE65100)
                        "COMPRAS" -> Color(0xFF0D9488)
                        "ALERTA" -> AlertRed
                        else -> SubtitleText
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = borderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) )){
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tag sticker
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tagColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp) ){
                                Text(
                                    text = log.tag,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateStr,
                                fontSize = 9.sp,
                                color = SubtitleText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTab(viewModel: FamilyViewModel) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()

    var showProfileCreatorDialog by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var profileNameInput by remember { mutableStateOf("") }
    var profileColorHexSelected by remember { mutableStateOf("#D0BCFF") }

    // Read details
    val bcvRateStr = settings.find { it.key == "bcv_rate" }?.value ?: "36.85"
    val totalLimitStr = settings.find { it.key == "total_limit_usd" }?.value ?: "1800.0"
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var inputRateField by remember { mutableStateOf(bcvRateStr) }
    var inputLimitField by remember { mutableStateOf(totalLimitStr) }

    androidx.compose.runtime.LaunchedEffect(settings) {
        val bcv = settings.find { it.key == "bcv_rate" }?.value
        if (bcv != null) {
            inputRateField = bcv
        }
        val limit = settings.find { it.key == "total_limit_usd" }?.value
        if (limit != null) {
            inputLimitField = limit
        }
    }

    val profileAvatarColors = listOf("#D0BCFF", "#EFB8C8", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#E0E0E0")
    var selectedCategory by remember { mutableStateOf(0) }
    val categoriesList = listOf("General", "Miembros")

    BoxWithConstraints(modifier = Modifier.fillMaxSize() ){
        val isWideScreen = maxWidth > 600.dp
        
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) ){
                NavigationSidebar(
                    categories = categoriesList,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                    modifier = Modifier.width(180.dp)
                )

                Box(modifier = Modifier.weight(1f) ){
                    ConfigMainContent(
                        selectedCategory = selectedCategory,
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        inputRateField = inputRateField,
                        inputLimitField = inputLimitField,
                        onInputRateChange = { inputRateField = it },
                        onInputLimitChange = { inputLimitField = it },
                        onSyncBcv = { viewModel.syncLiveBcvRate() },
                        onSaveBcv = { 
                            if (inputRateField.isNotBlank() ){
                                viewModel.updateValueSetting("bcv_rate", inputRateField)
                            }
                        },
                        onSaveLimit = {
                            if (inputLimitField.isNotBlank() ){
                                viewModel.updateValueSetting("total_limit_usd", inputLimitField)
                            }
                        },
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = { showProfileCreatorDialog = true },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            showProfileCreatorDialog = true
                        },
                        onDeleteProfileClick = { p -> viewModel.deleteProfile(p) },
                        viewModel = viewModel
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize() ){
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ajustes del Hogar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                HorizontalCategoryTabs(
                    categories = categoriesList,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp) ){
                    ConfigMainContent(
                        selectedCategory = selectedCategory,
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        inputRateField = inputRateField,
                        inputLimitField = inputLimitField,
                        onInputRateChange = { inputRateField = it },
                        onInputLimitChange = { inputLimitField = it },
                        onSyncBcv = { viewModel.syncLiveBcvRate() },
                        onSaveBcv = { 
                            if (inputRateField.isNotBlank() ){
                                viewModel.updateValueSetting("bcv_rate", inputRateField)
                            }
                        },
                        onSaveLimit = {
                            if (inputLimitField.isNotBlank() ){
                                viewModel.updateValueSetting("total_limit_usd", inputLimitField)
                            }
                        },
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = { showProfileCreatorDialog = true },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            showProfileCreatorDialog = true
                        },
                        onDeleteProfileClick = { p -> viewModel.deleteProfile(p) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // --- POPUP: PROFILE CREATOR/EDITOR DIALOG ---
    if (showProfileCreatorDialog) {
        AlertDialog(
            onDismissRequest = { 
                showProfileCreatorDialog = false 
                isEditingProfile = false
                editingProfile = null
                profileNameInput = ""
                profileColorHexSelected = "#D0BCFF"
            },
            title = { Text(if (isEditingProfile) "Editar Miembro del Hogar" else "Registrar Miembro del Hogar", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Nombre del Perfil") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Color de avatar representativo:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                        profileAvatarColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(hex.toColor())
                                    .clickable { profileColorHexSelected = hex }
                                    .border(
                                        width = if (profileColorHexSelected == hex) 2.dp else 0.dp,
                                        color = if (profileColorHexSelected == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                              )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (profileNameInput.isNotBlank() ){
                        if (isEditingProfile && editingProfile != null) {
                            viewModel.editProfile(editingProfile!!, profileNameInput, profileColorHexSelected)
                        } else {
                            viewModel.createProfile(profileNameInput, profileColorHexSelected)
                        }
                        profileNameInput = ""
                        showProfileCreatorDialog = false
                        isEditingProfile = false
                        editingProfile = null
                    }
                }) {
                    Text(if (isEditingProfile) "Guardar" else "Registrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showProfileCreatorDialog = false 
                    isEditingProfile = false
                    editingProfile = null
                    profileNameInput = ""
                    profileColorHexSelected = "#D0BCFF"
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteAccountDialog = false 
                deleteAccountError = null
            },
            title = { Text("¿Eliminar tu Cuenta?", color = AlertRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Esta acción es irreversible y permanente conforme a las políticas de Google Play. Se eliminarán tus credenciales de acceso, tus comprobantes en la nube y tus preferencias.")
                    deleteAccountError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(err, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAccount(
                            onSuccess = {
                                showDeleteAccountDialog = false
                                android.widget.Toast.makeText(context, "Cuenta eliminada correctamente", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                deleteAccountError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Sí, Eliminar Cuenta")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteAccountDialog = false 
                    deleteAccountError = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// --- POPUP DIALOG FOR SETTINGS/CONFIGURATION ---
@Composable
fun ConfigDialog(viewModel: FamilyViewModel, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = borderStroke(1.dp, OutlineGrey) ){
            Column(modifier = Modifier.fillMaxSize() ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes de la Aplicación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                androidx.compose.material3.HorizontalDivider(color = OutlineGrey.copy(alpha = 0.5f), thickness = 1.dp)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth() ){
                    ConfigTab(viewModel = viewModel)
                }
            }
        }
    }
}

// --- BRAND IDENTITY LOGO COMPOSE CANVAS ---
@Composable
fun FamiLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_nido_logo),
        contentDescription = "Nido Logo",
        modifier = modifier

    )
}


// --- TAB 5: METAS ---
@Composable
fun MetasTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("deseos") } // "deseos" or "notas"
    var activeGoalFilter by remember { mutableStateOf("pendientes") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalTitleInput by remember { mutableStateOf("") }
    var goalAmountInput by remember { mutableStateOf("") }
    var goalIsPrivateInput by remember { mutableStateOf(false) }    
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Metas y Notas",
                subtitle = "Proyectos, ahorros y apuntes compartidos",
                actionText = "Nuevo",
                onActionClick = { 
                    if (activeSubTab == "deseos") {
                        editingGoal = null
                        goalTitleInput = ""
                        goalAmountInput = ""
                        goalIsPrivateInput = false
                        showAddGoalDialog = true 
                    } else {
                        editingNote = null
                        noteTitleInput = ""
                        noteContentInput = ""
                        showAddNoteDialog = true 
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            ModernFilterPills(
                options = listOf(
                    "deseos" to "🎯 Metas y Deseos (${goals.size})",
                    "notas" to "📝 Libreta y Notas (${notes.size})"
                ),
                selectedId = activeSubTab,
                onSelect = { activeSubTab = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (activeSubTab == "deseos") {
            val pendingGoals = goals.filter { !it.isCompleted }
            val completedGoals = goals.filter { it.isCompleted }
            
            item {
                ModernFilterPills(
                    options = listOf(
                        "pendientes" to "En Curso (${pendingGoals.size})",
                        "completadas" to "Cumplidas (${completedGoals.size})"
                    ),
                    selectedId = activeGoalFilter,
                    onSelect = { activeGoalFilter = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val shownGoals = if (activeGoalFilter == "pendientes") pendingGoals else completedGoals

            if (shownGoals.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.Star,
                        title = if (activeGoalFilter == "pendientes") "No tienes metas pendientes" else "Aún no has cumplido metas",
                        subtitle = if (activeGoalFilter == "pendientes") "Crea metas de ahorro o deseos para tu hogar." else "Las metas alcanzadas se listarán aquí.",
                        actionText = if (activeGoalFilter == "pendientes") "+ Crear Meta" else null,
                        onActionClick = {
                            editingGoal = null
                            goalTitleInput = ""
                            goalAmountInput = ""
                            goalIsPrivateInput = false
                            showAddGoalDialog = true
                        }
                    )
                }
            } else {
                items(shownGoals) { goal ->
                    val cardBgColor = if (goal.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                        onClick = {
                            editingGoal = goal
                            goalTitleInput = goal.title
                            goalAmountInput = goal.targetAmount?.toString() ?: ""
                            goalIsPrivateInput = goal.isPrivate
                            showAddGoalDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val checkboxBg = if (goal.isCompleted) Color(0xFF10B981) else Color.Transparent
                            val checkboxBorderColor = if (goal.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(checkboxBg)
                                    .border(2.dp, checkboxBorderColor, CircleShape)
                                    .clickable { viewModel.editGoal(goal.copy(isCompleted = !goal.isCompleted)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (goal.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Desmarcar",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f) ){
                                Text(
                                    text = goal.title, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textDecoration = if (goal.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    color = if (goal.isCompleted) SubtitleText.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Por ${goal.createdBy}", fontSize = 11.sp, color = SubtitleText)
                                    if (goal.targetAmount != null && goal.targetAmount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("•", fontSize = 11.sp, color = SubtitleText)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Meta: $${goal.targetAmount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Notas


            if (notes.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.EditNote,
                        title = "Tu libreta está vacía",
                        subtitle = "Anota recordatorios, ideas o apuntes compartidos del hogar.",
                        actionText = "+ Nueva Nota",
                        onActionClick = {
                            editingNote = null
                            noteTitleInput = ""
                            noteContentInput = ""
                            showAddNoteDialog = true
                        }
                    )
                }
            } else {
                items(notes) { note ->
                    val cardBgColor = if (note.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                        onClick = {
                            editingNote = note
                            noteTitleInput = note.title
                            noteContentInput = note.content
                            showAddNoteDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val checkboxBg = if (note.isCompleted) Color(0xFF10B981) else Color.Transparent
                            val checkboxBorderColor = if (note.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(checkboxBg)
                                    .border(2.dp, checkboxBorderColor, CircleShape)
                                    .clickable { viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (note.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Desmarcar",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f) ){
                                Text(
                                    text = note.title, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textDecoration = if (note.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    color = if (note.isCompleted) SubtitleText.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(note.content, fontSize = 13.sp, color = if (note.isCompleted) SubtitleText.copy(alpha = 0.6f) else SubtitleText)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Por ${note.createdBy}", fontSize = 11.sp, color = SubtitleText)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddGoalDialog = false
                editingGoal = null
                goalTitleInput = ""
                goalAmountInput = ""
                goalIsPrivateInput = false
            },
            title = { Text(if (editingGoal != null) "Editar Deseo" else "Nuevo Deseo") },
            text = {
                Column {
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = goalTitleInput,
                        onValueChange = { goalTitleInput = it },
                        label = { Text("¿Qué deseas?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalAmountInput,
                        onValueChange = { goalAmountInput = it },
                        label = { Text("Monto (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { goalIsPrivateInput = !goalIsPrivateInput },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = goalIsPrivateInput,
                            onCheckedChange = { goalIsPrivateInput = it }
                        )
                        Text("Privado (Solo yo puedo verlo)", fontSize = 14.sp)
                    }
                }
            },
            dismissButton = {
                if (editingGoal != null) {
                    TextButton(onClick = { 
                        viewModel.deleteGoal(editingGoal!!)
                        showAddGoalDialog = false
                    }) {
                        Text("Eliminar", color = AlertRed)
                    }
                } else {
                    TextButton(onClick = { showAddGoalDialog = false }) {
                        Text("Cancelar")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amountVal = goalAmountInput.toDoubleOrNull()
                    if (goalTitleInput.isNotBlank() ){
                        if (editingGoal != null) {
                            val updated = editingGoal!!.copy(title = goalTitleInput, targetAmount = amountVal, isPrivate = goalIsPrivateInput)
                            viewModel.editGoal(updated) // we need to implement editGoal in viewmodel
                        } else {
                            viewModel.addGoal(goalTitleInput, amountVal, goalIsPrivateInput)
                        }
                        showAddGoalDialog = false
                        editingGoal = null
                        goalTitleInput = ""
                        goalAmountInput = ""
                        goalIsPrivateInput = false
                    }
                }) {
                    Text("Guardar")
                }
            }
        )
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddNoteDialog = false
                editingNote = null
                noteTitleInput = ""
                noteContentInput = ""
            },
            title = { Text(if (editingNote != null) "Editar Nota" else "Nueva Nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text("Contenido") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            dismissButton = {
                if (editingNote != null) {
                    TextButton(onClick = { 
                        viewModel.deleteNote(editingNote!!)
                        showAddNoteDialog = false
                    }) {
                        Text("Eliminar", color = AlertRed)
                    }
                } else {
                    TextButton(onClick = { showAddNoteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteTitleInput.isNotBlank() ){
                        if (editingNote != null) {
                            val updated = editingNote!!.copy(title = noteTitleInput, content = noteContentInput)
                            viewModel.editNote(updated) // we need to implement editNote in viewmodel
                        } else {
                            viewModel.addNote(noteTitleInput, noteContentInput)
                        }
                        showAddNoteDialog = false
                        editingNote = null
                        noteTitleInput = ""
                        noteContentInput = ""
                    }
                }) {
                    Text("Guardar")
                }
            }
        )
    }
}



@Composable
fun DeleteIconButton(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,
    onDeleteConfirm: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = tint, modifier = Modifier.size(iconSize))
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este elemento?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirm()
                    showDialog = false
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
