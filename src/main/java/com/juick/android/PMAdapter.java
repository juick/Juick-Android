package com.juick.android;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.juick.R;
import com.juick.api.model.Post;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by gerc on 15.02.2016.
 */
public class PMAdapter extends RecyclerView.Adapter<PMAdapter.VH> {

    private static final DateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final int TYPE_OUT_MESSAGE = 0;
    private static final int TYPE_IN_MESSAGE = 1;

    List<Post> postList = new ArrayList<>();
    int uid;

    public PMAdapter(int uid) {
        this.uid = uid;
    }

    public void addData(List<Post> data) {
        int oldCount = postList.size();
        Collections.sort(data, new Comparator<Post>() {
            @Override
            public int compare(Post lhs, Post rhs) {
                if (lhs != null && rhs != null) {
                    try {
                        return sourceDateFormat.parse(lhs.timestamp).compareTo(sourceDateFormat.parse(rhs.timestamp));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                return 0;
            }
        });
        postList.addAll(data);
        notifyItemRangeInserted(oldCount, postList.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (uid == getItem(position).user.uid) {
            return TYPE_IN_MESSAGE;
        } else {
            return TYPE_OUT_MESSAGE;
        }
    }

    public Post getItem(int position) {
        return postList.get(position);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_IN_MESSAGE) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_pm_in, parent, false));
        } else if (viewType == TYPE_OUT_MESSAGE) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_pm_out, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Post msg = getItem(position);
        holder.tv.setText(msg.body);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tv;

        public VH(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(R.id.text);
        }
    }
}
