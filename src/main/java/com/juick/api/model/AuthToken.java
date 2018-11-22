package com.juick.api.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class AuthToken {
    @JsonField
    private String account;
    @JsonField
    private String authCode;

    public String getAccount() {
        return account;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
}
