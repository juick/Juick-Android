package com.juick.api.model;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Created by gerc on 11.03.2016.
 */
public class Pms {

    private List<Chat> pms;

    @NonNull
    public List<Chat> getPms() {
        return pms;
    }

    public void setPms(List<Chat> pms) {
        this.pms = pms;
    }
}
