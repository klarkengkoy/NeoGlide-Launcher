package com.samidevstudio.neoglide.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AppRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return

        applicationScope.launch {
            when (action) {
                Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_CHANGED -> {
                    repository.updatePackage(packageName)
                }
                Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!replacing) {
                        repository.removePackage(packageName)
                    }
                }
            }
        }
    }
}
