package com.protone.seen.adapter

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.protone.api.context.APP
import com.protone.api.context.layoutInflater
import com.protone.api.context.onUiThread
import com.protone.api.getString
import com.protone.database.room.dao.DataBaseDAOHelper
import com.protone.seen.R
import com.protone.seen.databinding.GalleyBucketListLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.streams.toList

class GalleyBucketAdapter(
    context: Context,
    private var galleries: MutableList<Pair<Uri, Array<String>>>,
    val selectBucket: (String) -> Unit
) : SelectListAdapter<GalleyBucketListLayoutBinding, Pair<Uri, Array<String>>>(context) {

    override val select: (holder: Holder<GalleyBucketListLayoutBinding>, isSelect: Boolean) -> Unit =
        { holder, isSelect ->
            holder.binding.bucketCheck.isChecked = isSelect
        }

    override fun itemIndex(path: Pair<Uri, Array<String>>): Int {
        return galleries.indexOf(path)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder<GalleyBucketListLayoutBinding> {
        return Holder(
            GalleyBucketListLayoutBinding.inflate(context.layoutInflater, parent, false).apply {
                root.updateLayoutParams {
                    height = APP.screenHeight / 10
                }
                bucketThumb.scaleType = ImageView.ScaleType.CENTER_CROP
            })
    }

    override fun onBindViewHolder(holder: Holder<GalleyBucketListLayoutBinding>, position: Int) {
        galleries[position].let { data ->
            setSelect(holder, selectList.contains(data))
            holder.binding.apply {
                root.setOnLongClickListener {
                    DataBaseDAOHelper.execute {
                        val galley =
                            DataBaseDAOHelper.getGalleyBucketRs(galleries[position].second[0])
                        if (galley != null) withContext(Dispatchers.Main) {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.delete.getString())
                                .setPositiveButton(
                                    R.string.confirm
                                ) { dialog, _ ->
                                    DataBaseDAOHelper.deleteGalleyBucket(galley)
                                    deleteBucket(galleries[position])
                                    dialog.dismiss()
                                }.setNegativeButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }.create().show()
                        }
                    }
                    false
                }
                bucketThumb.let { thumb ->
                    Glide.with(context).load(data.first)
                        .into(thumb)
                }
                data.second.also { sec ->
                    bucketName.text = sec[0]
                    bucketItemNumber.text = sec[1]
                    bucket.setOnClickListener {
                        selectBucket(sec[0])
                        if (!selectList.contains(data)) checkSelect(holder, data)
                    }
                }
            }
        }

    }

    private fun deleteBucket(bucket: Pair<Uri, Array<String>>) {
        val index = galleries.indexOf(bucket)
        if (index != -1) {
            galleries.removeAt(index)
            selectList.remove(bucket)
            notifyItemRemoved(index)
        }
    }

    fun insertBucket(item: Pair<Uri, Array<String>>) {
        galleries.add(item)
        context.onUiThread {
            notifyItemInserted(galleries.size)
        }
    }

    fun performSelect() {
        galleries.stream()
            .filter { it.second[0] == R.string.all_galley.getString() }
            .toList()
            .let { if (it.isNotEmpty()) selectList.add(it[0]) }
        context.onUiThread {
            notifyItemChanged(0)
        }
    }

    override fun getItemCount(): Int {
        return galleries.size
    }
}