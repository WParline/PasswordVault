package com.passwordvault.app.data.repository

import com.passwordvault.app.data.local.dao.TotpDao
import com.passwordvault.app.data.local.entity.TotpEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotpRepository @Inject constructor(
    private val totpDao: TotpDao
) {
    fun getAllTotps(): Flow<List<TotpEntity>> = totpDao.getAllTotps()

    suspend fun getTotpByAccountId(accountId: Long): TotpEntity? = totpDao.getTotpByAccountId(accountId)

    suspend fun getTotpById(id: Long): TotpEntity? = totpDao.getTotpById(id)

    suspend fun insert(totp: TotpEntity): Long = totpDao.insert(totp)

    suspend fun update(totp: TotpEntity) = totpDao.update(totp)

    suspend fun delete(totp: TotpEntity) = totpDao.delete(totp)

    suspend fun deleteById(id: Long) = totpDao.deleteById(id)
}
