package com.passwordvault.app.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterPasswordManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyStoreManager: KeyStoreManager
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isMasterPasswordSet(): Boolean = prefs.contains(KEY_HASH)
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)
    fun setBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()

    fun setMasterPassword(password: String) {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        val encryptedHash = keyStoreManager.encrypt(KEYSTORE_ALIAS, hash)
        val encryptedSalt = keyStoreManager.encrypt(KEYSTORE_ALIAS, salt)

        prefs.edit()
            .putString(KEY_HASH, encryptedHash.joinToString(",") { it.toInt().toString() })
            .putString(KEY_SALT, encryptedSalt.joinToString(",") { it.toInt().toString() })
            .apply()
    }

    fun verifyMasterPassword(password: String): Boolean {
        if (!isMasterPasswordSet()) return false

        val encryptedHash = prefs.getString(KEY_HASH, null)!!
            .split(",").map { it.toInt().toByte() }.toByteArray()
        val encryptedSalt = prefs.getString(KEY_SALT, null)!!
            .split(",").map { it.toInt().toByte() }.toByteArray()

        val hash = keyStoreManager.decrypt(KEYSTORE_ALIAS, encryptedHash)
        val salt = keyStoreManager.decrypt(KEYSTORE_ALIAS, encryptedSalt)

        val inputHash = hashPassword(password, salt)
        return inputHash.contentEquals(hash)
    }

    fun getDatabasePassphrase(): ByteArray {
        val encryptedHash = prefs.getString(KEY_HASH, null)
            ?: return ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encryptedSalt = prefs.getString(KEY_SALT, null)
            ?: return ByteArray(32).also { SecureRandom().nextBytes(it) }

        val hash = keyStoreManager.decrypt(KEYSTORE_ALIAS,
            encryptedHash.split(",").map { it.toInt().toByte() }.toByteArray())
        val salt = keyStoreManager.decrypt(KEYSTORE_ALIAS,
            encryptedSalt.split(",").map { it.toInt().toByte() }.toByteArray())
        return hash + salt.take(16)
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val KEY_HASH = "master_password_hash"
        private const val KEY_SALT = "master_password_salt"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEYSTORE_ALIAS = "passwordvault_master_key"
    }
}
