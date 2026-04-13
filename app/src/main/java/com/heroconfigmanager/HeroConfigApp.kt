package com.heroconfigmanager

import android.app.Application
import com.google.android.material.color.DynamicColors
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Application class that acts as a ViewModelStoreOwner so that
 * SharedViewModel can be shared across Activities (MainActivity → HeroEditorActivity)
 * without relying on the same back-stack fragment scope.
 */
class HeroConfigApp : Application(), ViewModelStoreOwner {

    private val _appViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _appViewModelStore

    // Convenience alias used in HeroEditorActivity
    val appViewModelStore: ViewModelStore get() = _appViewModelStore

    override fun onCreate() {
        super.onCreate()
        // Enable Material 3 Dynamic Colors
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
