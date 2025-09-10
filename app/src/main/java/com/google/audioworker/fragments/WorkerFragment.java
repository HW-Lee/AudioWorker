package com.google.audioworker.fragments;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.View;

import com.google.audioworker.activities.MainActivity;
import com.google.audioworker.utils.Constants;

import java.lang.ref.WeakReference;

public abstract class WorkerFragment extends Fragment {
    private static final String TAG = Constants.packageTag("WorkerFragment");

    protected WeakReference<MainActivity> mActivityRef;

    protected abstract View getContainerView();

    @CallSuper
    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof MainActivity) mActivityRef = new WeakReference<>((MainActivity) ctx);
    }

    protected final int getPxByDp(float dp) {
        return (int)
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
