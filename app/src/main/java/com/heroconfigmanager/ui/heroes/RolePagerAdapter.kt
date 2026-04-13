package com.heroconfigmanager.ui.heroes

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class RolePagerAdapter(fragment: Fragment, private val roles: List<String>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount() = roles.size

    override fun createFragment(position: Int): Fragment {
        return HeroListFragment().apply {
            arguments = Bundle().also { it.putString(HeroListFragment.ARG_ROLE, roles[position]) }
        }
    }
}
