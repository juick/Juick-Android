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
package com.juick.android.fragment;

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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.juick.App;
import com.juick.R;
import com.juick.android.FeedBuilder;
import com.juick.api.RestClient;
import com.juick.api.model.Chat;
import com.juick.api.model.Pms;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.models.IDialog;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

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
        return new ChatsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.PMs);

        final DialogsListAdapter<Chat> dialogListAdapter = new DialogsListAdapter<>(new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url, Object object) {
                Glide.with(imageView.getContext())
                        .load(url)
                        .into(imageView);

            }
        });
        dialogListAdapter.setOnDialogClickListener(new DialogsListAdapter.OnDialogClickListener<Chat>() {
            @Override
            public void onDialogClick(Chat dialog) {
                getBaseActivity().replaceFragment(
                        FeedBuilder.chatFor(dialog.getDialogName(), Integer.valueOf(dialog.getId())));
            }
        });
        final DialogsList dialogsList = getBaseActivity().findViewById(R.id.dialogsList);
        dialogsList.setAdapter(dialogListAdapter);

        RestClient.getApi().groupsPms(10).enqueue(new Callback<Pms>() {
            @Override
            public void onResponse(Call<Pms> call, Response<Pms> response) {
                if (response.isSuccessful()) {
                    Pms pms = response.body();
                    if (pms.pms != null) {
                        dialogListAdapter.setItems(pms.pms);
                    }
                }
            }

            @Override
            public void onFailure(Call<Pms> call, Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }
}


