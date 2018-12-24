package com.google.audioworker.utils.communicate.wifip2p;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import com.google.audioworker.activities.MainActivity;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicable;
import com.google.audioworker.utils.communicate.base.Communicator;
import com.google.audioworker.utils.communicate.base.ExchangeableThread;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class WifiCommunicator extends BroadcastReceiver implements
        Communicable<String, String>, WifiMessageManager.WifiMessengeListener,
        WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    private final static String TAG = Constants.packageTag("WifiCommunicator");

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WeakReference<MainActivity> mActivity;
    private ExchangeListener<String, String> mListener;
    private IntentFilter mIntentFilter;
    private String mSerial;
    private HashMap<String, WifiP2pDevice> mPeers;

    private Method setDeviceNameMethod;

    private ExchangeableThread<String, String> mRunningThread;

    private boolean isCommEnabled = false;

    public WifiCommunicator(MainActivity activity, ExchangeListener<String, String> l) {
        super();
        mActivity = new WeakReference<>(activity);
        mListener = l;

        if (mActivity.get() == null) {
            return;
        }

        init();
    }

    @SuppressLint("MissingPermission")
    private void init() {
        mWifiP2pManager = (WifiP2pManager) mActivity.get().getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mActivity.get(), mActivity.get().getMainLooper(), null);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);

        mActivity.get().registerReceiver(this, mIntentFilter);
        mSerial = Build.getSerial();
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "failed to discover peers (" + reason + ")");
            }
        });

        try {
             setDeviceNameMethod = mWifiP2pManager.getClass().getMethod(
                    "setDeviceName",
                    WifiP2pManager.Channel.class, String.class,
                    WifiP2pManager.ActionListener.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            setDeviceNameMethod = null;
        }

        mPeers = new HashMap<>();
    }

    @Override
    public void refreshPeers() {
        mWifiP2pManager.discoverPeers(mChannel, null);
    }

    @Override
    public void notifyOnPause() {
        if (mActivity.get() != null) {
            mActivity.get().unregisterReceiver(this);
        }
    }

    @Override
    public void notifyOnResume() {
        if (mActivity.get() != null) {
            mActivity.get().registerReceiver(this, mIntentFilter);
            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "failed to discover peers (" + reason + ")");
                }
            });
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "get action: " + action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "The communcation is enabled");
                isCommEnabled = true;
            } else {
                Log.d(TAG, "The communication is disabled");
                isCommEnabled = false;
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (info.isConnected()) {
                Log.d(TAG, "the network is connected, get the information");
                mWifiP2pManager.requestConnectionInfo(mChannel, this);
            } else {
                Log.w(TAG, "the network is disconnected");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice d = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "this device: " + d.deviceName + "(" + d.deviceAddress + ")");
            Log.d(TAG, "" + d);
            if (setDeviceNameMethod != null && !d.deviceName.equals(mSerial)) {
                try {
                    setDeviceNameMethod.invoke(mWifiP2pManager, mChannel, mSerial, null);
                    Log.d(TAG, "succeed to change the device name to " + mSerial);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mActivity.get() != null) {
                mActivity.get().updateThisDeviceInfo(d.deviceName + "[" + d.deviceAddress + "]");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "now trying to request peers");
            mWifiP2pManager.requestPeers(mChannel, this);
        }
    }

    // WifiP2pManager.PeerListListener
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.d(TAG, "got " + peers.getDeviceList().size() + " peer(s)");
        updatePeerList(peers.getDeviceList());
    }

    private void updatePeerList(Collection<WifiP2pDevice> peers) {
        synchronized (this) {
            mPeers.clear();
            int connected_count = 0;
            for (WifiP2pDevice d : peers) {
                Log.d(TAG, "get peer: " + d.deviceName + "(" + d.deviceAddress + ")");
                Log.d(TAG, "" + d);
                mPeers.put(d.deviceName, d);
                if (d.status == WifiP2pDevice.CONNECTED)
                    connected_count++;
            }
            if (mActivity.get() != null) {
                ArrayList<Communicator.PeerInfo> peerInfo = new ArrayList<>(10);
                for (String name : mPeers.keySet()) {
                    Communicator.PeerInfo info = new Communicator.PeerInfo();
                    WifiP2pDevice device = mPeers.get(name);
                    if (device == null)
                        continue;
                    info.name = name;
                    info.status = getStatus(device.status);
                    if (device.status == WifiP2pDevice.CONNECTED && !isConnected(info.name)) {
                        info.status = getStatus(WifiP2pDevice.FAILED);
                    }
                    peerInfo.add(info);
                }
                mActivity.get().onPeerInfoUpdated(peerInfo);
            }

            if (connected_count == 0 && mRunningThread != null) {
            }
        }
    }

    private String getStatus(int code) {
        switch (code) {
            case 0:
                return "connected";
            case 1:
                return "invited";
            case 2:
                return "failed";
            case 3:
                return "available";
            case 4:
                return "unavailable";
            default:
                return "unknown";
        }
    }

    // WifiP2pManager.ConnectionInfoListener
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "got info:");
        Log.d(TAG, "" + info);
        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                if (mRunningThread == null) {
                    mRunningThread = new WifiMessageServer(this);
                    mRunningThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (info.groupFormed) {
            Log.d(TAG, "Connected as peer");
            try {
                mRunningThread = new WifiMessageClient(this, info.groupOwnerAddress);
                mRunningThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "The group has not been formed");
        }
    }

    @Override
    public void connectTo(final String name) {
        _connectTo(name, 15);
    }

    public void _connectTo(final String name, int groupOwnerIntent) {
        if (!mPeers.containsKey(name) || mPeers.get(name) == null)
            return;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = Objects.requireNonNull(mPeers.get(name)).deviceAddress;
        config.groupOwnerIntent = groupOwnerIntent;
        mWifiP2pManager.cancelConnect(mChannel, null);
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "succeed to connect peer '" + name + "'");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "failed to connect peer '" + name + "' (" + reason + ")");
            }
        });
    }

    @Override
    public boolean isConnected(String name) {
        synchronized (this) {
            if (!mPeers.containsKey(name))
                return false;
            if (!(WifiP2pDevice.CONNECTED == Objects.requireNonNull(mPeers.get(name)).status))
                return false;
            if (mRunningThread != null)
                return mRunningThread.isConnected();
            return true;
        }
    }

    @Override
    public Collection<String> getConnected() {
        ArrayList<String> connected = new ArrayList<>(10);
        for (String name : mPeers.keySet()) {
            if (isConnected(name))
                connected.add(name);
        }
        return connected;
    }

    @Override
    public void postSendData(int ret, String msg) {
        Log.d(TAG, "postSendData('" + msg + "') returns " + ret);
        if (mListener != null)
            mListener.postSendData(ret, msg);
    }

    @Override
    public void onDataReceived(WifiMessageManager from, String msg) {
        Log.d(TAG, "onDataReceived('" + msg + "') from " + from);
        if (mListener != null)
            mListener.onDataReceived(from.getRemoteName(), msg);
    }

    @Override
    public void send(final String to, final String msg) {
        if (!isConnected()) {
            Log.w(TAG, "The communicator has not connected to any peer");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mRunningThread.send(to, msg);
            }
        }).start();
    }

    @Override
    public boolean isConnected() {
        for (String name : getConnected()) {
            if (isConnected(name))
                return true;
        }
        return false;
    }
}
