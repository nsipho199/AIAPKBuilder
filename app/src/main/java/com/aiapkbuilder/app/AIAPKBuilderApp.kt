package com.aiapkbuilder.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aiapkbuilder.app.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AIAPKBuilderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        val logPath = "${filesDir}/logs/aiapkbuilder.log"
        AppLogger.initialize(logPath)
        AppLogger.i("AIAPKBuilder app initialized", "AIAPKBuilderApp")
    }
}

