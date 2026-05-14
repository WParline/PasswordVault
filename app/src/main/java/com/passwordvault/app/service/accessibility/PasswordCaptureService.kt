package com.passwordvault.app.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.passwordvault.app.MainActivity
import com.passwordvault.app.data.repository.AccountRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PasswordCaptureService : AccessibilityService() {

    @Inject lateinit var accountRepository: AccountRepository

    private var detectedUsername = ""
    private var detectedPassword = ""
    private var detectedPackage = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 300
        }
        serviceInfo = info
        showNotification("密码检测已开启", "密码库正在监听登录页面")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                detectedUsername = ""
                detectedPassword = ""
                detectedPackage = event.packageName?.toString() ?: ""
                detectLoginPage(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                captureInput(event)
            }
        }
    }

    private fun detectLoginPage(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        val usernameFields = mutableListOf<AccessibilityNodeInfo>()
        val passwordFields = mutableListOf<AccessibilityNodeInfo>()

        findLoginFields(root, usernameFields, passwordFields)

        if (usernameFields.isNotEmpty() && passwordFields.isNotEmpty()) {
            val pkg = event.packageName?.toString() ?: ""
            showNotification("检测到登录页面", "点此保存 $pkg 的密码")
        }

        usernameFields.forEach { it.recycle() }
        passwordFields.forEach { it.recycle() }
        root.recycle()
    }

    private fun captureInput(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = event.text?.joinToString("") ?: ""

        if (isUsernameField(source)) {
            detectedUsername = text
        }
        if (isPasswordField(source)) {
            detectedPassword = text
        }

        if (detectedUsername.isNotBlank() && detectedPassword.isNotBlank()) {
            showSaveNotification(detectedUsername, detectedPassword, detectedPackage)
            detectedUsername = ""
            detectedPassword = ""
        }

        source.recycle()
    }

    private fun findLoginFields(
        node: AccessibilityNodeInfo,
        usernameFields: MutableList<AccessibilityNodeInfo>,
        passwordFields: MutableList<AccessibilityNodeInfo>
    ) {
        if (isUsernameField(node)) usernameFields.add(node)
        if (isPasswordField(node)) passwordFields.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findLoginFields(child, usernameFields, passwordFields)
                child.recycle()
            }
        }
    }

    private fun isUsernameField(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.toString() != "android.widget.EditText") return false
        if (node.isPassword) return false
        val hint = (node.text?.toString() ?: "").lowercase() +
                (node.contentDescription?.toString() ?: "").lowercase()
        return hint.contains("username") || hint.contains("user") ||
                hint.contains("email") || hint.contains("account") ||
                hint.contains("login") || hint.contains("账号") ||
                hint.contains("邮箱") || hint.contains("手机") ||
                hint.contains("用户名")
    }

    private fun isPasswordField(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.toString() != "android.widget.EditText") return false
        return node.isPassword ||
                (node.contentDescription?.toString()?.lowercase()?.contains("password") == true) ||
                (node.contentDescription?.toString()?.lowercase()?.contains("密码") == true)
    }

    private fun showSaveNotification(username: String, password: String, packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("save_username", username)
            putExtra("save_password", password)
            putExtra("save_package", packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("保存到密码库")
            .setContentText("检测到 $username 的密码")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(200, notification)
    }

    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(300, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "密码检测",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当检测到其他应用的登录页面时发送通知"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {}

    companion object {
        private const val CHANNEL_ID = "password_capture"
    }
}
