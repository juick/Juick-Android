/*
 * Copyright (C) 2008-2020, Juick
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

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.juick.App;
import com.juick.R;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.GlideApp;
import com.juick.api.model.Post;
import com.juick.util.MessageUtils;
import com.juick.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import co.lujun.androidtagview.TagContainerLayout;
import co.lujun.androidtagview.TagView;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOOTER = 1;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = -1;
    private static final int TYPE_THREAD_POST = 2;

    List<Post> postList = new ArrayList<>();
    OnLoadMoreRequestListener loadMoreRequestListener;
    OnItemClickListener itemClickListener;
    OnItemClickListener itemMenuListener;
    OnScrollListener scrollListener;

    private Handler handler = new Handler();

    private boolean hasHeader;
    private boolean hasOldPosts = true;
    String inReplyTo = null;

    public JuickMessagesAdapter() {
        setHasStableIds(true);
        inReplyTo = App.getInstance().getString(R.string.In_reply_to_);
    }

    @Override
    public long getItemId(int position) {
        switch (getItemViewType(position)) {
            case TYPE_FOOTER:
                return -1;
            case TYPE_HEADER:
                return 0;
            default:
                Post post = getItem(position);
                // 123456/78 -> 1234560078
                return post == null ? 0 : (long)post.getMid() * 10000 + post.getRid();
        }
    }

    public void newData(List<Post> data) {
        postList.clear();
        postList.addAll(data);
        notifyDataSetChanged();
    }

    public void addData(List<Post> data) {
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
        if (loadMoreRequestListener != null && position == postList.size() + (hasHeader ? 1 : -1))
            return TYPE_FOOTER;
        if (position == 0 && hasHeader)
            return TYPE_HEADER;
        if (postList.get(position).getRid() == 0)
            return TYPE_THREAD_POST;
        return TYPE_ITEM;
    }

    public RecyclerView.ViewHolder onCreateFooterViewHolder(ViewGroup viewGroup) {
        final View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_footer, viewGroup, false);
        return new FH(v);
    }

    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        return new RecyclerView.ViewHolder(viewGroup) {
        };
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            return onCreateFooterViewHolder(parent);
        } else if (viewType == TYPE_HEADER) {
            return onCreateHeaderViewHolder(parent);
        } else if (viewType == TYPE_THREAD_POST) {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            vh.setOnMenuClickListener(itemMenuListener);
            return vh;
        } else {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thread_message, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            vh.setOnMenuClickListener(itemMenuListener);
            return vh;
        }
    }

    public void onBindFooterViewHolder(RecyclerView.ViewHolder holder) {
        FH footerHolder = (FH) holder;
        footerHolder.progressBar.setVisibility(View.VISIBLE);
        if (hasOldPosts && loadMoreRequestListener != null) {
            if (!loadMoreRequestListener.onLoadMore()) {
                footerHolder.progressBar.setVisibility(View.GONE);
            }
        }
    }

    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        int type = getItemViewType(position);
        if (type == TYPE_FOOTER) {
            onBindFooterViewHolder(viewHolder);
            return;
        } else if (type == TYPE_HEADER) {
            onBindHeaderViewHolder(viewHolder);
            return;
        }
        final VH holder = (VH) viewHolder;
        final Post post = postList.get(position);
        boolean isThread = type != TYPE_THREAD_POST;
        if (post.getUser() != null) {
            GlideApp.with(holder.itemView.getContext())
                    .load(post.getUser().getAvatar())
                    .fallback(R.drawable.av_96).into(holder.upicImageView);
            holder.usernameTextView.setText(post.getUser().getUname());
            holder.timestampTextView.setText(MessageUtils.formatMessageTimestamp(post));
            if (!isThread) {
                holder.tagContainerLayout.removeAllTags();
                holder.tagContainerLayout.setTags(post.getTags());
                holder.tagContainerLayout.setOnTagClickListener(new TagView.OnTagClickListener() {
                    @Override
                    public void onTagClick(int position, String text) {
                        Log.d("position", position + " " + text);
                        MainActivity activity = (MainActivity) holder.itemView.getContext();
                        activity.setTitle("*" + text);
                        activity.replaceFragment(
                                FeedBuilder.feedFor(
                                        UrlBuilder.getPostsByTag(post.getUser().getUid(), text)));
                    }

                    @Override
                    public void onTagLongClick(int position, String text) {
                        Log.d("positn", position + " " + text);
                    }

                    @Override
                    public void onSelectedTagDrag(int position, String text) {

                    }

                    @Override
                    public void onTagCrossClick(int position) {
                    }
                });
            }
            holder.textTextView.setText(StringUtils.EMPTY);
            if (!TextUtils.isEmpty(post.getBody())) {
                holder.textTextView.setText(formatMessageText(post));
            }
            holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (post.getPhoto() != null && post.getPhoto().getSmall() != null) {
                holder.photoLayout.setVisibility(View.VISIBLE);
                holder.photoDescriptionView.setVisibility(View.GONE);
                GlideApp.with(holder.itemView.getContext()).load(post.getPhoto().getSmall())
                        .into(holder.photoImageView);
            } else if (Previewer.hasViewableContent(post.getBody())) {
                holder.photoLayout.setVisibility(View.VISIBLE);
                Previewer.getPreviewUrl(post.getBody(), link -> {
                    if (link != null) {
                        GlideApp.with(holder.itemView.getContext()).load(link.getUrl())
                                .into(holder.photoImageView);
                        holder.photoDescriptionView.setVisibility(View.VISIBLE);
                        holder.photoDescriptionView.setText(link.getDescription());
                    } else {
                        holder.photoLayout.setVisibility(View.GONE);
                    }
                });
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

                    holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorPrimary));
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(() -> holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorSecondary)), 600);
                } else {
                    holder.backImageView.setVisibility(View.GONE);
                    holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorSecondary));
                }
            }
            if (post.getRid() > 0 && post.getReplyto() > 0) {
                holder.midTextView.setText(String.format("%s %s", inReplyTo, post.getTo().getUname()));
                holder.midTextView.setVisibility(View.VISIBLE);
                holder.midTextView.setTag(post);
                holder.midTextView.setOnClickListener(v -> {
                    Post p = ((Post) v.getTag());
                    if (scrollListener != null)
                        scrollListener.onScrollToPost(v, p.getReplyto(), p.getRid());
                });
            } else
                holder.midTextView.setVisibility(View.GONE);
        }
    }

    public Post getItem(int position) {
        if (position == postList.size()) return null;
        if (hasHeader)
            return postList.get(position - 1);
        else
            return postList.get(position);
    }

    @Override
    public int getItemCount() {
        if (postList.isEmpty()) return 0;
        return postList.size();
    }

    private SpannableStringBuilder formatMessageText(Post jmsg) {
        Spanned text = Html.fromHtml(MessageUtils.formatMessage(jmsg.getBody()));
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(text);
        URLSpan[] urlSpans = ssb.getSpans(0, ssb.length(), URLSpan.class);
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

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public void setOnLoadMoreRequestListener(OnLoadMoreRequestListener loadMoreRequestListener) {
        this.loadMoreRequestListener = loadMoreRequestListener;
    }

    public void setHasOldPosts(boolean hasOldPosts) {
        this.hasOldPosts = hasOldPosts;
    }

    public List<Post> getItems() {
        return postList;
    }

    public interface OnLoadMoreRequestListener {
        boolean onLoadMore();
    }

    static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {

        ViewGroup container;
        ImageView upicImageView;
        TextView usernameTextView;
        TextView timestampTextView;
        TagContainerLayout tagContainerLayout;
        ImageView photoImageView;
        RelativeLayout photoLayout;
        TextView photoDescriptionView;
        TextView textTextView;
        TextView repliesTextView;
        TextView likesTextView;
        TextView midTextView;
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
            tagContainerLayout = itemView.findViewById(R.id.tags_container);
            textTextView = itemView.findViewById(R.id.text);
            photoLayout = itemView.findViewById(R.id.photoWrapper);
            photoImageView = itemView.findViewById(R.id.photo);
            photoDescriptionView = itemView.findViewById(R.id.photo_description);
            likesTextView = itemView.findViewById(R.id.likes);
            if (likesTextView != null) {
                likesTextView.setCompoundDrawables(VectorDrawableCompat.create(itemView.getContext().getResources(),
                        R.drawable.ic_ei_heart, null), null, null, null);
            }
            midTextView = itemView.findViewById(R.id.mid);
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
                    menuClickListener.onItemClick(v, getAdapterPosition());
                }
                return;
            }
            if (itemClickListener != null){
                itemClickListener.onItemClick(v, getAdapterPosition());
            }
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            itemClickListener = listener;
        }

        void setOnMenuClickListener(OnItemClickListener listener) {
            menuClickListener = listener;
        }
    }

    protected class FH extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        FH(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
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
