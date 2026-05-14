package com.passwordvault.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "totps")
data class TotpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long = 0,
    val secret: String,
    val issuer: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
