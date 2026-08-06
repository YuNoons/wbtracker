package com.wbtracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.wbtracker.app.domain.repository.SyncScheduler
import com.wbtracker.app.ui.navigation.WbTrackerNavGraph
import com.wbtracker.app.ui.theme.WbTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncScheduler.schedulePeriodicUpdate()

        val sharedUrl = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            WbTrackerTheme {
                val navController = rememberNavController()
                WbTrackerNavGraph(navController = navController, sharedUrl = sharedUrl)
            }
        }
    }
}
