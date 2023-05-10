package com.fongmi.android.tv.server;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ServerEvent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Server implements Nano.Listener {

    private Nano nano;
    private int port;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public Server() {
        this.port = 9978;
    }

    public String getAddress() {
        return getAddress(false);
    }

    public String getAddress(String path) {
        return getAddress(true) + "/" + path;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : getIP()) + ":" + port;
    }

    public void start() {
        if (nano != null) return;
        do {
            try {
                nano = new Nano(port);
                nano.setListener(this);
                nano.start();
                break;
            } catch (Exception e) {
                ++port;
                nano.stop();
                nano = null;
            }
        } while (port < 9999);
    }

    public void stop() {
        if (nano != null) {
            nano.stop();
            nano = null;
        }
    }

    private String getIP() {
        try {
            WifiManager manager = (WifiManager) App.get().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int address = manager.getConnectionInfo().getIpAddress();
            if (address != 0) return Formatter.formatIpAddress(address);
            return getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    private String getHostAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface interfaces = en.nextElement();
            for (Enumeration<InetAddress> addresses = interfaces.getInetAddresses(); addresses.hasMoreElements(); ) {
                InetAddress inetAddress = addresses.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return "";
    }

    @Override
    public void onSearch(String word) {
        if (word.length() > 0) ServerEvent.search(word);
    }

    @Override
    public void onPush(String url) {
        if (url.length() > 0) ServerEvent.push(url);
    }

    @Override
    public void onApi(String url) {
        if (url.length() > 0) ServerEvent.api(url);
    }

    @Override
    public void onCast(String device, String config, String history) {
        CastEvent.post(device, config, history);
    }
}
