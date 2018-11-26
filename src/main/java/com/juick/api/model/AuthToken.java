package com.juick.api.model;

public class AuthToken {
    private String account;
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
