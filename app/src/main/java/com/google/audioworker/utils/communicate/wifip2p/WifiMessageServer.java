package com.google.audioworker.utils.communicate.wifip2p;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.ExchangeableThread;

import org.json.JSONException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WifiMessageServer extends ExchangeableThread<String, String> {
    private final static String TAG = Constants.packageTag("WifiMessengerServer");

    private ServerSocket mSocket;
    private WifiMessageManager.WifiMessengeListener mListener;
    private ArrayList<WifiMessageManager> mMessengeManagers;

    private boolean exitPending;

    private final ThreadPoolExecutor mPool =
            new ThreadPoolExecutor(Constants.WIFIP2PConstants.MAX_THREAD_COUNT,
                    Constants.WIFIP2PConstants.MAX_THREAD_COUNT,
                    Constants.WIFIP2PConstants.KEEP_ALIVE_TIME_SECONDS,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public WifiMessageServer(WifiMessageManager.WifiMessengeListener l)
            throws IOException, IllegalArgumentException {
        if (l == null)
            throw(new IllegalArgumentException("The listener must not be null"));

        mSocket = new ServerSocket(Constants.WIFIP2PConstants.SERVER_PORT);
        mListener = l;
        mMessengeManagers = new ArrayList<>();
    }

    public void tryStop() {
        exitPending = true;
    }

    @Override
    public void send(String to, String msg) {
        if (to == null)
            return;

        boolean success = false;
        for (WifiMessageManager manager : mMessengeManagers) {
            if (to.equals(manager.getRemoteName())) {
                manager.write(msg);
                success = true;
                break;
            }
        }

        if (success)
            Log.d(TAG, "successfully send '" + msg + "' to '" + to + "'");
        else
            Log.w(TAG, "the target '" + to + "' is not in the connected list");
    }

    @Override
    public boolean isConnected() {
        return mMessengeManagers.size() > 0;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        exitPending = false;
        while (!exitPending) {
            try {
                WifiMessageManager manager = new WifiMessageManager(mSocket.accept(), mListener);
                mPool.execute(manager);
                mMessengeManagers.add(manager);
                Log.d(TAG, "Launching the I/O handler");

                Log.d(TAG, "send SN '" + Build.getSerial() + "'");
                CommandHelper.Command nameInfo = new CommandHelper.Command();
                try {
                    nameInfo.put(Constants.MessageSpecification.COMMAND_TAG_NAME, Build.getSerial());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                manager.write(nameInfo.toString());
            } catch (IOException e) {
                Log.e(TAG, "error: " + e);
                e.printStackTrace();
                exitPending = true;
            }
        }
        Log.i(TAG, "terminate " + this);
        try {
            if (mSocket != null && !mSocket.isClosed())
                mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPool.shutdownNow();
    }
}
