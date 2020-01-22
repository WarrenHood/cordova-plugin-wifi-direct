package com.android.plugins.wifidirect.library;

import java.net.Socket;
import java.net.*;
import java.util.Scanner;
import java.io.*;
import java.lang.Thread;
import java.io.InputStream;
import com.android.plugins.wifidirect.WifiDirect;
import android.util.Log;


public class Listener extends Thread {

    private boolean isConnected;
    private ServerSocket server;
    private Socket clientSocket;
    private boolean userDisconnected;
    private WifiDirect cordovaPlugin;

    public Listener(WifiDirect cp) {
        super();
        userDisconnected = false;
        cordovaPlugin = cp;
        clientSocket = null;
    }


    private class MessageReceiver extends Thread {
        @Override
        public void run() {
            while(isConnected && !userDisconnected){
                if(clientSocket == null) {
                    continue;
                }
                try{
                    int numRead;
                    String result = new String();

                    do {
                        char[] buf = new char[1000];
                        InputStream instream = clientSocket.getInputStream();
                        BufferedReader in = new BufferedReader(new InputStreamReader(instream));
                        numRead = in.read(buf, 0, 1000);
                        for(int b=0; b<numRead; b++) result += buf[b];
                        buf = new char[1000];
                    } while (numRead == 1000);

                    if(result.length() > 0){
                        if(result.equals("[NON-GO]")) {
                            String ip=(((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
                            WifiDirect.logError("THIS IS NOT AN ERROR. Setting up socket to client with remote: " +ip);
                            cordovaPlugin.getNode().setupPeerConnection(ip, true);
                        }

                        else {
                            WifiDirect.messages.add(result);
                        }

                    }
                }
                catch (Exception e) {
                    WifiDirect.logError("Error fetching message: " + e.getMessage() + "...");
                }
            }
        }

    }


    public void disconnect() {
        // Only call this when we want to permanently stop the connection
        isConnected = false;
        userDisconnected = true;
        try {
            clientSocket.close();
        }
        catch (Exception e) {
            WifiDirect.logError(e.getMessage());
        }
        try{
            server.close();
        }
        catch (Exception e) {
            WifiDirect.logError(e.getMessage());
        }

    }

    private boolean getNextClient() {
        try {
            clientSocket = server.accept();
            return true;
        }
        catch(Exception e) {
            // Failed to get next client

            // Uncomment the below line to show the error in the the chat (Don't forget to set the DEBUG_LOGS in the WifiDirect class to true if you want to do this...)
            // This applies to the rest of the WifiDirect.logError calls as well
            //WifiDirect.logError(e.getMessage());
            return false;
        }
    }

    private boolean getNextClientContinuously(int maxAttempts) {
        for(int i=0; i<maxAttempts; i++) {
            if(getNextClient()) return true;
        }
        //WifiDirect.logError("Failed to get new client after" + maxAttempts + " attempts");
        return false;
    }

    private void closeClientConnection() {
        try {
            clientSocket.close();
        }
        catch(Exception e) {
            // Error closing connection
        }
    }


    @Override
    public void run() {

        isConnected = true;
        try {
            server = new ServerSocket();

            /*
            * I set reuseAddress so that we can bind to the same port again
            * without worry about the timeouts. This probably can be
            * removed at some point..
            * */
            server.setReuseAddress(true);


            server.bind(new InetSocketAddress("0.0.0.0", 5336));
        }
        catch (Exception e){
            //WifiDirect.logError("Listener creation error: " + e.getMessage());
            isConnected = false;
        }


        /*
        * This makes a new thread to constantly receive messages from the current client
        * */
        new MessageReceiver().start();


        // This will keep running until the disconnect methid is called
        // Will not run if server failed to bind earlier
        while(isConnected && !userDisconnected){
            // Keep setting any new client to be the current client
            // since there will only be 1 peer at a time for now
            try {
                clientSocket = server.accept();
            }
            catch(Exception e) {
                // This will probably never happen unless server is null or something goes terribly wrong
                //WifiDirect.logError("Couldn't find client... (" + e.getMessage() + ")");
            }
        }

    }
}