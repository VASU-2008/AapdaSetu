package com.example.aapdasetu.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
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

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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
                // Top Header Row with Title and 3D Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Welcome back ",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            Text(text = "👋", fontSize = 22.sp)
                        }
                        Text(
                            text = "Login to continue your journey",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }

                    AuthHeaderBadge(isLock = true)
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                    placeholder = "Enter your password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                // Remember Me & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = uiState.rememberMe,
                            onCheckedChange = viewModel::onRememberMeChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryPurple,
                                uncheckedColor = Color(0xFFCBD4E6)
                            )
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                    }

                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Password reset link sent", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Forgot password?",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }

                // Primary Login Button
                AuthPrimaryButton(
                    text = "Login",
                    onClick = { viewModel.login() },
                    isLoading = uiState.isLoading
                )

                // Social Logins
                SocialAuthSection(
                    onSocialClick = { provider ->
                        Toast.makeText(context, "Sign in with $provider selected", Toast.LENGTH_SHORT).show()
                    }
                )

                // Bottom Account Navigation Card
                BottomSwitchCard(
                    prompt = "Don't have an account?",
                    actionText = "Create an account",
                    onAction = onNavigateToRegister
                )

                // Server IP config link for dev/testing
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showServerIpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Backend Server",
                            tint = Color(0xFFCBD4E6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
                        "For Android Emulator: http://10.0.2.2:3000/\nFor Physical Device: http://<YOUR_PC_IP>:3000/",
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
