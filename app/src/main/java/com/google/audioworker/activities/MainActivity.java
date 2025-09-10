package com.google.audioworker.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.audioworker.R;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.services.AudioWorkerService;
import com.google.audioworker.utils.Constants;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = Constants.packageTag("MainActivity");

    public interface ControllerReadyListener {
        void onControllerReady(MainController mainController);
    }

    private FragmentTabHost mTabHost;

    private MainController mMainController;
    private AudioWorkerService mAudioWorkerService;
    private boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermissionCheck();
        initUI();
        initVersion();

        Intent intent = new Intent(this, AudioWorkerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }

    private final ServiceConnection serviceConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    AudioWorkerService.AudioWorkerBinder binder =
                            (AudioWorkerService.AudioWorkerBinder) service;
                    mAudioWorkerService = binder.getService();
                    mMainController = mAudioWorkerService.getMainController();
                    mIsBound = true;

                    // Notify all attached fragments that the controller is ready.
                    // This handles the initial startup race condition.
                    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                        if (fragment instanceof ControllerReadyListener) {
                            ((ControllerReadyListener) fragment).onControllerReady(mMainController);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                    mIsBound = false;
                }
            };

    private void initPermissionCheck() {
        for (String permission : Constants.PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS_REQUIRED, 1);
                break;
            }
        }
    }

    private void initUI() {
        mTabHost = findViewById(R.id.tab_host);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.fragment_container);

        for (Constants.Fragments.FragmentInfo info : Constants.Fragments.FRAGMENT_INFOS) {
            // noinspection deprecation
            mTabHost.addTab(
                    // This usage of setIndicator is deprecated. After evaluation, the decision
                    // was made to keep the legacy FragmentTabHost for simplicity. The modern
                    // alternative involves a larger refactor to ViewPager2 and TabLayout.
                    mTabHost.newTabSpec(info.spec).setIndicator(info.label, null),
                    info.classTarget,
                    null);
        }

        getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(
                        new FragmentManager.FragmentLifecycleCallbacks() {
                            @Override
                            public void onFragmentResumed(
                                    @NonNull FragmentManager fm, @NonNull Fragment f) {
                                super.onFragmentResumed(fm, f);
                                // When a fragment is resumed (e.g. switching tabs),
                                // check if the controller is already available and notify it.
                                if (mIsBound && mMainController != null) {
                                    if (f instanceof ControllerReadyListener) {
                                        ((ControllerReadyListener) f)
                                                .onControllerReady(mMainController);
                                    }
                                }
                            }
                        },
                        true);
    }

    public MainController getMainController() {
        return mMainController;
    }

    private Fragment getFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void handleFragment() {}

    private void initVersion() {
        try {
            String fullVersion =
                    getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0))
                            .versionName;
            String displayVersion = fullVersion;
            int lastVIndex = fullVersion.lastIndexOf("-");
            if (lastVIndex != -1) {
                displayVersion = fullVersion.substring(lastVIndex + 1);
            }
            setTitle(getString(R.string.app_name_with_version, displayVersion));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
