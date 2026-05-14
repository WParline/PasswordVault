package com.passwordvault.app.data.model

data class TotpCode(
    val id: Long,
    val accountTitle: String,
    val issuer: String,
    val code: String,
    val remainingSeconds: Int,
    val period: Int = 30
) {
    val progress: Float get() = remainingSeconds.toFloat() / period.toFloat()
}
