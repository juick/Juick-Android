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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.http.util.ByteArrayBuffer;

/**
 *
 * @author Ugnich Anton
 */
public class WsClient {

    static final byte keepAlive[] = {(byte) 0x81, (byte) 0x01, (byte) 0x20};
    static final byte closeConnection[] = {(byte) 0x88, (byte) 0x00};
    Socket sock;
    InputStream is;
    OutputStream os;
    WsClientListener listener = null;

    public WsClient() {
    }

    public void setListener(WsClientListener listener) {
        this.listener = listener;
    }

    public boolean connect(String host, int port, String location, String headers) {
        try {
            sock = new Socket(host, port);
            is = sock.getInputStream();
            os = sock.getOutputStream();

            String handshake = "GET " + location + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Origin: http://juick.com/\r\n"
                    + "User-Agent: JuickAndroid\r\n"
                    + "Sec-WebSocket-Key: SomeKey\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "Pragma: no-cache\r\n"
                    + "Cache-Control: no-cache\r\n";
            if (headers != null) {
                handshake += headers;
            }
            handshake += "\r\n";
            os.write(handshake.getBytes());

            return true;
        } catch (Exception e) {
            System.err.println(e);
            //e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        return sock.isConnected();
    }

    public void readLoop() {
        try {
            int b;
            ByteArrayBuffer buf = new ByteArrayBuffer(16);
            boolean flagInside = false;
            int byteCnt = 0;
            int PacketLength = 0;
            boolean bigPacket = false;
            while ((b = is.read()) != -1) {
                if (flagInside) {
                    byteCnt++;

                    if (byteCnt == 1) {
                        if (b < 126) {
                            PacketLength = b + 1;
                            bigPacket = false;
                        } else {
                            bigPacket = true;
                        }
                    } else {
                        if (byteCnt == 2 && bigPacket) {
                            PacketLength = b << 8;
                        }
                        if (byteCnt == 3 && bigPacket) {
                            PacketLength |= b;
                            PacketLength += 3;
                        }

                        if (byteCnt > 3 || !bigPacket) {
                            buf.append((char) b);
                        }
                    }

                    if (byteCnt == PacketLength && listener != null) {
                        if (PacketLength > 2) {
                            listener.onWebSocketTextFrame(new String(buf.toByteArray(), "utf-8"));
                        } else {
                            os.write(keepAlive);
                            os.flush();
                        }
                        flagInside = false;
                    }
                } else if (b == 0x81) {
                    buf.clear();
                    flagInside = true;
                    byteCnt = 0;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void disconnect() {
        try {
            os.write(closeConnection);
            os.flush();
            is.close();
            os.close();
            sock.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
