package com.example.aapdasetu.ui.auth

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun AuthNavContainer(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoggedIn) {
        onAuthenticated()
    } else {
        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
                if (targetState == AuthScreenState.REGISTER) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "AuthNavAnimation"
        ) { screenState ->
            when (screenState) {
                AuthScreenState.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = { viewModel.switchScreen(AuthScreenState.REGISTER) },
                        modifier = modifier
                    )
                }
                AuthScreenState.REGISTER -> {
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { viewModel.switchScreen(AuthScreenState.LOGIN) },
                        modifier = modifier
                    )
                }
            }
        }
    }
}
