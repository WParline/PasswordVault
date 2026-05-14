package com.passwordvault.app.ui.screens.vault

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultAccount(
    val entity: AccountEntity,
    val totpCode: String = "",
    val totpRemaining: Int = 0,
    val hotpCode: String = "",
    val hotpCounter: Long = 0
)

data class VaultState(
    val accounts: List<VaultAccount> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VaultState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    private var timerJob: Job? = null

    init {
        observeAccounts()
        startTotpTimer()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            _query.flatMapLatest { query ->
                if (query.isBlank()) accountRepository.getAllAccounts()
                else accountRepository.searchAccounts(query)
            }.collect { entities ->
                _state.update {
                    it.copy(
                        accounts = entities.map { e ->
                            val hasTotp = e.totpSecret.isNotBlank()
                            val hasHotp = !hasTotp && e.hotpSecret.isNotBlank()
                            VaultAccount(
                                entity = e,
                                totpCode = if (hasTotp) TotpGenerator.generateCode(e.totpSecret) else "",
                                totpRemaining = if (hasTotp) TotpGenerator.getRemainingSeconds() else 0,
                                hotpCode = if (hasHotp) TotpGenerator.generateHotpCode(e.hotpSecret, e.hotpCounter) else "",
                                hotpCounter = e.hotpCounter
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun startTotpTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { state ->
                    state.copy(accounts = state.accounts.map { a ->
                        if (a.entity.totpSecret.isNotBlank()) {
                            val remaining = TotpGenerator.getRemainingSeconds()
                            if (remaining == 0 || remaining == 29) {
                                a.copy(
                                    totpCode = TotpGenerator.generateCode(a.entity.totpSecret),
                                    totpRemaining = remaining
                                )
                            } else {
                                a.copy(totpRemaining = remaining)
                            }
                        } else a
                    })
                }
            }
        }
    }

    fun search(query: String) {
        _query.value = query
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
