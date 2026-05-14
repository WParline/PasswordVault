package com.passwordvault.app.ui.screens.unlock

import android.app.Application
import android.app.KeyguardManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passwordvault.app.crypto.MasterPasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnlockState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isFirstTime: Boolean = false,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val error: String? = null,
    val biometricAvailable: Boolean = false
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    application: Application,
    private val masterPasswordManager: MasterPasswordManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    init {
        val isFirstTime = !masterPasswordManager.isMasterPasswordSet()
        val km = application.getSystemService(Application.KEYGUARD_SERVICE) as KeyguardManager
        val bioAvailable = km.isDeviceSecure

        _state.update {
            it.copy(
                isFirstTime = isFirstTime,
                biometricAvailable = bioAvailable && !isFirstTime
            )
        }
    }

    fun onBiometricSuccess() {
        _state.update { it.copy(isUnlocked = true) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _state.update { it.copy(confirmPassword = password, error = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun unlock() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val valid = masterPasswordManager.verifyMasterPassword(_state.value.password)
            if (valid) {
                _state.update { it.copy(isUnlocked = true) }
            } else {
                _state.update { it.copy(error = "密码错误", isLoading = false) }
            }
        }
    }

    fun setupMasterPassword() {
        val s = _state.value
        if (s.password.length < 4) {
            _state.update { it.copy(error = "密码至少4位") }
            return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(error = "两次密码不一致") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            masterPasswordManager.setMasterPassword(s.password)
            _state.update { it.copy(isUnlocked = true) }
        }
    }

    fun onBiometricError(error: String) {
        _state.update { it.copy(error = error) }
    }
}
