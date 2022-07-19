package com.protone.seenn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.protone.api.entity.GalleyMedia
import com.protone.seen.databinding.RichVideoLayoutBinding

class GalleyViewFragment(private val galleyMedia: GalleyMedia) : Fragment() {

    private lateinit var binding: RichVideoLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RichVideoLayoutBinding.inflate(inflater, container, false)
        binding.richVideo.setVideoPath(galleyMedia.uri)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.richVideo.release()
    }

    override fun onResume() {
        super.onResume()
        binding.richVideo.play()
    }

    override fun onPause() {
        super.onPause()
        binding.richVideo.pause()
    }
}