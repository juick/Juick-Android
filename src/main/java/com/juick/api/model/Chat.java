package com.juick.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stfalcon.chatkit.commons.models.IDialog;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by gerc on 11.03.2016.
 */
public class Chat implements IDialog {

    private String uname;
    private int uid;
    private String avatar;
    private int MessagesCount;
    private Date lastMessageTimestamp;
    private String lastMessageText;

    private IMessage lastMessage;

    @Override
    public String getId() {
        return String.valueOf(uid);
    }

    @Override
    public String getDialogPhoto() {
        return avatar;
    }

    @Override
    public String getDialogName() {
        return uname;
    }

    @Override
    public List<? extends IUser> getUsers() {
        return Collections.singletonList(new User(uid, uname));
    }

    @Override
    public IMessage getLastMessage() {
        if (lastMessage != null) {
            return lastMessage;
        }
        Post dummyPost = Post.empty();
        dummyPost.setUser((User) getUsers().get(0));
        dummyPost.setBody(lastMessageText);
        dummyPost.setTimestamp(lastMessageTimestamp);
        return dummyPost;
    }

    @Override
    public void setLastMessage(IMessage message) {
        lastMessage = message;
    }

    @Override
    public int getUnreadCount() {
        return MessagesCount;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getMessagesCount() {
        return MessagesCount;
    }

    public void setMessagesCount(int messagesCount) {
        MessagesCount = messagesCount;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }
}
