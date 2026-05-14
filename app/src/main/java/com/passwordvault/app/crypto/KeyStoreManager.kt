package com.passwordvault.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateKey(keyAlias: String): SecretKey {
        if (keyStore.containsAlias(keyAlias)) {
            return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(keyAlias: String, plaintext: ByteArray): ByteArray {
        val key = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        return iv + encrypted
    }

    fun decrypt(keyAlias: String, ciphertext: ByteArray): ByteArray {
        val key = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ciphertext.copyOfRange(0, 12)
        val encrypted = ciphertext.copyOfRange(12, ciphertext.size)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }

    fun keyExists(keyAlias: String): Boolean = keyStore.containsAlias(keyAlias)

    fun deleteKey(keyAlias: String) {
        keyStore.deleteEntry(keyAlias)
    }
}
