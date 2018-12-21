package com.google.audioworker.utils.communicate.wifip2p;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.ExchangeableThread;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WifiMessageClient extends ExchangeableThread<String, String> {
    private final static String TAG = Constants.packageTag("WifiMessengerClient");

    private InetAddress mServerAddr;
    private WifiMessageManager.WifiMessengeListener mListener;
    private WifiMessageManager mManager;

    public WifiMessageClient(WifiMessageManager.WifiMessengeListener l, InetAddress serverAddr)
            throws IOException, IllegalArgumentException {
        if (l == null)
            throw(new IllegalArgumentException("The listener must not be null"));

        mListener = l;
        mServerAddr = serverAddr;
    }

    public void tryStop() {
        mManager.tryStop();
    }

    @Override
    public void send(String to, String msg) {
        mManager.write(msg);
    }

    @Override
    public boolean isConnected() {
        if (mManager == null)
            return false;
        return mManager.getSocket().isConnected();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mServerAddr.getHostAddress(),
                    Constants.WIFIP2PConstants.SERVER_PORT), 30000);
            Log.d(TAG, "Launching the I/O handler");
            mManager = new WifiMessageManager(socket, mListener);
            new Thread(mManager).start();

            Log.d(TAG, "send SN '" + Build.getSerial() + "'");
            CommandHelper.Command nameInfo = new CommandHelper.Command();
            try {
                nameInfo.put(Constants.MessageSpecification.COMMAND_TAG_NAME, Build.getSerial());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mManager.write(nameInfo.toString());
        } catch (IOException e) {
            Log.e(TAG, "error: " + e);
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
