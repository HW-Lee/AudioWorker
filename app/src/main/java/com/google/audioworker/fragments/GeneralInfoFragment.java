package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.audioworker.utils.Constants;

public class GeneralInfoFragment extends WorkerFragment {
    private static final String TAG = Constants.packageTag("GeneralInfoFragment");

    @Override
    protected View getContainerView() {
        return getView();
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
