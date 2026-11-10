package com.example.database

import com.google.firebase.database.PropertyName

data class TransactionItem(
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = ""
)

data class Transaction(
    var id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val date: Long = 0L,
    val registeredBy: String = "",
    @get:PropertyName("isRecurring") @set:PropertyName("isRecurring")
    var isRecurring: Boolean = false,
    val currency: String = "USD",
    val originalAmount: Double = 0.0,
    val bcvRateUsed: Double = 1.0,
    @get:PropertyName("isIncome") @set:PropertyName("isIncome")
    var isIncome: Boolean = false,
    val receiptUrl: String = "",
    val balanceDate: Long = 0L,
    val items: List<TransactionItem> = emptyList()
)

data class CategoryDetail(
    var categoryName: String = "",
    val limitAmount: Double = 0.0,
    val colorHex: String = "",
    var iconEmoji: String = "",
    var limitsPerMonth: Map<String, Double> = emptyMap()
)

data class Chore(
    var id: String = "",
    val title: String = "",
    val assignedTo: String = "",
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val createdDate: Long = 0L,
    val completedDate: Long? = null,
    val createdBy: String = "",
    val description: String = "",
    val priority: String = "Medio",
    val category: String = "General",
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate")
    var isPrivate: Boolean = false
)

data class ShoppingItem(
    var id: String = "",
    val name: String = "",
    @get:PropertyName("isBought") @set:PropertyName("isBought")
    var isBought: Boolean = false,
    val quantity: String = "1",
    val addedBy: String = "",
    val dateAdded: Long = 0L,
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate")
    var isPrivate: Boolean = false,
    val listName: String = "General"
)

data class ActivityLog(
    var id: String = "",
    val timestamp: Long = 0L,
    val tag: String = "",
    val message: String = "",
    val user: String = ""
)

data class UserProfile(
    var name: String = "",
    val colorHex: String = ""
)

data class AppSetting(
    var key: String = "",
    val value: String = ""
)

data class Goal(
    var id: String = "",
    val title: String = "",
    val targetAmount: Double? = null,
    val savedAmount: Double = 0.0,
    val createdBy: String = "",
    val createdDate: Long = 0L,
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate")
    var isPrivate: Boolean = false
)

data class Note(
    var id: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdDate: Long = 0L,
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate")
    var isPrivate: Boolean = true
)

data class RecurringPayment(
    var id: String = "",
    val name: String = "",
    val defaultAmount: Double = 0.0,
    val currency: String = "USD",
    val category: String = "Servicios",
    val dueDay: Int = 5,
    val iconEmoji: String = "⚡",
    val createdBy: String = "",
    val paidMonths: Map<String, Boolean> = emptyMap(),
    val paidTransactionIds: Map<String, String> = emptyMap()
)

