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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.scaleClickable(onClick = onClick),
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = border
        ) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = border
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

    var scannedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var scannedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var saveReceiptPhoto by remember { mutableStateOf(true) }

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
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats)
                    if (parsedTx != null) {
                        scannedAmount = parsedTx.originalAmount.toString()
                        scannedDesc = parsedTx.description
                        scannedCurrency = parsedTx.currency
                        scannedCategory = if (parsedTx.category in availableCats) parsedTx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = parsedTx.date
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
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats)
                    if (parsedTx != null) {
                        scannedAmount = parsedTx.originalAmount.toString()
                        scannedDesc = parsedTx.description
                        scannedCurrency = parsedTx.currency
                        scannedCategory = if (parsedTx.category in availableCats) parsedTx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = parsedTx.date
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
                val tabOrder = listOf("inicio", "cuentas", "tareas", "compras")
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
                                android.widget.Toast.makeText(context, "Descargando actualización...", android.widget.Toast.LENGTH_LONG).show()
                                OtaUpdateManager.downloadAndInstallUpdate(context, updateUrl)
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

// Custom Floating Pill Bottom Navigation Bar
@Composable
fun FamilyBottomNavigation(currentTab: String, isDarkTheme: Boolean, onTabSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, bottom = 24.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(16.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(50))
                .background(if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f))
                .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50) )){
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
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                label = "indicatorOffset"
            )

            // The single fluid pill backdrop
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (tabId, label, icon) ->
                    val selected = currentTab == tabId
                    val textColor = if (selected) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.6f)
                    val iconSize by animateDpAsState(targetValue = if (selected) 26.dp else 22.dp, label = "tabIconSize")

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
                                tint = textColor,
                                modifier = Modifier.size(iconSize)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
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
    val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(selectedMonth.time)
    val presupuestadoUsd = categories.sumOf { it.limitsPerMonth[monthKey] ?: it.limitAmount } // Suma de categorias
    val remainingBudgetUsd = presupuestadoUsd + totalIncomeUsd - totalSpentUsd
    val libreBudgetUsd = baseLimitUsd - presupuestadoUsd
    val totalAvailableForPercentage = presupuestadoUsd + totalIncomeUsd
    val spentPercentage = if (totalAvailableForPercentage > 0) (totalSpentUsd / totalAvailableForPercentage * 100).roundToInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp) ){
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
                                text = "Nido",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            // Elegant gold star particle
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 2.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFCA28))
                            )
                        }
                        Text(
                            text = "Hogar & Vida Compartida",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SubtitleText
                        )
                    }
                }

                // Configuracin gear button at the top right
                IconButton(
                    onClick = onOpenConfig,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape) ){
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuracin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            MonthSelector(selectedMonth = selectedMonth, onMonthChange = { viewModel.updateSelectedMonth(it) })
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Premium Dashboard Budget Card
        item {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.1f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                    .padding(16.dp) ){
                    Column {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BALANCE GENERAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconToggleButton(
                                    checked = showBalance,
                                    onCheckedChange = { 
                                        showBalance = it
                                        viewModel.updateValueSetting("hide_balance", if (it) "false" else "true")
                                    },
                                    modifier = Modifier.size(20.dp) ){
                                    Icon(
                                        imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Balance",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            // BCV sync chip
                            Surface(
                                onClick = { viewModel.syncLiveBcvRate() },
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
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
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Main Balance Display (Remaining Budget)
                        Text("DISPONIBLE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f), letterSpacing = 0.5.sp)
                        Text(
                            text = if (showBalance) "${remainingBudgetUsd.roundToInt()} USD" else "•••••• USD",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Stats row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("BASE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(if (showBalance) "${baseLimitUsd.roundToInt()} USD" else "•••••• USD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRESUPUESTADO", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(if (showBalance) "${presupuestadoUsd.roundToInt()} USD" else "•••••• USD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CONSUMIDO", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(if (showBalance) "${totalSpentUsd.roundToInt()} USD" else "•••••• USD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A65))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (if (totalLimit > 0) (totalSpentUsd / totalLimit) else 0.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (spentPercentage >= 85) Color(0xFFFF5252) else Color(0xFF69F0AE),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Status alert
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (spentPercentage >= 85) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (spentPercentage >= 85) Color(0xFFFF5252) else Color(0xFF69F0AE),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    spentPercentage >= 100 -> "Excediste tu presupuesto por ${(totalSpentUsd - presupuestadoUsd).roundToInt()} USD"
                                    spentPercentage >= 85 -> "Alerta: Has consumido el $spentPercentage% de tu límite"
                                    else -> "Saludable: Has consumido el $spentPercentage% del presupuesto"
                                },
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp) ){
                val userChores = chores.filter { !it.isPrivate || it.createdBy == currentUser || it.assignedTo == currentUser }
                val pendingChore = userChores.find { !it.isCompleted }
                val userShopping = shoppingItems.filter { !it.isPrivate || it.addedBy == currentUser }
                val shoppingCount = userShopping.filter { !it.isBought }.size

                // Next Chore
                PremiumCard(
                    modifier = Modifier.weight(1f) ){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Text(
                            text = "PRÓXIMA TAREA",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubtitleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (pendingChore != null) {
                            Text(
                                text = pendingChore.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val priorityColor = when (pendingChore.priority.lowercase() ){
                                "alto" -> AlertRed
                                "medio" -> Color(0xFFF57C00)
                                else -> Color(0xFF00796B)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(priorityColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp) ){
                                Text(
                                    text = pendingChore.priority.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityColor
                                )
                            }
                        } else {
                            Text(
                                text = "Tareas libres 🎉",
                                fontSize = 12.sp,
                                color = SubtitleText
                            )
                        }
                    }
                }

                // Shopping tracker counter
                PremiumCard(
                    modifier = Modifier.weight(1f) ){
                    Column(modifier = Modifier.padding(14.dp) ){
                        Text(
                            text = "LISTA DE COMPRAS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SubtitleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$shoppingCount artículos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "compra de pendientes",
                            fontSize = 10.sp,
                            color = SubtitleText
                        )
                    }
                }
            }
        }

        // --- SECCIÓN UNIFICADA ESCÁNER DE RECIBOS Y REGISTRO NLP (Se movió abajo) ---

        // --- POPUP DIALOGS FOR SCAN CONFIRMATION & EMAIL SYNC ---
        item {
            if (showConfirmScanDialog) {
                AlertDialog(
                    onDismissRequest = { onSetShowConfirmScanDialog(false) },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Confirmar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Confirmar Gasto con IA ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth() ){
                            Text(
                                "La IA leyó este recibo. Revise y ajuste los datos detectados antes de confirmarlo en sus registros.",
                                fontSize = 11.sp,
                                color = SubtitleText,
                                lineHeight = 14.sp
                            )

                            // Description/Shop field
                            OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                                value = scannedDesc,
                                onValueChange = { onSetScannedDesc(it) },
                                label = { Text("Comercio / Detalle", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                singleLine = true
                            )

                            // Date Selector field
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scaleClickable { val calendar = Calendar.getInstance().apply { timeInMillis = scannedDate }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val newCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                }
                                                onSetScannedDate(newCal.timeInMillis)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                            ) {
                                OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                                    value = sdf.format(Date(scannedDate)),
                                    onValueChange = {},
                                    label = { Text("Fecha de Factura / Gasto", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = OutlineGrey,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )
                            }

                            // Amount field
                            OutlinedTextField(
                                value = scannedAmount,
                                onValueChange = { onSetScannedAmount(it) },
                                label = { Text("Monto", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true
                            )

                            // Currency Toggle (USD vs VES)
                            Text("Moneda:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SubtitleText)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                                listOf("USD", "VES").forEach { m ->
                                    val isSelected = scannedCurrency == m
                                    Button(
                                        onClick = { onSetScannedCurrency(m) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(0.dp) ){
                                        Text(m + if (m == "USD") " ($)" else " (Bs.)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Category Selector row
                            Text("Categoría asignada:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SubtitleText)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth() ){
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
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = saveReceiptPhoto,
                                    onCheckedChange = { onSetSaveReceiptPhoto(it) },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar foto como respaldo", fontSize = 13.sp)
                            }
                        }
                    },
                    confirmButton = {
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                val amt = scannedAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0 && scannedDesc.isNotBlank() ){
                                    coroutineScope.launch {
                                        var receiptUrl = ""
                                        if (saveReceiptPhoto && scannedBitmap != null) {
                                            receiptUrl = viewModel.uploadReceiptImage(scannedBitmap!!) ?: ""
                                        }
                                        viewModel.addTransaction(
                                            originalAmount = amt,
                                            category = if (scannedCategory.isEmpty()) (categories.firstOrNull()?.categoryName ?: "General") else scannedCategory,
                                            description = scannedDesc,
                                            currency = scannedCurrency,
                                            isIncome = false,
                                            date = scannedDate,
                                            receiptUrl = receiptUrl
                                        )
                                        onSetSuccessMessageScan("Gasto registrado: $amt $scannedCurrency en $scannedDesc")
                                        onSetShowConfirmScanDialog(false)
                                    }
                                } else {
                                    onSetErrorMessageScan("Por favor ingrese un monto y descripción válidos.")
                                    onSetShowConfirmScanDialog(false)
                                }
                            },
                            shape = RoundedCornerShape(12.dp) ){
                            Text("Registrar Gasto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onSetShowConfirmScanDialog(false) }) {
                            Text("Descartar", color = SubtitleText, fontSize = 11.sp)
                        }
                    }
                )
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
                            // COST ANSWER: Let's explain nicely directly to the user
                            PremiumCard(
                                modifier = Modifier.fillMaxWidth() ){
                                Column(modifier = Modifier.padding(0.dp) ){
                                    Text(
                                        "",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

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
                                                kotlinx.coroutines.delay(1200) // Beautiful realism animation
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
                                                selectedEmailsToSync = setOf(0, 1) // default preselected PedidosYa and Netflix
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

        // Conversational Intelligent NLP block & Receipt Scanner Unificado
        item {
            PremiumCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp) ){
                Column(modifier = Modifier.padding(18.dp) ){
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REGISTRO INTELIGENTE CON IA ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Escribe tu gasto o toma una foto del recibo para que la IA lo registre por ti.",
                        fontSize = 11.sp,
                        color = SubtitleText,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = nlpInputText,
                        onValueChange = { nlpInputText = it },
                        placeholder = { Text("ej: Gasté 450 bs en comida...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            if (isRegisteringNlp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                            Icon(Icons.Default.CameraAlt, contentDescription = "Tomar Foto", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }

                        // Select Gallery Button (Gallery)
                        Button(
                            onClick = { onLaunchGallery() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) ){
                            Icon(Icons.Default.Image, contentDescription = "Galería", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Scan Loading Indicators
                    if (isScanningDirect) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = "IA analizando imagen... Espere.",
                                fontSize = 10.sp,
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (errorMessageScan != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                            Icon(Icons.Default.Error, "Error", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Text(
                                text = errorMessageScan!!,
                                fontSize = 10.sp,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (successMessageScan != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                            Icon(Icons.Default.CheckCircle, "Éxito", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Text(
                                text = successMessageScan!!,
                                fontSize = 10.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
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
                            // Render completed chores first
                            dayCompletedChores.forEach { chore ->
                                PremiumCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9) )){
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completada",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f) ){
                                                Text(chore.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                Text("Tarea • Asignado: ${chore.assignedTo} • Prioridad: ${chore.priority}", fontSize = 8.sp, color = Color(0xFF388E3C), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            }
                                        }
                                        Text(
                                            text = "COMPLETADA",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
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
        Calendar.APRIL -> "Mayo"
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


// --- TAB 2: CUENTAS, CATEGORÍAS & GASTOS ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(selectedMonth.time)

    var activeAccountsSection by remember { mutableStateOf("gastos") } // "gastos" or "categorias"
    var historyCategoryFilter by remember { mutableStateOf("Todas") }
    var showHistoryCategoryDropdown by remember { mutableStateOf(false) }

    // Dialog sheets state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var isEditingCategory by remember { mutableStateOf(false) }

    var showEditTxDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var photoUrlToShow by remember { mutableStateOf<String?>(null) }

    // Category input fields
    var catNameInput by remember { mutableStateOf("") }
    var catLimitInput by remember { mutableStateOf("") }
    var catColorSelected by remember { mutableStateOf("") }
    var oldCategoryName by remember { mutableStateOf<String?>(null) }

    // Gasto input fields
    var txAmountInput by remember { mutableStateOf("") }
    var txDescInput by remember { mutableStateOf("") }
    var txCurrencySelected by remember { mutableStateOf("USD") } // "USD" or "VES"
    var txCategorySelected by remember { mutableStateOf("") }
    var txIsIncomeSelected by remember { mutableStateOf(false) }
    var txDateInput by remember { mutableStateOf(System.currentTimeMillis()) }
    var txBalanceDateInput by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val bcvRateStr = settings.find { it.key == "bcv_rate" }?.value ?: "36.85"
    val bcvRate = bcvRateStr.toDoubleOrNull() ?: 36.85
    val totalLimitStr = settings.find { it.key == "total_limit_usd" }?.value ?: "1800.0"
    val totalLimit = totalLimitStr.toDoubleOrNull() ?: 1800.0

    var dialogBcvRate by remember { mutableStateOf(bcvRate) }

    LaunchedEffect(bcvRate) {
        dialogBcvRate = bcvRate
    }

    LaunchedEffect(txDateInput, txCurrencySelected) {
        if (txCurrencySelected == "VES") {
            val rate = BcvExchangeRateService.fetchRateForDate(txDateInput)
            if (rate > 0.0) {
                dialogBcvRate = rate
            }
        }
    }

    val filteredTransactionsAccounts = transactions.filter { tx ->
        val txCal = java.util.Calendar.getInstance().apply { timeInMillis = if (tx.balanceDate > 0L) tx.balanceDate else tx.date }
        txCal.get(java.util.Calendar.YEAR) == selectedMonth.get(java.util.Calendar.YEAR) && txCal.get(java.util.Calendar.MONTH) == selectedMonth.get(java.util.Calendar.MONTH)
    }
    val userTransactions = filteredTransactionsAccounts.sortedByDescending { if (it.balanceDate > 0) it.balanceDate else it.date }
        .filter { historyCategoryFilter == "Todas" || it.category.equals(historyCategoryFilter, ignoreCase = true) }
    val spentUsd = userTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val totalIncomeUsdTab = userTransactions.filter { it.isIncome }.sumOf { it.amount }

    if (showHistoryCategoryDropdown) {
        val catNames = listOf("Todas") + categories.map { it.categoryName }.distinct()
        AlertDialog(
            onDismissRequest = { showHistoryCategoryDropdown = false },
            title = { Text("Filtrar por Categoría", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp) ){
                    items(catNames) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scaleClickable { historyCategoryFilter = cat
                                    showHistoryCategoryDropdown = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, fontSize = 16.sp, fontWeight = if (historyCategoryFilter == cat) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryCategoryDropdown = false }) { Text("Cerrar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp) ){
        item {
            MonthSelector(selectedMonth = selectedMonth, onMonthChange = { viewModel.updateSelectedMonth(it) })
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Presupuestos & Gastos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Sub Tab selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp) ){
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeAccountsSection == "gastos") MaterialTheme.colorScheme.surface else Color.Transparent)
                        .scaleClickable { activeAccountsSection = "gastos" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Historial de Gastos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeAccountsSection == "gastos") MaterialTheme.colorScheme.primary else SubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeAccountsSection == "categorias") MaterialTheme.colorScheme.surface else Color.Transparent)
                        .scaleClickable { activeAccountsSection = "categorias" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Presupuestos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeAccountsSection == "categorias") MaterialTheme.colorScheme.primary else SubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (activeAccountsSection == "gastos") {
            // General multi-currency stats bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .scaleClickable { showHistoryCategoryDropdown = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = "Categoría Actual", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("FILTRAR POR CATEGORÍA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(historyCategoryFilter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Categoría", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                PremiumCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) ){
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GASTOS DEL HOGAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${spentUsd.roundToInt()} USD", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (categories.isNotEmpty() ){
                                    txCategorySelected = categories.first().categoryName
                                }
                                txDateInput = System.currentTimeMillis()
                                dialogBcvRate = bcvRate
                                showAddTxDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                            Icon(Icons.Default.Add, contentDescription = "Add Gasto", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Registrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Gasto list items mapped
            if (userTransactions.isEmpty() ){
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se han registrado transacciones aún.", color = SubtitleText, fontSize = 12.sp)
                    }
                }
            } else {
                items(userTransactions) { tx ->
                    val format = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                    val displayDate = if (tx.balanceDate > 0) tx.balanceDate else tx.date
                    val dateString = format.format(Date(displayDate))
                    val assocColor = if (tx.isIncome) "#2E7D32" else (categories.find { it.categoryName.equals(tx.category, ignoreCase = true) }?.colorHex ?: "#6750A4")

                    Box {
                        PremiumCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .scaleClickable { editingTransaction = tx
                                    txAmountInput = tx.originalAmount.toString()
                                    txDescInput = tx.description
                                    txCurrencySelected = tx.currency
                                    txCategorySelected = tx.category
                                    txIsIncomeSelected = tx.isIncome
                                    txDateInput = tx.date
                                    txBalanceDateInput = if (tx.balanceDate > 0) tx.balanceDate else tx.date
                                    dialogBcvRate = tx.bcvRateUsed
                                    showEditTxDialog = true
                                }) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f) ){
                                // Category color avatar dot
                                val foundCat = categories.find { it.categoryName.equals(tx.category, ignoreCase = true) }
                                val iconStr = foundCat?.iconEmoji ?: ""
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (tx.isIncome || iconStr.isEmpty()) assocColor.toColor() else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tx.isIncome) {
                                        Text("I", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    } else if (iconStr.isNotEmpty() ){
                                        Text(text = iconStr, fontSize = 24.sp)
                                    } else {
                                        Text(
                                            text = tx.category.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(tx.description, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tx.category, fontSize = 10.sp, color = SubtitleText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("•", fontSize = 10.sp, color = OutlineGrey)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(dateString, fontSize = 10.sp, color = SubtitleText)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    // Displays original registered currency
                                    val symbol = if (tx.currency == "USD") "$" else "Bs"
                                    val prefix = if (tx.isIncome) "+" else "-"
                                    val amountColor = if (tx.isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    Text(
                                        "$prefix ${tx.originalAmount} $symbol",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
                                    )
                                    // If Bolivars, also show equivalent dollar conversion review
                                    if (tx.currency != "USD") {
                                        Text(
                                            "(${String.format("%.2f", tx.amount)} $ • ${tx.bcvRateUsed} Bs)",
                                            fontSize = 9.sp,
                                            color = SubtitleText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp) ){
                                        Text(tx.registeredBy, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                                        if (tx.receiptUrl.isNotEmpty() ){
                                            IconButton(onClick = {
                                                photoUrlToShow = tx.receiptUrl
                                            }, modifier = Modifier.size(22.dp) ){
                                                Icon(Icons.Default.Image, contentDescription = "Ver Foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(onClick = {
                                            editingTransaction = tx
                                            txAmountInput = tx.originalAmount.toString()
                                            txDescInput = tx.description
                                            txCurrencySelected = tx.currency
                                            txCategorySelected = tx.category
                                            txIsIncomeSelected = tx.isIncome
                                            txDateInput = tx.date
                                            txBalanceDateInput = if (tx.balanceDate > 0) tx.balanceDate else tx.date
                                            dialogBcvRate = tx.bcvRateUsed
                                            showEditTxDialog = true
                                        }, modifier = Modifier.size(22.dp) ){
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteTransaction(tx) }, modifier = Modifier.size(22.dp) ){
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        } else {
            // --- CATEGORIES SUB TAB ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Configuración de Límites", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubtitleText)
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                        Icon(Icons.Default.Add, contentDescription = "Nueva categoría", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Categoría", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Categories loop
            if (categories.isEmpty() ){
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No se han creado categorías aún.", color = SubtitleText, fontSize = 12.sp)
                    }
                }
            } else {
                items(categories) { category ->
                    val catSpent = userTransactions.filter { !it.isIncome && it.category.equals(category.categoryName, ignoreCase = true) }.sumOf { it.amount }
                    val currentLimit = category.limitsPerMonth[monthKey] ?: category.limitAmount
                    val fraction = if (currentLimit > 0) (catSpent / currentLimit).toFloat() else 0f
                    val isExceeded = catSpent > currentLimit

                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp) ){
                        Column(modifier = Modifier.padding(14.dp) ){
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (category.iconEmoji.isNotBlank() ){
                                        Text(category.iconEmoji, fontSize = 16.sp)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(category.colorHex.toColor())
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category.categoryName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${catSpent.roundToInt()} / ${currentLimit.roundToInt()} USD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExceeded) AlertRed else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            catNameInput = category.categoryName
                                            oldCategoryName = category.categoryName
                                            catLimitInput = currentLimit.roundToInt().toString()
                                            catColorSelected = category.iconEmoji
                                            isEditingCategory = true
                                            showAddCategoryDialog = true
                                        },
                                        modifier = Modifier.size(28.dp) ){
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteCategory(category) },
                                        modifier = Modifier.size(28.dp) ){
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { fraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isExceeded) AlertRed else category.colorHex.toColor(),
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }

    // --- POPUP: ADD/EDIT CATEGORY DIALOG ---
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddCategoryDialog = false 
                isEditingCategory = false
                catNameInput = ""
                oldCategoryName = null
                catLimitInput = ""
            },
            title = { Text(if (isEditingCategory) "Editar Categoría de Gasto" else "Nueva Categoría de Gasto", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = catNameInput,
                        onValueChange = { catNameInput = it },
                        label = { Text("Nombre de la categoría") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = catLimitInput,
                        onValueChange = { catLimitInput = it },
                        label = { Text("Límite mensual en $ (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Emoji input instead of color swatches
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = catColorSelected,
                        onValueChange = { catColorSelected = it },
                        label = { Text("Emoji representativo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val limitNum = catLimitInput.toDoubleOrNull() ?: 200.0
                    if (catNameInput.isNotBlank() ){
                        viewModel.createOrEditCategory(oldCategoryName, catNameInput, limitNum, catColorSelected, monthKey)
                        catNameInput = ""
                        oldCategoryName = null
                        catLimitInput = ""
                        catColorSelected = ""
                        isEditingCategory = false
                        showAddCategoryDialog = false
                    }
                }) {
                    Text(if (isEditingCategory) "Guardar" else "Crear", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddCategoryDialog = false 
                    isEditingCategory = false
                    catNameInput = ""
                    oldCategoryName = null
                    catLimitInput = ""
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // --- POPUP: ADD MANUALLY TRANSACTION ---
    if (showAddTxDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddTxDialog = false
                txIsIncomeSelected = false
                txAmountInput = ""
                txDescInput = ""
            },
            title = { Text(if (txIsIncomeSelected) "Registrar Ingreso Manual" else "Registrar Gasto Manual", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    // Transaction Type Selector: Gasto vs. Ingreso
                    Text("Tipo de registro:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp) ){
                        listOf(false, true).forEach { isInc ->
                            val isSelected = txIsIncomeSelected == isInc
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) (if (isInc) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary) else Color.Transparent)
                                    .scaleClickable { txIsIncomeSelected = isInc }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isInc) "Ingreso (+)" else "Gasto (-)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SubtitleText
                                )
                            }
                        }
                    }

                    // Currency Selector: Dollar ($) vs. Venezuelan Bolivars (Bs)
                    Text("Selecciona la moneda:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp) ){
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (txCurrencySelected == "USD") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .scaleClickable { txCurrencySelected = "USD" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Dólar ($)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (txCurrencySelected == "USD") Color.White else SubtitleText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (txCurrencySelected == "VES") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .scaleClickable { txCurrencySelected = "VES" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Bolívares (Bs)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (txCurrencySelected == "VES") Color.White else SubtitleText
                            )
                        }
                    }

                    // Date Selector Outlined Box
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleClickable { val calendar = Calendar.getInstance().apply { timeInMillis = txDateInput }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        txDateInput = newCal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = sdf.format(Date(txDateInput)),
                            onValueChange = {},
                            label = { Text("Fecha de registro") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = OutlineGrey,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Optional Balance Date Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleClickable { val calendar = Calendar.getInstance().apply { timeInMillis = txBalanceDateInput ?: txDateInput }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        txBalanceDateInput = newCal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = txBalanceDateInput?.let { sdf.format(Date(it)) } ?: "",
                            onValueChange = {},
                            label = { Text("Fecha de Balance (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = OutlineGrey,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            placeholder = { Text("Usar misma que el registro") }
                        )
                    }

                    OutlinedTextField(
                        value = txAmountInput,
                        onValueChange = { txAmountInput = it },
                        label = { Text(if (txCurrencySelected == "USD") "Monto en Dólares ($)" else "Monto en Bolívares (Bs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = txDescInput,
                        onValueChange = { txDescInput = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // conversion preview
                    if (txCurrencySelected == "VES") {
                        val amountVal = txAmountInput.toDoubleOrNull() ?: 0.0
                        val equiv = if (dialogBcvRate > 0.0) amountVal / dialogBcvRate else 0.0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(10.dp) ){
                            Text(
                                text = "Equivale a ~${String.format("%.2f", equiv)} $ USD (Tasa de la fecha: ${String.format("%.2f", dialogBcvRate)} Bs/USD)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!txIsIncomeSelected) {
                        Text("Selecciona la categoría correspondiente:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp) ){
                            categories.forEach { cat ->
                                val isSelected = txCategorySelected == cat.categoryName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                        .scaleClickable { txCategorySelected = cat.categoryName }
                                        .padding(horizontal = 8.dp, vertical = 6.dp) ){
                                    Text(
                                        text = cat.categoryName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val originalVal = txAmountInput.toDoubleOrNull()
                    if (originalVal != null && originalVal > 0.0 && txDescInput.isNotBlank() && (txIsIncomeSelected || txCategorySelected.isNotBlank() )){
                        viewModel.addTransaction(
                            originalAmount = originalVal,
                            category = if (txIsIncomeSelected) "Ingreso" else txCategorySelected,
                            description = txDescInput,
                            currency = txCurrencySelected,
                            isIncome = txIsIncomeSelected,
                            date = txDateInput,
                            balanceDate = txBalanceDateInput ?: txDateInput
                        )
                        txAmountInput = ""
                        txDescInput = ""
                        txBalanceDateInput = null
                        txIsIncomeSelected = false
                        showAddTxDialog = false
                    }
                }) {
                    Text("Registrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddTxDialog = false 
                    txIsIncomeSelected = false
                    txAmountInput = ""
                    txDescInput = ""
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // --- POPUP: EDIT GASTO DIALOG ---
    if (showEditTxDialog && editingTransaction != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditTxDialog = false 
                editingTransaction = null
                txAmountInput = ""
                txDescInput = ""
                txIsIncomeSelected = false
                txBalanceDateInput = null
            },
            title = { Text(if (txIsIncomeSelected) "Editar Ingreso" else "Editar Gasto", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    // Transaction Type Selector: Gasto vs. Ingreso
                    Text("Tipo de registro:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp) ){
                        listOf(false, true).forEach { isInc ->
                            val isSelected = txIsIncomeSelected == isInc
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) (if (isInc) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary) else Color.Transparent)
                                    .scaleClickable { txIsIncomeSelected = isInc }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isInc) "Ingreso (+)" else "Gasto (-)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SubtitleText
                                )
                            }
                        }
                    }

                    Text("Selecciona la moneda:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp) ){
                        listOf("USD", "VES").forEach { cur ->
                            val isSelected = txCurrencySelected == cur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .scaleClickable { txCurrencySelected = cur }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cur == "USD") "Dólar ($ USD)" else "Bolívares (Bs VES)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Date Selector Outlined Box (Edit)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleClickable { val calendar = Calendar.getInstance().apply { timeInMillis = txDateInput }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        txDateInput = newCal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = sdf.format(Date(txDateInput)),
                            onValueChange = {},
                            label = { Text("Fecha de registro") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = OutlineGrey,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Optional Balance Date Selector (Edit)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleClickable { val calendar = Calendar.getInstance().apply { timeInMillis = txBalanceDateInput ?: txDateInput }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        txBalanceDateInput = newCal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = txBalanceDateInput?.let { sdf.format(Date(it)) } ?: "",
                            onValueChange = {},
                            label = { Text("Fecha de Balance (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = OutlineGrey,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            placeholder = { Text("Usar misma que el registro") }
                        )
                    }

                    OutlinedTextField(
                        value = txAmountInput,
                        onValueChange = { txAmountInput = it },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = txDescInput,
                        onValueChange = { txDescInput = it },
                        label = { Text("Detalle / Concepto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (txCurrencySelected == "VES") {
                        val amountVal = txAmountInput.toDoubleOrNull() ?: 0.0
                        val equiv = if (dialogBcvRate > 0.0) amountVal / dialogBcvRate else 0.0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(10.dp) ){
                            Text(
                                text = "Equivale a ~${String.format("%.2f", equiv)} $ USD (Tasa de la fecha: ${String.format("%.2f", dialogBcvRate)} Bs/USD)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!txIsIncomeSelected) {
                        Text("Selecciona la categoría correspondiente:", fontSize = 12.sp, color = SubtitleText, fontWeight = FontWeight.Medium)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp) ){
                            categories.forEach { cat ->
                                val isSelected = txCategorySelected == cat.categoryName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                        .scaleClickable { txCategorySelected = cat.categoryName }
                                        .padding(horizontal = 8.dp, vertical = 6.dp) ){
                                    Text(
                                        text = cat.categoryName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    
                    if (editingTransaction?.receiptUrl?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val localContext = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                photoUrlToShow = editingTransaction!!.receiptUrl
                            },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp) ){
                            Icon(Icons.Default.Image, contentDescription = "Ver Foto", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver foto del recibo", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val originalVal = txAmountInput.toDoubleOrNull()
                    if (originalVal != null && originalVal > 0.0 && txDescInput.isNotBlank() && (txIsIncomeSelected || txCategorySelected.isNotBlank() )){
                        val updated = editingTransaction!!.copy(
                            originalAmount = originalVal,
                            category = if (txIsIncomeSelected) "Ingreso" else txCategorySelected,
                            description = txDescInput,
                            currency = txCurrencySelected,
                            isIncome = txIsIncomeSelected,
                            date = txDateInput,
                            balanceDate = txBalanceDateInput ?: txDateInput
                        )
                        viewModel.editTransaction(updated)
                        txAmountInput = ""
                        txDescInput = ""
                        txBalanceDateInput = null
                        showEditTxDialog = false
                        editingTransaction = null
                        txIsIncomeSelected = false
                    }
                }) {
                    Text("Guardar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditTxDialog = false 
                    editingTransaction = null
                    txAmountInput = ""
                    txDescInput = ""
                    txIsIncomeSelected = false
                }) {
                    Text("Cancelar", color = SubtitleText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (photoUrlToShow != null) {
        AlertDialog(
            onDismissRequest = { photoUrlToShow = null },
            title = { Text("Foto de Recibo", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                if (photoUrlToShow!!.startsWith("data:image/jpeg;base64,") ){
                    val base64Str = photoUrlToShow!!.substringAfter("base64,")
                    val imageBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bmp != null) {
                        var scale by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Recibo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .pointerInput(Unit) {
                                    val viewSize = this.size
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val maxOffsetX = (viewSize.width.toFloat() * (scale - 1f)) / 2f
                                        val maxOffsetY = (viewSize.height.toFloat() * (scale - 1f)) / 2f
                                        offset = androidx.compose.ui.geometry.Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    } else {
                        Text("Error al cargar la imagen.")
                    }
                } else {
                    Text("Enlace no válido para imagen base64.")
                }
            },
            confirmButton = {
                TextButton(onClick = { photoUrlToShow = null }) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
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
        contentPadding = PaddingValues(bottom = 120.dp) ){
        // App top header of tasks
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tareas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Progreso: ${completedChores.size}/${userChores.size} completadas",
                        fontSize = 11.sp,
                        color = SubtitleText
                    )
                }

                Button(
                    onClick = {
                        isEditingChore = false
                        editingChore = null
                        taskTitle = ""
                        taskDesc = ""
                        taskCategorySelected = if (selectedListName == "Todas") "General" else selectedListName
                        taskAssignee = if (profiles.isNotEmpty()) profiles.first().name else "Julio"
                        taskPrioritySelected = "Medio"
                        taskIsPrivate = false
                        showChoreDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva Tarea", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showListManager = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "Lista Actual", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("LISTA ACTUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedListName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Lista", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pill filter selectors tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (activeTaskFilter == "pendientes") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTaskFilter = "pendientes" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pendientes (${pendingChores.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTaskFilter == "pendientes") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (activeTaskFilter == "completadas") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTaskFilter = "completadas" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Completadas (${completedChores.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTaskFilter == "completadas") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
        if (shownChores.isEmpty() ){
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp) ){
                        Text(
                            text = if (activeTaskFilter == "pendientes") "¡Todo listo por ahora!" else "No hay tareas completadas todavía.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (activeTaskFilter == "pendientes") "Disfruta de tu tiempo libre 🎉" else "Completa tareas para verlas aquí.",
                            fontSize = 11.sp,
                            color = SubtitleText
                        )
                    }
                }
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
                                        IconButton(onClick = {
                                            editingChore = chore
                                            isEditingChore = true
                                            taskTitle = chore.title
                                            taskDesc = chore.description
                                            taskCategorySelected = chore.category
                                            taskAssignee = chore.assignedTo
                                            taskPrioritySelected = chore.priority
                                            taskIsPrivate = chore.isPrivate
                                            showChoreDialog = true
                                        }, modifier = Modifier.size(24.dp) ){
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        DeleteIconButton(
                                            modifier = Modifier.size(24.dp),
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lista de Compras",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${boughtShopping.size}/${userShoppingItems.size} artículos comprados",
                        fontSize = 11.sp,
                        color = SubtitleText
                    )
                }
                
                Button(
                    onClick = {
                        itemNameInput = ""
                        itemQtyInput = ""
                        itemIsPrivate = false
                        showAddItemDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo Artículo", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showListManager = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "Lista Actual", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("LISTA ACTUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedListName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Lista", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }



        // Pill filter selectors tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (activeShoppingFilter == "pendientes") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeShoppingFilter = "pendientes" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pendientes (${pendingShopping.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeShoppingFilter == "pendientes") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (activeShoppingFilter == "comprados") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeShoppingFilter = "comprados" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Comprados (${boughtShopping.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeShoppingFilter == "comprados") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Shopping List tracker items
        if (shownShopping.isEmpty() ){
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeShoppingFilter == "pendientes") "🎒 ¡Lista de compras vacía!" else "No has comprado ningún artículo aún.",
                        fontSize = 12.sp,
                        color = SubtitleText,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                                IconButton(onClick = {
                                    editingItem = item
                                    editName = item.name
                                    editQty = item.quantity
                                    itemIsPrivate = item.isPrivate
                                    showEditDialog = true
                                }, modifier = Modifier.size(22.dp) ){
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                DeleteIconButton(
                                    modifier = Modifier.size(22.dp),
                                    iconSize = 16.dp,
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
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
                        Text("CÃ³digo Familiar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Comparte este cÃ³digo para que otros miembros se unan a la familia.", fontSize = 11.sp, color = SubtitleText)
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
                                    val clip = android.content.ClipData.newPlainText("CÃ³digo Familiar", familyId ?: "")
                                    clipboardManager.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "CÃ³digo copiado", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ){
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            modifier = Modifier.fillMaxWidth() ){
                            Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ajustes de Fami",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
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
        contentPadding = PaddingValues(bottom = 120.dp) ){
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Metas y Notas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Gestiona tus deseos y apuntes",
                        fontSize = 11.sp,
                        color = SubtitleText
                    )
                }
                
                Button(
                    onClick = { 
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                Button(
                    onClick = { activeSubTab = "deseos" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "deseos") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeSubTab == "deseos") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ) ){
                    Text("Deseos")
                }
                Button(
                    onClick = { activeSubTab = "notas" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "notas") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeSubTab == "notas") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ) ){
                    Text("Libreta")
                }
            }
        }

        if (activeSubTab == "deseos") {
            val pendingGoals = goals.filter { !it.isCompleted }
            val completedGoals = goals.filter { it.isCompleted }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (activeGoalFilter == "pendientes") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeGoalFilter = "pendientes" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pendientes (${pendingGoals.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeGoalFilter == "pendientes") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (activeGoalFilter == "completadas") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeGoalFilter = "completadas" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Completadas (${completedGoals.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeGoalFilter == "completadas") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val shownGoals = if (activeGoalFilter == "pendientes") pendingGoals else completedGoals

            if (shownGoals.isEmpty() ){
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tienes deseos registrados.", color = SubtitleText, fontSize = 14.sp)
                    }
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


            if (notes.isEmpty() ){
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tienes notas.", color = SubtitleText, fontSize = 14.sp)
                    }
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
