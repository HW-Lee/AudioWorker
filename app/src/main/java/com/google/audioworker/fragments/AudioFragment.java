package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.audioworker.R;
import com.google.audioworker.utils.Constants;

public class AudioFragment extends WorkerFragment {
    private final static String TAG = Constants.packageTag("AudioFragment");

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.audio_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mActivityRef.get() == null)
            return;

        FragmentTabHost tabHost = mActivityRef.get().findViewById(R.id.audio_tab_host);
        tabHost.setup(mActivityRef.get(), getFragmentManager(), R.layout.audio_fragment);
        for (Constants.Fragments.FragmentInfo info : Constants.Fragments.Audio.FRAGMENT_INFOS) {
            tabHost.addTab(tabHost.newTabSpec(info.spec).setIndicator(info.label), info.classTarget, null);
        }
    }
}
