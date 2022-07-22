package com.protone.seen.adapter

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
import com.protone.api.context.onUiThread
import com.protone.seen.R
import com.protone.seen.databinding.GalleyBucketListLayoutBinding
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.streams.toList

class GalleyBucketAdapter(
    context: Context,
    val selectBucket: (String) -> Unit
) : SelectListAdapter<GalleyBucketListLayoutBinding, Pair<Uri, Array<String>>>(context) {

    private var galleries: MutableList<Pair<Uri, Array<String>>> = mutableListOf()

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
            GalleyBucketListLayoutBinding.inflate(context.newLayoutInflater, parent, false).apply {
                root.updateLayoutParams {
                    height = SApplication.screenHeight / 10
                }
                bucketThumb.scaleType = ImageView.ScaleType.CENTER_CROP
            })
    }

    override fun onBindViewHolder(holder: Holder<GalleyBucketListLayoutBinding>, position: Int) {
        galleries[position].let { data ->
            setSelect(holder, selectList.contains(data))
            holder.binding.apply {
                root.setOnLongClickListener {
                    DatabaseHelper.instance.execute {
                        val galley = DatabaseHelper
                            .instance
                            .galleyBucketDAOBridge
                            .getGalleyBucketRs(galleries[position].second[0])
                        if (galley != null) withContext(Dispatchers.Main) {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.delete.getString())
                                .setPositiveButton(
                                    R.string.confirm
                                ) { dialog, _ ->
                                    DatabaseHelper
                                        .instance
                                        .galleyBucketDAOBridge
                                        .deleteGalleyBucketAsync(galley)
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
        val index = galleries.indexOf(bucket)
        if (index != -1) {
            galleries.removeAt(index)
            selectList.remove(bucket)
            notifyItemRemoved(index)
        }
    }

    fun refreshBucket(item: Pair<Uri, Array<String>>) {
        val iterator = galleries.iterator()
        var index = 0
        while (iterator.hasNext()) {
            if (iterator.next().second[0] == item.second[0]) {
                galleries[index] = item
                context.onUiThread {
                    notifyItemChanged(index)
                }
                break
            }
            index++
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