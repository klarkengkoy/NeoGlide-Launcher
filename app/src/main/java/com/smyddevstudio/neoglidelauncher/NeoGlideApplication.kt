package com.smyddevstudio.neoglidelauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeoGlideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
