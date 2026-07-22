package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.FamilyViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

@Composable
fun AuthScreen(viewModel: FamilyViewModel) {
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val context = LocalContext.current as FragmentActivity
    
    // Si ya tiene una familia asociada localmente, va a la pantalla de LOGIN (desbloqueo).
    // Si no tiene familia, va a la pantalla de SELECCIÓN (crear o unirse).
    var currentMode by remember(familyId) { mutableStateOf(if (familyId != null) "login" else "select") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nido_logo),
                contentDescription = "Nido Logo",
                modifier = Modifier
                    .size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (currentMode) {
                "select" -> SelectModeContent(
                    onCreateFamily = { currentMode = "create_family" },
                    onJoinFamily = { currentMode = "join_family" },
                    onLoginExisting = { currentMode = "login_existing" }
                )
                "login" -> LoginContent(
                    viewModel = viewModel,
                    onReset = { 
                        // Permite borrar los datos para cambiar de familia
                        currentMode = "select" 
                    }
                )
                "create_family" -> CreateFamilyContent(
                    viewModel = viewModel,
                    onBack = { currentMode = "select" }
                )
                "join_family" -> JoinFamilyContent(
                    viewModel = viewModel,
                    onBack = { currentMode = "select" }
                )
                "login_existing" -> LoginExistingFamilyContent(
                    viewModel = viewModel,
                    onBack = { currentMode = "select" }
                )
            }
        }
    }
}

@Composable
fun SelectModeContent(onCreateFamily: () -> Unit, onJoinFamily: () -> Unit, onLoginExisting: () -> Unit) {
    Text(
        text = "Bienvenido a Nido",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
    Text(
        text = "Organiza las finanzas y tareas de tu hogar",
        fontSize = 14.sp,
        color = Color(0xFF64748B),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    Button(
        onClick = onCreateFamily,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Crear Nuevo Nido", fontSize = 16.sp)
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedButton(
        onClick = onJoinFamily,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Unirse a un Nido", fontSize = 16.sp, color = Color(0xFF6366F1))
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    TextButton(onClick = onLoginExisting) {
        Text("¿Ya tienes un perfil? Iniciar Sesión", fontSize = 14.sp, color = Color(0xFF64748B))
    }
}

@Composable
fun LoginContent(viewModel: FamilyViewModel, onReset: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(Unit) {
        // Intentar biometría automáticamente
        showBiometricPrompt(context, onSuccess = {
            viewModel.loginBiometric()
        })
    }

    Text(
        text = "Desbloquear Nido",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 5) pin = it },
        label = { Text("PIN de 5 dígitos") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(
            onClick = {
                showBiometricPrompt(context, onSuccess = {
                    viewModel.loginBiometric()
                })
            },
            modifier = Modifier.size(56.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = "Huella", tint = Color(0xFF6366F1))
        }

        Button(
            onClick = { viewModel.loginWithPin(pin) },
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Entrar", fontSize = 16.sp)
        }
    }
}

@Composable
fun CreateFamilyContent(viewModel: FamilyViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Text(
        text = "Crear Nuevo Nido",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Correo Electrónico") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Contraseña (mínimo 6 caracteres)") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 5) pin = it },
        label = { Text("PIN Numérico (5 dígitos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = { if (email.isNotBlank() && password.length >= 6 && pin.length == 5) viewModel.createFamily(email, pin, password) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Crear y Entrar", fontSize = 16.sp)
    }
    TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
        Text("Volver")
    }
}

@Composable
fun JoinFamilyContent(viewModel: FamilyViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Text(
        text = "Unirse a Nido",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text("Código de Familia") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Tu Nombre") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Correo Electrónico") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Contraseña (mínimo 6 caracteres)") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 5) pin = it },
        label = { Text("Crea tu PIN (5 dígitos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = { if (code.isNotBlank() && name.isNotBlank() && email.isNotBlank() && password.length >= 6 && pin.length == 5) viewModel.joinFamily(code, name, pin, email, password) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Unirse", fontSize = 16.sp)
    }
    TextButton(
        onClick = {
            if (email.isBlank()) {
                android.widget.Toast.makeText(context, "Ingresa tu correo en el formulario primero", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.widget.Toast.makeText(context, "Enlace de recuperación enviado a $email", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            android.widget.Toast.makeText(context, "Error: ${task.exception?.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
            }
        },
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text("¿Olvidaste la contraseña de Firebase?", color = Color(0xFF6366F1))
    }
    TextButton(onClick = onBack, modifier = Modifier.padding(top = 0.dp)) {
        Text("Volver")
    }
}

private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val biometricManager = BiometricManager.from(activity)
    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear Nido")
        .setSubtitle("Usa tu huella para acceder")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}


@Composable
fun LoginExistingFamilyContent(viewModel: FamilyViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Text(
        text = "Iniciar Sesión",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
    Text(
        text = "Ingresa con tu correo y contraseña registrados",
        fontSize = 12.sp,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Correo Electrónico") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 5) pin = it },
        label = { Text("Define o ingresa tu PIN (5 dígitos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = { if (email.isNotBlank() && password.length >= 6 && pin.length == 5) viewModel.loginExisting(email, password, pin) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Entrar", fontSize = 16.sp)
    }
    TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
        Text("Volver")
    }
}
