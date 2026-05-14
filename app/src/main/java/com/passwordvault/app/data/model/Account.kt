package com.passwordvault.app.data.model

data class Account(
    val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayUrl: String get() = url.ifBlank {
        if (title.contains("@")) "mailto:$title" else ""
    }
}
