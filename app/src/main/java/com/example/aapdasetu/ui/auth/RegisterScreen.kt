package com.example.aapdasetu.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aapdasetu.ui.auth.components.*

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showServerIpDialog by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf(uiState.serverIp) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFEEF0FD), Color(0xFFE4E8FA), Color(0xFFF5F3FF))
                )
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Nav Row: Back Button & Server Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to login",
                            tint = TextMain
                        )
                    }

                    IconButton(
                        onClick = { showServerIpDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Backend Server",
                            tint = Color(0xFFCBD4E6)
                        )
                    }
                }

                // Header Row with Title and 3D Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Create account ",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            Text(text = "✨", fontSize = 22.sp)
                        }
                        Text(
                            text = "Join us and start your journey",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }

                    AuthHeaderBadge(isLock = false)
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Full Name Input
                AuthTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = "Full Name",
                    placeholder = "Enter your full name",
                    leadingIcon = Icons.Default.Person,
                    keyboardType = KeyboardType.Text
                )

                // Email Input
                AuthTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    placeholder = "you@example.com",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                // Password Input
                AuthTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    placeholder = "Create a password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                // Confirm Password Input
                AuthTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm Password",
                    placeholder = "Confirm your password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                // Live Password Criteria List
                PasswordCriteriaList(
                    isMinLength = uiState.isMinLengthValid,
                    hasNumber = uiState.hasNumber,
                    hasUppercase = uiState.hasUppercase
                )

                // Primary Register Button
                AuthPrimaryButton(
                    text = "Register",
                    onClick = { viewModel.register() },
                    isLoading = uiState.isLoading
                )

                // Social Logins
                SocialAuthSection(
                    onSocialClick = { provider ->
                        Toast.makeText(context, "Sign up with $provider selected", Toast.LENGTH_SHORT).show()
                    }
                )

                // Bottom Account Navigation Card
                BottomSwitchCard(
                    prompt = "Already have an account?",
                    actionText = "Login",
                    onAction = onNavigateToLogin
                )
            }
        }
    }

    if (showServerIpDialog) {
        AlertDialog(
            onDismissRequest = { showServerIpDialog = false },
            title = { Text("Backend Server URL", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your Mac's Wi-Fi IP is: http://192.168.0.103:3000/\n(Make sure phone is on the same Wi-Fi network)",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setServerIp(tempIp)
                        showServerIpDialog = false
                        Toast.makeText(context, "Server updated to $tempIp", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
