package com.example.record_lib.cameraview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * @author Dat Bui T. on 4/26/19.
 */
class CameraPagerStateAdapter(
    private val items: List<String>,
    fragmentManager: FragmentManager
) :
    FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        // make the first pager bigger than others
        return CameraItemStateFragment.instance(items[position])
    }

    override fun getCount(): Int = items.size
}
