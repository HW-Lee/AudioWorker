package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.audioworker.R;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.WorkerFunctionView;

import java.util.Collection;

public class AudioFragment extends WorkerFragment {
    private final static String TAG = Constants.packageTag("AudioFragment");

    interface TxRxSupportCommon {
        String getControllerName();
        WorkerFunction getInfoRequestFunction();
    }

    public interface TxSupport extends TxRxSupportCommon {
        void initTxSupport();
        DataView getTxDataView();
        LinearLayout getTxAuxViewContainer();
        LinearLayout getTxInfoContainer();
        String getTxInfoTitle();
        Object[] getTxReturns(WorkerFunction.Ack ack);
    }

    public interface RxSupport extends TxRxSupportCommon {
        void initRxSupport();
        LinearLayout getRxAuxViewContainer();
        LinearLayout getRxInfoContainer();
        String getRxInfoTitle();
        Object[] getRxReturns(WorkerFunction.Ack ack);
    }

    public interface WorkerFunctionAuxSupport {
        Collection<? extends String> getSupportedIntents();
        WorkerFunctionView getWorkerFunctionView();
        boolean needToShowAuxView(String action);
    }

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
