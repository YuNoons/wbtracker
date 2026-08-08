package com.wbtracker.app.data.local.dao

import androidx.room.*
import com.wbtracker.app.data.local.entity.NotificationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: NotificationRuleEntity)

    @Query("SELECT * FROM notification_rules WHERE productId = :productId AND isActive = 1")
    suspend fun getActiveRulesForProduct(productId: Long): List<NotificationRuleEntity>

    @Query("SELECT * FROM notification_rules WHERE productId = :productId LIMIT 1")
    suspend fun getRuleForProduct(productId: Long): NotificationRuleEntity?

    @Query("SELECT * FROM notification_rules WHERE productId = :productId")
    fun observeRulesForProduct(productId: Long): Flow<List<NotificationRuleEntity>>

    @Query("UPDATE notification_rules SET isActive = :active WHERE id = :id")
    suspend fun setRuleActive(id: Long, active: Boolean)

    @Delete
    suspend fun deleteRule(rule: NotificationRuleEntity)
}
