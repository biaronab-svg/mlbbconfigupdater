package com.heroconfigmanager

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.color.DynamicColors
import com.heroconfigmanager.data.repository.AppSettingsRepository

class HeroConfigApp : Application(), ViewModelStoreOwner {

    private val _appViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _appViewModelStore

    val appViewModelStore: ViewModelStore get() = _appViewModelStore

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        AppSettingsRepository.initialize(this)
    }
}
