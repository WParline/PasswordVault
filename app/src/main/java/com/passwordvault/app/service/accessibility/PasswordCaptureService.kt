package com.passwordvault.app.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.widget.Toast

class PasswordCaptureService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                detectLoginPage(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                detectPasswordField(event)
            }
        }
    }

    private fun detectLoginPage(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        val usernameFields = mutableListOf<AccessibilityNodeInfo>()
        val passwordFields = mutableListOf<AccessibilityNodeInfo>()

        findLoginFields(root, usernameFields, passwordFields)

        if (usernameFields.isNotEmpty() && passwordFields.isNotEmpty()) {
            val packageName = event.packageName?.toString() ?: ""
            Toast.makeText(this, "检测到登录页面: $packageName", Toast.LENGTH_SHORT).show()
            showAutofillNotification(packageName)
        }

        root.recycle()
    }

    private fun detectPasswordField(event: AccessibilityEvent) {
        val source = event.source ?: return
        val isPassword = isPasswordInput(source)
        source.recycle()

        if (isPassword) {
            Toast.makeText(this, "检测到密码输入框", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findLoginFields(
        node: AccessibilityNodeInfo,
        usernameFields: MutableList<AccessibilityNodeInfo>,
        passwordFields: MutableList<AccessibilityNodeInfo>
    ) {
        if (isUsernameInput(node)) {
            usernameFields.add(node)
        }
        if (isPasswordInput(node)) {
            passwordFields.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findLoginFields(child, usernameFields, passwordFields)
                child.recycle()
            }
        }
    }

    private fun isUsernameInput(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.toString() != "android.widget.EditText") return false
        val hint = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        return hint.contains("username") || hint.contains("account") ||
                hint.contains("邮箱") || hint.contains("账号") ||
                hint.contains("手机") || hint.contains("email") ||
                hint.contains("user") ||
                contentDesc.contains("username") || contentDesc.contains("账号")
    }

    private fun isPasswordInput(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.toString() != "android.widget.EditText") return false
        return node.isPassword || 
                (node.text?.toString()?.lowercase()?.contains("password") == true) ||
                (node.contentDescription?.toString()?.lowercase()?.contains("password") == true) ||
                (node.text?.toString()?.lowercase()?.contains("密码") == true)
    }

    private fun showAutofillNotification(packageName: String) {
        Toast.makeText(this, "密码库: 检测到 $packageName 的登录页面", Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() {}
}
