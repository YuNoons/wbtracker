package com.wbtracker.app.domain.repository

interface SyncScheduler {
    fun schedulePeriodicUpdate(intervalHours: Long = 6)
    fun cancelUpdates()
}
