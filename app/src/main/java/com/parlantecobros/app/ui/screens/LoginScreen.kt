package com.parlantecobros.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parlantecobros.app.R
import com.parlantecobros.app.data.AuthRepository
import com.parlantecobros.app.data.AuthResult
import com.parlantecobros.app.data.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val deviceId = remember { SessionManager.getDeviceId() }

    fun doLogin() {
        focusManager.clearFocus()
        if (isLoading) return
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val res = AuthRepository.login(email, password)) {
                is AuthResult.Success -> {
                    isLoading = false
                    onLoginSuccess()
                }
                is AuthResult.Error -> {
                    isLoading = false
                    errorMessage = res.message
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D12))
    ) {
        // 1. Imagen completa proporcionada por el usuario (incluye fondo, logo, títulos y detalles gráficos)
        Image(
            painter = painterResource(id = R.drawable.bg_login_architectural),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Contenido interactivo: Formulario alineado perfectamente bajo el texto integrado de la imagen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Espaciador exacto para que los campos de entrada comiencen justo debajo del texto integrado en la imagen
            Spacer(modifier = Modifier.height(315.dp))

            // Banner flotante de Error (en caso de credenciales incorrectas)
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { errorText ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF261216).copy(alpha = 0.94f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorText,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Tarjeta Campo: Correo Electrónico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F1522).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF232D3F).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = Color(0xFFA5AAB4),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "CORREO ELECTRÓNICO",
                            color = Color(0xFFA5AAB4),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        BasicTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            decorationBox = { innerTextField ->
                                if (email.isEmpty()) {
                                    Text(
                                        text = "comercio@ejemplo.com",
                                        color = Color(0xFF6B7280),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF1E2838))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta Campo: Contraseña
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F1522).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF232D3F).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFFA5AAB4),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "CONTRASEÑA",
                            color = Color(0xFFA5AAB4),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        BasicTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { doLogin() }
                            ),
                            decorationBox = { innerTextField ->
                                if (password.isEmpty()) {
                                    Text(
                                        text = "••••••••",
                                        color = Color(0xFF6B7280),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF1E2838))
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = Color(0xFFA5AAB4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Botón Principal Píldora: "Ingresar a Punk"
            Button(
                onClick = { doLogin() },
                enabled = !isLoading,
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEFE7DC),
                    disabledContainerColor = Color(0xFF8A857E)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF0B0E14),
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Ingresar a Punk",
                            color = Color(0xFF0B0E14),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0B0E14),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Tarjeta de Licencia y Protección Anti-Piratería por Dispositivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F1522).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Licencia Vinculada a este Dispositivo",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ID: ${deviceId.take(12)}... (Protección anti-piratería)",
                            color = Color(0xFF8E95A5),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pie de página de ayuda
            Text(
                text = "¿No tienes credenciales? Solicítalas a tu administrador",
                color = Color(0xFF7E8494),
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}
