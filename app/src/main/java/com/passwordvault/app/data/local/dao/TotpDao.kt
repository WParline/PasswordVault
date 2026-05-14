package com.passwordvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.passwordvault.app.data.local.entity.TotpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TotpDao {

    @Query("SELECT * FROM totps ORDER BY issuer ASC")
    fun getAllTotps(): Flow<List<TotpEntity>>

    @Query("SELECT * FROM totps WHERE account_id = :accountId")
    suspend fun getTotpByAccountId(accountId: Long): TotpEntity?

    @Query("SELECT * FROM totps WHERE id = :id")
    suspend fun getTotpById(id: Long): TotpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(totp: TotpEntity): Long

    @Update
    suspend fun update(totp: TotpEntity)

    @Delete
    suspend fun delete(totp: TotpEntity)

    @Query("DELETE FROM totps WHERE id = :id")
    suspend fun deleteById(id: Long)
}
