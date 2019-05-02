package com.example.record_lib.cameraview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.record_lib.R
import kotlinx.android.synthetic.main.viewpager_item_view.*

/**
 * @author Dat Bui T. on 4/26/19.
 */
class CameraItemStateFragment : Fragment() {

    companion object {
        fun instance(title: String) = CameraItemStateFragment().apply {
            this.title = title
        }

    }

    private var title = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.viewpager_item_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvTitle.text = title
    }
}
