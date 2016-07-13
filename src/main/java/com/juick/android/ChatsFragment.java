/*
 * Juick
 * Copyright (C) 2008-2013, ugnich
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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.juick.R;
import com.juick.remote.api.RestClient;
import com.juick.remote.model.Chat;
import com.juick.remote.model.Pms;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ugnich
 */
public class ChatsFragment extends BaseFragment {

    public ChatsFragment() {
    }

    public static ChatsFragment newInstance() {
        ChatsFragment fragment = new ChatsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        final ChatsAdapter adapter = new ChatsAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new ChatsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Chat item = adapter.getItem(position);
                getBaseActivity().replaceFragment(PMFragment.newInstance(item.uname, item.uid));
            }
        });

        RestClient.getApi().groupsPms(10).enqueue(new Callback<Pms>() {
            @Override
            public void onResponse(Call<Pms> call, Response<Pms> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    adapter.addData(response.body().pms);
                }
            }

            @Override
            public void onFailure(Call<Pms> call, Throwable t) {
            }
        });
    }

    static class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.VH> {

        List<Chat> items = new ArrayList<>();
        OnItemClickListener itemClickListener;

        public void addData(List<Chat> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            VH vh = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false));
            vh.setOnItemClickListener(itemClickListener);
            return vh;
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Chat chat = items.get(position);

            holder.textView.setText(chat.uname);
            holder.avatarImageView.setVisibility(View.VISIBLE);

            Glide.with(holder.avatarImageView.getContext()).load("https://i.juick.com/as/" + chat.uid + ".png").into(holder.avatarImageView);

            if (chat.MessagesCount > 0) {
                holder.unreadTextView.setText(Integer.toString(chat.MessagesCount));
                holder.unreadTextView.setVisibility(View.VISIBLE);
            } else {
                holder.unreadTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public Chat getItem(int position) {
            return items.get(position);
        }

        public void setOnItemClickListener(OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        public interface OnItemClickListener {
            void onItemClick(View view, int pos);
        }

        static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {

            TextView textView;
            ImageView avatarImageView;
            TextView unreadTextView;
            private OnItemClickListener itemClickListener;

            public VH(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.text);
                avatarImageView = (ImageView) itemView.findViewById(R.id.icon);
                unreadTextView = (TextView) itemView.findViewById(R.id.unreadMessages);
                itemView.setOnClickListener(this);
            }

            public void setOnItemClickListener(OnItemClickListener listener) {
                itemClickListener = listener;
            }

            @Override
            public void onClick(View v) {
                if (itemClickListener != null){
                    itemClickListener.onItemClick(v, getAdapterPosition());
                }
            }
        }
    }
}


