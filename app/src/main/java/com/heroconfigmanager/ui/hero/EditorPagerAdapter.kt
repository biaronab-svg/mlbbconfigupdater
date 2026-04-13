package com.heroconfigmanager.ui.hero

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EditorPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0    -> BasicInfoFragment()
        1    -> SkinsFragment()
        else -> UpgradeSkinsFragment()
    }
}
