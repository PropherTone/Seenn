package com.protone.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.protone.api.baseType.getString
import com.protone.api.context.SApplication
import com.protone.api.context.newLayoutInflater
import com.protone.ui.R
import com.protone.ui.databinding.GalleyBucketListLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleyBucketAdapter(
    context: Context,
    private val galleyBucketAdapterDataProxy: GalleyBucketAdapterDataProxy,
    private val selectBucket: (String) -> Unit
) : SelectListAdapter<GalleyBucketListLayoutBinding, Pair<Uri, Array<String>>, GalleyBucketAdapter.GalleyBucketEvent>(
    context, true
) {

    sealed class GalleyBucketEvent {
        data class DeleteBucket(val bucket: Pair<Uri, Array<String>>) : GalleyBucketEvent()
        data class RefreshBucket(val bucket: Pair<Uri, Array<String>>) : GalleyBucketEvent()
        data class InsertBucket(val bucket: Pair<Uri, Array<String>>) : GalleyBucketEvent()
    }

    private var galleries: MutableList<Pair<Uri, Array<String>>> = mutableListOf()

    override suspend fun onEventIO(data: GalleyBucketEvent) {
        when (data) {
            is GalleyBucketEvent.DeleteBucket -> {
                galleries.find { it.second[0] == data.bucket.second[0] }?.let {
                    val index = galleries.indexOf(it)
                    galleries.removeAt(index)
                    selectList.remove(it)
                    withContext(Dispatchers.Main) {
                        notifyItemRemoved(index)
                    }
                }
            }
            is GalleyBucketEvent.RefreshBucket -> {
                if (data.bucket.second[0] != R.string.all_galley.getString() &&
                    data.bucket.second[1].toInt() <= 0
                ) {
                    galleries.find { data.bucket.second[0] == it.second[0] }
                        ?.let { deleteBucket(it) }
                    return
                }
                val iterator = galleries.iterator()
                var index = 0
                while (iterator.hasNext()) {
                    if (iterator.next().second[0] == data.bucket.second[0]) {
                        if (selectList.size > 0) {
                            if (selectList[0].second[0] == data.bucket.second[0]) {
                                selectList[0] = data.bucket
                            }
                        }
                        galleries[index] = data.bucket
                        withContext(Dispatchers.Main) {
                            notifyItemChanged(index)
                        }
                        break
                    }
                    index++
                }
            }
            is GalleyBucketEvent.InsertBucket -> {
                galleries.add(data.bucket)
                withContext(Dispatchers.Main) {
                    notifyItemInserted(galleries.size)
                }
            }
        }
    }

    override val select: (holder: Holder<GalleyBucketListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect -> holder.binding.bucketCheck.isChecked = isSelect }

    override fun itemIndex(path: Pair<Uri, Array<String>>): Int {
        return galleries.indexOf(path)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyBucketListLayoutBinding> {
        return Holder(
            GalleyBucketListLayoutBinding.inflate(context.newLayoutInflater, parent, false).apply {
                root.updateLayoutParams { height = SApplication.screenHeight / 10 }
                bucketThumb.scaleType = ImageView.ScaleType.CENTER_CROP
            })
    }

    override fun onBindViewHolder(holder: Holder<GalleyBucketListLayoutBinding>, position: Int) {
        galleries[position].let { data ->
            setSelect(holder, data in selectList)
            holder.binding.apply {
                root.setOnLongClickListener {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.delete.getString())
                        .setPositiveButton(
                            R.string.confirm
                        ) { dialog, _ ->
                            galleyBucketAdapterDataProxy.deleteGalleyBucket(galleries[position].second[0])
                            deleteBucket(galleries[position])
                            dialog.dismiss()
                        }.setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }.create().show()
                    false
                }
                bucketThumb.let { thumb ->
                    if (thumb.tag != data.first) {
                        thumb.tag = data.first
                        Glide.with(context).load(data.first)
                            .into(thumb)
                    }
                }
                data.second.also { sec ->
                    bucketName.text = sec[0]
                    bucketItemNumber.text = sec[1]
                    bucket.setOnClickListener {
                        checkSelect(holder, data)
                        selectBucket(sec[0])
                    }
                }
            }
        }
    }

    override fun checkSelect(
        holder: Holder<GalleyBucketListLayoutBinding>,
        item: Pair<Uri, Array<String>>
    ) {
        if (!multiChoose) clearSelected()
        selectList.add(item)
        setSelect(holder, true)
    }

    private fun deleteBucket(bucket: Pair<Uri, Array<String>>) {
        emit(GalleyBucketEvent.DeleteBucket(bucket))
    }

    fun refreshBucket(item: Pair<Uri, Array<String>>) {
        emit(GalleyBucketEvent.RefreshBucket(item))
    }

    fun insertBucket(item: Pair<Uri, Array<String>>) {
        emit(GalleyBucketEvent.InsertBucket(item))
    }

    override fun getItemCount(): Int {
        return galleries.size
    }

    interface GalleyBucketAdapterDataProxy {
        fun deleteGalleyBucket(bucket: String)
    }
}