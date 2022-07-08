package com.protone.seen.adapter

import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.protone.api.context.newLayoutInflater
import com.protone.api.context.onUiThread
import com.protone.database.room.entity.GalleyMedia
import com.protone.seen.customView.ScaleImageView
import com.protone.seen.databinding.GalleyVp2AdapterLayoutBinding
import java.util.*
import kotlin.concurrent.timerTask

class GalleyViewPager2Adapter(context: Context, private val data: MutableList<GalleyMedia>) :
    BaseAdapter<GalleyVp2AdapterLayoutBinding>(context) {

    private val clk = longArrayOf(0, 0)

    private var timer: Timer? = null

    private val delay = 260L

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyVp2AdapterLayoutBinding> =
        Holder(GalleyVp2AdapterLayoutBinding.inflate(context.newLayoutInflater, parent, false))

    override fun onBindViewHolder(holder: Holder<GalleyVp2AdapterLayoutBinding>, position: Int) {
        (holder.binding.root as ScaleImageView).also { iv ->
            Glide.with(context)
                .asDrawable()
                .load(data[position].uri)
                .skipMemoryCache(true)
                .into(iv)
            holder.binding.root.setOnClickListener {
                System.arraycopy(clk, 1, clk, 0, clk.size - 1)
                clk[clk.size - 1] = SystemClock.uptimeMillis()
                if (timer == null) timer = Timer()
                timer?.schedule(timerTask {
                    context.onUiThread {
                        onClk?.invoke()
                    }
                }, delay)
                if (clk[clk.size - 1] - clk[0] < delay) {
                    timer?.cancel()
                    timer = null
                    iv.performZoom()
                }
            }
        }
    }

    var onClk: (() -> Unit)? = null

    override fun getItemCount(): Int = data.size


}