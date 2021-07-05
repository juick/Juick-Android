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

package com.juick.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.juick.App;
import com.juick.android.fragment.BaseFragment;

/**
 * Created by gerc on 03.06.2016.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> loginLauncher;

    public void showLogin() {
        if (!Utils.hasAuth()) {
            loginLauncher.launch(new Intent(this, SignInActivity.class));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        App.getInstance().setAuthorizationCallback(() -> {
            Intent updatePasswordIntent = new Intent(this, SignInActivity.class);
            updatePasswordIntent.putExtra(SignInActivity.EXTRA_ACTION, SignInActivity.ACTION_PASSWORD_UPDATE);
            loginLauncher.launch(updatePasswordIntent);
        });
        super.onCreate(savedInstanceState);
        loginLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), response -> {
            if (response.getResultCode() == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
    }

    public void addFragment(BaseFragment fragment, boolean addToBackStack) {
        if (getTabsBarLayoutId() > 0) {
            findViewById(getTabsBarLayoutId()).setVisibility(View.GONE);
        }
        showFragment(fragment, false, addToBackStack);
    }

    public void replaceFragment(BaseFragment fragment) {
        if (getTabsBarLayoutId() > 0) {
            findViewById(getTabsBarLayoutId()).setVisibility(View.GONE);
        }
        showFragment(fragment, true, true);
    }

    public void reloadFragment(){
        getCurrentFragment().reload();
    }

    public void showFragment(BaseFragment fragment, boolean isReplace, boolean addToBackStack) {
        Log.d(fragment.getClass().getSimpleName(), "showFragment");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (isReplace) {
            fragmentTransaction.replace(fragmentContainerLayoutId(), fragment);
        } else {
            fragmentTransaction.add(fragmentContainerLayoutId(), fragment);
        }
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        fragmentTransaction.commit();
    }

    public BaseFragment getCurrentFragment(){
        return (BaseFragment)getSupportFragmentManager().findFragmentById(fragmentContainerLayoutId());
    }

    public abstract int fragmentContainerLayoutId();
    public abstract int getTabsBarLayoutId();

}
