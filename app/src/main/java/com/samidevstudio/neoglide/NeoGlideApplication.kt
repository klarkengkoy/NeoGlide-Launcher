package com.samidevstudio.neoglide

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeoGlideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NeoGlideInit", "Step 1: Application onCreate - Process started.")
    }
}
