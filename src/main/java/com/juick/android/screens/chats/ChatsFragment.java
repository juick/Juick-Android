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
package com.juick.android.screens.chats;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.juick.R;
import com.juick.api.model.Chat;
import com.juick.databinding.FragmentDialogListBinding;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

/**
 *
 * @author ugnich
 */
public class ChatsFragment extends Fragment {

    private FragmentDialogListBinding model;
    private ChatsViewModel vm;

    private final DialogsListAdapter<Chat> chatsAdapter;

    public ChatsFragment() {
        super(R.layout.fragment_dialog_list);
        chatsAdapter = new DialogsListAdapter<>((imageView, url, object) ->
                Glide.with(imageView.getContext())
                        .load(url)
                        .transition(withCrossFade())
                        .into(imageView));
        chatsAdapter.setOnDialogClickListener(dialog -> {
            NavController navController = Navigation.findNavController(getView());
            ChatsFragmentDirections.ActionChatsToPMFragment action =
                    ChatsFragmentDirections.actionChatsToPMFragment(dialog.getDialogName());
            action.setUid(Integer.parseInt(dialog.getId()));
            action.setUname(dialog.getDialogName());
            navController.navigate(action);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(R.string.PMs);

        model = FragmentDialogListBinding.bind(view);
        model.dialogsList.setAdapter(chatsAdapter);
        vm = new ViewModelProvider(this).get(ChatsViewModel.class);
        vm.getChats().observe(getViewLifecycleOwner(), chatsAdapter::setItems);
        vm.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            NavController navController = Navigation.findNavController(getView());
            if (!isAuthenticated) {
                NavDirections action = ChatsFragmentDirections.actionChatsToNoAuth();
                navController.navigate(action);
            } else {
                navController.popBackStack(R.id.chats, false);
            }
        });
    }

    @Override
    public void onDestroyView() {
        model = null;
        super.onDestroyView();
    }
}


