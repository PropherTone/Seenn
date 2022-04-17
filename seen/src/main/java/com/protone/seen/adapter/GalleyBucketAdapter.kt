package com.protone.seen.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.protone.api.Config
import com.protone.api.context.layoutInflater
import com.protone.seen.databinding.GalleyBucketListLayoutBinding

class GalleyBucketAdapter(
    context: Context,
    var videoBucket: MutableList<Pair<Uri, Array<String>>>,
    val selectBucket: (String) -> Unit
) : SelectListAdapter<GalleyBucketListLayoutBinding, Pair<Uri, Array<String>>>(context) {

    override val select: (holder: Holder<GalleyBucketListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.bucketCheck.isChecked = isSelect
        }

    override fun itemIndex(path: Pair<Uri, Array<String>>): Int {
        return videoBucket.indexOf(path)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyBucketListLayoutBinding> {
        return Holder(
            GalleyBucketListLayoutBinding.inflate(context.layoutInflater, parent, false).apply {
                root.updateLayoutParams {
                    height = Config.screenHeight / 10
                }
                bucketThumb.scaleType = ImageView.ScaleType.CENTER_CROP
            })
    }

    override fun onBindViewHolder(holder: Holder<GalleyBucketListLayoutBinding>, position: Int) {
        videoBucket[position].let { data ->
            setSelect(holder, selectList.contains(data))
            holder.binding.apply {
                bucketThumb.let { thumb ->
                    Glide.with(context).load(data.first)
                        .into(thumb)
                }
                data.second.also { sec ->
                    bucketName.text = sec[0]
                    bucketItemNumber.text = sec[1]
                    bucket.setOnClickListener {
                        selectBucket(sec[0])
                        checkSelect(holder, data)
                    }
                }
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun noticeDataUpdate(item: MutableList<Pair<Uri, Array<String>>>) {
        videoBucket.clear()
        videoBucket.addAll(item)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return videoBucket.size
    }
}