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
package com.juick.android;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.juick.App;
import com.juick.BuildConfig;
import com.juick.R;
import com.juick.android.widget.util.BlurTransformation;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.model.Post;
import com.juick.util.MessageUtils;
import com.juick.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author Ugnich Anton
 */
public class JuickMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_THREAD_POST = 2;

    List<Post> postList = new ArrayList<>();
    OnLoadMoreRequestListener loadMoreRequestListener;
    OnItemClickListener itemClickListener;
    OnItemClickListener itemMenuListener;
    OnScrollListener scrollListener;

    private boolean hasMoreData = true;

    public JuickMessagesAdapter() {
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        Post post = getItem(position);
        // 123456/78 -> 1234560078
        return post == null ? 0 : (long)post.getMid() * 10000 + post.getRid();
    }

    public void newData(List<Post> data) {
        int oldCount = postList.size();
        postList.clear();
        notifyItemRangeRemoved(0, oldCount);
        postList.addAll(data);
        notifyItemRangeInserted(0, data.size());
    }

    public void addData(List<Post> data) {
        hasMoreData = data.size() > 0;
        int oldCount = postList.size();
        postList.addAll(data);
        notifyItemRangeInserted(oldCount, postList.size());
    }

    public void addData(Post data) {
        int oldCount = postList.size();
        postList.add(data);
        notifyItemRangeInserted(oldCount, postList.size());
    }

    public void addDisabledItem(String txt, int position) {
        Post post = new Post();
        post.setBody(txt);
        postList.add(position, post);
        notifyItemRangeInserted(position, postList.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (hasMoreData && loadMoreRequestListener != null && position == postList.size() - 1) {
            loadMoreRequestListener.onLoadMore();
        }
        if (postList.get(position).getRid() == 0)
            return TYPE_THREAD_POST;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_THREAD_POST) {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            vh.setOnMenuClickListener(itemMenuListener);
            return vh;
        } else {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thread_message, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            vh.setOnMenuClickListener(itemMenuListener);
            vh.replyToTextView.setOnClickListener(v -> {
                Post p = ((Post) v.getTag());
                if (scrollListener != null)
                    scrollListener.onScrollToPost(v, p.getReplyto(), p.getRid());
            });
            return vh;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        int type = getItemViewType(position);
        final VH holder = (VH) viewHolder;
        final Post post = postList.get(position);
        boolean isThread = type != TYPE_THREAD_POST;
        if (post.getUser() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getUser().getAvatar())
                    .transition(withCrossFade())
                    .fallback(R.drawable.av_96).into(holder.upicImageView);
            holder.usernameTextView.setText(post.getUser().getUname());
            holder.timestampTextView.setText(MessageUtils.formatMessageTimestamp(post));
            holder.textTextView.setText(StringUtils.EMPTY);
            holder.textTextView.setText(formatMessageText(post));
            holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (post.getPhoto() != null && post.getPhoto().getSmall() != null) {
                holder.photoLayout.setVisibility(View.VISIBLE);
                holder.photoDescriptionView.setVisibility(View.GONE);
                RequestBuilder<Drawable> drawable = Glide.with(holder.itemView.getContext())
                        .load(post.getPhoto().getSmall())
                        .transition(withCrossFade());
                if (BuildConfig.HIDE_NSFW && MessageUtils.haveNSFWContent(post)) {
                    drawable.apply(RequestOptions.bitmapTransform(new BlurTransformation()))
                            .into(holder.photoImageView);
                } else {
                    drawable.into(holder.photoImageView);
                }
            } else if (App.getInstance().hasViewableContent(post.getBody())) {
                holder.photoLayout.setVisibility(View.VISIBLE);
                // TODO: support multiple previewers
                if (App.getInstance().getPreviewers().size() > 0) {
                    App.getInstance().getPreviewers().get(0).getPreviewUrl(post.getBody(), link -> {
                        if (link != null) {
                            Glide.with(holder.itemView.getContext()).load(link.getUrl())
                                    .transition(withCrossFade())
                                    .into(holder.photoImageView);
                            holder.photoDescriptionView.setVisibility(View.VISIBLE);
                            holder.photoDescriptionView.setText(link.getDescription());
                        } else {
                            holder.photoLayout.setVisibility(View.GONE);
                        }
                    });
                }
            } else {
                holder.photoLayout.setVisibility(View.GONE);
            }
            if (!isThread) {
                if (post.getReplies() > 0) {
                    holder.repliesTextView.setVisibility(View.VISIBLE);
                    holder.repliesTextView.setText(Integer.toString(post.getReplies()));
                } else {
                    holder.repliesTextView.setVisibility(View.GONE);
                }
                if (post.getLikes() > 0) {
                    holder.likesTextView.setVisibility(View.VISIBLE);
                    holder.likesTextView.setText(Integer.toString(post.getLikes()));
                } else {
                    holder.likesTextView.setVisibility(View.GONE);
                }
            } else {
                if (post.nextRid == post.getRid()) {
                    holder.backImageView.setVisibility(View.VISIBLE);
                    holder.backImageView.setTag(post);
                    holder.backImageView.setOnClickListener(v -> {
                        Post p = ((Post) v.getTag());
                        if (scrollListener != null)
                            scrollListener.onScrollToPost(v, p.prevRid, 0);
                        v.setVisibility(View.GONE);
                    });
                } else {
                    holder.backImageView.setVisibility(View.GONE);
                }
            }
            if (post.getRid() > 0 && post.getReplyto() > 0) {
                Glide.with(holder.itemView.getContext())
                        .load(post.getTo().getAvatar())
                        .transition(withCrossFade())
                        .fallback(R.drawable.av_96).into(new CustomTarget<Drawable>(48, 48) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.replyToTextView.setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        holder.replyToTextView.setCompoundDrawablesWithIntrinsicBounds(placeholder, null, null, null);
                    }
                });
                holder.replyToTextView.setText(post.getTo().getUname());
                holder.replyToTextView.setVisibility(View.VISIBLE);
                holder.replyToTextView.setTag(post);
            } else
                holder.replyToTextView.setVisibility(View.GONE);
        }
    }

    public Post getItem(int position) {
        if (position >= postList.size()) return null;
        return postList.get(position);
    }

    @Override
    public int getItemCount() {
        if (postList.isEmpty()) return 0;
        return postList.size();
    }

    private SpannableStringBuilder formatMessageText(Post jmsg) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int nextSpanStart = 0;
        for (String tag : jmsg.getTags()) {
            String text = String.format("#%s", tag);
            ssb.append(text);
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    widget.setTag("clicked");
                    MainActivity activity = (MainActivity)widget.getContext();
                    activity.setTitle("#" + tag);
                    /*activity.replaceFragment(
                            FeedBuilder.feedFor(
                                    UrlBuilder.getPostsByTag(jmsg.getUser().getUid(), tag)));*/
                }
            }, nextSpanStart, nextSpanStart + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(" ");
            nextSpanStart += text.length() + 1;
        }
        Spanned text = HtmlCompat.fromHtml(
                MessageUtils.formatMessage(StringUtils.defaultString(jmsg.getBody())),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        );
        ssb.append(text);
        URLSpan[] urlSpans = ssb.getSpans(nextSpanStart, ssb.length(), URLSpan.class);
        // handle deep links
        for (URLSpan span : urlSpans) {
            int start = ssb.getSpanStart(span);
            int end = ssb.getSpanEnd(span);
            final String link = span.getURL();
            ssb.removeSpan(span);
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    widget.setTag("clicked");
                    Uri data = Uri.parse(link);
                    MainActivity activity = (MainActivity)widget.getContext();
                    if (data.getHost().equals("juick.com")) {
                        activity.processUri(data);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, data);
                        widget.getContext().startActivity(intent);
                    }
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    public void setOnLoadMoreRequestListener(OnLoadMoreRequestListener loadMoreRequestListener) {
        this.loadMoreRequestListener = loadMoreRequestListener;
    }

    public List<Post> getItems() {
        return postList;
    }

    public interface OnLoadMoreRequestListener {
        void onLoadMore();
    }

    static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {

        ViewGroup container;
        ImageView upicImageView;
        TextView usernameTextView;
        TextView timestampTextView;
        ImageView photoImageView;
        RelativeLayout photoLayout;
        TextView photoDescriptionView;
        TextView textTextView;
        TextView repliesTextView;
        TextView likesTextView;
        TextView replyToTextView;
        ImageView menuImageView;
        ImageView backImageView;
        OnItemClickListener itemClickListener;
        OnItemClickListener menuClickListener;

        VH(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            upicImageView = itemView.findViewById(R.id.userpic);
            usernameTextView = itemView.findViewById(R.id.username);
            timestampTextView = itemView.findViewById(R.id.timestamp);
            textTextView = itemView.findViewById(R.id.text);
            photoLayout = itemView.findViewById(R.id.photoWrapper);
            photoImageView = itemView.findViewById(R.id.photo);
            photoDescriptionView = itemView.findViewById(R.id.photo_description);
            likesTextView = itemView.findViewById(R.id.likes);
            if (likesTextView != null) {
                likesTextView.setCompoundDrawables(VectorDrawableCompat.create(itemView.getContext().getResources(),
                        R.drawable.ic_ei_heart, null), null, null, null);
            }
            replyToTextView = itemView.findViewById(R.id.replyto);
            ViewUtil.setDrawableTint(likesTextView);
            repliesTextView = itemView.findViewById(R.id.replies);
            if (repliesTextView != null) {
                repliesTextView.setCompoundDrawables(VectorDrawableCompat.create(itemView.getContext().getResources(),
                        R.drawable.ic_ei_comment, null), null, null, null);
            }
            ViewUtil.setDrawableTint(repliesTextView);
            backImageView = itemView.findViewById(R.id.back_imageView);
            menuImageView = itemView.findViewById(R.id.menu_imageView);
            menuImageView.setOnClickListener(this);
            itemView.setOnClickListener(this);
            textTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.menu_imageView) {
                if (menuClickListener != null){
                    menuClickListener.onItemClick(v, getBindingAdapterPosition());
                }
                return;
            }
            if (itemClickListener != null){
                itemClickListener.onItemClick(v, getBindingAdapterPosition());
            }
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            itemClickListener = listener;
        }

        void setOnMenuClickListener(OnItemClickListener listener) {
            menuClickListener = listener;
        }
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setOnMenuListener(OnItemClickListener listener) {
        itemMenuListener = listener;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        scrollListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int pos);
    }

    public interface OnScrollListener {
        void onScrollToPost(View v, int replyTo, int rid);
    }
}
