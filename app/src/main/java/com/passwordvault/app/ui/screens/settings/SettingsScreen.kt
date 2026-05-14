package com.passwordvault.app.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(false) }
    var autofillEnabled by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cameraCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
        cameraGranted = cameraCheck == PackageManager.PERMISSION_GRANTED

        val autofillPkg = try {
            Settings.Secure.getString(context.contentResolver, "autofill_service") ?: ""
        } catch (_: Exception) { "" }
        autofillEnabled = autofillPkg.contains(context.packageName)

        val accessibilityPkg = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        } catch (_: Exception) { "" }
        accessibilityEnabled = accessibilityPkg.contains(context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Default.CameraAlt,
                title = "相机权限",
                description = "用于扫描 TOTP 二维码",
                enabled = cameraGranted,
                enabledText = "已授权",
                disabledText = "未授权"
            ) {
                Button(onClick = {
                    Toast.makeText(context, "请在系统设置中开启相机权限", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Default.TextFields,
                title = "自动填充服务",
                description = "在其他应用中自动填充账号密码",
                enabled = autofillEnabled,
                enabledText = "已启用",
                disabledText = "未启用"
            ) {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                        putExtra("service", "${context.packageName}/.service.autofill.VaultAutofillService")
                    }
                    try { context.startActivity(intent) }
                    catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }) { Text("启用自动填充") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                icon = Icons.Default.Fingerprint,
                title = "密码检测（无障碍）",
                description = "检测其他应用的密码输入并提示保存",
                enabled = accessibilityEnabled,
                enabledText = "已开启",
                disabledText = "未开启"
            ) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("开启无障碍") }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("关于", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本地密码库 v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "完全离线 · 无任何联网权限",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    enabledText: String,
    disabledText: String,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (enabled) enabledText else disabledText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
            if (!enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                action()
            }
        }
    }
}
