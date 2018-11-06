package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.juick.api.JuickDateConverter;
import com.juick.api.RestClient;
import com.stfalcon.chatkit.commons.models.IDialog;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by gerc on 11.03.2016.
 */
@JsonObject
public class Chat implements IDialog {

    @JsonField
    public String uname;
    @JsonField
    public int uid;
    @JsonField
    public int MessagesCount;
    @JsonField(typeConverter = JuickDateConverter.class)
    public Date lastMessageTimestamp;
    @JsonField
    public String lastMessageText;

    private Post lastMessage;

    @Override
    public String getId() {
        return String.valueOf(uid);
    }

    @Override
    public String getDialogPhoto() {
        return RestClient.getImagesUrl() + "a/" + uid + ".png";
    }

    @Override
    public String getDialogName() {
        return uname;
    }

    @Override
    public List<? extends IUser> getUsers() {
        return Collections.singletonList(User.newInstance(uid, uname));
    }

    @Override
    public IMessage getLastMessage() {
        if (lastMessage != null) {
            return lastMessage;
        }
        Post dummyPost = Post.empty();
        dummyPost.user = (User) getUsers().get(0);
        dummyPost.body = lastMessageText;
        dummyPost.timestamp = lastMessageTimestamp;
        return dummyPost;
    }

    @Override
    public void setLastMessage(IMessage message) {
        lastMessage = (Post)message;
    }

    @Override
    public int getUnreadCount() {
        return MessagesCount;
    }
}
