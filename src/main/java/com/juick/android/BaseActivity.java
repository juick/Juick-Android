package com.juick.android;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.juick.android.fragment.BaseFragment;

/**
 * Created by gerc on 03.06.2016.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 5;

    public void showLogin() {
        if (!Utils.hasAuth()) {
            startActivityForResult(new Intent(this, SignInActivity.class), REQUEST_CODE_LOGIN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            } else {
                finish();
            }
        }
    }

    public void addFragment(BaseFragment fragment, boolean addToBackStack) {
        showFragment(fragment, false, addToBackStack);
    }

    public void replaceFragment(BaseFragment fragment) {
        showFragment(fragment, true, true);
    }

    public void showFragment(BaseFragment fragment, boolean isReplace, boolean addToBackStack) {
        Log.e(fragment.getClass().getSimpleName(), "showFragment");
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

    public abstract int fragmentContainerLayoutId();
    public abstract int getTabsBarLayoutId();

}
