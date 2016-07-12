/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.juick.App;
import com.juick.R;
import com.juick.remote.api.RestClient;
import com.juick.remote.model.Post;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessageMenu implements OnClickListener, JuickMessagesAdapter.OnItemClickListener {

    Context context;
    List<Post> postList;
    Post selectedPost;
    int menuLength;

    public JuickMessageMenu(List<Post> postList) {
        this.postList = postList;
    }

    private void confirmAction(final int resId, final Runnable action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(App.getInstance().getResources().getString(resId));
        builder.setPositiveButton(R.string.Yes, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                action.run();
            }
        });
        builder.setNegativeButton(R.string.Cancel, null);
        builder.show();
    }

    private void postMessage(final String body, final String ok) {
        RestClient.getApi().post(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Toast.makeText(App.getInstance(), (response.isSuccessful()) ? ok : App.getInstance().getResources().getString(R.string.Error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {

            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        context = view.getContext();
        selectedPost = postList.get(position);

        menuLength = 4;
        if (selectedPost.rid == 0) {
            menuLength++;
        }
        CharSequence[] items = new CharSequence[menuLength];
        int i = 0;
        if (selectedPost.rid == 0) {
            items[i++] = App.getInstance().getResources().getString(R.string.Recommend_message);
        }
        String UName = selectedPost.user.uname;
        items[i++] = '@' + UName + " " + App.getInstance().getResources().getString(R.string.blog);
        items[i++] = App.getInstance().getResources().getString(R.string.Subscribe_to) + " @" + UName;
        items[i++] = App.getInstance().getResources().getString(R.string.Blacklist) + " @" + UName;
        items[i++] = App.getInstance().getResources().getString(R.string.Share);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(items, this);
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (selectedPost.rid != 0) {
            which += 1;
        }
        switch (which) {
            case 0:
                confirmAction(R.string.Are_you_sure_recommend, new Runnable() {

                    public void run() {
                        postMessage("! #" + selectedPost.mid, App.getInstance().getResources().getString(R.string.Recommended));
                    }
                });
                break;
            case 1:
                ((BaseActivity) context).replaceFragment(PostsPageFragment.newInstance(selectedPost.user.uid, selectedPost.user.uname, null, null, 0, false));
                break;
            case 2:
                confirmAction(R.string.Are_you_sure_subscribe, new Runnable() {

                    public void run() {
                        postMessage("S @" + selectedPost.user.uname, App.getInstance().getResources().getString(R.string.Subscribed));
                    }
                });
                break;
            case 3:
                confirmAction(R.string.Are_you_sure_blacklist, new Runnable() {

                    public void run() {
                        postMessage("BL @" + selectedPost.user.uname, App.getInstance().getResources().getString(R.string.Added_to_BL));
                    }
                });
                break;
            case 4:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, selectedPost.toString());
                context.startActivity(intent);
                break;
        }
    }
}
