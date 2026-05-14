package com.passwordvault.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.passwordvault.app.data.local.dao.AccountDao
import com.passwordvault.app.data.local.dao.TotpDao
import com.passwordvault.app.data.local.entity.AccountEntity
import com.passwordvault.app.data.local.entity.TotpEntity
import javax.inject.Singleton

@Database(
    entities = [AccountEntity::class, TotpEntity::class],
    version = 3,
    exportSchema = false
)
@Singleton
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun totpDao(): TotpDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "passwordvault.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
