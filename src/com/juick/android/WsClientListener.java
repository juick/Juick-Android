/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.juick.android;

import java.io.IOException;

/**
 *
 * @author ugnich
 */
public interface WsClientListener {

    public void onWebSocketTextFrame(String data) throws IOException;
}
