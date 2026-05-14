package com.passwordvault.app.di

import android.content.Context
import com.passwordvault.app.data.local.AppDatabase
import com.passwordvault.app.data.local.dao.AccountDao
import com.passwordvault.app.data.local.dao.TotpDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.create(context)
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTotpDao(db: AppDatabase): TotpDao = db.totpDao()
}
