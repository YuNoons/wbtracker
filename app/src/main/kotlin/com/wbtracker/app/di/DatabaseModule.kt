package com.wbtracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wbtracker.app.data.local.WbDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "wb_db_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphrase = sharedPreferences.getString("db_passphrase", null)
        if (passphrase == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphrase = randomBytes.joinToString("") { "%02x".format(it) }
            sharedPreferences.edit().putString("db_passphrase", passphrase).apply()
        }
        return passphrase.toByteArray(Charsets.UTF_8)
    }

    @Provides 
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WbDatabase {
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportFactory(passphrase)

        return buildAndVerifyDatabase(context, factory)
    }

    private fun buildAndVerifyDatabase(context: Context, factory: SupportFactory): WbDatabase {
        fun createDb(): WbDatabase {
            return Room.databaseBuilder(
                context,
                WbDatabase::class.java,
                "wb_tracker.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        var db = createDb()
        try {
            // Force header verification / database open
            db.openHelper.writableDatabase
            return db
        } catch (e: Exception) {
            // Self-healing: if header check fails ("file is not a database" / corrupt unencrypted db)
            try {
                db.close()
            } catch (_: Exception) {}

            deleteDatabaseFiles(context, "wb_tracker.db")

            db = createDb()
            try {
                db.openHelper.writableDatabase
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            return db
        }
    }

    private fun deleteDatabaseFiles(context: Context, dbName: String) {
        try {
            context.deleteDatabase(dbName)
        } catch (_: Exception) {}

        val suffixes = listOf("", "-shm", "-wal", "-journal")
        for (suffix in suffixes) {
            try {
                val file = context.getDatabasePath("$dbName$suffix")
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    }

    @Provides fun provideProductDao(db: WbDatabase) = db.productDao()
    @Provides fun providePriceHistoryDao(db: WbDatabase) = db.priceHistoryDao()
    @Provides fun provideReviewSnapshotDao(db: WbDatabase) = db.reviewSnapshotDao()
    @Provides fun provideNotificationRuleDao(db: WbDatabase) = db.notificationRuleDao()
}

