package com.passwordvault.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passwordvault.app.data.local.entity.AccountEntity
import com.passwordvault.app.data.repository.AccountRepository
import com.passwordvault.app.domain.totp.TotpGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AccountDetailState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val otpSecret: String = "",
    val isHotp: Boolean = false,
    val hotpCounter: Long = 0,
    val otpCode: String = "",
    val totpRemaining: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailState())
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    private var currentId: Long? = null
    private var timerJob: Job? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun loadAccount(accountId: Long, prefilledSeed: String = "") {
        if (prefilledSeed.isNotBlank()) {
            val data = TotpGenerator.parseTotpUri(prefilledSeed)
            if (data != null) {
                _state.update {
                    it.copy(otpSecret = data.secret, isHotp = data.isHotp, hotpCounter = data.counter,
                        title = if (it.title.isBlank()) data.issuer else it.title)
                }
            } else {
                _state.update { it.copy(otpSecret = prefilledSeed) }
            }
        }
        if (accountId == -1L) {
            startOtpTimer()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val account = accountRepository.getAccountById(accountId)
            if (account != null) {
                currentId = account.id
                val hasHotp = account.hotpSecret.isNotBlank()
                val secret = if (hasHotp) account.hotpSecret else account.totpSecret
                val code = if (hasHotp) TotpGenerator.generateHotpCode(account.hotpSecret, account.hotpCounter) else ""
                _state.update {
                    it.copy(
                        title = account.title,
                        username = account.username,
                        password = account.password,
                        url = account.url,
                        notes = account.notes,
                        otpSecret = secret,
                        isHotp = hasHotp,
                        hotpCounter = account.hotpCounter,
                        otpCode = code,
                        createdAt = account.createdAt,
                        updatedAt = account.updatedAt,
                        isLoading = false
                    )
                }
            }
            startOtpTimer()
        }
    }

    private fun startOtpTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value
                if (s.otpSecret.isNotBlank()) {
                    if (s.isHotp) {
                        val code = TotpGenerator.generateHotpCode(s.otpSecret, s.hotpCounter)
                        _state.update { it.copy(otpCode = code) }
                    } else {
                        val code = TotpGenerator.generateCode(s.otpSecret)
                        val remaining = TotpGenerator.getRemainingSeconds()
                        _state.update { it.copy(otpCode = code, totpRemaining = remaining) }
                    }
                }
            }
        }
    }

    fun onTitleChanged(v: String) = _state.update { it.copy(title = v) }
    fun onUsernameChanged(v: String) = _state.update { it.copy(username = v) }
    fun onPasswordChanged(v: String) = _state.update { it.copy(password = v) }
    fun onUrlChanged(v: String) = _state.update { it.copy(url = v) }
    fun onNotesChanged(v: String) = _state.update { it.copy(notes = v) }

    fun onOtpSecretChanged(v: String) {
        val data = TotpGenerator.parseTotpUri(v)
        if (data != null) {
            _state.update {
                it.copy(otpSecret = data.secret, isHotp = data.isHotp, hotpCounter = data.counter,
                    title = if (it.title.isBlank()) data.issuer else it.title)
            }
        } else {
            _state.update { it.copy(otpSecret = v.uppercase()) }
        }
    }

    fun generateNextHotp() {
        val s = _state.value
        if (s.otpSecret.isBlank() || !s.isHotp) return
        val code = TotpGenerator.generateHotpCode(s.otpSecret, s.hotpCounter)
        _state.update { it.copy(hotpCounter = s.hotpCounter + 1, otpCode = code) }
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            val entity = AccountEntity(
                id = currentId ?: 0,
                title = s.title,
                username = s.username,
                password = s.password,
                url = s.url,
                notes = s.notes,
                totpSecret = if (!s.isHotp) s.otpSecret else "",
                hotpSecret = if (s.isHotp) s.otpSecret else "",
                hotpCounter = s.hotpCounter,
                createdAt = if (currentId != null) s.createdAt else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (currentId != null) accountRepository.update(entity)
            else accountRepository.insert(entity)
        }
    }

    fun delete() {
        currentId?.let { viewModelScope.launch { accountRepository.deleteById(it) } }
    }

    fun formatTime(millis: Long): String {
        if (millis == 0L) return ""
        return dateFormatter.format(Instant.ofEpochMilli(millis))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
