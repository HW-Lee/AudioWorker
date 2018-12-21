package com.google.audioworker.utils.communicate.wifip2p;

import android.util.Log;

import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Exchangeable;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class WifiMessageManager implements Runnable {
    private final static String TAG = Constants.packageTag("WifiMessengeManager");

    interface WifiMessengeListener extends Exchangeable.ExchangeListener<WifiMessageManager, String> {
    }

    private String mRemoteName;
    private Socket mSocket;
    private WifiMessengeListener mListener;
    private InputStream mIs;
    private OutputStream mOs;

    private boolean exitPending;

    public WifiMessageManager(Socket socket, WifiMessengeListener l) {
        mSocket = socket;
        mListener = l;
        mRemoteName = null;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void tryStop() {
        exitPending = true;
    }

    @Override
    public void run() {
        exitPending = false;
        try {
            mIs = mSocket.getInputStream();
            mOs = mSocket.getOutputStream();

            byte[] buffer = new byte[Constants.WIFIP2PConstants.MSG_BUFFER_SIZE];
            int num_bytes;

            while (!exitPending) {
                num_bytes = mIs.read(buffer);
                if (num_bytes == -1) {
                    Log.d(TAG, "no byte to be read");
                    exitPending = true;
                    continue;
                }

                String msg = new String(Arrays.copyOfRange(buffer, 0, num_bytes));
                Log.d(TAG, "Receive: '" + msg + "'");
                try {
                    CommandHelper.Command cmd = new CommandHelper.Command(msg);
                    if (cmd.has(Constants.MessageSpecification.COMMAND_TAG_NAME)) {
                        mRemoteName = cmd.getString(Constants.MessageSpecification.COMMAND_TAG_NAME);
                        Log.d(TAG, "Manager(" + this + ") registers name '" + mRemoteName + "'");
                        continue; // no callback
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mListener.onDataReceived(this, msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void write(String msg) {
        try {
            mOs.write(msg.getBytes());
            mListener.postSendData(0, msg);
        } catch (IOException e) {
            Log.e(TAG, "write data '" + msg + "' failed");
            mListener.postSendData(-1, msg);
        }
    }

    public String getRemoteName() {
        return mRemoteName;
    }
}
