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

import com.google.audioworker.R;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.utils.Constants;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity
        implements CommandHelper.BroadcastHandler.FunctionReceivedListener, WorkerFunction.WorkerFunctionListener {
    private final static String TAG = Constants.packageTag("MainActivity");

    private FragmentTabHost mTabHost;

    private MainController mMainController;
    private CommandHelper.BroadcastHandler mBroadcastReceiver;

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
    }


    @Override
    protected void onPause() {
        super.onPause();
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
        mBroadcastReceiver = CommandHelper.BroadcastHandler.registerReceiver(this, this);
        mMainController = new MainController();
        mMainController.activate(this);
    }

    public MainController getMainController() {
        return mMainController;
    }

    private Fragment getFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
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
            if (!sender.contains("::")) {
                Log.w(TAG, "invalid target name: " + sender);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleFragment() {
    }
}
