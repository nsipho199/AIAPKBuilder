package com.aiapkbuilder.app.data.service

import android.content.Intent

class BuildService : android.app.Service() {
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
}
