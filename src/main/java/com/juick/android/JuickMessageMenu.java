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
package com.juick.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.juick.App;
import com.juick.R;
import com.juick.api.model.Post;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Ugnich Anton
 */
public class JuickMessageMenu implements OnClickListener, JuickMessagesAdapter.OnItemClickListener {
    private final static int MENU_ACTION_RECOMMEND = 1;
    private final static int MENU_ACTION_BLOG = 2;
    private final static int MENU_ACTION_SUBSCRIBE = 3;
    private final static int MENU_ACTION_BLACKLIST = 4;
    private final static int MENU_ACTION_SHARE = 5;
    private final static int MENU_ACTION_DELETE_POST = 6;
    private final static int MENU_ACTION_SOME_LAST_CMD = 7;

    private Context context;
    private final List<Post> postList;
    private Post selectedPost;
    private final int[] currentActions = new int[MENU_ACTION_SOME_LAST_CMD];

    public JuickMessageMenu(List<Post> postList) {
        this.postList = postList;
    }

    private void confirmAction(final int resId, final Runnable action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(App.getInstance().getResources().getString(resId));
        builder.setPositiveButton(R.string.Yes, (dialog, which) -> action.run());
        builder.setNegativeButton(R.string.Cancel, null);
        builder.show();
    }

    private void postMessage(final String body, final String ok) {
        postMessage(body, ok, false);
    }
    private void postMessage(final String body, final String ok, final boolean isReload) {
        App.getInstance().getApi().post(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Toast.makeText(App.getInstance(), (response.isSuccessful()) ? ok : App.getInstance().getResources().getString(R.string.Error), Toast.LENGTH_SHORT).show();
                /*if(isReload)
                    reloadView();*/
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                /*if(isReload)
                    reloadView();*/

                Toast.makeText(App.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        context = view.getContext();
        selectedPost = postList.get(position);
        CharSequence[] items;
        int menuLength;
        if (Utils.getMyId() == 0) {
            menuLength = 2;
            items = Arrays.asList(
                    '@' + selectedPost.getUser().getUname() + " " + context.getString(R.string.blog),
                    context.getString(R.string.Share)
            ).toArray(new CharSequence[0]);
            currentActions[0] = MENU_ACTION_BLOG;
            currentActions[1] = MENU_ACTION_SHARE;
        } else if (selectedPost.getUser().getUid() == Utils.getMyId()) {
            menuLength = 1;
            items = new CharSequence[menuLength];
            items[0] = selectedPost.getRid() == 0 ? context.getString(R.string.DeletePost) : context.getString(R.string.DeleteComment);
            currentActions[0] = MENU_ACTION_DELETE_POST;
        } else {
            menuLength = 4;
            if (selectedPost.getRid() == 0) {
                menuLength++;
            }
            items = new CharSequence[menuLength];

            int i = 0;
            if (selectedPost.getRid() == 0) {
                items[i++] = context.getString(R.string.Recommend_message);
                currentActions[i - 1] = MENU_ACTION_RECOMMEND;
            }
            String UName = selectedPost.getUser().getUname();
            items[i++] = '@' + UName + " " + context.getString(R.string.blog);
            currentActions[i - 1] = MENU_ACTION_BLOG;
            items[i++] = App.getInstance().getResources().getString(R.string.Subscribe_to) + " @" + UName;
            currentActions[i - 1] = MENU_ACTION_SUBSCRIBE;
            items[i++] = App.getInstance().getResources().getString(R.string.Blacklist) + " @" + UName;
            currentActions[i - 1] = MENU_ACTION_BLACKLIST;
            items[i++] = App.getInstance().getResources().getString(R.string.Share);
            currentActions[i - 1] = MENU_ACTION_SHARE;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(items, this);
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int action = currentActions[which];
        switch (action) {
            case MENU_ACTION_RECOMMEND:
                confirmAction(R.string.Are_you_sure_recommend,
                        () -> postMessage("! #" + selectedPost.getMid(), context.getString(R.string.Recommended)));
                break;
            case MENU_ACTION_BLOG:
                ((Activity)context).setTitle(selectedPost.getUser().getUname());
               /* ((BaseActivity) context).replaceFragment(
                        FeedBuilder.feedFor(
                                UrlBuilder.getUserPostsByName(selectedPost.getUser().getUname())
                        )
                );*/
                break;
            case MENU_ACTION_SUBSCRIBE:
                confirmAction(R.string.Are_you_sure_subscribe,
                        () -> postMessage("S @" + selectedPost.getUser().getUname(), context.getString(R.string.Subscribed)));
                break;
            case MENU_ACTION_BLACKLIST:
                confirmAction(R.string.Are_you_sure_blacklist,
                        () -> postMessage("BL @" + selectedPost.getUser().getUname(), context.getString(R.string.Added_to_BL)));
                break;
            case MENU_ACTION_DELETE_POST:
                confirmAction(R.string.Are_you_sure_delete, () -> postMessage("D #" +
                        (selectedPost.getRid() == 0 ?
                                String.valueOf(selectedPost.getMid()) :
                                String.format("%s/%s", selectedPost.getMid(), selectedPost.getRid())),
                        context.getString(R.string.Deleted), true));
                break;
            case MENU_ACTION_SHARE:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "https://juick.com/" + selectedPost.getMid());
                context.startActivity(intent);
                break;
        }
    }
}
