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
import android.net.Uri;
import android.os.Handler;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TextView;
import co.lujun.androidtagview.TagContainerLayout;
import co.lujun.androidtagview.TagView;
import com.juick.App;
import com.juick.R;
import com.juick.android.widget.util.ViewUtil;
import com.juick.api.GlideApp;
import com.juick.api.model.Post;

import java.text.DateFormat;
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

    public static final Pattern urlPattern = Pattern.compile("((?<=\\A)|(?<=\\s))(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);
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
        if (post.getUser() != null) {
            GlideApp.with(holder.itemView.getContext())
                    .load(post.getUser().getAvatar())
                    .fallback(R.drawable.av_96).into(holder.upicImageView);
            holder.usernameTextView.setText(post.getUser().getUname());
            holder.timestampTextView.setText(formatMessageTimestamp(post));
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
                    public void onTagCrossClick(int position) {
                    }
                });
            }
            holder.textTextView.setText("");
            if (!TextUtils.isEmpty(post.getBody())) {
                holder.textTextView.setText(formatMessageText(post));
            }
            holder.textTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (post.getPhoto() != null && post.getPhoto().getSmall() != null) {
                GlideApp.with(holder.itemView.getContext()).load(post.getPhoto().getSmall()).into(holder.photoImageView);
                holder.photoImageView.setVisibility(View.VISIBLE);
            } else {
                holder.photoImageView.setVisibility(View.GONE);
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
            if (post.getRid() > 0 && post.getReplyto() > 0) {
                holder.midTextView.setText(String.format("%s %s", inReplyTo, post.getTo().getUname()));
                holder.midTextView.setVisibility(View.VISIBLE);
                holder.midTextView.setTag(post);
                holder.midTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Post p = ((Post) v.getTag());
                        if (scrollListener != null)
                            scrollListener.onScrollToPost(v, p.getReplyto(), p.getRid());
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
        return outDateFormat.format(jmsg.getTimestamp());
    }


    // TODO: taken from juick-core, need merge

    private static Pattern regexLinks2 = Pattern.compile("((?<=\\s)|(?<=\\A))([\\[\\{]|&lt;)((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\\"\\)\\!]+)/?(?:[^\\]\\}](?<!&gt;))*)([\\]\\}]|&gt;)");

    public static String formatMessage(String msg) {
        msg = msg.replaceAll("&", "&amp;");
        msg = msg.replaceAll("<", "&lt;");
        msg = msg.replaceAll(">", "&gt;");

        // --
        // &mdash;
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\-\\-?((?=\\s)|(?=\\Z))", "$1&mdash;$2");

        // http://juick.com/last?page=2
        // <a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\n\\\"]+)/?[^\\s\\n\\\"]*)", "$1<a href=\"$2\" rel=\"nofollow\">$3</a>");

        // [link text][http://juick.com/last?page=2]
        // <a href="http://juick.com/last?page=2" rel="nofollow">link text</a>
        msg = msg.replaceAll("\\[([^\\]]+)\\]\\[((?:ht|f)tps?://[^\\]]+)\\]", "<a href=\"$2\" rel=\"nofollow\">$1</a>");
        msg = msg.replaceAll("\\[([^\\]]+)\\]\\(((?:ht|f)tps?://[^\\)]+)\\)", "<a href=\"$2\" rel=\"nofollow\">$1</a>");

        // #12345
        // <a href="http://juick.com/12345">#12345</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)((?=\\s)|(?=\\Z)|(?=\\))|(?=\\.)|(?=\\,))", "$1<a href=\"https://juick.com/thread/$2\">#$2</a>$3");

        // #12345/65
        // <a href="http://juick.com/12345#65">#12345/65</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/thread/$2#$3\">#$2/$3</a>$4");

        // *bold*
        // <b>bold</b>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))\\*([^\\*\\n<>]+)\\*((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<b>$2</b>$3");

        // /italic/
        // <i>italic</i>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))/([^\\/\\n<>]+)/((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<i>$2</i>$3");

        // _underline_
        // <span class="u">underline</span>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))_([^\\_\\n<>]+)_((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<span class=\"u\">$2</span>$3");

        // /12
        // <a href="#12">/12</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"#$2\">/$2</a>$3");

        // @username@jabber.org
        // <a href="http://juick.com/username@jabber.org/">@username@jabber.org</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-\\.]+@[\\w\\-\\.]+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/$2/\">@$2</a>$3");

        // @username
        // <a href="http://juick.com/username/">@username</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-]{2,16})((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"https://juick.com/$2/\">@$2</a>$3");

        // (http://juick.com/last?page=2)
        // (<a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>)
        Matcher m = regexLinks2.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(3).replace(" ", "%20").replaceAll("\\s+", "");
            m.appendReplacement(sb, "$1$2<a href=\"" + url + "\" rel=\"nofollow\">$4</a>$5");
        }
        m.appendTail(sb);
        msg = sb.toString();

        // > citate
        msg = msg.replaceAll("(?:(?<=\\n)|(?<=\\A))&gt; *(.*)?(\\n|(?=\\Z))", "<q>$1</q>");
        msg = msg.replaceAll("</q><q>", "\n");

        msg = msg.replaceAll("\n", "<br/>\n");
        return msg;
    }

    private SpannableStringBuilder formatMessageText(Post jmsg) {
        Spanned text = Html.fromHtml(formatMessage(jmsg.getBody()));
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
                public void onClick(View widget) {
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
            photoImageView = itemView.findViewById(R.id.photo);
            likesTextView = itemView.findViewById(R.id.likes);
            if (likesTextView != null) {
                likesTextView.setCompoundDrawables(VectorDrawableCompat.create(itemView.getContext().getResources(),
                        R.drawable.ic_ei_heart, null), null, null, null);
            }
            midTextView = itemView.findViewById(R.id.mid);
            ViewUtil.setTint(likesTextView);
            repliesTextView = itemView.findViewById(R.id.replies);
            if (repliesTextView != null) {
                repliesTextView.setCompoundDrawables(VectorDrawableCompat.create(itemView.getContext().getResources(),
                        R.drawable.ic_ei_comment, null), null, null, null);
            }
            ViewUtil.setTint(repliesTextView);
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
