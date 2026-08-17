package com.example.aapdasetu.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapdasetu.data.model.UserModel
import com.example.aapdasetu.data.repository.AuthRepository
import com.example.aapdasetu.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreenState {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val currentScreen: AuthScreenState = AuthScreenState.LOGIN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val currentUser: UserModel? = null,
    val serverIp: String = "http://192.168.0.103:3000/"
) {
    val isMinLengthValid: Boolean get() = password.length >= 8
    val hasNumber: Boolean get() = password.any { it.isDigit() }
    val hasUppercase: Boolean get() = password.any { it.isUpperCase() }
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = repository.isLoggedIn(),
            currentUser = repository.getSavedUser(),
            serverIp = repository.getServerIp()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(pass: String) {
        _uiState.update { it.copy(password = pass, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmPassword = confirm, errorMessage = null) }
    }

    fun onRememberMeChange(checked: Boolean) {
        _uiState.update { it.copy(rememberMe = checked) }
    }

    fun switchScreen(screen: AuthScreenState) {
        _uiState.update { 
            it.copy(
                currentScreen = screen, 
                errorMessage = null, 
                successMessage = null
            ) 
        }
    }

    fun setServerIp(ip: String) {
        repository.setServerIp(ip)
        _uiState.update { it.copy(serverIp = ip) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.login(state.email, state.password, state.rememberMe)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = result.data.user,
                            successMessage = result.data.message ?: "Login Successful!"
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name") }
            return
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email") }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
            return
        }
        if (!state.hasNumber) {
            _uiState.update { it.copy(errorMessage = "Password must include a number") }
            return
        }
        if (!state.hasUppercase) {
            _uiState.update { it.copy(errorMessage = "Password must include an uppercase letter") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.register(state.fullName, state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = result.data.user,
                            successMessage = "Account created successfully!"
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    currentUser = null,
                    password = "",
                    confirmPassword = "",
                    currentScreen = AuthScreenState.LOGIN
                )
            }
        }
    }
}
