package com.passwordvault.app.domain.totp

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpGenerator {

    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generateCode(secretBase32: String, timeMillis: Long = System.currentTimeMillis(), period: Int = 30, digits: Int = 6): String {
        val counter = timeMillis / 1000 / period
        val secret = base32Decode(secretBase32)
        return generateTOTP(secret, counter, digits)
    }

    fun getRemainingSeconds(period: Int = 30): Int {
        return period - ((System.currentTimeMillis() / 1000) % period).toInt()
    }

    fun generateTotpUri(secretBase32: String, issuer: String, accountName: String): String {
        val params = mapOf(
            "secret" to secretBase32,
            "issuer" to issuer,
            "algorithm" to "SHA1",
            "digits" to "6",
            "period" to "30"
        )
        val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "otpauth://totp/$issuer:${accountName}?$queryString"
    }

    fun parseTotpUri(uri: String): TotpUriData? {
        if (!uri.startsWith("otpauth://totp/")) return null
        val rest = uri.removePrefix("otpauth://totp/")
        val parts = rest.split("?")
        if (parts.size != 2) return null
        val label = parts[0]
        val query = parts[1]
        val params = query.split("&").mapNotNull {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()

        val secret = params["secret"] ?: return null
        val issuer = params["issuer"] ?: label.split(":").firstOrNull() ?: ""
        val accountName = label.split(":").lastOrNull() ?: label

        return TotpUriData(
            secret = secret,
            issuer = issuer,
            accountName = accountName,
            algorithm = params["algorithm"] ?: "SHA1",
            digits = params["digits"]?.toIntOrNull() ?: 6,
            period = params["period"]?.toIntOrNull() ?: 30
        )
    }

    fun generateHotpCode(secretBase32: String, counter: Long, digits: Int = 6): String {
        val secret = base32Decode(secretBase32)
        return generateTOTP(secret, counter, digits)
    }

    fun generateRandomSecret(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return base32Encode(bytes)
    }

    private fun generateTOTP(secret: ByteArray, counter: Long, digits: Int): String {
        val counterBytes = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (c and 0xFF).toByte()
            c = c shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(secret, "RAW")
        mac.init(keySpec)
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0xF
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % Math.pow(10.0, digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }

    private fun base32Decode(base32: String): ByteArray {
        val cleaned = base32.replace(" ", "").replace("-", "").uppercase()
            .trimEnd('=')

        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (char in cleaned) {
            val value = BASE32_CHARS.indexOf(char)
            if (value < 0) continue

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                bytes.add((buffer shr bitsLeft).toByte())
                buffer = buffer and ((1 shl bitsLeft) - 1)
            }
        }

        return bytes.toByteArray()
    }

    private fun base32Encode(bytes: ByteArray): String {
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8

            while (bitsLeft >= 5) {
                bitsLeft -= 5
                val index = (buffer shr bitsLeft) and 0x1F
                result.append(BASE32_CHARS[index])
            }
        }

        if (bitsLeft > 0) {
            buffer = buffer shl (5 - bitsLeft)
            val index = buffer and 0x1F
            result.append(BASE32_CHARS[index])
        }

        return result.toString()
    }

    data class TotpUriData(
        val secret: String,
        val issuer: String,
        val accountName: String,
        val algorithm: String,
        val digits: Int,
        val period: Int
    )
}
