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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `alert_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `productTitle` TEXT NOT NULL,
                    `thumbnailUrl` TEXT NOT NULL,
                    `triggeredPrice` REAL NOT NULL,
                    `targetPrice` REAL NOT NULL,
                    `alertType` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `isRead` INTEGER NOT NULL,
                    `oldPrice` REAL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE price_history ADD COLUMN primaryPrice REAL NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE price_history SET primaryPrice = CASE 
                    WHEN walletPrice > 0 THEN walletPrice 
                    ELSE sellerPrice 
                END
                """.trimIndent()
            )
        }
    }

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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
    @Provides fun provideAlertHistoryDao(db: WbDatabase) = db.alertHistoryDao()
}

