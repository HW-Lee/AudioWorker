package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.playback.PlaybackInfoFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.AudioController;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.ArrayList;
import java.util.Collection;

public class AudioPlaybackFragment extends AudioRxSupportFragment implements ControllerBase.ControllerStateListener {
    private final static String TAG = Constants.packageTag("AudioPlaybackFragment");

    private WorkerFunctionView mWorkerFunctionView;
    private LinearLayout mRxAuxViewContainer;
    private LinearLayout mRxInfoContainer;

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.audio_playback_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mActivityRef.get() == null)
            return;

        mActivityRef.get().getMainController().unregisterStateListener(this);
    }

    @Override
    public void initRxSupport() {
        if (mActivityRef.get() == null)
            return;

        mWorkerFunctionView = mActivityRef.get().findViewById(R.id.playback_func_attr_container);
        mRxAuxViewContainer = mActivityRef.get().findViewById(R.id.playback_aux_view_container);
        mRxInfoContainer = mActivityRef.get().findViewById(R.id.playback_info_container);

        mActivityRef.get().getMainController().registerStateListener(this);
    }

    @Override
    public LinearLayout getRxAuxViewContainer() {
        return mRxAuxViewContainer;
    }

    @Override
    public LinearLayout getRxInfoContainer() {
        return mRxInfoContainer;
    }

    @Override
    public String getRxInfoTitle() {
        return "Playback Info";
    }

    @Override
    public Object[] getRxReturns(WorkerFunction.Ack ack) {
        return ack.getReturns();
    }

    @Override
    public Collection<? extends String> getSupportedIntents() {
        ArrayList<String> supportIntents = new ArrayList<>();
        for (String intentAction : Constants.MasterInterface.INTENT_NAMES)
            if (Constants.getIntentOwner(intentAction).equals(Constants.INTENT_OWNER_PLAYBACK))
                supportIntents.add(intentAction);

        return supportIntents;
    }

    @Override
    public WorkerFunctionView getWorkerFunctionView() {
        return mWorkerFunctionView;
    }

    @Override
    public boolean needToShowAuxView(String action) {
        switch (action) {
            case Constants.MasterInterface.INTENT_PLAYBACK_START:
            case Constants.MasterInterface.INTENT_PLAYBACK_STOP:
                return true;
        }
        return false;
    }

    @Override
    public String getControllerName() {
        return Constants.Controllers.NAME_PLAYBACK;
    }

    @Override
    public WorkerFunction getInfoRequestFunction() {
        return new PlaybackInfoFunction();
    }

    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        super.onFunctionAckReceived(ack);

        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(getControllerName());
        TextView statusView = mActivityRef.get().findViewById(R.id.playback_status);
        if (statusView == null)
            return;

        if (controller instanceof AudioController.RxSupport && ((AudioController.RxSupport) controller).isRxRunning())
            statusView.setText("Status: running (" + ((AudioController.RxSupport) controller).getNumRxRunning() + " tracks)");
        else
            statusView.setText("Status: idle");
    }

    @Override
    public void onStateChanged(ControllerBase controller) {
        onFunctionAckReceived(null);
    }
}
