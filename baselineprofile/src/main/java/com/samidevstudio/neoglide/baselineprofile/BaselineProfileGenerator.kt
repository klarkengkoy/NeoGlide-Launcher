package com.samidevstudio.neoglide.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.samidevstudio.neoglide",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        
        // Add interactions here to profile specific flows
        // For example, opening the drawer
        // device.waitForIdle()
    }
}
