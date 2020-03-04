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
package com.juick.android.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.juick.App;
import com.juick.R;
import com.juick.android.BaseActivity;
import com.juick.android.FeedBuilder;
import com.juick.android.UrlBuilder;
import com.juick.api.model.Tag;
import com.juick.databinding.TagsListBinding;

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

    private TagsListBinding model;

    public interface OnTagAppliedListener {
        void onTagApplied(String tag);
    }

    private static final String ARG_UID = "ARG_UID";

    private int uid = 0;
    private OnTagAppliedListener callback;

    static TagsFragment newInstance(int uid) {
        TagsFragment fragment = new TagsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_UID, uid);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnTagAppliedListener(@NonNull OnTagAppliedListener callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        model = TagsListBinding.inflate(inflater, container, false);
        return model.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            uid = args.getInt(ARG_UID, 0);
        }

        if (uid == 0) {
            getActivity().setTitle(R.string.Popular_tags);
        }

        model.progressBar.setVisibility(View.VISIBLE);
        model.list.setHasFixedSize(true);
        final TagsAdapter adapter = new TagsAdapter();
        model.list.setAdapter(adapter);
        adapter.setOnItemClickListener((view1, position) -> {
            String tag = adapter.getItem(position);
            if (callback != null) {
                getBaseActivity().getSupportFragmentManager().popBackStackImmediate();
                callback.onTagApplied(tag);
            }
        });
        adapter.setOnItemLongClickListener((view12, position) -> ((BaseActivity) getActivity())
                .replaceFragment(FeedBuilder.feedFor(
                        UrlBuilder.getPostsByTag(uid, adapter.getItem(position)))));

        App.getInstance().getApi().tags(uid).enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                model.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && isAdded()) {
                    model.progressBar.setVisibility(View.GONE);
                    List<String> listAdapter = new ArrayList<>();
                    for (Tag tag : response.body()) {
                        listAdapter.add(tag.getTag());
                    }
                    adapter.addData(listAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Tag>> call, Throwable t) {
                Log.d("", t.toString());
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    static class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.VH> {

        List<String> items = new ArrayList<>();
        OnItemClickListener itemClickListener;
        OnItemLongClickListener itemLongClickListener;

        void addData(List<String> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
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
            Log.d("TagsAdapter", "onBindViewHolder: " + position + " " + tag);
            holder.textView.setText(tag);
        }

        @Override
        public int getItemCount() {
            Log.d("TagsAdapter", "getItemCount: " + items.size());
            return items.size();
        }

        String getItem(int position) {
            Log.d("TagsAdapter", "getItem: " + position);

            return items.get(position);
        }

        void setOnItemClickListener(OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        void setOnItemLongClickListener(OnItemLongClickListener itemClickListener) {
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

            VH(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            void setOnItemClickListener(OnItemClickListener listener) {
                itemClickListener = listener;
            }

            void setOnItemLongClickListener(OnItemLongClickListener listener) {
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

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}
