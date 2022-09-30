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
package com.juick.android.screens.post;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.juick.App;
import com.juick.R;
import com.juick.api.model.Tag;
import com.juick.databinding.FragmentTagsListBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class TagsFragment extends BottomSheetDialogFragment {

    private FragmentTagsListBinding model;

    private static final String ARG_UID = "ARG_UID";

    private int uid = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        model = FragmentTagsListBinding.inflate(inflater, container, false);
        return model.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            uid = args.getInt(ARG_UID, 0);
        }

        model.progressBar.setVisibility(View.VISIBLE);
        model.list.setHasFixedSize(true);
        final TagsAdapter adapter = new TagsAdapter();
        model.list.setAdapter(adapter);
        adapter.setOnItemClickListener((tagItemView, position) -> {
            String tag = adapter.getItem(position);
            NavController navController = NavHostFragment.findNavController(this);
            navController.getPreviousBackStackEntry().getSavedStateHandle().set("tag", tag);
            TagsFragment.this.dismiss();
        });
       /* adapter.setOnItemLongClickListener((view12, position) -> ((BaseActivity) getActivity())
                .replaceFragment(FeedBuilder.feedFor(
                        UrlBuilder.getPostsByTag(uid, adapter.getItem(position)))));*/

        App.getInstance().getApi().tags(uid).enqueue(new Callback<List<Tag>>() {
            @Override
            public void onResponse(@NonNull Call<List<Tag>> call, @NonNull Response<List<Tag>> response) {
                if (response.isSuccessful() && isAdded()) {
                    model.progressBar.setVisibility(View.GONE);
                    List<Tag> tags = response.body();
                    if (tags != null) {
                        List<String> listAdapter = new ArrayList<>();
                        for (Tag tag : tags) {
                            listAdapter.add(tag.getTag());
                        }
                        adapter.addData(listAdapter);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Tag>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    model.progressBar.setVisibility(View.GONE);
                    Log.d(TagsFragment.this.getClass().getSimpleName(), t.toString());
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
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
                    itemClickListener.onItemClick(v, getBindingAdapterPosition());
                }
            }

            @Override
            public boolean onLongClick(View v) {
                if (itemLongClickListener != null) {
                    itemLongClickListener.onItemLongClick(v, getBindingAdapterPosition());
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
