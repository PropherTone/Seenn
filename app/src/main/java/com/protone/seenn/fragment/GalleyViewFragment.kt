package com.protone.seenn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.protone.api.entity.GalleyMedia
import com.protone.seen.databinding.GalleyVp2AdapterLayoutBinding
import com.protone.seen.databinding.RichVideoLayoutBinding

class GalleyViewFragment(
    private val galleyMedia: GalleyMedia,
    private val singleClick: () -> Unit
) : Fragment() {

    private var videoBinding: RichVideoLayoutBinding? = null
    private var imageBinding: GalleyVp2AdapterLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (galleyMedia.isVideo) {
            videoBinding = RichVideoLayoutBinding.inflate(inflater, container, false)
            videoBinding!!.root
        } else {
            imageBinding = GalleyVp2AdapterLayoutBinding.inflate(inflater, container, false)
            imageBinding?.image?.onSingleTap = singleClick
            imageBinding!!.root
        }
    }

    override fun onStart() {
        super.onStart()
        if (galleyMedia.isVideo) {
            videoBinding?.richVideo?.setVideoPath(galleyMedia.uri)
            videoBinding?.richVideo?.title = galleyMedia.name
        } else {
            if (galleyMedia.name.contains("gif")) {
                imageBinding?.image?.let { Glide.with(this).load(galleyMedia.uri).into(it) }
            } else {
                imageBinding?.image?.setImageResource(galleyMedia.uri)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (galleyMedia.isVideo) videoBinding?.richVideo?.release()
        else imageBinding?.image?.clear()
    }

    override fun onResume() {
        super.onResume()
        if (galleyMedia.isVideo) videoBinding?.richVideo?.play()
        else imageBinding?.image?.locate()
    }

    override fun onPause() {
        super.onPause()
        if (galleyMedia.isVideo) videoBinding?.richVideo?.pause()
    }
}