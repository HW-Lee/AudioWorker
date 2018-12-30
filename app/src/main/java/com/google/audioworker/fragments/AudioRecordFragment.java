package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.functions.controllers.RecordController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.ArrayList;
import java.util.List;

public class AudioRecordFragment extends WorkerFragment
        implements RecordController.RecordRunnable.RecordDataListener, ControllerBase.ControllerStateListener {
    private final static String TAG = Constants.packageTag("AudioRecordFragment");

    private ArrayList<String> mSupportIntents;
    private DataView[] mSignalViews;

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.audio_record_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(Constants.Controllers.NAME_RECORD);
        controller.unregisterStateListener(this);
        if (controller instanceof RecordController)
            ((RecordController) controller).unregisterDataListener(this);
    }

    private void init() {
        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(Constants.Controllers.NAME_RECORD);
        controller.registerStateListener(this);
        if (controller instanceof RecordController)
            ((RecordController) controller).registerDataListener(this);

        mSupportIntents = new ArrayList<>();
        for (String intentAction : Constants.MasterInterface.INTENT_NAMES)
            if (Constants.getIntentOwner(intentAction).equals(Constants.INTENT_OWNER_RECORD))
                mSupportIntents.add(intentAction);

        if (controller instanceof RecordController && ((RecordController) controller).isRecording())
            ((TextView) mActivityRef.get().findViewById(R.id.record_status)).setText("status: running");
        else
            ((TextView) mActivityRef.get().findViewById(R.id.record_status)).setText("status: idle");

        ((WorkerFunctionView) mActivityRef.get().findViewById(R.id.record_func_attr_container)).setSupportedIntentActions(mSupportIntents);
        ((WorkerFunctionView) mActivityRef.get().findViewById(R.id.record_func_attr_container)).setController(controller);

        mSignalViews = new DataView[2];
        mSignalViews[0] = mActivityRef.get().findViewById(R.id.record_signal_1);
        mSignalViews[1] = mActivityRef.get().findViewById(R.id.record_signal_2);
        for (DataView v : mSignalViews) {
            v.setGridSlotsY(4);
            v.setGridSlotsX(10);
        }
    }

    @Override
    public void onDataUpdated(List<? extends Double>[] signal, RecordStartFunction function) {
        for (int i = 0; i < signal.length; i++) {
            mSignalViews[i].plot(signal[i]);
        }
    }

    @Override
    public void onStateChanged(ControllerBase controller) {
        if (!(controller instanceof RecordController))
            return;

        if (((RecordController) controller).isRecording()) {
            ((TextView) mActivityRef.get().findViewById(R.id.record_status)).setText("status: running");
        } else {
            ((TextView) mActivityRef.get().findViewById(R.id.record_status)).setText("status: idle");
            for (DataView v : mSignalViews) {
                v.reset();
            }
        }
    }
}
