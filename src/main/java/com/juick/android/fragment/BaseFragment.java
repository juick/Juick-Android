package com.juick.android.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import com.juick.android.BaseActivity;

/**
 * Created by gerc on 10.01.2016.
 */
public class BaseFragment extends Fragment {
    boolean showTabsBar;

    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    public void hideTabsBar() {
        showTabsBar = false;
        getBaseActivity().findViewById(getBaseActivity().getTabsBarLayoutId()).setVisibility(View.GONE);
    }

    public void showTabsBar() {
        showTabsBar = true;
        getBaseActivity().findViewById(getBaseActivity().getTabsBarLayoutId()).setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //showTabsBarOld = getBaseActivity().findViewById(getBaseActivity().getTabsBarLayoutId()).getVisibility() == View.VISIBLE;
        hideTabsBar();
    }
}
