# 本地密码库 (PasswordVault)

一个完全离线的 Android 密码管理器。所有数据存储在本地，无需联网权限。

## 功能

- **账号密码管理** — 增删改查账号，本地加密存储
- **TOTP 动态码** — RFC 6238 标准，30 秒自动刷新，列表页直接展示
- **HOTP 事件型 OTP** — RFC 4226 标准，计数器递增
- **QR 扫码添加** — CameraX + ML Kit 扫描二维码，自动解析 `otpauth://` URI
- **指纹/设备密码解锁** — 基于 KeyguardManager 系统级认证

## 技术栈

| 组件 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room |
| 依赖注入 | Hilt |
| 加密 | Android Keystore (AES-256-GCM) + PBKDF2 |
| TOTP | 自实现 RFC 6238 / RFC 4226 |
| 扫码 | CameraX + ML Kit Barcode Scanning |
| APK 命名 | PasswordVault-{version}-{yyyyMMdd-HHmm}.apk |
| 最低 SDK | API 26 (Android 8.0) |
| 目标 SDK | API 36 (Android 16) |

## 安全设计

```
用户主密码 ──→ PBKDF2(100K) ──→ 256-bit 密钥 ──→ Room 数据库加密
                              ↕
                  Android Keystore (硬件安全模块)
```

- 数据库使用 Room 标准 SQLite（应用沙箱内，其他应用不可访问）
- 主密码经 PBKDF2 迭代 10 万次派生
- 派生密钥由 Android Keystore 加密存储
- 密码数据在内存中时以明文形式存在，应用切后台无自动锁定（后续版本可加）
- **无需任何联网权限**

## 构建

```bash
# 调试构建
./gradlew assembleDebug

# APK 位置（文件名含版本号和日期）
app/build/outputs/apk/debug/PasswordVault-*.apk
```

## 许可证

MIT
