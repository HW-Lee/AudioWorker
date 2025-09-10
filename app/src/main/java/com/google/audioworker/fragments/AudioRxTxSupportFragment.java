package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.view.View;

import com.google.audioworker.activities.MainActivity;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.functions.controllers.RecordController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.HashMap;
import java.util.List;

public abstract class AudioRxTxSupportFragment extends WorkerFragment
        implements AudioFragment.RxSupport,
                AudioFragment.TxSupport,
                AudioFragment.WorkerFunctionAuxSupport,
                WorkerFunctionView.ActionSelectedListener,
                RecordController.RecordRunnable.RecordDataListener,
                MainActivity.ControllerReadyListener {
    private static final String TAG = Constants.packageTag("AudioRxTxSupportFragment");

    protected final AudioRxSupportFragment.Factory.Bundle mRxBundle =
            new AudioRxSupportFragment.Factory.Bundle();
    protected final AudioTxSupportFragment.Factory.Bundle mTxBundle =
            new AudioTxSupportFragment.Factory.Bundle();

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @CallSuper
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onControllerReady(MainController mainController) {
        init();
        View container = getContainerView();
        if (container != null) {
            container.setVisibility(View.VISIBLE);
        }
    }

    private void init() {
        AudioRxSupportFragment.Factory.init(this, mRxBundle);
        AudioTxSupportFragment.Factory.init(this, mTxBundle);
    }

    @Override
    public void onDataUpdated(List<? extends Double>[] signal, RecordStartFunction function) {
        getTxDataView().plot(signal);
    }

    @CallSuper
    @Override
    public void onActionSelected(
            String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        AudioRxSupportFragment.Factory.onActionSelected(this, mRxBundle, action, views);
        AudioTxSupportFragment.Factory.onActionSelected(this, mTxBundle, action, views);
    }

    @Override
    public void onFunctionSent(WorkerFunction function) {}

    @CallSuper
    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        AudioRxSupportFragment.Factory.onFunctionAckReceived(this, mRxBundle, ack);
        AudioTxSupportFragment.Factory.onFunctionAckReceived(this, mTxBundle, ack);
    }
}
