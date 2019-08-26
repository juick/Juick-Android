package com.juick.android.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.juick.android.BaseActivity;

/**
 * Created by gerc on 10.01.2016.
 */
public class BaseFragment extends Fragment {

    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    public void reload(){

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
