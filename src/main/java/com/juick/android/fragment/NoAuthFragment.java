package com.juick.android.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.juick.R;

/**
 * Created by alx on 13.12.16.
 */

public class NoAuthFragment extends BaseFragment {
    TextView msg;

    public NoAuthFragment(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_noauth, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        msg = view.findViewById(R.id.msg);
        msg.setText(R.string.NoAuthMessage);
    }
}
