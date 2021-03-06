package com.google.audioworker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.voip.VoIPInfoFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.AudioController;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class AudioVoIPFragment extends AudioRxTxSupportFragment implements ControllerBase.ControllerStateListener {
    private final static String TAG = Constants.packageTag("AudioVoIPFragment");

    private DataView mSignalView;
    private LinearLayout mTxAuxViewContainer;
    private LinearLayout mRxAuxViewContainer;
    private LinearLayout mTxInfoContainer;
    private LinearLayout mRxInfoContainer;

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.audio_voip_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String getControllerName() {
        return Constants.Controllers.NAME_VOIP;
    }

    @Override
    public WorkerFunction getInfoRequestFunction() {
        return new VoIPInfoFunction();
    }

    @Override
    public void initTxSupport() {
        if (mActivityRef.get() == null)
            return;

        mSignalView = mActivityRef.get().findViewById(R.id.voip_tx_signal);
        mSignalView.setDataPaint(1, Color.RED);

        mTxAuxViewContainer = mActivityRef.get().findViewById(R.id.voip_tx_aux_view_container);
        mTxInfoContainer = mActivityRef.get().findViewById(R.id.voip_tx_info_container);
    }

    @Override
    public DataView getTxDataView() {
        return mSignalView;
    }

    @Override
    public LinearLayout getTxAuxViewContainer() {
        return mTxAuxViewContainer;
    }

    @Override
    public LinearLayout getTxInfoContainer() {
        return mTxInfoContainer;
    }

    @Override
    public String getTxInfoTitle() {
        return "VoIP Tx Info";
    }

    @Override
    public Object[] getTxReturns(WorkerFunction.Ack ack) {
        ArrayList<Object> returns = new ArrayList<>();
        Collections.addAll(returns, ack.getReturns());
        if (returns.size() > 0)
            returns.remove(0);
        return returns.toArray();
    }

    @Override
    public void initRxSupport() {
        if (mActivityRef.get() == null)
            return;

        mRxAuxViewContainer = mActivityRef.get().findViewById(R.id.voip_rx_aux_view_container);
        mRxInfoContainer = mActivityRef.get().findViewById(R.id.voip_rx_info_container);
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
        return "VoIP Rx Info";
    }

    @Override
    public Object[] getRxReturns(WorkerFunction.Ack ack) {
        ArrayList<Object> returns = new ArrayList<>();
        Collections.addAll(returns, ack.getReturns());
        return returns.toArray();
    }

    @Override
    public Collection<? extends String> getSupportedIntents() {
        ArrayList<String> supportIntents = new ArrayList<>();
        for (String intentAction : Constants.MasterInterface.INTENT_NAMES)
            if (Constants.getIntentOwner(intentAction).equals(Constants.INTENT_OWNER_VOIP))
                supportIntents.add(intentAction);

        return supportIntents;
    }

    @Override
    public WorkerFunctionView getWorkerFunctionView() {
        if (mActivityRef.get() == null)
            return null;

        return ((WorkerFunctionView) mActivityRef.get().findViewById(R.id.voip_func_attr_container));
    }

    @Override
    public boolean needToShowAuxView(String action) {
        switch (action) {
            case Constants.MasterInterface.INTENT_VOIP_DETECT_REGISTER:
            case Constants.MasterInterface.INTENT_VOIP_DETECT_UNREGISTER:
            case Constants.MasterInterface.INTENT_VOIP_DETECT_SETPARAMS:
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
        final TextView statusTxView = mActivityRef.get().findViewById(R.id.voip_tx_status);
        if (statusTxView == null)
            return;

        if (controller instanceof AudioController.TxSupport && ((AudioController.TxSupport) controller).isTxRunning()) {
            mActivityRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTxView.setText("VoIP Tx: running");
                }
            });
        } else {
            mActivityRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTxView.setText("VoIP Tx: idle");
                    if (getTxDataView() != null)
                        getTxDataView().reset();
                }
            });
        }

        final TextView statusRxView = mActivityRef.get().findViewById(R.id.voip_rx_status);
        if (statusRxView == null)
            return;

        if (controller instanceof AudioController.RxSupport && ((AudioController.RxSupport) controller).isRxRunning()) {
            mActivityRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusRxView.setText("VoIP Rx: running");
                }
            });
        } else {
            mActivityRef.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusRxView.setText("VoIP Rx: idle");
                }
            });
        }

        final TextView statusPhoneStateView = mActivityRef.get().findViewById(R.id.voip_phone_state);
        if (statusPhoneStateView == null)
            return;

        final int mode = ((AudioManager) mActivityRef.get().getSystemService(Context.AUDIO_SERVICE)).getMode();
        mActivityRef.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusPhoneStateView.setText("Phone state: " + mode);
            }
        });
    }

    @Override
    public void onStateChanged(ControllerBase controller) {
    }
}
