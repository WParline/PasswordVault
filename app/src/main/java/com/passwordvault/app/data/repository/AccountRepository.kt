package com.passwordvault.app.data.repository

import com.passwordvault.app.data.local.dao.AccountDao
import com.passwordvault.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun searchAccounts(query: String): Flow<List<AccountEntity>> = accountDao.searchAccounts(query)

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun insert(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun update(account: AccountEntity) = accountDao.update(account)

    suspend fun delete(account: AccountEntity) = accountDao.delete(account)

    suspend fun deleteById(id: Long) = accountDao.deleteById(id)

    suspend fun findAccountsByUrl(url: String): List<AccountEntity> = accountDao.findAccountsByUrl(url)

    suspend fun getAllAccountsSync(): List<AccountEntity> = accountDao.getAllAccountsSync()
}
