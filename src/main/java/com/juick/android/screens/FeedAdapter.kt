/*
 * Copyright (C) 2008-2022, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.QuoteSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.juick.App
import com.juick.BuildConfig
import com.juick.R
import com.juick.android.MainActivity
import com.juick.android.widget.util.BlurTransformation
import com.juick.android.widget.util.setCompatElevation
import com.juick.android.widget.util.setDrawableTint
import com.juick.api.model.LinkPreview
import com.juick.api.model.Post
import com.juick.util.MessageUtils
import com.juick.util.StringUtils

/**
 *
 * @author Ugnich Anton
 */
class FeedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var postList: MutableList<Post> = ArrayList()
    var loadMoreRequestListener: OnLoadMoreRequestListener? = null
    var itemClickListener: ((View?, Int) -> Unit)? = null
    var itemMenuListener: OnItemClickListener? = null
    var scrollListener: ((View?, Int, Int) -> Unit)? = null
    private var hasMoreData = true

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val post = getItem(position)
        // 123456/78 -> 1234560078
        return if (post == null) 0 else post.mid.toLong() * 10000 + post.rid
    }

    fun newData(data: List<Post>) {
        val oldCount = postList.size
        postList.clear()
        notifyItemRangeRemoved(0, oldCount)
        postList.addAll(data)
        notifyItemRangeInserted(0, data.size)
    }

    fun addData(data: List<Post>) {
        hasMoreData = data.size > 0
        val oldCount = postList.size
        postList.addAll(data)
        notifyItemRangeInserted(oldCount, postList.size)
    }

    fun addData(data: Post) {
        val oldCount = postList.size
        postList.add(data)
        notifyItemRangeInserted(oldCount, postList.size)
    }

    override fun getItemViewType(position: Int): Int {
        if (hasMoreData && position == postList.size - 1) {
            loadMoreRequestListener?.onLoadMore()
        }
        return if (postList[position].rid == 0) TYPE_THREAD_POST else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_THREAD_POST) {
            val vh =
                VH(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))
            vh.setOnItemClickListener(itemClickListener)
            vh.setOnMenuClickListener(itemMenuListener)
            vh
        } else {
            val vh = VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_thread_message, parent, false)
            )
            vh.setOnItemClickListener(itemClickListener)
            vh.setOnMenuClickListener(itemMenuListener)
            vh.replyToTextView?.setOnClickListener { v: View ->
                val p = v.tag as Post?
                p?.let {
                    scrollListener?.invoke(v, p.replyto, p.rid)
                }
            }
            vh
        }
    }

    internal inner class IconTarget(private val holder: VH) : CustomTarget<Drawable?>(48, 48) {
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
            holder.replyToTextView?.setCompoundDrawablesWithIntrinsicBounds(
                resource,
                null,
                null,
                null
            )
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            holder.replyToTextView?.setCompoundDrawablesWithIntrinsicBounds(
                placeholder,
                null,
                null,
                null
            )
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val type = getItemViewType(position)
        val holder = viewHolder as VH
        val post = postList[position]
        val isThread = type != TYPE_THREAD_POST
        Glide.with(holder.itemView.context)
            .load(post.user.avatar)
            .transition(DrawableTransitionOptions.withCrossFade())
            .fallback(R.drawable.av_96).into(holder.upicImageView)
        holder.usernameTextView.text = post.user.uname
        holder.timestampTextView.text = MessageUtils.formatMessageTimestamp(post)
        holder.textTextView.text = StringUtils.EMPTY
        holder.textTextView.text = formatMessageText(holder.itemView.context, post)
        holder.textTextView.movementMethod = LinkMovementMethod.getInstance()
        if (post.photo != null && post.photo?.small != null) {
            holder.photoLayout.visibility = View.VISIBLE
            holder.photoDescriptionView.visibility = View.GONE
            val drawable = Glide.with(holder.itemView.context)
                .load(post.photo!!.small)
                .transition(DrawableTransitionOptions.withCrossFade())
            if (BuildConfig.HIDE_NSFW && MessageUtils.haveNSFWContent(post)) {
                drawable.apply(RequestOptions.bitmapTransform(BlurTransformation()))
                    .into(holder.photoImageView)
            } else {
                drawable.into(holder.photoImageView)
            }
        } else if (App.instance.hasViewableContent(post.text)) {
            holder.photoLayout.visibility = View.VISIBLE
            // TODO: support multiple previewers
            App.instance.previewers.firstOrNull()
                ?.getPreviewUrl(post.text) { link: LinkPreview? ->
                    if (link != null) {
                        Glide.with(holder.itemView.context).load(link.url)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(holder.photoImageView)
                        holder.photoDescriptionView.visibility = View.VISIBLE
                        holder.photoDescriptionView.text = link.description
                    } else {
                        holder.photoLayout.visibility = View.GONE
                    }
                }
        } else {
            holder.photoLayout.visibility = View.GONE
        }
        if (!isThread) {
            val replies = if (post.replies > 0) post.replies else holder.itemView.context.getString(R.string.reply)
            val likes = if (post.likes > 0) post.likes else holder.itemView.context.getString(R.string.recommend)
            holder.repliesTextView?.visibility = View.VISIBLE
            holder.repliesTextView?.text = "$replies"
            holder.likesTextView?.visibility = View.VISIBLE
            holder.likesTextView?.text = "$likes"
            holder.bottomBarLayout?.setCompatElevation(holder.bottomBarDividerView)
        } else {
            if (post.nextRid == post.rid) {
                holder.backImageView?.visibility = View.VISIBLE
                holder.backImageView?.tag = post
                holder.backImageView?.setOnClickListener { v: View ->
                    val p = v.tag as Post
                    scrollListener?.invoke(v, p.prevRid, 0)
                    v.visibility = View.GONE
                }
            } else {
                holder.backImageView?.visibility = View.GONE
            }
        }
        if (post.rid > 0 && post.replyto > 0) {
            Glide.with(holder.itemView.context)
                .load(post.to?.avatar)
                .transition(DrawableTransitionOptions.withCrossFade())
                .fallback(R.drawable.av_96).into(IconTarget(holder))
            holder.replyToTextView?.text = post.to?.uname
            holder.replyToTextView?.visibility = View.VISIBLE
            holder.replyToTextView?.tag = post
        } else {
            holder.replyToTextView?.visibility = View.INVISIBLE
        }
    }

    fun getItem(position: Int): Post? {
        return if (position >= postList.size) null else postList[position]
    }

    override fun getItemCount(): Int {
        return if (postList.isEmpty()) 0 else postList.size
    }

    fun setOnLoadMoreRequestListener(loadMoreRequestListener: OnLoadMoreRequestListener?) {
        this.loadMoreRequestListener = loadMoreRequestListener
    }

    val items: List<Post>
        get() = postList

    interface OnLoadMoreRequestListener {
        fun onLoadMore()
    }

    internal class VH(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var container: ViewGroup
        var upicImageView: ImageView
        var usernameTextView: TextView
        var timestampTextView: TextView
        var photoImageView: ImageView
        var photoLayout: RelativeLayout
        var photoDescriptionView: TextView
        var textTextView: TextView
        var repliesTextView: TextView?
        var likesTextView: TextView?
        var bottomBarLayout: LinearLayout?
        var replyToTextView: TextView?
        var menuImageView: ImageView
        var backImageView: ImageView?
        var itemClickListener: ((View?, Int) -> Unit)? = null
        var menuClickListener: OnItemClickListener? = null
        var bottomBarDividerView: View?

        init {
            container = itemView.findViewById(R.id.container)
            upicImageView = itemView.findViewById(R.id.user_picture)
            usernameTextView = itemView.findViewById(R.id.username)
            timestampTextView = itemView.findViewById(R.id.timestamp)
            textTextView = itemView.findViewById(R.id.text)
            photoLayout = itemView.findViewById(R.id.photo_wrapper)
            photoImageView = itemView.findViewById(R.id.photo)
            photoDescriptionView = itemView.findViewById(R.id.photo_description)
            likesTextView = itemView.findViewById(R.id.likes)
            likesTextView?.setCompoundDrawables(
                VectorDrawableCompat.create(
                    itemView.context.resources,
                    R.drawable.ic_ei_heart, null
                ), null, null, null
            )
            replyToTextView = itemView.findViewById(R.id.replyto)
            bottomBarLayout = itemView.findViewById(R.id.bottom_bar_layout)
            bottomBarDividerView = itemView.findViewById(R.id.bottom_bar_elevation_pre_lollipop)
            likesTextView?.setDrawableTint()
            repliesTextView = itemView.findViewById(R.id.replies)
            repliesTextView?.setCompoundDrawables(
                VectorDrawableCompat.create(
                    itemView.context.resources,
                    R.drawable.ic_ei_comment, null
                ), null, null, null
            )
            repliesTextView?.setDrawableTint()
            backImageView = itemView.findViewById(R.id.back_imageView)
            menuImageView = itemView.findViewById(R.id.menu_dots)
            menuImageView.setOnClickListener(this)
            itemView.setOnClickListener(this)
            textTextView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (v.id == R.id.menu_dots) {
                menuClickListener?.onItemClick(v, bindingAdapterPosition)
                return
            }
            itemClickListener?.invoke(v, bindingAdapterPosition)
        }

        fun setOnItemClickListener(listener: ((View?, Int) -> Unit)?) {
            itemClickListener = listener
        }

        fun setOnMenuClickListener(listener: OnItemClickListener?) {
            menuClickListener = listener
        }
    }

    fun setOnItemClickListener(itemClickListener: (View?, Int) -> Unit) {
        this.itemClickListener = itemClickListener
    }

    fun setOnMenuListener(listener: OnItemClickListener?) {
        itemMenuListener = listener
    }

    fun setOnScrollListener(listener: (View?, Int, Int) -> Unit) {
        scrollListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, pos: Int)
    }

    companion object {
        fun formatMessageText(context: Context, jmsg: Post): SpannableStringBuilder {
            val tagLine = SpannableStringBuilder()
            var nextSpanStart = 0
            for (tag in jmsg.tags) {
                val text = "#$tag"
                tagLine.append(text)
                tagLine.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        widget.tag = "clicked"
                        val activity = widget.context as MainActivity
                        activity.title = "#$tag"
                        /*activity.replaceFragment(
                                FeedBuilder.feedFor(
                                        UrlBuilder.getPostsByTag(jmsg.getUser().getUid(), tag)));*/
                    }
                }, nextSpanStart, nextSpanStart + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                tagLine.append(" ")
                nextSpanStart += text.length + 1
            }
            val formattedMessage = MessageUtils.formatMessage(StringUtils.defaultString(jmsg.getBody()))
            val text = HtmlCompat.fromHtml(
                formattedMessage,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            val textContent = SpannableStringBuilder()
            textContent.append(text)
            val quotes = textContent.getSpans<QuoteSpan>()
            for (quote in quotes) {
                val start = textContent.getSpanStart(quote)
                val end = textContent.getSpanEnd(quote)
                textContent.removeSpan(quote)
                textContent.setSpan(
                    QuoteSpan(ContextCompat.getColor(context, R.color.colorDimmed)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            val urlSpans = textContent.getSpans<URLSpan>()
            // handle deep links
            for (span in urlSpans) {
                val start = textContent.getSpanStart(span)
                val end = textContent.getSpanEnd(span)
                val link = span.url
                textContent.removeSpan(span)
                textContent.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        widget.tag = "clicked"
                        val data = Uri.parse(link)
                        val activity = widget.context as MainActivity
                        if (data.host == "juick.com") {
                            activity.processUri(data)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, data)
                            widget.context.startActivity(intent)
                        }
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return SpannableStringBuilder()
                .append(tagLine)
                .append(textContent)
        }

        private const val TYPE_ITEM = 0
        private const val TYPE_THREAD_POST = 2
    }
}