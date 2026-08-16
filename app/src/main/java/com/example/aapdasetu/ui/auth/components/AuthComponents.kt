package com.example.aapdasetu.ui.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Tokens matching design
val PrimaryPurple = Color(0xFF5C4DF0)
val PrimaryPurpleDark = Color(0xFF4A3DE0)
val PrimaryPurpleLight = Color(0xFFECE9FE)
val CardBgColor = Color(0xFFFFFFFF)
val TextMain = Color(0xFF1E2029)
val TextMuted = Color(0xFF828799)
val BorderLight = Color(0xFFE3E7F2)
val InputBg = Color(0xFFFAFBFE)
val SuccessGreen = Color(0xFF22C55E)

@Composable
fun AuthHeaderBadge(
    isLock: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Top-Right Sphere
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(20.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFD5D0FF), Color(0xFF867AFF), Color(0xFF5C4DF0))
                    )
                )
        )

        // Glossy 3D Tilted Card Base
        Box(
            modifier = Modifier
                .size(76.dp)
                .rotate(-3f)
                .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x335C4DF0))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F3FF))
                    )
                )
                .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8074FF), PrimaryPurple, Color(0xFF4334DA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLock) Icons.Filled.Lock else Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = Color(0xFFA4ACBF)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (value.isNotEmpty()) PrimaryPurple else Color(0xFF9AA1B5),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF9AA1B5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = InputBg,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = BorderLight,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain
            )
        )
    }
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x665C4DF0)),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPurple,
            disabledContainerColor = PrimaryPurple.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6757FF), PrimaryPurpleDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SocialAuthSection(
    onSocialClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E9F2))
            Text(
                text = "or continue with",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B92A5)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E9F2))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocialButton(
                label = "Google",
                icon = Icons.Outlined.GTranslate,
                iconTint = Color(0xFF4285F4),
                modifier = Modifier.weight(1f),
                onClick = { onSocialClick("Google") }
            )
            SocialButton(
                label = "Apple",
                icon = Icons.Filled.Star,
                iconTint = Color.Black,
                modifier = Modifier.weight(1f),
                onClick = { onSocialClick("Apple") }
            )
            SocialButton(
                label = "Phone",
                icon = Icons.Filled.Phone,
                iconTint = SuccessGreen,
                modifier = Modifier.weight(1f),
                onClick = { onSocialClick("Phone") }
            )
        }
    }
}

@Composable
fun SocialButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BorderLight, BorderLight))),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4A5168)
            )
        }
    }
}

@Composable
fun PasswordCriteriaList(
    isMinLength: Boolean,
    hasNumber: Boolean,
    hasUppercase: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CriteriaItem(text = "At least 8 characters", isValid = isMinLength)
        CriteriaItem(text = "Include a number", isValid = hasNumber)
        CriteriaItem(text = "Include an uppercase letter", isValid = hasUppercase)
    }
}

@Composable
private fun CriteriaItem(text: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isValid) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFD2D7E5), CircleShape)
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isValid) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isValid) TextMain else TextMuted
        )
    }
}

@Composable
fun BottomSwitchCard(
    prompt: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAction() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F5FF)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFE5E9FA), Color(0xFFE5E9FA)))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = prompt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryPurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
