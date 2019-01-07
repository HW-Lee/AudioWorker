package com.google.audioworker.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.record.RecordInfoFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.AudioController;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.ArrayList;
import java.util.Collection;

public class AudioRecordFragment extends AudioTxSupportFragment implements ControllerBase.ControllerStateListener {
    private final static String TAG = Constants.packageTag("AudioRecordFragment");

    private DataView[] mSignalViews;
    private LinearLayout mAuxViewContainer;
    private LinearLayout mInfoContainer;
    private WorkerFunctionView mWorkerFunctionView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.audio_record_fragment, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mActivityRef.get() == null)
            return;

        mActivityRef.get().getMainController().unregisterStateListener(this);
    }

    @Override
    public void initTxSupport() {
        if (mActivityRef.get() == null)
            return;

        mSignalViews = new DataView[2];
        mSignalViews[0] = mActivityRef.get().findViewById(R.id.record_signal_1);
        mSignalViews[1] = mActivityRef.get().findViewById(R.id.record_signal_2);

        mAuxViewContainer = mActivityRef.get().findViewById(R.id.record_aux_view_container);
        mInfoContainer = mActivityRef.get().findViewById(R.id.record_info_container);
        mWorkerFunctionView = mActivityRef.get().findViewById(R.id.record_func_attr_container);

        mActivityRef.get().getMainController().registerStateListener(this);
    }

    @Override
    public DataView[] getTxDataViews() {
        return mSignalViews;
    }

    @Override
    public LinearLayout getTxAuxViewContainer() {
        return mAuxViewContainer;
    }

    @Override
    public LinearLayout getTxInfoContainer() {
        return mInfoContainer;
    }

    @Override
    public String getTxInfoTitle() {
        return "Record Info";
    }

    @Override
    public Object[] getTxReturns(WorkerFunction.Ack ack) {
        return ack.getReturns();
    }

    @Override
    public String getControllerName() {
        return Constants.Controllers.NAME_RECORD;
    }

    @Override
    public WorkerFunction getInfoRequestFunction() {
        return new RecordInfoFunction();
    }

    @Override
    public Collection<? extends String> getSupportedIntents() {
        ArrayList<String> supportIntents = new ArrayList<>();
        for (String intentAction : Constants.MasterInterface.INTENT_NAMES)
            if (Constants.getIntentOwner(intentAction).equals(Constants.INTENT_OWNER_RECORD))
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
            case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
            case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
            case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
                return true;

            default:
                break;
        }
        return false;
    }

    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        super.onFunctionAckReceived(ack);

        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(getControllerName());
        TextView v = mActivityRef.get().findViewById(R.id.record_status);
        if (controller instanceof AudioController.TxSupport && ((AudioController.TxSupport) controller).isTxRunning()) {
            v.setText("Status: running");
        } else {
            v.setText("Status: idle");
            for (DataView dv : getTxDataViews())
                dv.reset();
        }
    }

    @Override
    public void onStateChanged(ControllerBase controller) {
        onFunctionAckReceived(null);
    }
}
