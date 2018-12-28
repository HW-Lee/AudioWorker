package com.google.audioworker.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.audioworker.R;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicable;
import com.google.audioworker.utils.communicate.base.Communicator;
import com.google.audioworker.utils.communicate.base.Exchangeable;
import com.google.audioworker.utils.communicate.wifip2p.WifiCommunicator;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity
        implements Exchangeable.ExchangeListener<String, String>, Communicator.CommunicatorListener,
            CommandHelper.BroadcastHandler.FunctionReceivedListener, WorkerFunction.WorkerFunctionListener {
    private final static String TAG = Constants.packageTag("MainActivity");

    private Communicable<String, String> mCommunicator;

    private FragmentTabHost mTabHost;

    private MainController mMainController;
    private CommandHelper.BroadcastHandler mBroadcastReceiver;

    private final ArrayList<Communicator.PeerInfo> mPeers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermissionCheck();
        initUI();
        initControllers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCommunicator.notifyOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.notifyOnPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommandHelper.BroadcastHandler.unregisterReceiver(this, mBroadcastReceiver);
        mMainController.destroy();
    }

    private void initPermissionCheck() {
        for (String permission : Constants.PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS_REQUIRED, 1);
                break;
            }
        }
    }

    private void initUI() {
        mTabHost = findViewById(R.id.tab_host);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.fragment_container);

        for (Constants.Fragments.FragmentInfo info : Constants.Fragments.FRAGMENT_INFOS) {
            mTabHost.addTab(mTabHost.newTabSpec(info.spec).setIndicator(info.label, null), info.classTarget, null);
        }

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentStarted(fm, f);
                handleFragment();
            }

            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                handleFragment();
            }
        }, true);
    }

    private void initControllers() {
        mCommunicator = new WifiCommunicator(this, this);
        mBroadcastReceiver = CommandHelper.BroadcastHandler.registerReceiver(this, this);
        mBroadcastReceiver.registerCommuncator(mCommunicator);
        mMainController = new MainController();
        mMainController.activate(this);
    }

    public Communicable<String, String> getCommunicator() {
        return mCommunicator;
    }

    private Fragment getFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    public void postSendData(int ret, String msg) {
        Log.d(TAG, "postSendData('" + msg + "') returns " + ret);
        WorkerFunction function = CommandHelper.getFunction(msg);
        if (function != null) {
            mMainController.addRequestedFunction(function);
        }
    }

    @Override
    public void onDataReceived(final String from, final String msg) {
        Log.d(TAG, "onDataReceived('" + msg + "') from " + from);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "got msg '" + msg + "' from '" + from + "'", Toast.LENGTH_SHORT).show();
            }
        });

        WorkerFunction function = CommandHelper.getFunction(msg);
        if (function != null) {
            this.onFunctionReceived(function);
            return;
        }

        WorkerFunction.Ack ack = WorkerFunction.Ack.parseAck(msg);
        if (ack != null) {
            Log.d(TAG, "send ack to controller: " + ack);
            mMainController.receiveAck(ack);
        }
    }

    @Override
    public void onFunctionReceived(WorkerFunction function) {
        Log.d(TAG, "onFunctionReceived(" + function + ")");
        mMainController.execute(function, this);
    }

    @Override
    public void onAckReceived(WorkerFunction.Ack ack) {
        Log.d(TAG, "onAckReceived(" + ack + ")");
        String sender;
        try {
            sender = ack.getString(Constants.MessageSpecification.COMMAND_ACK_TARGET);
            if (sender.contains("::")) {
                sender = sender.split("::")[0];
                mCommunicator.send(sender, ack.toString());
            } else {
                Log.w(TAG, "invalid target name: " + sender);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPeerInfoUpdated(Collection<Communicator.PeerInfo> peers) {
        synchronized (mPeers) {
            mPeers.clear();
            mPeers.addAll(peers);
        }
        notifyPeerInfoUpdated();
    }

    private void notifyPeerInfoUpdated() {
        if (getFragment() instanceof Communicator.CommunicatorListener) {
            ((Communicator.CommunicatorListener) getFragment()).onPeerInfoUpdated(mPeers);
        }
    }

    private void handleFragment() {
        notifyPeerInfoUpdated();
    }
}
