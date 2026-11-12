package com.zone.app

import android.app.Application
import com.zone.app.data.ZoneStore

class ZoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ZoneStore.init(this)
    }
}
