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
package com.juick.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juick.R;
import com.juick.android.BaseActivity;
import com.juick.android.ITagable;
import com.juick.android.UrlBuilder;
import com.juick.api.RestClient;
import com.juick.api.model.Tag;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ugnich Anton
 */
public class TagsFragment extends BaseFragment {

    private static final String ARG_UID = "ARG_UID";
    public static final String ARG_TAG = "ARG_TAG";

    public static final String TAG_SELECT_ACTION = "TAG_SELECT_ACTION";

    int uid = 0;
    ITagable mCallback;

    public TagsFragment() {
    }

    public static TagsFragment newInstance(int uid) {
        TagsFragment fragment = new TagsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_UID, uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        try {
            mCallback = (ITagable) c;
        } catch (ClassCastException e) {

        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            uid = args.getInt(ARG_UID, 0);
        }

        if (uid == 0) {
            getActivity().setTitle(R.string.Popular_tags);
        }

        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        final TagsAdapter adapter = new TagsAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new TagsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String tag = adapter.getItem(position);
                if(mCallback != null)
                    mCallback.onTagApplied(tag);
                else
                    getActivity().sendBroadcast(new Intent(TAG_SELECT_ACTION).putExtra(ARG_TAG, tag));
            }
        });
        adapter.setOnItemLongClickListener(new TagsAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position){
                ((BaseActivity) getActivity()).replaceFragment(PostsPageFragment.newInstance(UrlBuilder.getPostsByTag(uid, adapter.getItem(position))));
            }
        });

        String url = "https://api.juick.com/tags";
        if (uid > 0) {
            url += "?user_id=" + uid;
        }
        RestClient.getApi().tags(url).enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                progressBar.setVisibility(View.GONE);
                List<String> listAdapter = new ArrayList<>();
                for (Tag tag : response.body()) {
                    listAdapter.add(tag.tag);
                }
                adapter.addData(listAdapter);
            }

            @Override
            public void onFailure(Call<List<Tag>> call, Throwable t) {
                Log.d("", t.toString());
                Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG);
            }
        });
    }

    static class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.VH> {

        List<String> items = new ArrayList<>();
        OnItemClickListener itemClickListener;
        OnItemLongClickListener itemLongClickListener;

        public void addData(List<String> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            vh.setOnItemLongClickListener(itemLongClickListener);
            Log.d("TagsAdapter", "onCreateViewHolder");
            return vh;
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            String tag = items.get(position);
            Log.d("TagsAdapter", "onBindViewHolder: " + String.valueOf(position) + " " + tag);
            holder.textView.setText(tag);
        }

        @Override
        public int getItemCount() {
            Log.d("TagsAdapter", "getItemCount: " + String.valueOf(items.size()));
            return items.size();
        }

        public String getItem(int position) {
            Log.d("TagsAdapter", "getItem: " + String.valueOf(position));

            return items.get(position);
        }

        public void setOnItemClickListener(OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener itemClickListener) {
            this.itemLongClickListener = itemClickListener;
        }

        public interface OnItemClickListener {
            void onItemClick(View view, int pos);
        }

        public interface OnItemLongClickListener {
            void onItemLongClick(View view, int pos);
        }

        static class VH extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            TextView textView;
            OnItemClickListener itemClickListener;
            OnItemLongClickListener itemLongClickListener;

            public VH(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(android.R.id.text1);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            public void setOnItemClickListener(OnItemClickListener listener) {
                itemClickListener = listener;
            }

            public void setOnItemLongClickListener(OnItemLongClickListener listener) {
                itemLongClickListener = listener;
            }

            @Override
            public void onClick(View v) {
                if (itemClickListener != null){
                    itemClickListener.onItemClick(v, getAdapterPosition());
                }
            }

            @Override
            public boolean onLongClick(View v) {
                if (itemLongClickListener != null) {
                    itemLongClickListener.onItemLongClick(v, getAdapterPosition());
                    return true;
                }
                return false;
            }
        }
    }

}
