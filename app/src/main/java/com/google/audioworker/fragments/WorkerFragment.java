package com.google.audioworker.fragments;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;

import com.google.audioworker.activities.MainActivity;
import com.google.audioworker.utils.Constants;

import java.lang.ref.WeakReference;

public class WorkerFragment extends Fragment {
    private final static String TAG = Constants.packageTag("WorkerFragment");

    protected WeakReference<MainActivity> mActivityRef;

    @CallSuper
    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof MainActivity)
            mActivityRef = new WeakReference<>((MainActivity) ctx);
    }
}
