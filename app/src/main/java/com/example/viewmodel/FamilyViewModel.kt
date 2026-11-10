package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiParser
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyViewModel(
    private val repository: FamilyRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow("")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _selectedMonth = MutableStateFlow(java.util.Calendar.getInstance())
    val selectedMonth: StateFlow<java.util.Calendar> = _selectedMonth.asStateFlow()

    fun updateSelectedMonth(calendar: java.util.Calendar) {
        _selectedMonth.value = calendar
    }


    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryDetail>> = repository.allCategories
        .map { list -> list.sortedBy { it.categoryName.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chores: StateFlow<List<Chore>> = repository.allChores
        .map { list -> list.sortedBy { it.title.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingItem>> = repository.allShoppingItems
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<List<AppSetting>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkTheme: StateFlow<Boolean> = dataStoreManager.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkTheme(isDark)
        }
    }

    val activityLogs: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = combine(repository.allGoals, _currentUser) { list, user ->
        list.filter { !it.isPrivate || it.createdBy == user }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(repository.allNotes, _currentUser) { list, user ->
        list.filter { it.createdBy == user } // Notas totalmente privadas
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringPayments: StateFlow<List<com.example.database.RecurringPayment>> = repository.allRecurringPayments
        .map { list -> list.sortedBy { it.dueDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createOrEditRecurringPayment(payment: com.example.database.RecurringPayment) {
        viewModelScope.launch {
            try {
                repository.insertRecurringPayment(payment, _currentUser.value)
                notifyUser("Servicio guardado: '${payment.name}'")
            } catch (e: Exception) {
                notifyUser("Error al guardar servicio: ${e.localizedMessage}")
            }
        }
    }

    fun deleteRecurringPayment(payment: com.example.database.RecurringPayment) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringPayment(payment, _currentUser.value)
                notifyUser("Servicio eliminado: '${payment.name}'")
            } catch (e: Exception) {
                notifyUser("Error al eliminar servicio: ${e.localizedMessage}")
            }
        }
    }

    fun markRecurringPaymentPaid(payment: com.example.database.RecurringPayment, monthKey: String, amount: Double, currency: String, date: Long) {
        viewModelScope.launch {
            try {
                val alert = repository.markRecurringPaymentPaid(payment, monthKey, amount, currency, date, _currentUser.value)
                if (alert != null) notifyUser(alert)
                else notifyUser("Pago registrado: '${payment.name}' para $monthKey")
            } catch (e: Exception) {
                notifyUser("Error al registrar pago: ${e.localizedMessage}")
            }
        }
    }

    fun markRecurringPaymentUnpaid(payment: com.example.database.RecurringPayment, monthKey: String) {
        viewModelScope.launch {
            try {
                repository.markRecurringPaymentUnpaid(payment, monthKey, _currentUser.value)
                notifyUser("Pago desmarcado: '${payment.name}' para $monthKey")
            } catch (e: Exception) {
                notifyUser("Error al desmarcar pago: ${e.localizedMessage}")
            }
        }
    }

    private val _isRegisteringNlp = MutableStateFlow(false)
    val isRegisteringNlp: StateFlow<Boolean> = _isRegisteringNlp.asStateFlow()

    private val _nlpError = MutableStateFlow<String?>(null)
    val nlpError: StateFlow<String?> = _nlpError.asStateFlow()

    private val _topNotification = MutableStateFlow<String?>(null)
    val topNotification: StateFlow<String?> = _topNotification.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()
    fun triggerConfetti() { _confettiTrigger.value += 1 }

    init {
        viewModelScope.launch {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                dataStoreManager.clearAuthData()
                _familyId.value = null
                _currentUser.value = ""
            } else {
                val fid = dataStoreManager.familyIdFlow.firstOrNull()
                val name = dataStoreManager.userNameFlow.firstOrNull()
                if (fid != null && name != null) {
                    _familyId.value = fid
                    _currentUser.value = name
                    repository.setFamilyId(fid)
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    dataStoreManager.clearAuthData()
                }
            }
        }
    }

    // --- Authentication ---
    fun loginWithPin(inputPin: String): Boolean {
        var success = false
        viewModelScope.launch {
            val savedPin = dataStoreManager.userPinFlow.firstOrNull()
            if (savedPin == inputPin) {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                    _isAuthenticated.value = true
                    success = true
                    checkDefaultsAndSync()
                } else {
                    notifyUser("Sesión de Firebase expirada. Inicie sesión de nuevo.")
                    dataStoreManager.clearAuthData()
                    _familyId.value = null
                }
            } else {
                notifyUser("PIN incorrecto")
            }
        }
        return success
    }

    fun loginBiometric() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            _isAuthenticated.value = true
            checkDefaultsAndSync()
        } else {
            notifyUser("Sesión expirada. Inicie sesión de nuevo.")
            viewModelScope.launch {
                dataStoreManager.clearAuthData()
                _familyId.value = null
            }
        }
    }

    fun createFamily(email: String, pin: String, password: String) {
        viewModelScope.launch {
            try {
                val authResult = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el UID de Firebase")
                
                val newFamilyId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                val userName = email.substringBefore("@")
                
                repository.registerUserMapping(uid, newFamilyId, userName)
                repository.registerFamilyMember(newFamilyId, uid)
                
                dataStoreManager.saveAuthData(newFamilyId, userName, pin)
                _familyId.value = newFamilyId
                repository.setFamilyId(newFamilyId)
                
                repository.populateDefaultsIfNeeded()
                repository.insertProfile(UserProfile(userName, "#D0BCFF"), userName)
                
                _currentUser.value = userName
                _isAuthenticated.value = true
                checkDefaultsAndSync()
                notifyUser("Familia creada. Código: $newFamilyId")
            } catch (e: Exception) {
                notifyUser("Error al crear familia: ${e.localizedMessage}")
            }
        }
    }

    fun joinFamily(familyCode: String, name: String, pin: String, email: String, password: String) {
        viewModelScope.launch {
            val code = familyCode.trim().uppercase()
            try {
                val familyExists = repository.checkFamilyExists(code)
                if (!familyExists) {
                    notifyUser("Código familiar no encontrado. Verifica e intenta de nuevo.")
                    return@launch
                }
                
                val authResult = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.trim(), password).await()
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email.trim(), password).await()
                }
                val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el UID")
                
                repository.registerUserMapping(uid, code, name.trim())
                repository.registerFamilyMember(code, uid)
                
                dataStoreManager.saveAuthData(code, name.trim(), pin)
                _familyId.value = code
                repository.setFamilyId(code)
                
                repository.insertProfile(UserProfile(name.trim(), "#EFB8C8"), name.trim())
                _currentUser.value = name.trim()
                _isAuthenticated.value = true
                checkDefaultsAndSync()
                notifyUser("Unido a familia: $code")
            } catch (e: Exception) {
                notifyUser("Error al unirse: ${e.localizedMessage}")
            }
        }
    }

    fun loginExisting(email: String, password: String, pin: String) {
        viewModelScope.launch {
            try {
                val authResult = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email.trim(), password).await()
                val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el UID")
                
                val snapshot = repository.getUserMapping(uid)
                val fid = snapshot.child("familyId").getValue(String::class.java)
                val userName = snapshot.child("userName").getValue(String::class.java)
                
                if (fid == null || userName == null) {
                    notifyUser("Información de usuario no encontrada en el servidor.")
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    return@launch
                }
                
                dataStoreManager.saveAuthData(fid, userName, pin)
                _familyId.value = fid
                repository.setFamilyId(fid)
                _currentUser.value = userName
                _isAuthenticated.value = true
                checkDefaultsAndSync()
                notifyUser("¡Bienvenido de vuelta, $userName!")
            } catch (e: Exception) {
                notifyUser("Error al iniciar sesión: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            dataStoreManager.clearAuthData()
            _isAuthenticated.value = false
            _familyId.value = null
            _currentUser.value = ""
        }
    }

    private fun checkDefaultsAndSync() {
        viewModelScope.launch {
            try {
                repository.updateBcvRateFromLiveApi("Sistema")
            } catch (e: Exception) { }
        }
    }

    fun clearNlpError() {
        _nlpError.value = null
    }

    private fun notifyUser(message: String) {
        _topNotification.value = message
    }

    fun dismissNotification() {
        _topNotification.value = null
    }

    // --- Core functionality copied from previous implementation ---
    suspend fun uploadReceiptImage(bitmap: android.graphics.Bitmap): String? {
        return repository.uploadReceiptImage(bitmap)
    }

    fun addTransaction(originalAmount: Double, category: String, description: String, currency: String = "USD", isIncome: Boolean = false, date: Long = System.currentTimeMillis(), receiptUrl: String = "", balanceDate: Long = 0L, items: List<com.example.database.TransactionItem> = emptyList()) {
        viewModelScope.launch {
            val user = _currentUser.value
            val tx = Transaction(amount = originalAmount, category = category, description = description, date = date, registeredBy = user, currency = currency, originalAmount = originalAmount, isIncome = isIncome, receiptUrl = receiptUrl, balanceDate = balanceDate, items = items)
            val alertMessage = repository.insertTransaction(tx, user)
            if (alertMessage != null) notifyUser(alertMessage)
            else notifyUser("¡Guardado!: $originalAmount en '$description'")
        }
    }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { repository.deleteTransaction(transaction, _currentUser.value) } }
    fun editTransaction(transaction: Transaction) { viewModelScope.launch { repository.insertTransaction(transaction, _currentUser.value) } }

    fun createOrEditCategory(oldName: String?, name: String, limit: Double, iconEmoji: String, monthKey: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val newName = name.trim()
            var oldCat = categories.value.find { it.categoryName == oldName }
            if (oldName != null && oldName != newName) {
                if (oldCat != null) repository.deleteCategory(oldCat, user)
                val txsToUpdate = transactions.value.filter { it.category == oldName }
                for (tx in txsToUpdate) {
                    repository.insertTransaction(tx.copy(category = newName), user)
                }
            } else if (oldCat == null) {
                oldCat = categories.value.find { it.categoryName == newName }
            }
            
            val newLimitsMap = oldCat?.limitsPerMonth?.toMutableMap() ?: mutableMapOf()
            newLimitsMap[monthKey] = limit
            
            repository.insertCategory(CategoryDetail(newName, oldCat?.limitAmount ?: limit, colorHex = oldCat?.colorHex ?: "", iconEmoji = iconEmoji, limitsPerMonth = newLimitsMap), user)
        }
    }
    fun deleteCategory(category: CategoryDetail) { viewModelScope.launch { repository.deleteCategory(category, _currentUser.value) } }

    fun createProfile(name: String, colorHex: String) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isNotEmpty()) {
                repository.insertProfile(UserProfile(cleanName, colorHex), _currentUser.value)
            }
        }
    }

    fun editProfile(oldProfile: UserProfile, newName: String, newColorHex: String) {
        viewModelScope.launch {
            val cleanName = newName.trim()
            if (cleanName.isNotEmpty()) {
                if (oldProfile.name != cleanName) repository.deleteProfile(oldProfile, _currentUser.value)
                repository.insertProfile(UserProfile(cleanName, newColorHex), _currentUser.value)
                if (_currentUser.value == oldProfile.name) {
                    _currentUser.value = cleanName
                    dataStoreManager.saveAuthData(_familyId.value ?: "", cleanName, dataStoreManager.userPinFlow.firstOrNull() ?: "")
                }
            }
        }
    }
    fun deleteProfile(profile: UserProfile) { viewModelScope.launch { repository.deleteProfile(profile, _currentUser.value) } }

    fun syncLiveBcvRate() { viewModelScope.launch { repository.updateBcvRateFromLiveApi(_currentUser.value) } }
    fun updateValueSetting(key: String, value: String) { viewModelScope.launch { repository.insertSetting(key, value, _currentUser.value) } }

    fun registerFromNaturalLanguage(inputText: String) {
        if (inputText.isBlank()) return
        viewModelScope.launch {
            _isRegisteringNlp.value = true
            _nlpError.value = null
            try {
                val user = _currentUser.value
                val customKey = settings.value.find { it.key == "gemini_api_key" }?.value
                val categoryNameList = categories.value.map { it.categoryName }.ifEmpty { listOf("Alimentos", "Servicios", "Otros") }
                val (transaction, error, successMsg) = GeminiParser.parseTextToTransaction(inputText, user, categoryNameList, customKey)
                if (error != null) {
                    _nlpError.value = error
                } else if (transaction != null) {
                    repository.insertTransaction(transaction, user)
                    notifyUser(successMsg.ifBlank { "IA registró gasto/ingreso exitosamente." })
                }
            } catch (e: Exception) {
                _nlpError.value = "Error: ${e.message}"
            } finally {
                _isRegisteringNlp.value = false
            }
        }
    }

    fun addChore(title: String, assignedTo: String, description: String, priority: String, category: String = "General", isPrivate: Boolean = false) {
        viewModelScope.launch {
            val chore = Chore(title = title, assignedTo = assignedTo, createdBy = _currentUser.value, description = description, priority = priority, category = category)
            chore.isPrivate = isPrivate
            repository.insertChore(chore, _currentUser.value)
        }
    }
    fun toggleChoreCompletion(chore: Chore) { if (!chore.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.updateChoreStatus(chore.id, chore.title, !chore.isCompleted, _currentUser.value) } }
    fun deleteChore(chore: Chore) { viewModelScope.launch { repository.deleteChore(chore, _currentUser.value) } }
    fun editChore(chore: Chore) { viewModelScope.launch { repository.insertChore(chore, _currentUser.value) } }

    fun addShoppingItem(name: String, quantity: String, isPrivate: Boolean = false, listName: String = "General") {
        viewModelScope.launch {
            val item = ShoppingItem(name = name, quantity = quantity, addedBy = _currentUser.value, dateAdded = System.currentTimeMillis(), listName = listName)
            item.isPrivate = isPrivate
            repository.insertShoppingItem(item, _currentUser.value)
        }
    }
    fun toggleShoppingItemBought(item: ShoppingItem) { if (!item.isBought) triggerConfetti()
        viewModelScope.launch { repository.updateShoppingItemStatus(item.id, item.name, !item.isBought, _currentUser.value) } }
    fun deleteShoppingItem(item: ShoppingItem) { viewModelScope.launch { repository.deleteShoppingItem(item, _currentUser.value) } }
    fun editShoppingItem(item: ShoppingItem) { viewModelScope.launch { repository.insertShoppingItem(item, _currentUser.value) } }

    fun renameShoppingList(oldName: String, newName: String) {
        viewModelScope.launch {
            val itemsToRename = shoppingItems.value.filter { it.listName == oldName }
            itemsToRename.forEach { item ->
                repository.insertShoppingItem(item.copy(listName = newName), _currentUser.value)
            }
            val currentSetting = settings.value.find { it.key == "shopping_lists" }?.value
            if (currentSetting != null) {
                val lists = currentSetting.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val newLists = lists.map { if (it == oldName) newName else it }
                updateValueSetting("shopping_lists", newLists.joinToString(","))
            }
        }
    }

    fun deleteShoppingList(listName: String) {
        viewModelScope.launch {
            val itemsToDelete = shoppingItems.value.filter { it.listName == listName }
            itemsToDelete.forEach { item ->
                repository.deleteShoppingItem(item, _currentUser.value)
            }
            val currentSetting = settings.value.find { it.key == "shopping_lists" }?.value
            if (currentSetting != null) {
                val lists = currentSetting.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != listName }
                updateValueSetting("shopping_lists", lists.joinToString(","))
            }
        }
    }

    fun addGoal(title: String, targetAmount: Double?, isPrivate: Boolean = false) {
        viewModelScope.launch {
            val user = _currentUser.value
            val goal = Goal(
                title = title,
                targetAmount = targetAmount,
                createdBy = user,
                createdDate = System.currentTimeMillis(),
                isPrivate = isPrivate
            )
            repository.insertGoal(goal, user)
        }
    }
    fun editGoal(goal: Goal) { if (goal.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.insertGoal(goal, _currentUser.value) } }
    fun deleteGoal(goal: Goal) { viewModelScope.launch { repository.deleteGoal(goal, _currentUser.value) } }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(title = title, content = content, createdBy = _currentUser.value, createdDate = System.currentTimeMillis())
            repository.insertNote(note, _currentUser.value)
        }
    }
    fun editNote(note: Note) { viewModelScope.launch { repository.insertNote(note, _currentUser.value) } }
    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                val uid = user?.uid
                if (uid != null) {
                    try {
                        com.google.firebase.database.FirebaseDatabase.getInstance().reference
                            .child("users").child(uid).removeValue().await()
                    } catch (e: Exception) {
                        // Continuar si ya fue removido
                    }
                }
                user?.delete()?.await()
                dataStoreManager.clearAuthData()
                _isAuthenticated.value = false
                _familyId.value = null
                _currentUser.value = ""
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al eliminar la cuenta. Se requiere reautenticación.")
            }
        }
    }
}

class FamilyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            val repository = FamilyRepository()
            val dataStore = DataStoreManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(repository, dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
