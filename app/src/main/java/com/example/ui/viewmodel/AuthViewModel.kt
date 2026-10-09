package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserAccount
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val message: String) : AuthUiState
    data class Error(val error: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserAccount?> = authRepository.currentUser

    val allRegisteredUsers: StateFlow<List<UserAccount>> = authRepository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _recoveryQuestion = MutableStateFlow<String?>(null)
    val recoveryQuestion: StateFlow<String?> = _recoveryQuestion.asStateFlow()

    fun clearState() {
        _uiState.value = AuthUiState.Idle
        _recoveryQuestion.value = null
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, pass)
            result.onSuccess {
                _uiState.value = AuthUiState.Success("Bem-vindo de volta, ${it.name}!")
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.localizedMessage ?: "Erro ao realizar login.")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        phone: String,
        question: String,
        answer: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(
                name = name,
                email = email,
                password = pass,
                phone = phone,
                securityQuestion = question,
                securityAnswer = answer
            )
            result.onSuccess {
                _uiState.value = AuthUiState.Success("Conta criada com sucesso! Lista pessoal iniciada.")
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.localizedMessage ?: "Erro ao cadastrar conta.")
            }
        }
    }

    fun lookupSecurityQuestion(email: String) {
        viewModelScope.launch {
            val q = authRepository.findUserSecurityQuestion(email)
            if (q != null) {
                _recoveryQuestion.value = q
            } else {
                _uiState.value = AuthUiState.Error("E-mail não encontrado no sistema.")
            }
        }
    }

    fun resetPassword(email: String, answer: String, newPass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.resetPassword(email, answer, newPass)
            result.onSuccess {
                _uiState.value = AuthUiState.Success("Senha alterada com sucesso! Faça login com a nova senha.")
                _recoveryQuestion.value = null
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.localizedMessage ?: "Erro ao redefinir senha.")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState.Idle
    }

    fun switchAccount(user: UserAccount) {
        authRepository.switchUserDirectly(user)
        _uiState.value = AuthUiState.Success("Conectado como ${user.name} (${user.email})")
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
