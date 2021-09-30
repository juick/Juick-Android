/*
 * Copyright (C) 2008-2021, Juick
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

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.juick.App;
import com.juick.R;
import com.juick.android.FeedBuilder;
import com.juick.api.GlideApp;
import com.juick.api.model.Chat;
import com.juick.databinding.FragmentDialogListBinding;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

import java.util.List;

/**
 *
 * @author ugnich
 */
public class ChatsFragment extends BaseFragment implements App.ChatsListener {

    private FragmentDialogListBinding model;

    private DialogsListAdapter<Chat> chatsAdapter;

    public ChatsFragment() {
        super(R.layout.fragment_dialog_list);
        chatsAdapter = new DialogsListAdapter<>((imageView, url, object) ->
                GlideApp.with(imageView.getContext())
                        .load(url)
                        .transition(withCrossFade())
                        .into(imageView));
        chatsAdapter.setOnDialogClickListener(dialog -> getBaseActivity().replaceFragment(
                FeedBuilder.chatFor(dialog.getDialogName(), Integer.parseInt(dialog.getId()))));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(R.string.PMs);

        model = FragmentDialogListBinding.bind(view);
        model.dialogsList.setAdapter(chatsAdapter);
    }

    @Override
    public void onChatsReceived(List<Chat> chats) {
        chatsAdapter.setItems(chats);
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}


