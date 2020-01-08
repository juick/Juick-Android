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

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.juick.App;
import com.juick.R;
import com.juick.android.Utils;
import com.juick.api.model.Chat;
import com.juick.api.model.Pms;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsViewModel extends ViewModel {
    private MutableLiveData<List<Chat>> chats;

    public LiveData<List<Chat>> getChats() {
        if (chats == null) {
            chats = new MutableLiveData<>();
            loadChats();
        }
        return chats;
    }

    private final MutableLiveData<Boolean> authenticated = new MutableLiveData<>(Utils.hasAuth());

    public LiveData<Boolean> isAuthenticated() {
        return authenticated;
    }


    private void loadChats() {
        App.getInstance().getApi().groupsPms(10).enqueue(new Callback<Pms>() {
            @Override
            public void onResponse(@NonNull Call<Pms> call, @NonNull Response<Pms> response) {
                if (response.isSuccessful()) {
                    authenticated.postValue(true);
                    Pms pms = response.body();
                    if (pms != null) {
                        chats.postValue(pms.getPms());
                    }
                } else {
                    authenticated.postValue(response.code() == 401);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pms> call, @NonNull Throwable t) {
                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
