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
import com.protone.seen.R
import com.protone.seen.databinding.GalleyBucketListLayoutBinding
import com.protone.seenn.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.streams.toList

class GalleyBucketAdapter(
    context: Context,
    val selectBucket: (String) -> Unit
) : SelectListAdapter<GalleyBucketListLayoutBinding, Pair<Uri, Array<String>>, GalleyBucketAdapter.GalleyBucketEvent>(
    context, true
) {

    sealed class GalleyBucketEvent {
        data class DeleteBucket(val bucket: Pair<Uri, Array<String>>) : GalleyBucketEvent()
        data class RefreshBucket(val item: Pair<Uri, Array<String>>) : GalleyBucketEvent()
        data class InsertBucket(val item: Pair<Uri, Array<String>>) : GalleyBucketEvent()
    }

    private var galleries: MutableList<Pair<Uri, Array<String>>> = mutableListOf()

    override suspend fun onEventIO(data: GalleyBucketEvent) {
        when (data) {
            is GalleyBucketEvent.DeleteBucket -> {
                galleries.first { it.second[0] == data.bucket.second[0] }.let {
                    val index = galleries.indexOf(it)
                    galleries.removeAt(index)
                    selectList.removeAt(index)
                    withContext(Dispatchers.Main) {
                        notifyItemRemoved(index)
                    }
                }
            }
            is GalleyBucketEvent.RefreshBucket -> {
                if (data.item.second[1].toInt() <= 0) {
                    deleteBucket(data.item)
                    return
                }
                val iterator = galleries.iterator()
                var index = 0
                while (iterator.hasNext()) {
                    if (iterator.next().second[0] == data.item.second[0]) {
                        galleries[index] = data.item
                        withContext(Dispatchers.Main) {
                            notifyItemChanged(index)
                        }
                        break
                    }
                    index++
                }
            }
            is GalleyBucketEvent.InsertBucket -> {
                galleries.add(data.item)
                withContext(Dispatchers.Main) {
                    notifyItemInserted(galleries.size)
                }
            }
        }
    }

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
                    launch {
                        val galley = withContext(Dispatchers.IO) {
                            DatabaseHelper
                                .instance
                                .galleyBucketDAOBridge
                                .getGalleyBucketRs(galleries[position].second[0])
                        }
                        if (galley != null) {
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

    fun performSelect() {
        launch(Dispatchers.IO) {
            var index = -1
            galleries.stream()
                .filter { it.second[0] == R.string.all_galley.getString() }
                .toList()
                .let {
                    if (it.isNotEmpty()) {
                        selectList.add(it[0])
                        index = galleries.indexOf(it[0])
                    }
                }
            withContext(Dispatchers.Main) {
                if (index != -1) notifyItemChanged(0)
            }
        }
    }

    private fun deleteBucket(bucket: Pair<Uri, Array<String>>) {
        launch {
            adapterFlow.emit(GalleyBucketEvent.DeleteBucket(bucket))
        }
    }

    fun refreshBucket(item: Pair<Uri, Array<String>>) {
        launch {
            adapterFlow.emit(GalleyBucketEvent.RefreshBucket(item))
        }
    }

    fun insertBucket(item: Pair<Uri, Array<String>>) {
        launch {
            adapterFlow.emit(GalleyBucketEvent.InsertBucket(item))
        }
    }

    override fun getItemCount(): Int {
        return galleries.size
    }
}