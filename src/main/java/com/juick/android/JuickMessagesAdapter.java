/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import co.lujun.androidtagview.TagContainerLayout;
import co.lujun.androidtagview.TagView;
import com.bumptech.glide.Glide;
import com.juick.App;
import com.juick.android.fragment.PostsPageFragment;
import com.juick.android.fragment.ThreadFragment;
import com.juick.api.RestClient;
import com.juick.api.model.Post;
import com.juick.android.widget.util.ViewUtil;
import com.juick.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FOOTER = 1;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = -1;
    private static final int TYPE_THREAD_POST = 2;

    public static final Pattern boldPattern = Pattern.compile("\\*([^\\*]+)\\*");
    public static final Pattern italicPattern = Pattern.compile("(?:^|\\s|,)/([^/]+)/");
    public static final Pattern underlinePattern = Pattern.compile("_([^_]+)_");
    public static final Pattern urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
    public static final Pattern juickUrlPattern = Pattern.compile("\\[([^\\]]+)\\]\\[(http[^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
    public static final Pattern msgPattern = Pattern.compile("#([0-9]+)");
    private static final Pattern usrPattern = Pattern.compile("@[a-zA-Z0-9\\-]{2,16}");
    private static final DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat outDateFormat = new SimpleDateFormat("HH:mm dd MMM yyyy");

    List<Post> postList = new ArrayList<>();
    OnLoadMoreRequestListener loadMoreRequestListener;
    OnItemClickListener itemClickListener;
    OnItemClickListener itemMenuListener;
    OnScrollListener scrollListener;

    private Handler handler = new Handler();

    private boolean hasHeader;
    private boolean hasOldPosts = true;
    String inReplyTo = null;

    static {
        outDateFormat.setTimeZone(TimeZone.getDefault());
        sourceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public JuickMessagesAdapter() {
        //setHasStableIds(true);
        inReplyTo = App.getInstance().getString(R.string.In_reply_to_);
    }

    /*@Override
    public long getItemId(int position) {
        switch (getItemViewType(position)) {
            case TYPE_FOOTER:
                return -1;
            case TYPE_HEADER:
                return 0;
            default:
                Post post = getItem(position);
                return post == null ? 0 : post.user.uid;
        }
    }*/

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
        post.body = txt;
        postList.add(position, post);
        notifyItemRangeInserted(position, postList.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (loadMoreRequestListener != null && position == postList.size() + (hasHeader ? 1 : -1))
            return TYPE_FOOTER;
        if (position == 0 && hasHeader)
            return TYPE_HEADER;
        if (postList.get(position).rid == 0)
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
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
        if (post.user != null && post.body != null) {
            Glide.with(holder.itemView.getContext()).load(RestClient.getImagesUrl() + "a/" + post.user.uid + ".png").into(holder.upicImageView);
            holder.usernameTextView.setText(post.user.uname);
            holder.timestampTextView.setText(formatMessageTimestamp(post));
            if (!isThread) {
                holder.tagContainerLayout.removeAllTags();
                holder.tagContainerLayout.setTags(post.tags);
                holder.tagContainerLayout.setOnTagClickListener(new TagView.OnTagClickListener() {
                    @Override
                    public void onTagClick(int position, String text) {
                        Log.d("position", position + " " + text);
                        ((BaseActivity) holder.itemView.getContext()).replaceFragment(
                                PostsPageFragment.newInstance(
                                        UrlBuilder.getPostsByTag(post.user.uid, text)));
                    }

                    @Override
                    public void onTagLongClick(int position, String text) {
                        Log.d("positn", position + " " + text);
                    }

                    @Override
                    public void onTagCrossClick(int position) {
                    }
                });
            }

            holder.textTextView.setText(formatMessageText(post));
            holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (post.photo != null && post.photo.small != null) {
                Glide.with(holder.itemView.getContext()).load(post.photo.small).into(holder.photoImageView);
                holder.photoImageView.setVisibility(View.VISIBLE);
            } else {
                holder.photoImageView.setVisibility(View.GONE);
            }
            if (!isThread) {
                if (post.replies > 0) {
                    holder.repliesTextView.setVisibility(View.VISIBLE);
                    holder.repliesTextView.setText(Integer.toString(post.replies));
                } else {
                    holder.repliesTextView.setVisibility(View.GONE);
                }
                if (post.likes > 0) {
                    holder.likesTextView.setVisibility(View.VISIBLE);
                    holder.likesTextView.setText(Integer.toString(post.likes));
                } else {
                    holder.likesTextView.setVisibility(View.GONE);
                }
            } else {
                holder.replyQuoteTextView.setText(post.replyQuote);
                holder.replyQuoteTextView.setVisibility(View.VISIBLE);
                if (post.nextRid == post.rid) {
                    holder.backImageView.setVisibility(View.VISIBLE);
                    holder.backImageView.setTag(post);
                    holder.backImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Post p = ((Post) v.getTag());
                            if (scrollListener != null)
                                scrollListener.onScrollToPost(v, p.prevRid, 0);
                            v.setVisibility(View.GONE);
                        }
                    });

                    holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorPrimary));
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorSecondary));
                        }
                    }, 600);
                } else {
                    holder.backImageView.setVisibility(View.GONE);
                    holder.container.setBackgroundColor(ContextCompat.getColor(holder.container.getContext(), R.color.colorSecondary));
                }
            }
            if (post.rid > 0 && post.replyto > 0) {
                holder.midTextView.setText(String.format("/%1$d %2$s /%3$d", post.rid, inReplyTo, post.replyto));
                holder.midTextView.setVisibility(View.VISIBLE);
                holder.midTextView.setTag(post);
                holder.midTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Post p = ((Post) v.getTag());
                        if (scrollListener != null)
                            scrollListener.onScrollToPost(v, p.replyto, p.rid);
                    }
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

    private String formatMessageTimestamp(Post jmsg) {
        try {
            return outDateFormat.format(sourceDateFormat.parse(jmsg.timestamp));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private SpannableStringBuilder formatMessageText(Post jmsg) {
        String tmp = jmsg.body.replaceAll("\n", "<br>");
        tmp = boldPattern.matcher(tmp).replaceAll("<b>$1</b>");
        tmp = italicPattern.matcher(tmp).replaceAll("<i>$1</i>");
        tmp = underlinePattern.matcher(tmp).replaceAll("<u>$1</u>");
        tmp = juickUrlPattern.matcher(tmp).replaceAll("<a href=\"$2\">$1</a>");

        Spanned text = Html.fromHtml(tmp);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(text);

        // Highlight links http://example.com/
        int pos = 0;
        Matcher m = urlPattern.matcher(text);
        while (m.find(pos)) {
            ssb.setSpan(new MyClickableSpan(m.group()), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(App.getInstance(), R.color.colorAccent)), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        // Highlight messages #1234
        pos = 0;
        m = msgPattern.matcher(text);
        while (m.find(pos)) {
            ssb.setSpan(new MessageClickableSpan(m.group(1)), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(App.getInstance(), R.color.colorAccent)), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }

        // Highlight usernames @username
        pos = 0;
        m = usrPattern.matcher(text);
        while (m.find(pos)) {
            ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(App.getInstance(), R.color.colorAccent)), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
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
        TextView textTextView;
        TextView repliesTextView;
        TextView likesTextView;
        TextView replyQuoteTextView;
        TextView midTextView;
        ImageView menuImageView;
        ImageView backImageView;
        OnItemClickListener itemClickListener;
        OnItemClickListener menuClickListener;

        VH(View itemView) {
            super(itemView);
            container = (ViewGroup) itemView.findViewById(R.id.container);
            upicImageView = (ImageView) itemView.findViewById(R.id.userpic);
            usernameTextView = (TextView) itemView.findViewById(R.id.username);
            timestampTextView = (TextView) itemView.findViewById(R.id.timestamp);
            tagContainerLayout = (TagContainerLayout) itemView.findViewById(R.id.tags_container);
            replyQuoteTextView = (TextView) itemView.findViewById(R.id.replyQuote);
            textTextView = (TextView) itemView.findViewById(R.id.text);
            photoImageView = (ImageView) itemView.findViewById(R.id.photo);
            likesTextView = (TextView) itemView.findViewById(R.id.likes);
            midTextView = (TextView) itemView.findViewById(R.id.mid);
            ViewUtil.setTint(likesTextView);
            repliesTextView = (TextView) itemView.findViewById(R.id.replies);
            ViewUtil.setTint(repliesTextView);
            backImageView = (ImageView) itemView.findViewById(R.id.back_imageView);
            menuImageView = (ImageView) itemView.findViewById(R.id.menu_imageView);
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

        public void setOnItemClickListener(OnItemClickListener listener) {
            itemClickListener = listener;
        }

        public void setOnMenuClickListener(OnItemClickListener listener) {
            menuClickListener = listener;
        }
    }

    protected class FH extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public FH(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
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

    public static class MyClickableSpan extends ClickableSpan {
        String link;

        public MyClickableSpan(String link) {
            this.link = link;
        }

        @Override
        public void onClick(View widget) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            widget.getContext().startActivity(intent);
        }
    }

    public static class MessageClickableSpan extends ClickableSpan {
        String mid;

        public MessageClickableSpan(String mid) {
            this.mid = mid;
        }

        @Override
        public void onClick(View widget) {
            try {
                ((BaseActivity) widget.getContext()).replaceFragment(ThreadFragment.newInstance(Integer.parseInt(mid)));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(UrlBuilder.getNormalPostById(mid)));
                widget.getContext().startActivity(intent);
            }
        }
    }
}
