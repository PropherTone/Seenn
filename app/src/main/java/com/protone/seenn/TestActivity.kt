package com.protone.seenn

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.protone.api.context.UPDATE_GALLEY
import com.protone.api.context.root
import com.protone.seenn.broadcast.workLocalBroadCast
import com.protone.seenn.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {

    lateinit var binding: ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater, root, false)
        setContentView(binding.root)

        binding.btn1.setOnClickListener {
            workLocalBroadCast.sendBroadcast(Intent(UPDATE_GALLEY))
        }
        binding.btn2.setOnClickListener {
            workLocalBroadCast.sendBroadcast(Intent(UPDATE_GALLEY).putExtra("uri", "content://"))
        }
    }
}