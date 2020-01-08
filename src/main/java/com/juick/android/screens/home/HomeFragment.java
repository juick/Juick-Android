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

package com.juick.android.screens.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.juick.android.JuickMessagesAdapter;
import com.juick.api.model.Post;
import com.juick.databinding.FragmentPostsPageBinding;

/**
 * Created by gerc on 03.06.2016.
 */
public class HomeFragment extends Fragment {

    private FragmentPostsPageBinding binding;
    private HomeViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostsPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        JuickMessagesAdapter adapter = new JuickMessagesAdapter();
        binding.list.setAdapter(adapter);
        adapter.setOnItemClickListener((itemView, pos) -> {
            Post post = adapter.getItem(pos);
            HomeFragmentDirections.ActionDiscoverFragmentToThreadFragment action
                    = HomeFragmentDirections.actionDiscoverFragmentToThreadFragment();
            action.setMid(post.getMid());
            Navigation.findNavController(view).navigate(action);
        });
        vm = new ViewModelProvider(this).get(HomeViewModel.class);
        vm.getFeed().observe(getViewLifecycleOwner(), (posts) -> {
            stopRefreshing();
            adapter.newData(posts);
        });
    }

    private void stopRefreshing() {
        binding.swipeContainer.setRefreshing(false);
        binding.list.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
