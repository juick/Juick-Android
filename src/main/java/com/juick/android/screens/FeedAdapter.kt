/*
 * Copyright (C) 2008-2023, Juick
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
import com.juick.android.JuickMessageMenuListener
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
class FeedAdapter(private val showSubscriptions: Boolean = false) : ListAdapter<Post, FeedAdapter.PostViewHolder>(DIFF_CALLBACK) {
    private var loadMoreRequestListener: OnLoadMoreRequestListener? = null
    var postUpdatedListener: OnPostUpdatedListener? = null
    private var itemClickListener: ((View?, Int) -> Unit)? = null
    private var itemMenuListener: OnItemClickListener? = null
    private var scrollListener: ((View?, Int, Int) -> Unit)? = null
    private var hasMoreData = true

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val post = getItem(position)
        // 123456/78 -> 1234560078
        return if (post == null) 0 else post.mid.toLong() * 10000 + post.rid
    }

    override fun getItemViewType(position: Int): Int {
        if (hasMoreData && position == currentList.size - 1) {
            loadMoreRequestListener?.onLoadMore()
        }
        return if (currentList[position].rid == 0) TYPE_MESSAGE else TYPE_THREAD_REPLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return if (viewType == TYPE_MESSAGE) {
            val vh =
                PostViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
                )
            vh
        } else {
            val vh = PostViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_thread_reply, parent, false)
            )
            vh.replyToTextView?.setOnClickListener { v: View ->
                val p = v.tag as Post?
                p?.let {
                    scrollListener?.invoke(v, p.replyto, p.rid)
                }
            }
            vh
        }
    }

    internal inner class IconTarget(private val holder: PostViewHolder) :
        CustomTarget<Drawable?>(48, 48) {
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

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val type = getItemViewType(position)
        val post = currentList[position]
        val isReply = type != TYPE_MESSAGE
        Glide.with(holder.itemView.context)
            .load(post.user.avatar)
            .transition(DrawableTransitionOptions.withCrossFade())
            .fallback(R.drawable.av_96).into(holder.upicImageView)
        holder.usernameTextView.text = post.user.uname
        holder.premiumBadge.visibility = if (post.user.premium) View.VISIBLE else View.GONE
        holder.timestampTextView.text = MessageUtils.formatMessageTimestamp(post)
        if (post.friendsOnly) {
            holder.messagePrivacyView!!.visibility = View.VISIBLE
        }
        holder.textTextView.text = StringUtils.EMPTY
        holder.textTextView.text = formatMessageText(holder.itemView.context, post)
        holder.textTextView.movementMethod = LinkMovementMethod.getInstance()
        if (post.photo != null && post.photo?.medium != null) {
            holder.photoLayout.visibility = View.VISIBLE
            holder.photoDescriptionView.visibility = View.GONE
            val drawable = Glide.with(holder.itemView.context)
                .load(post.photo!!.medium!!.url)
                .override(post.photo!!.medium!!.width, post.photo!!.medium!!.height)
                .transition(DrawableTransitionOptions.withCrossFade())
            if (BuildConfig.HIDE_NSFW && MessageUtils.haveNSFWContent(post)) {
                drawable.apply(RequestOptions.bitmapTransform(BlurTransformation()))
                    .into(holder.photoImageView)
            } else {
                drawable.into(holder.photoImageView)
                holder.photoImageView.setOnClickListener {
                    itemMenuListener?.onLinkClick(it, post.photo?.url as String)
                }
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
                        holder.photoImageView.setOnClickListener {
                            itemMenuListener?.onLinkClick(it, link.source)
                        }
                    } else {
                        holder.photoLayout.visibility = View.GONE
                    }
                }
        } else {
            holder.photoLayout.visibility = View.GONE
        }
        if (!isReply) {
            if (showSubscriptions) {
                holder.repliesTextView?.setOnClickListener {
                    itemMenuListener?.onSubscribeToggleClick(holder.repliesTextView, post)
                }
                if (post.subscribed) {
                    holder.repliesTextView?.setCompoundDrawables(
                        VectorDrawableCompat.create(
                            holder.itemView.context.resources,
                            R.drawable.ic_ei_check, null
                        ), null, null, null
                    )
                    holder.repliesTextView?.setDrawableTint()
                    holder.repliesTextView?.text = holder.itemView.context.getString(R.string.subscribed)
                } else {
                    holder.repliesTextView?.setCompoundDrawables(
                        VectorDrawableCompat.create(
                            holder.itemView.context.resources,
                            R.drawable.ic_ei_eye, null
                        ), null, null, null
                    )
                    holder.repliesTextView?.setDrawableTint()
                    holder.repliesTextView?.text = holder.itemView.context.getString(R.string.subscribe)
                }
            } else {
                val replies =
                    if (post.replies > 0) post.replies else holder.itemView.context.getString(R.string.reply)
                val likes =
                    if (post.likes > 0) post.likes else holder.itemView.context.getString(R.string.recommend)
                holder.repliesTextView?.visibility = View.VISIBLE
                holder.repliesTextView?.text = "$replies"
                holder.likesTextView?.visibility = View.VISIBLE
                holder.likesTextView?.text = "$likes"
                holder.bottomBarLayout?.setCompatElevation(holder.bottomBarDividerView)
            }
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
        holder.likesTextView?.setOnClickListener {
            itemMenuListener?.onLikeClick(holder.likesTextView, post)
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
        holder.menuImageView.setOnClickListener {
            itemMenuListener?.onItemClick(it, post)
        }
        holder.itemView.setOnLongClickListener {
            itemMenuListener?.onItemClick(it, post)
            true
        }
        holder.textTextView.setOnLongClickListener {
            itemMenuListener?.onItemClick(it, post)
            true
        }
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(it, position)
        }
        holder.textTextView.setOnClickListener {
            itemClickListener?.invoke(it, position)
        }
    }

    fun setOnLoadMoreRequestListener(loadMoreRequestListener: OnLoadMoreRequestListener?) {
        this.loadMoreRequestListener = loadMoreRequestListener
    }

    interface OnLoadMoreRequestListener {
        fun onLoadMore()
    }

    interface OnPostUpdatedListener {
        fun postUpdated(post: Post)
        fun postLikeChanged(post: Post, isLiked: Boolean)
        fun postSubscriptionChanged(post: Post, isSubscribed: Boolean)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: ViewGroup
        var upicImageView: ImageView
        var usernameTextView: TextView
        var premiumBadge: ImageView
        var timestampTextView: TextView
        var messagePrivacyView: ImageView?
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
        var bottomBarDividerView: View?

        init {
            container = itemView.findViewById(R.id.container)
            upicImageView = itemView.findViewById(R.id.user_picture)
            usernameTextView = itemView.findViewById(R.id.username)
            premiumBadge = itemView.findViewById(R.id.premium_image_view)
            timestampTextView = itemView.findViewById(R.id.timestamp)
            messagePrivacyView = itemView.findViewById(R.id.message_privacy_status)
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
        }
    }

    fun setOnItemClickListener(itemClickListener: (View?, Int) -> Unit) {
        this.itemClickListener = itemClickListener
    }

    fun setOnMenuListener(listener: JuickMessageMenuListener) {
        itemMenuListener = listener
    }

    fun setOnScrollListener(listener: (View?, Int, Int) -> Unit) {
        scrollListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, post: Post)
        fun onLikeClick(view: View?, post: Post)
        fun onSubscribeToggleClick(view: View?, post: Post)
        fun onLinkClick(view: View?, url: String)
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
            val formattedMessage =
                MessageUtils.formatMessage(StringUtils.defaultString(jmsg.getBody()))
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
                        activity.processUri(data)
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return SpannableStringBuilder()
                .append(tagLine)
                .append(textContent)
        }

        private const val TYPE_THREAD_REPLY = 0
        private const val TYPE_MESSAGE = 2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.likes == newItem.likes && oldItem.getBody() == newItem.getBody()
                        && oldItem.subscribed == newItem.subscribed && oldItem.friendsOnly == newItem.friendsOnly
            }
        }
    }
}