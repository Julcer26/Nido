package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.CategoryDetail
import com.example.database.Transaction
import com.example.database.RecurringPayment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Helper extension for color hex string to Color
private fun String.toColorSafe(): Color {
    return try {
        if (this.startsWith("#")) {
            Color(android.graphics.Color.parseColor(this))
        } else {
            Color(0xFF6366F1)
        }
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }
}

// 1. NAVEGADOR DE MES ULTRA-MODERNO (FLOATING PILL WIDGET)
@Composable
fun ModernMonthSelector(
    selectedMonth: Calendar,
    onMonthChange: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    val monthStr = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = selectedMonth.clone() as Calendar
                    prev.add(Calendar.MONTH, -1)
                    onMonthChange(prev)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Mes Anterior",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, _ ->
                                val newCal = selectedMonth.clone() as Calendar
                                newCal.set(Calendar.YEAR, year)
                                newCal.set(Calendar.MONTH, month)
                                newCal.set(Calendar.DAY_OF_MONTH, 1)
                                onMonthChange(newCal)
                            },
                            selectedMonth.get(Calendar.YEAR),
                            selectedMonth.get(Calendar.MONTH),
                            1
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = monthStr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = {
                    val next = selectedMonth.clone() as Calendar
                    next.add(Calendar.MONTH, 1)
                    onMonthChange(next)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Mes Siguiente",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 2. MASTER CARD DE BALANCE Y ACCIONES FINANCIERAS
@Composable
fun ModernFinanceBalanceCard(
    spentUsd: Double,
    incomeUsd: Double,
    bcvRate: Double,
    onExportGoogleSheets: () -> Unit,
    onRegisterExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netBalance = incomeUsd - spentUsd
    val isPositive = netBalance >= 0

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Label & Net Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESUMEN DEL MES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Surface(
                    color = (if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, (if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isPositive) "● Superávit" else "● Déficit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Metric: Total Spent
            Text(
                text = "Total Gastado",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", spentUsd)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (spentUsd > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "USD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary metrics row (Ingresos & Balance Neto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ingresos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+$${incomeUsd.roundToInt()} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tasa BCV", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", bcvRate)} Bs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance Neto", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val prefix = if (netBalance > 0) "+" else ""
                    Text(
                        text = "$prefix$${netBalance.roundToInt()} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Hoja de Cálculo & Registrar Gasto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exportar Hoja de Cálculo (Google Sheets / Excel)
                OutlinedButton(
                    onClick = onExportGoogleSheets,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.75f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF10B981)
                    )
                ) {
                    Text("📊", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hoja de Cálculo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Registrar Gasto
                Button(
                    onClick = onRegisterExpense,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuevo Gasto",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 2B. BARRA DE ACCIONES FINANCIERAS COMPACTA (Sustituye la tarjeta de balance redundante)
@Composable
fun ModernFinanceActionBar(
    onExportGoogleSheets: () -> Unit,
    onRegisterExpense: () -> Unit,
    bcvRate: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵 BCV: ", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", bcvRate)} Bs/$",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "Acciones Rápidas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exportar Hoja de Cálculo (Google Sheets / Excel)
                OutlinedButton(
                    onClick = onExportGoogleSheets,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.75f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF10B981)
                    )
                ) {
                    Text("📊", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hoja de Cálculo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Registrar Gasto
                Button(
                    onClick = onRegisterExpense,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuevo Gasto",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 3. SELECTOR SEGMENTADO ANIMADO (HISTORIAL vs PRESUPUESTOS)
@Composable
fun ModernSegmentedTab(
    activeSection: String,
    onSectionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val isGastos = activeSection == "gastos"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGastos) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSectionSelected("gastos") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Historial de Gastos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGastos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isPresupuestos = activeSection == "categorias"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPresupuestos) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSectionSelected("categorias") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Presupuestos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresupuestos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 4. CARRUSEL DE FILTROS DE CATEGORÍA ULTRA-RÁPIDO Y LIGERO
@Composable
fun ModernCategoryFilterChips(
    categories: List<CategoryDetail>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val allOptions = listOf("Todas") + categories.map { it.categoryName }.distinct()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allOptions.forEach { catName ->
            val isSelected = selectedCategory.equals(catName, ignoreCase = true)
            val catDetail = categories.find { it.categoryName.equals(catName, ignoreCase = true) }
            val emoji = if (catName == "Todas") "✨" else (catDetail?.iconEmoji?.ifBlank { "🏷️" } ?: "🏷️")

            Surface(
                onClick = { onCategorySelected(catName) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = catName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// 5. TARJETA DE TRANSACCIÓN MODERNA Y UNIFORME (ESTRICTAMENTE ESTANDARIZADA)
@Composable
fun ModernTransactionCard(
    tx: Transaction,
    categories: List<CategoryDetail>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShowPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sanitización robusta: eliminar cualquier salto de línea \r \n devuelto por OCR o digitado
    val cleanDescription = tx.description
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .trim()
        .ifEmpty { "Gasto sin concepto" }

    val cleanCategory = tx.category
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .trim()
        .ifEmpty { "General" }

    val format = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
    val displayDate = if (tx.balanceDate > 0) tx.balanceDate else tx.date
    val dateString = format.format(Date(displayDate))

    val foundCat = categories.find { it.categoryName.equals(cleanCategory, ignoreCase = true) }
    val iconEmoji = foundCat?.iconEmoji ?: ""
    val catColor = if (tx.isIncome) Color(0xFF10B981) else (foundCat?.colorHex?.toColorSafe() ?: Color(0xFF6366F1))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp, max = 74.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Izquierda: Squircle Avatar & Textos limpios de 2 líneas
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.15f))
                        .border(1.dp, catColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (tx.isIncome) {
                        Text("💵", fontSize = 18.sp)
                    } else if (iconEmoji.isNotBlank()) {
                        Text(iconEmoji, fontSize = 18.sp)
                    } else {
                        Text(
                            text = cleanCategory.take(1).uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = cleanDescription,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cleanCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = catColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (tx.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(5.dp)
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

                        Spacer(modifier = Modifier.width(5.dp))
                        Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = dateString,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Derecha: Monto, Equivalencia USD y Usuario
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val symbol = if (tx.currency == "USD") "$" else "Bs."
                val prefix = if (tx.isIncome) "+" else "-"
                val amountColor = if (tx.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface

                Text(
                    text = "$prefix$symbol${String.format(Locale.US, "%,.2f", tx.originalAmount)}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (tx.currency != "USD") {
                        Text(
                            text = "≈ $${String.format(Locale.US, "%.2f", tx.amount)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    if (tx.registeredBy.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = tx.registeredBy,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                maxLines = 1
                            )
                        }
                    }

                    if (tx.receiptUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("📷", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// 6. SECCIÓN MODERNA DE PAGOS RECURRENTES Y SERVICIOS (INICIO & FINANZAS)
@Composable
fun ModernRecurringPaymentsSection(
    recurringPayments: List<RecurringPayment>,
    monthKey: String,
    onPaymentClick: (RecurringPayment) -> Unit,
    onAddPaymentClick: () -> Unit,
    onSeedDefaultPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paidCount = recurringPayments.count { it.paidMonths[monthKey] == true }
    val totalCount = recurringPayments.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
    // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "⚡ Servicios y Pagos Frecuentes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        color = if (paidCount == totalCount) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$paidCount de $totalCount pagados este mes",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (paidCount == totalCount) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Surface(
                onClick = onAddPaymentClick,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuevo servicio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nuevo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (recurringPayments.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏢 🛡️ ⚡ 🌐",
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No has registrado servicios fijos (Condominio, Seguros, Luz, Internet...)",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSeedDefaultPayments,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Cargar Servicios Frecuentes (Condominio, Seguros, Luz, WiFi)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                this@LazyRow.items(recurringPayments) { rec ->
                    val isPaid = rec.paidMonths[monthKey] == true
                    Surface(
                        onClick = { onPaymentClick(rec) },
                        color = if (isPaid) Color(0xFF10B981).copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isPaid) Color(0xFF10B981).copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        ),
                        shadowElevation = 1.dp,
                        modifier = Modifier.width(185.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Emoji & Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isPaid) Color(0xFF10B981).copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(rec.iconEmoji.ifEmpty { "⚡" }, fontSize = 18.sp)
                                }

                                Surface(
                                    color = if (isPaid) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isPaid) "PAGADO 🟢" else "POR PAGAR 🔴",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = rec.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "📅 Vence el día ${rec.dueDay}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (rec.defaultAmount > 0.0) {
                                    val symbol = if (rec.currency == "USD") "$" else "Bs."
                                    Text(
                                        text = "$symbol${String.format(Locale.US, "%,.2f", rec.defaultAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "Monto variable",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = if (isPaid) "Detalles ✓" else "Pagar ➔",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaid) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Tarjeta final para agregar un nuevo servicio frecuente desde el carrusel
                item {
                    Surface(
                        onClick = onAddPaymentClick,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .width(145.dp)
                            .height(128.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Servicio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Agregar Servicio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernRecurringPaymentDialogs(
    monthKey: String,
    selectedMonth: java.util.Calendar,
    selectedRecurringPayment: RecurringPayment?,
    showMarkPaidDialog: Boolean,
    showPaidDetailsDialog: Boolean,
    showAddRecurringDialog: Boolean,
    recNameInput: String,
    recAmountInput: String,
    recCurrencySelected: String,
    recDueDayInput: String,
    recEmojiInput: String,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDueDayChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onDismissMarkPaid: () -> Unit,
    onDismissPaidDetails: () -> Unit,
    onDismissAddRecurring: () -> Unit,
    onSwitchToEdit: (RecurringPayment) -> Unit,
    onConfirmMarkPaid: (RecurringPayment, Double, String, Long) -> Unit,
    onConfirmMarkUnpaid: (RecurringPayment) -> Unit,
    onSaveRecurring: (RecurringPayment) -> Unit,
    onDeleteRecurring: (RecurringPayment) -> Unit
) {
    if (showMarkPaidDialog && selectedRecurringPayment != null) {
        val rec = selectedRecurringPayment
        val defaultAmt = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
        AlertDialog(
            onDismissRequest = onDismissMarkPaid,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(rec.iconEmoji.ifEmpty { "⚡" }, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Pago 🟢", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            onDismissMarkPaid()
                            onSwitchToEdit(rec)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Servicio: ${rec.name} ($monthKey)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Ingresa el monto pagado este mes o deja el monto sugerido para registrarlo en tus gastos.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = recAmountInput,
                        onValueChange = onAmountChange,
                        label = { Text("Monto Pagado (${if (rec.defaultAmount > 0) "${rec.defaultAmount} ${rec.currency}" else "Opcional"})") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(defaultAmt.ifEmpty { "0.00" }) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("USD", "VES").forEach { cur ->
                            val isSelected = recCurrencySelected == cur
                            Button(
                                onClick = { onCurrencyChange(cur) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(if (cur == "USD") "$ USD" else "Bs. VES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = recAmountInput.replace(",", ".").toDoubleOrNull() ?: rec.defaultAmount
                        val paidCal = selectedMonth.clone() as java.util.Calendar
                        val dayToUse = rec.dueDay.coerceIn(1, paidCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                        paidCal.set(java.util.Calendar.DAY_OF_MONTH, dayToUse)
                        onConfirmMarkPaid(rec, amt, recCurrencySelected, paidCal.timeInMillis)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirmar Pago", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissMarkPaid) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPaidDetailsDialog && selectedRecurringPayment != null) {
        val rec = selectedRecurringPayment
        AlertDialog(
            onDismissRequest = onDismissPaidDetails,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(rec.iconEmoji.ifEmpty { "⚡" }, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Servicio al Día 🟢", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            onDismissPaidDetails()
                            onSwitchToEdit(rec)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Servicio: ${rec.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Estado para $monthKey: ¡Pago Registrado con Éxito!",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Día de corte mensual: Día ${rec.dueDay}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (rec.defaultAmount > 0.0) {
                        Text("Monto base sugerido: ${rec.defaultAmount} ${rec.currency}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmMarkUnpaid(rec) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desmarcar Pago", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPaidDetails) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showAddRecurringDialog) {
        var nameValidationError by remember { mutableStateOf(false) }
        val emojiPresets = listOf("⚡", "🏢", "🌐", "🛡️", "🏥", "☎️", "🚗", "💧", "♻️", "📺")
        val quickServices = listOf(
            "🏢" to "Condominio",
            "⚡" to "Electricidad (Luz)",
            "🌐" to "Internet / WiFi",
            "🏥" to "Seguro Médico",
            "💧" to "Agua / Hidrológica",
            "☎️" to "Teléfono / Plan Móvil"
        )

        AlertDialog(
            onDismissRequest = onDismissAddRecurring,
            title = {
                Text(
                    text = if (selectedRecurringPayment == null) "Nuevo Servicio o Pago Fijo" else "Editar Servicio Frecuente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedRecurringPayment == null) {
                        Text(
                            text = "Sugerencias rápidas (toca para rellenar):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            this@LazyRow.items(quickServices) { (emoji, label) ->
                                Surface(
                                    onClick = {
                                        onEmojiChange(emoji)
                                        onNameChange(label)
                                        nameValidationError = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "$emoji $label",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = recNameInput,
                        onValueChange = {
                            onNameChange(it)
                            if (it.isNotBlank()) nameValidationError = false
                        },
                        label = { Text("Nombre del servicio (ej. Condominio, Luz)") },
                        isError = nameValidationError,
                        supportingText = if (nameValidationError) {
                            { Text("Escribe un nombre para el servicio", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = recAmountInput,
                        onValueChange = onAmountChange,
                        label = { Text("Monto Mensual Sugerido (Opcional)") },
                        placeholder = { Text("0.00 (deja vacío si varía cada mes)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = recDueDayInput,
                            onValueChange = onDueDayChange,
                            label = { Text("Día de pago (1-31)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = recEmojiInput,
                            onValueChange = onEmojiChange,
                            label = { Text("Icono / Emoji") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Selector rápido de Emojis
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        this@LazyRow.items(emojiPresets) { em ->
                            val isSelected = recEmojiInput == em
                            Surface(
                                onClick = { onEmojiChange(em) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = em,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text("Moneda por defecto:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("USD", "VES").forEach { cur ->
                            val isSelected = recCurrencySelected == cur
                            Button(
                                onClick = { onCurrencyChange(cur) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(if (cur == "USD") "$ USD" else "Bs. VES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedRecurringPayment != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onDeleteRecurring(selectedRecurringPayment) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eliminar este servicio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recNameInput.isBlank()) {
                            nameValidationError = true
                        } else {
                            val amount = recAmountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val due = (recDueDayInput.toIntOrNull() ?: 5).coerceIn(1, 31)
                            val item = (selectedRecurringPayment ?: RecurringPayment()).copy(
                                name = recNameInput.trim(),
                                defaultAmount = amount,
                                currency = recCurrencySelected,
                                dueDay = due,
                                iconEmoji = recEmojiInput.ifBlank { "⚡" }
                            )
                            onSaveRecurring(item)
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAddRecurring) {
                    Text("Cancelar")
                }
            }
        )
    }
}

