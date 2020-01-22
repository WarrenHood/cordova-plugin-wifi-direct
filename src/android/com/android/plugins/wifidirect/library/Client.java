package com.android.plugins.wifidirect.library;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;
import android.util.Log;

import com.android.plugins.wifidirect.WifiDirect;

public class Client extends Thread {
    private String ip;
    private int port;
    private boolean isConnected;
    private boolean userDisconnected;
    private boolean isGroupOwner;
    private Socket client;

    public Client(String ip, int port, boolean isGO) {
        super();
        this.ip = ip;
        this.port = port;
        userDisconnected = false;
        isGroupOwner = isGO;
    }


    public void sendMessage(final String msg) {
        // Attempts to send a message to the server, up to 5 times... Just in case

        for(int i=0; i<5; i++){
            try {
                OutputStream out = client.getOutputStream();
                out.write(msg.getBytes());
                return;
            }
            catch (Exception e) {
                //WifiDirect.logError("Failed to send msg: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        // This should be called whenrver
        userDisconnected = true;
        try{
            isConnected = false;
            client.close();
        }
        catch (Exception e) {
            WifiDirect.logError(e.getMessage());
        }
    }

    private boolean connectToServer() {
        try {
            client = new Socket();
            //WifiDirect.logError("Attempting to connect to server at " + ip + ":" + port );
            client.connect(new InetSocketAddress(ip, port), 0);
            //WifiDirect.logError("Successfully connected to server at " + ip + ":" + port );
            return true;
        }
        catch(Exception e) {
            //WifiDirect.logError("Couldn't connect to server at " + ip + ":" + port + "(" + e.getMessage() + ")");
            return false;
        }
    }

    private boolean connectToServerContinuously(int maxAttempts) {
        for(int i=0; i<maxAttempts; i++) {
            if(connectToServer()) return true;
            //WifiDirect.logError("Failed to connect to server after " + i+1 + " attempts");
        }
        //WifiDirect.logError("Failed to connect to server after " + maxAttempts + " attempts... Aborting");
        return false;
    }


    @Override
    public void run() {
        while(!connectToServer());
        if(!isGroupOwner) {
            // Let the server know to make a connection to us
            sendMessage("[NON-GO]");
        }
    }

}