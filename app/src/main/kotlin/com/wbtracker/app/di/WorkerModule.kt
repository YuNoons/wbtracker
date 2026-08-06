package com.wbtracker.app.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import com.wbtracker.app.data.worker.WorkManagerScheduler
import com.wbtracker.app.domain.repository.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface WorkerModule {
    @Binds
    fun bindWorkerFactory(factory: HiltWorkerFactory): WorkerFactory

    @Binds
    fun bindSyncScheduler(scheduler: WorkManagerScheduler): SyncScheduler
}
