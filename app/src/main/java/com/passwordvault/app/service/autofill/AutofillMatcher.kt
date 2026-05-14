package com.passwordvault.app.service.autofill

import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue

data class LoginField(
    val autofillId: AutofillId,
    val type: FieldType,
    val value: String? = null
) {
    enum class FieldType { USERNAME, PASSWORD }
}

class AutofillMatcher {

    private val usernameHints = listOf(
        "username", "user", "email", "login", "account",
        "手机", "账号", "邮箱", "用户名"
    )

    private val passwordHints = listOf(
        "password", "pass", "pwd", "密码"
    )

    fun findLoginFields(focusedIds: Array<out AutofillId>, values: Map<AutofillId, AutofillValue>): AutofillMatch? {
        val usernameFields = mutableListOf<LoginField>()
        val passwordFields = mutableListOf<LoginField>()

        for (id in focusedIds) {
            val value = values[id]
            val text = value?.textValue?.toString()?.lowercase() ?: ""
            val hint = id.toString().lowercase()

            when {
                isPasswordField(hint, text) -> {
                    passwordFields.add(LoginField(id, LoginField.FieldType.PASSWORD))
                }
                isUsernameField(hint, text) -> {
                    usernameFields.add(LoginField(id, LoginField.FieldType.USERNAME, value?.textValue?.toString()))
                }
            }
        }

        if (usernameFields.isNotEmpty() && passwordFields.isNotEmpty()) {
            return AutofillMatch(usernameFields, passwordFields)
        }
        return null
    }

    private fun isUsernameField(hint: String, text: String): Boolean {
        return usernameHints.any { hint.contains(it) || text.contains(it) }
    }

    private fun isPasswordField(hint: String, text: String): Boolean {
        return passwordHints.any { hint.contains(it) || text.contains(it) }
    }

    data class AutofillMatch(
        val usernameFields: List<LoginField>,
        val passwordFields: List<LoginField>
    )
}
