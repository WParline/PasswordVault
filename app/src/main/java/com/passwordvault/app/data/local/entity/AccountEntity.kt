package com.passwordvault.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    @ColumnInfo(name = "totp_secret") val totpSecret: String = "",
    @ColumnInfo(name = "totp_issuer") val totpIssuer: String = "",
    @ColumnInfo(name = "hotp_secret") val hotpSecret: String = "",
    @ColumnInfo(name = "hotp_counter") val hotpCounter: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
