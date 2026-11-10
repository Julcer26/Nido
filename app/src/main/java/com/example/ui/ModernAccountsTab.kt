package com.example.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.BcvExchangeRateService
import com.example.api.FinanceAiAnalyzer
import com.example.api.FinanceAiReport
import com.example.database.CategoryDetail
import com.example.database.RecurringPayment
import com.example.database.Transaction
import com.example.database.TransactionItem
import com.example.util.FinanceExportHelper
import com.example.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Paleta Fintech Moderna
private val EmeraldGreen = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val CoralRed = Color(0xFFEF4444)
private val IndigoAccent = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernAccountsTab(
    viewModel: FamilyViewModel,
    onOpenConfig: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val recurringPayments by viewModel.recurringPayments.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val monthKey = remember(selectedMonth.timeInMillis) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedMonth.time)
    }

    // Estados de Navegación y Filtros Rápidos
    var activeSection by remember { mutableStateOf("gastos") } // "gastos" | "categorias"
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Estados de IA Financiera
    var aiReport by remember { mutableStateOf<FinanceAiReport?>(null) }
    var isAnalyzingAi by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAiBottomSheet by remember { mutableStateOf(false) }

    // Estados de BottomSheets y Diálogos
    var showTxSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    var showCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryDetail?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryDetail?>(null) }
    var photoUrlToShow by remember { mutableStateOf<String?>(null) }

    // Estados de Pagos Recurrentes
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var showPaidDetailsDialog by remember { mutableStateOf(false) }
    var selectedRecurringPayment by remember { mutableStateOf<RecurringPayment?>(null) }
    var recNameInput by remember { mutableStateOf("") }
    var recAmountInput by remember { mutableStateOf("") }
    var recCurrencySelected by remember { mutableStateOf("USD") }
    var recDueDayInput by remember { mutableStateOf("5") }
    var recEmojiInput by remember { mutableStateOf("⚡") }

    // Configuración Global (Tasa BCV y Presupuesto Total)
    val bcvRate = remember(settings) {
        settings.find { it.key == "bcv_rate" }?.value?.toDoubleOrNull() ?: 36.85
    }
    val totalMonthlyBudgetUsd = remember(settings) {
        settings.find { it.key == "total_limit_usd" }?.value?.toDoubleOrNull() ?: 1800.0
    }

    // Transacciones del mes seleccionado
    val monthTransactions = remember(transactions, selectedMonth.timeInMillis) {
        transactions.filter { tx ->
            val effectiveDate = if (tx.balanceDate > 0L) tx.balanceDate else tx.date
            val txCal = Calendar.getInstance().apply { timeInMillis = effectiveDate }
            txCal.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) &&
                txCal.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH)
        }.sortedByDescending { if (it.balanceDate > 0L) it.balanceDate else it.date }
    }

    // Filtrado en tiempo real por Categoría + Búsqueda
    val filteredTransactions = remember(monthTransactions, selectedCategoryFilter, searchQuery) {
        monthTransactions.filter { tx ->
            val matchesCat = selectedCategoryFilter == "Todas" ||
                tx.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                tx.items.any { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
            val matchesQuery = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.registeredBy.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    // Agrupación cronológica por día
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val effectiveDate = if (tx.balanceDate > 0L) tx.balanceDate else tx.date
            formatDayHeader(effectiveDate)
        }
    }

    // Métricas Financieras
    val totalSpentUsd = remember(monthTransactions) {
        monthTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val totalIncomeUsd = remember(monthTransactions) {
        monthTransactions.filter { it.isIncome }.sumOf { it.amount }
    }
    val filteredSpentUsd = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }

    // Análisis con IA
    fun runAiAnalysis(openModal: Boolean = false) {
        if (openModal) showAiBottomSheet = true
        isAnalyzingAi = true
        aiErrorMessage = null
        coroutineScope.launch {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val monthLabel = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
            val customKey = settings.find { it.key == "gemini_api_key" }?.value
            val result = FinanceAiAnalyzer.analyze(
                transactions = monthTransactions,
                categories = categories,
                monthName = monthLabel,
                customUserKey = customKey
            )
            result.onSuccess { report ->
                aiReport = report
                isAnalyzingAi = false
            }.onFailure { err ->
                aiErrorMessage = err.localizedMessage ?: "Error al analizar las finanzas"
                isAnalyzingAi = false
            }
        }
    }

    LaunchedEffect(selectedMonth.timeInMillis, monthTransactions.size) {
        if (monthTransactions.isNotEmpty()) {
            runAiAnalysis(openModal = false)
        } else {
            aiReport = null
        }
    }

    // Modal de IA BottomSheet
    FinanceAiBottomSheet(
        isOpen = showAiBottomSheet,
        onDismiss = { showAiBottomSheet = false },
        isLoading = isAnalyzingAi,
        report = aiReport,
        errorMessage = aiErrorMessage,
        onRefresh = { runAiAnalysis(openModal = true) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Selector de Mes Moderno
            item {
                ModernMonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthChange = { viewModel.updateSelectedMonth(it) }
                )
            }

            // 2. Tarjeta Hero de Asistente IA
            item {
                FinanceAiHeroCard(
                    report = aiReport,
                    isLoading = isAnalyzingAi,
                    onOpenAnalysis = {
                        if (aiReport == null) runAiAnalysis(openModal = true)
                        else showAiBottomSheet = true
                    }
                )
            }

            // 3. Master Hero Financiero Bimoneda + Salud Presupuestaria
            item {
                MasterFinancialHeroCard(
                    spentUsd = totalSpentUsd,
                    incomeUsd = totalIncomeUsd,
                    monthlyBudgetUsd = totalMonthlyBudgetUsd,
                    bcvRate = bcvRate,
                    onSyncBcv = { viewModel.syncLiveBcvRate() },
                    onExportCsv = {
                        FinanceExportHelper.exportToGoogleSheetsCsv(
                            context = context,
                            transactions = monthTransactions,
                            monthCalendar = selectedMonth
                        )
                    },
                    onQuickAdd = {
                        editingTransaction = null
                        showTxSheet = true
                    }
                )
            }

            // 4. Control Segmentado + Botón de Búsqueda
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(4.dp)
                    ) {
                        SegmentedTabButton(
                            title = "Movimientos (${monthTransactions.size})",
                            isSelected = activeSection == "gastos",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSection = "gastos" }
                        )
                        SegmentedTabButton(
                            title = "Presupuestos (${categories.size})",
                            isSelected = activeSection == "categorias",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSection = "categorias" }
                        )
                    }

                    if (activeSection == "gastos") {
                        FilledTonalIconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                }
            }

            // 5. SECCIÓN A: HISTORIAL DE MOVIMIENTOS
            if (activeSection == "gastos") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Buscador Expandible
                        AnimatedVisibility(visible = showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por concepto, categoría o miembro...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Carrusel horizontal de Chips de Categoría
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "Todas",
                                    onClick = { selectedCategoryFilter = "Todas" },
                                    label = { Text("Todas", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                            items(categories) { cat ->
                                val isSelected = selectedCategoryFilter.equals(cat.categoryName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategoryFilter = if (isSelected) "Todas" else cat.categoryName
                                    },
                                    leadingIcon = if (cat.iconEmoji.isNotBlank()) {
                                        { Text(cat.iconEmoji, fontSize = 14.sp) }
                                    } else null,
                                    label = { Text(cat.categoryName, fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        // Banner de filtro activo
                        if (selectedCategoryFilter != "Todas" || searchQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtro: ${filteredTransactions.size} reg. • Total: $${String.format(Locale.US, "%.2f", filteredSpentUsd)} USD",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Limpiar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        selectedCategoryFilter = "Todas"
                                        searchQuery = ""
                                    }
                                )
                            }
                        }

                        // Carrusel de Servicios y Pagos Recurrentes
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
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Condominio", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🏢", dueDay = 5))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Seguro Médico / Familiar", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🛡️", dueDay = 10))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Servicio Eléctrico (Luz)", defaultAmount = 0.0, category = "Servicios", iconEmoji = "⚡", dueDay = 15))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Internet / WiFi", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🌐", dueDay = 20))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Seguro Vehicular", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🚗", dueDay = 25))
                            }
                        )
                    }
                }

                // Listado de Transacciones Agrupadas por Día
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🧾",
                            title = "Sin movimientos en esta vista",
                            subtitle = "Registra un gasto o ingreso tocando el botón '+ Nuevo Registro' o escanea una factura."
                        )
                    }
                } else {
                    groupedTransactions.forEach { (dayLabel, dayTxList) ->
                        val dayNetSpent = dayTxList.filter { !it.isIncome }.sumOf { it.amount }
                        item(key = "header_$dayLabel") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayLabel.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                                if (dayNetSpent > 0) {
                                    Text(
                                        text = "-$${String.format(Locale.US, "%.2f", dayNetSpent)} USD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(dayTxList, key = { it.id.ifEmpty { "${it.date}_${it.description}_${it.originalAmount}" } }) { tx ->
                            val matchedCategory = categories.find {
                                it.categoryName.equals(tx.category, ignoreCase = true)
                            }
                            ModernTransactionCardItem(
                                tx = tx,
                                category = matchedCategory,
                                onClick = {
                                    editingTransaction = tx
                                    showTxSheet = true
                                },
                                onViewReceipt = {
                                    if (tx.receiptUrl.isNotEmpty()) {
                                        photoUrlToShow = tx.receiptUrl
                                    }
                                },
                                onDeleteRequest = { txToDelete = tx }
                            )
                        }
                    }
                }
            } else {
                // 6. SECCIÓN B: PRESUPUESTOS POR CATEGORÍA
                if (categories.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🎯",
                            title = "Configura tus presupuestos",
                            subtitle = "Crea categorías con límites mensuales en dólares para recibir alertas antes de excederte."
                        )
                    }
                } else {
                    items(categories, key = { it.categoryName }) { category ->
                        val catSpent = monthTransactions
                            .filter { !it.isIncome && (it.category.equals(category.categoryName, ignoreCase = true) || it.items.any { item -> item.category.equals(category.categoryName, ignoreCase = true) }) }
                            .sumOf { tx ->
                                if (tx.items.isNotEmpty()) {
                                    tx.items.filter { it.category.equals(category.categoryName, ignoreCase = true) }.sumOf { it.amount }
                                } else {
                                    tx.amount
                                }
                            }
                        val currentLimit = category.limitsPerMonth[monthKey] ?: category.limitAmount

                        ModernCategoryBudgetCardItem(
                            category = category,
                            spentUsd = catSpent,
                            limitUsd = currentLimit,
                            bcvRate = bcvRate,
                            onEdit = {
                                editingCategory = category
                                showCategorySheet = true
                            },
                            onFilterClick = {
                                selectedCategoryFilter = category.categoryName
                                activeSection = "gastos"
                            }
                        )
                    }
                }
            }
        }

        // Botón flotante posicionado por encima de la barra de navegación inferior sin solaparse
        ExtendedFloatingActionButton(
            onClick = {
                if (activeSection == "gastos") {
                    editingTransaction = null
                    showTxSheet = true
                } else {
                    editingCategory = null
                    showCategorySheet = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 88.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (activeSection == "gastos") "Nuevo Registro" else "Nueva Categoría",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }

    // --- BOTTOM SHEET: CREAR / EDITAR TRANSACCIÓN ---
    if (showTxSheet) {
        TransactionFormBottomSheet(
            initialTransaction = editingTransaction,
            categories = categories,
            defaultBcvRate = bcvRate,
            defaultDateMillis = selectedMonth.timeInMillis,
            onDismiss = {
                showTxSheet = false
                editingTransaction = null
            },
            onSaveNew = { amount, category, desc, currency, isIncome, date, balanceDate ->
                viewModel.addTransaction(
                    originalAmount = amount,
                    category = category,
                    description = desc,
                    currency = currency,
                    isIncome = isIncome,
                    date = date,
                    balanceDate = balanceDate
                )
                showTxSheet = false
            },
            onSaveEdit = { updatedTx ->
                viewModel.editTransaction(updatedTx)
                showTxSheet = false
                editingTransaction = null
            },
            onDelete = { tx ->
                showTxSheet = false
                editingTransaction = null
                txToDelete = tx
            },
            onViewReceipt = { receiptUrl ->
                photoUrlToShow = receiptUrl
            }
        )
    }

    // --- BOTTOM SHEET: CREAR / EDITAR CATEGORÍA ---
    if (showCategorySheet) {
        CategoryFormBottomSheet(
            initialCategory = editingCategory,
            monthKey = monthKey,
            onDismiss = {
                showCategorySheet = false
                editingCategory = null
            },
            onSave = { oldName, newName, limitUsd, emoji ->
                viewModel.createOrEditCategory(oldName, newName, limitUsd, emoji, monthKey)
                showCategorySheet = false
                editingCategory = null
            },
            onDelete = { category ->
                showCategorySheet = false
                editingCategory = null
                categoryToDelete = category
            }
        )
    }

    // --- DIÁLOGOS DE PAGOS RECURRENTES ---
    ModernRecurringPaymentDialogs(
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

    // --- DIÁLOGOS DE CONFIRMACIÓN DE BORRADO SEGURO ---
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralRed) },
            title = { Text("¿Eliminar registro?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    "Se eliminará \"${txToDelete?.description}\" por ${txToDelete?.originalAmount} ${txToDelete?.currency}. Esta acción no se puede deshacer.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete?.let { viewModel.deleteTransaction(it) }
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralRed) },
            title = { Text("¿Eliminar categoría?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    "Se eliminará la categoría \"${categoryToDelete?.categoryName}\". Los gastos asociados conservarán su nombre.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCategory(it) }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- VISOR DE RECIBO FULLSCREEN CON ZOOM ---
    if (photoUrlToShow != null) {
        ReceiptViewerDialog(
            photoUrl = photoUrlToShow!!,
            onDismiss = { photoUrlToShow = null }
        )
    }
}

// ============================================================================
// COMPONENTES VISUALES FINTECH
// ============================================================================

@Composable
private fun MasterFinancialHeroCard(
    spentUsd: Double,
    incomeUsd: Double,
    monthlyBudgetUsd: Double,
    bcvRate: Double,
    onSyncBcv: () -> Unit,
    onExportCsv: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val netBalance = incomeUsd - spentUsd
    val budgetRatio = if (monthlyBudgetUsd > 0) (spentUsd / monthlyBudgetUsd).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = budgetRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )
    val statusColor by animateColorAsState(
        targetValue = when {
            budgetRatio >= 1.0f -> CoralRed
            budgetRatio >= 0.8f -> WarningAmber
            else -> EmeraldGreen
        },
        label = "statusColor"
    )

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Fila Superior: Etiqueta + Chip de Tasa BCV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GASTO TOTAL DEL MES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.1.sp
                )
                Surface(
                    onClick = onSyncBcv,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏛️ BCV: ${String.format(Locale.US, "%.2f", bcvRate)} Bs/$",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sincronizar BCV",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Monto Principal en USD + Equivalente en VES
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", spentUsd)}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "≈ Bs. ${String.format(Locale.US, "%,.2f", spentUsd * bcvRate)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Píldora de Estado Contable
                val isPositive = netBalance >= 0
                Surface(
                    color = (if (isPositive) EmeraldGreen else CoralRed).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, (if (isPositive) EmeraldGreen else CoralRed).copy(alpha = 0.4f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPositive) "SUPERÁVIT" else "FLUJO NEGATIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPositive) EmeraldGreen else CoralRed
                        )
                        val sign = if (netBalance > 0) "+" else ""
                        Text(
                            text = "$sign$${String.format(Locale.US, "%,.2f", netBalance)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPositive) EmeraldGreen else CoralRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de Salud del Presupuesto Global del Hogar
            if (monthlyBudgetUsd > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val remaining = (monthlyBudgetUsd - spentUsd).coerceAtLeast(0.0)
                    Text(
                        text = if (spentUsd <= monthlyBudgetUsd)
                            "Disponible: $${String.format(Locale.US, "%,.0f", remaining)} de $${monthlyBudgetUsd.roundToInt()}"
                        else
                            "Excedido por $${String.format(Locale.US, "%,.2f", spentUsd - monthlyBudgetUsd)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(budgetRatio * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Métricas Secundarias + Acciones Rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldGreen.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📈", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Ingresos Mes", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "+$${String.format(Locale.US, "%,.2f", incomeUsd)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("📊", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sheets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onQuickAdd,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModernTransactionCardItem(
    tx: Transaction,
    category: CategoryDetail?,
    onClick: () -> Unit,
    onViewReceipt: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayTime = timeFormat.format(Date(if (tx.balanceDate > 0) tx.balanceDate else tx.date))
    val iconEmoji = when {
        tx.isIncome -> "💵"
        !category?.iconEmoji.isNullOrBlank() -> category!!.iconEmoji
        else -> "💳"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Izquierda: Avatar con Emoji + Descripción y Metadatos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (tx.isIncome) EmeraldGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description.ifBlank { "Gasto sin concepto" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = tx.category.ifBlank { "General" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tx.items.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🧾 ${tx.items.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        if (tx.registeredBy.isNotBlank()) {
                            Text(
                                text = tx.registeredBy,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = displayTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Chip para ver factura si tiene foto adjunta
                    if (tx.receiptUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = onViewReceipt,
                            shape = RoundedCornerShape(6.dp),
                            color = IndigoAccent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = "Ver Recibo",
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ver factura",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Derecha: Monto en USD protagonista + Conversión BCV debajo
            Column(horizontalAlignment = Alignment.End) {
                val sign = if (tx.isIncome) "+" else "-"
                val amountColor = if (tx.isIncome) EmeraldGreen else MaterialTheme.colorScheme.onSurface

                Text(
                    text = "$sign$${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )

                if (tx.currency == "VES") {
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", tx.originalAmount)} Bs",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "USD",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernCategoryBudgetCardItem(
    category: CategoryDetail,
    spentUsd: Double,
    limitUsd: Double,
    bcvRate: Double,
    onEdit: () -> Unit,
    onFilterClick: () -> Unit
) {
    val ratio = if (limitUsd > 0) (spentUsd / limitUsd).toFloat() else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "catProgress"
    )
    val barColor = when {
        ratio > 1.0f -> CoralRed
        ratio >= 0.8f -> WarningAmber
        else -> EmeraldGreen
    }
    val remainingUsd = limitUsd - spentUsd

    Surface(
        onClick = onFilterClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (ratio > 1f) CoralRed.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.iconEmoji.ifBlank { "📁" },
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.categoryName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (remainingUsd >= 0)
                                "Quedan $${String.format(Locale.US, "%.2f", remainingUsd)} USD"
                            else
                                "Excedido por $${String.format(Locale.US, "%.2f", -remainingUsd)} USD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = barColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${spentUsd.roundToInt()} / $${limitUsd.roundToInt()} USD",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ratio > 1f) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(ratio * 100).roundToInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar Presupuesto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: REGISTRO Y EDICIÓN DE TRANSACCIÓN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionFormBottomSheet(
    initialTransaction: Transaction?,
    categories: List<CategoryDetail>,
    defaultBcvRate: Double,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSaveNew: (Double, String, String, String, Boolean, Long, Long) -> Unit,
    onSaveEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val isEditing = initialTransaction != null
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isIncome by remember { mutableStateOf(initialTransaction?.isIncome ?: false) }
    var currency by remember { mutableStateOf(initialTransaction?.currency ?: "USD") }
    var amountInput by remember { mutableStateOf(initialTransaction?.originalAmount?.toString() ?: "") }
    var descriptionInput by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category
                ?: categories.firstOrNull()?.categoryName
                ?: "General"
        )
    }
    var txDate by remember { mutableStateOf(initialTransaction?.date ?: System.currentTimeMillis()) }
    var balanceDate by remember {
        mutableStateOf(
            initialTransaction?.balanceDate?.takeIf { it > 0L } ?: txDate
        )
    }
    var activeBcvRate by remember {
        mutableStateOf(initialTransaction?.bcvRateUsed?.takeIf { it > 0.0 } ?: defaultBcvRate)
    }

    LaunchedEffect(txDate, currency) {
        if (currency == "VES") {
            val fetched = BcvExchangeRateService.fetchRateForDate(txDate)
            if (fetched > 0.0) activeBcvRate = fetched
        }
    }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera + Botón Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Movimiento" else "Nuevo Movimiento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                if (isEditing) {
                    IconButton(onClick = { onDelete(initialTransaction!!) }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = CoralRed
                        )
                    }
                }
            }

            // Selector Gasto vs Ingreso y Moneda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tipo: Gasto / Ingreso
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    listOf(false to "Gasto (-)", true to "Ingreso (+)").forEach { (inc, label) ->
                        val selected = isIncome == inc
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) (if (inc) EmeraldGreen else MaterialTheme.colorScheme.primary)
                                    else Color.Transparent
                                )
                                .clickable { isIncome = inc }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Moneda: USD / VES
                Row(
                    modifier = Modifier
                        .weight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    listOf("USD" to "$ USD", "VES" to "Bs VES").forEach { (cur, label) ->
                        val selected = currency == cur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) IndigoAccent else Color.Transparent)
                                .clickable { currency = cur }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input de Monto Protagonista + Conversión BCV en Vivo
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text(if (currency == "USD") "Monto en Dólares ($)" else "Monto en Bolívares (Bs)") },
                prefix = {
                    Text(
                        text = if (currency == "USD") "$ " else "Bs. ",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (currency == "VES") {
                val parsed = amountInput.toDoubleOrNull() ?: 0.0
                val usdEquiv = if (activeBcvRate > 0) parsed / activeBcvRate else 0.0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Conversión BCV (${String.format(Locale.US, "%.2f", activeBcvRate)} Bs/$):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "≈ $${String.format(Locale.US, "%.2f", usdEquiv)} USD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = IndigoAccent
                        )
                    }
                }
            }

            // Concepto / Descripción
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Concepto o comercio (ej. Automercado, Farmacia)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Selectores de Fecha (Registro y Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = txDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                txDate = newCal.timeInMillis
                                if (!isEditing) balanceDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Fecha factura", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sdf.format(Date(txDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = balanceDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                balanceDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Mes contable", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sdf.format(Date(balanceDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Selección de Categoría con Emojis
            if (!isIncome && categories.isNotEmpty()) {
                Text(
                    text = "Categoría",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.categoryName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.categoryName },
                            leadingIcon = if (cat.iconEmoji.isNotBlank()) {
                                { Text(cat.iconEmoji, fontSize = 14.sp) }
                            } else null,
                            label = {
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Desglose de Productos Escaneados por IA (si existen)
            if (!initialTransaction?.items.isNullOrEmpty()) {
                val itemsList = initialTransaction!!.items
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧾 Desglose del Recibo (${itemsList.size} ítems)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Extraído con IA",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        itemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.category.isNotBlank()) {
                                        Text(
                                            text = item.category,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.amount)} USD",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            // Botón para ver factura adjunta si existe
            if (!initialTransaction?.receiptUrl.isNullOrEmpty()) {
                FilledTonalButton(
                    onClick = { onViewReceipt(initialTransaction!!.receiptUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Foto del Recibo Original", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón Guardar Principal
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0.0 && descriptionInput.isNotBlank()) {
                        val finalCat = if (isIncome) "Ingreso" else selectedCategory
                        if (isEditing) {
                            val effectiveRate = if (currency == "VES") {
                                if (activeBcvRate > 0.0) activeBcvRate else defaultBcvRate.coerceAtLeast(1.0)
                            } else 1.0
                            val updatedUsdAmount = if (currency == "VES") amountVal / effectiveRate else amountVal
                            onSaveEdit(
                                initialTransaction!!.copy(
                                    amount = updatedUsdAmount,
                                    originalAmount = amountVal,
                                    bcvRateUsed = effectiveRate,
                                    category = finalCat,
                                    description = descriptionInput.trim(),
                                    currency = currency,
                                    isIncome = isIncome,
                                    date = txDate,
                                    balanceDate = balanceDate
                                )
                            )
                        } else {
                            onSaveNew(
                                amountVal,
                                finalCat,
                                descriptionInput.trim(),
                                currency,
                                isIncome,
                                txDate,
                                balanceDate
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Guardar Cambios" else "Confirmar Registro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: CREAR / EDITAR CATEGORÍA DE PRESUPUESTO
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormBottomSheet(
    initialCategory: CategoryDetail?,
    monthKey: String,
    onDismiss: () -> Unit,
    onSave: (String?, String, Double, String) -> Unit,
    onDelete: (CategoryDetail) -> Unit
) {
    val isEditing = initialCategory != null
    val currentLimit = initialCategory?.limitsPerMonth?.get(monthKey) ?: initialCategory?.limitAmount ?: 200.0

    var nameInput by remember { mutableStateOf(initialCategory?.categoryName ?: "") }
    var limitInput by remember { mutableStateOf(if (isEditing) currentLimit.roundToInt().toString() else "") }
    var emojiInput by remember { mutableStateOf(initialCategory?.iconEmoji ?: "🛒") }

    val quickEmojis = listOf("🛒", "🏠", "🚗", "💊", "🍽️", "⚡", "🐾", "🎉", "📱", "✈️")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Presupuesto" else "Nueva Categoría",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                if (isEditing) {
                    IconButton(onClick = { onDelete(initialCategory!!) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = CoralRed)
                    }
                }
            }

            // Selector rápido de Emojis
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickEmojis) { emoji ->
                    val selected = emojiInput == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { emojiInput = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = emojiInput,
                    onValueChange = { emojiInput = it },
                    label = { Text("Icono") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(85.dp)
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre de categoría") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = limitInput,
                onValueChange = { limitInput = it },
                label = { Text("Límite mensual en Dólares ($ USD)") },
                prefix = { Text("$ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val limitVal = limitInput.toDoubleOrNull() ?: 200.0
                    if (nameInput.isNotBlank()) {
                        onSave(initialCategory?.categoryName, nameInput.trim(), limitVal, emojiInput.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Guardar Presupuesto" else "Crear Categoría",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ============================================================================
// COMPONENTES AUXILIARES
// ============================================================================

@Composable
private fun SegmentedTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyStateCard(emoji: String, title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 38.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptViewerDialog(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val bmp = remember(photoUrl) {
            try {
                val base64Str = if (photoUrl.contains("base64,")) {
                    photoUrl.substringAfter("base64,")
                } else {
                    photoUrl
                }
                val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra superior limpia sin solaparse con la imagen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Comprobante / Recibo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pellizca para hacer zoom o arrastra",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scale > 1.05f) {
                            FilledTonalButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Restablecer 1x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Recibo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val maxX = (size.width * (scale - 1f)) / 2f
                                        val maxY = (size.height * (scale - 1f)) / 2f
                                        offset = Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
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
                        Text(
                            text = "No se pudo decodificar la imagen del recibo.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatDayHeader(timestampMillis: Long): String {
    val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Hoy"
        targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "Ayer"
        else -> SimpleDateFormat("EEEE, d MMMM", Locale("es", "ES"))
            .format(Date(timestampMillis))
            .replaceFirstChar { it.uppercase() }
    }
}
