package one.mixin.android.ui.conversation.adapter

import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.item_chat_gallery.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.round
import one.mixin.android.widget.gallery.internal.entity.Item

class GalleryItemAdapter(
    private val needCamera: Boolean
) : RecyclerView.Adapter<GalleryItemAdapter.ItemViewHolder>() {
    var items: List<Item>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var listener: GalleryCallback? = null
    var size: Int = 0

    var selectedPos: Int? = null
    var selectedUri: Uri? = null

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val params = holder.itemView.layoutParams
        params.width = size
        params.height = size
        val ctx = holder.itemView.context
        holder.itemView.layoutParams = params
        val imageView = holder.itemView.thumbnail_iv
        val blurView = holder.itemView.blur_view
        if (position == 0 && needCamera) {
            holder.itemView.gif_tv.isVisible = false
            holder.itemView.video_iv.isVisible = false
            holder.itemView.duration_tv.isVisible = false
            blurView.isVisible = false
            holder.itemView.send_tv.isVisible = false
            imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                width = ctx.dpToPx(42f)
                height = ctx.dpToPx(42f)
            }
            imageView.setImageResource(R.drawable.ic_gallery_camera)
            holder.itemView.bg.setBackgroundResource(R.drawable.bg_gray_black_round_8dp)
            imageView.setOnClickListener { listener?.onCameraClick() }
        } else {
            imageView.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
            imageView.round(ctx.dpToPx(8f))
            blurView.round(ctx.dpToPx(8f))
            val item = items!![if (needCamera) position - 1 else position]
            holder.itemView.bg.setBackgroundResource(0)
            if (item.isGif) {
                holder.itemView.gif_tv.isVisible = true
                holder.itemView.video_iv.isVisible = false
                holder.itemView.duration_tv.isVisible = false
                imageView.loadGif(item.uri.toString(), centerCrop = true, holder = R.drawable.ic_giphy_place_holder)
            } else {
                holder.itemView.gif_tv.isVisible = false
                if (item.isVideo) {
                    holder.itemView.video_iv.isVisible = true
                    holder.itemView.duration_tv.isVisible = true
                    holder.itemView.duration_tv.text = DateUtils.formatElapsedTime(item.duration / 1000)
                } else {
                    holder.itemView.video_iv.isVisible = false
                    holder.itemView.duration_tv.isVisible = false
                }
                imageView.loadImageCenterCrop(item.uri, R.drawable.image_holder)
            }
            if (selectedUri == item.uri) {
                blurView.isVisible = true
                blurView.post { Blurry.with(ctx).capture(imageView).into(blurView) }
                holder.itemView.send_tv.isVisible = true
            } else {
                blurView.isVisible = false
                holder.itemView.send_tv.isVisible = false
            }
            imageView.setOnClickListener {
                if (selectedUri == item.uri) {
                    selectedUri = null
                    selectedPos = null
                    listener?.onItemClick(position, item.uri)
                    notifyItemChanged(position)
                } else {
                    selectedPos?.let { notifyItemChanged(it) }
                    selectedUri = item.uri
                    selectedPos = position
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = if (needCamera) {
        items?.size ?: +1
    } else {
        items?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_gallery, parent, false)
        return ItemViewHolder(view)
    }

    @Synchronized
    fun hideBLur() {
        if (selectedPos == null) return

        val currentPos = selectedPos
        selectedUri = null
        selectedPos = null
        currentPos?.let { notifyItemChanged(it) }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
