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

package com.juick.android.screens.me;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.juick.App;
import com.juick.R;
import com.juick.android.Utils;
import com.juick.api.model.SecureUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeViewModel extends ViewModel {
    private MutableLiveData<SecureUser> me;

    public LiveData<SecureUser> getMe() {
        if (me == null) {
            me = new MutableLiveData<>();
            loadProfile();
        }
        return me;
    }

    public void loadProfile() {
        if (Utils.hasAuth()) {
            App.getInstance().getApi().me().enqueue(new Callback<SecureUser>() {
                @Override
                public void onResponse(@NonNull Call<SecureUser> call, @NonNull Response<SecureUser> response) {
                    me.postValue(response.body());
                }
                @Override
                public void onFailure(@NonNull Call<SecureUser> call, @NonNull Throwable t) {
                    Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
