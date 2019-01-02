package com.google.audioworker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStartFunction;
import com.google.audioworker.functions.common.ParameterizedWorkerFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.ViewUtils;
import com.google.audioworker.views.WorkerFunctionView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class AudioRxSupportFragment extends WorkerFragment
        implements AudioFragment.RxSupport, AudioFragment.WorkerFunctionAuxSupport, WorkerFunctionView.ActionSelectedListener {
    private final static String TAG = Constants.packageTag("AudioRxSupportFragment");

    protected FrameLayout mRxInfoCollapseToggleView;
    protected LinearLayout mRxInfoContentView;

    protected HashMap<String, SparseArray<JSONObject>> mRunningUsecases;

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
        initRxSupport();
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        initRxSupport();
        init();
    }

    public void init() {
        if (mActivityRef.get() == null)
            return;

        WorkerFunctionView workerFunctionView = getWorkerFunctionView();
        workerFunctionView.setSupportedIntentActions(getSupportedIntents(), this);
        workerFunctionView.setController(mActivityRef.get().getMainController().getSubControllerByName(getControllerName()));

        initInfoView(getRxInfoTitle());
        LinearLayout rxInfoContainer = getRxInfoContainer();
        rxInfoContainer.removeAllViews();
        rxInfoContainer.addView(mRxInfoCollapseToggleView);
        rxInfoContainer.addView(mRxInfoContentView);

        mRunningUsecases = new HashMap<>();
        mRunningUsecases.put(PlaybackFunction.TASK_OFFLOAD, new SparseArray<JSONObject>());
        mRunningUsecases.put(PlaybackFunction.TASK_NONOFFLOAD, new SparseArray<JSONObject>());

        mActivityRef.get().getMainController().execute(getInfoRequestFunction(), new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(WorkerFunction.Ack ack) {
                onFunctionAckReceived(ack);
            }
        });
    }

    protected void initInfoView(String title) {
        mRxInfoCollapseToggleView = new FrameLayout(mActivityRef.get());
        mRxInfoCollapseToggleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
        mRxInfoContentView = new LinearLayout(mActivityRef.get());
        mRxInfoContentView.setOrientation(LinearLayout.VERTICAL);
        mRxInfoContentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mRxInfoContentView.setVisibility(View.VISIBLE);

        TextView tv = new TextView(mActivityRef.get());
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final TextView toggle = new TextView(mActivityRef.get());
        toggle.setText("v");
        toggle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        toggle.setTextSize(20);
        toggle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRxInfoContentView.getVisibility() == View.GONE) {
                    toggle.setText("v");
                    mRxInfoContentView.setVisibility(View.VISIBLE);
                } else {
                    toggle.setText(">");
                    mRxInfoContentView.setVisibility(View.GONE);
                }
            }
        });

        mRxInfoCollapseToggleView.addView(tv);
        mRxInfoCollapseToggleView.addView(toggle);
    }

    @Override
    public void onActionSelected(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (action == null || !needToShowAuxView(action))
            hideRxAuxView();
        else
            showRxAuxView(action, views);
    }

    @Override
    public void onFunctionSent(WorkerFunction function) {
    }

    @CallSuper
    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController();
        controller.execute(getInfoRequestFunction(), new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(WorkerFunction.Ack ack) {
                final Object[] returns = getRxReturns(ack);

                mActivityRef.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateRxInfoContent(returns);
                    }
                });
            }
        });
    }

    protected void updateRxInfoContent(Object[] returns) {
        if (returns.length < 1 || mActivityRef.get() == null)
            return;


        try {
            JSONObject rxInfo = new JSONObject(returns[0].toString());
            for (String type : mRunningUsecases.keySet()) {
                SparseArray<JSONObject> array = mRunningUsecases.get(type);
                if (!rxInfo.has(type) || rxInfo.getJSONObject(type) == null || array == null)
                    continue;

                array.clear();
                JSONObject jsonOffload = rxInfo.getJSONObject(type);
                Iterator<String> iterator = jsonOffload.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    int id;
                    try {
                        id = Integer.valueOf(key);
                        JSONObject usecase = jsonOffload.getJSONObject(key);
                        if (usecase != null)
                            array.put(id, usecase);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        mRxInfoContentView.removeAllViews();
        if (getNumRunningTracks() == 0) {
            {
                TextView tv = new TextView(mActivityRef.get());
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setTextSize(16);
                tv.setText("no running tracks");
                mRxInfoContentView.addView(tv);
            }
        } else {
            for (String type : mRunningUsecases.keySet()) {
                SparseArray<JSONObject> usecases = mRunningUsecases.get(type);
                if (usecases == null || usecases.size() == 0)
                    continue;

                {
                    TextView tv = new TextView(mActivityRef.get());
                    tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
                    tv.setGravity(Gravity.CENTER_VERTICAL);
                    tv.setTextSize(16);
                    tv.setText(usecases.size() + " running " + type + " tracks");
                    mRxInfoContentView.addView(tv);
                }

                LinearLayout configContainer = new LinearLayout(mActivityRef.get());
                configContainer.setOrientation(LinearLayout.VERTICAL);
                configContainer.setPadding(getPxByDp(6), getPxByDp(6), getPxByDp(6), getPxByDp(6));
                configContainer.setBackgroundResource(R.drawable.border2);
                configContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                for (int i = 0; i < usecases.size(); i++) {
                    if (i > 0) {
                        View border = ViewUtils.getHorizontalBorder(mActivityRef.get(), getPxByDp(2));
                        border.setBackgroundColor(Color.argb(64, 0, 0, 0));
                        configContainer.addView(border);
                    }
                    int idx = usecases.keyAt(i);
                    appendTrackInfoView(configContainer, usecases.get(idx));
                }

                mRxInfoContentView.addView(configContainer);
            }
        }

        mRxInfoContentView.invalidate();
    }

    private int getNumRunningTracks() {
        int cnt = 0;
        for (String type : mRunningUsecases.keySet()) {
            SparseArray<JSONObject> dummy = mRunningUsecases.get(type);
            if (dummy == null)
                continue;

            cnt += dummy.size();
        }

        return cnt;
    }

    private void appendTrackInfoView(LinearLayout configContainer, JSONObject jsonInfo) {
        try {
            if (jsonInfo == null || jsonInfo.getJSONObject(ParameterizedWorkerFunction.KEY_PARAMS) == null)
                return;

            JSONObject jsonConfig = jsonInfo.getJSONObject(ParameterizedWorkerFunction.KEY_PARAMS);
            Iterator<String> iterator = jsonConfig.keys();

            while (iterator.hasNext()) {
                String key = iterator.next();
                if (key.equals(PlaybackStartFunction.ATTR_TYPE))
                    continue;

                LinearLayout container = new LinearLayout(mActivityRef.get());
                container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                container.setOrientation(LinearLayout.HORIZONTAL);

                TextView tv = new TextView(mActivityRef.get());
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(16);
                tv.setText(key);
                container.addView(tv);

                EditText et = new EditText(mActivityRef.get());
                et.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                et.setGravity(Gravity.CENTER);
                et.setTextSize(16);
                et.setEnabled(false);
                et.setText(jsonConfig.get(key).toString());
                container.addView(et);

                configContainer.addView(container);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void hideRxAuxView() {
        LinearLayout container = getRxAuxViewContainer();
        if (container == null)
            return;

        container.setVisibility(View.GONE);
        container.removeAllViews();
        container.invalidate();
    }

    protected void showRxAuxView(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        LinearLayout container = getRxAuxViewContainer();
        if (container == null)
            return;

        switch (action) {
            case Constants.MasterInterface.INTENT_PLAYBACK_START:
                container.setVisibility(View.VISIBLE);
                updateRxAuxViewForStart(views);
                return;
            case Constants.MasterInterface.INTENT_PLAYBACK_STOP:
                container.setVisibility(View.VISIBLE);
                updateRxAuxViewForStop(views);
                return;

            default:
                break;
        }
    }

    private void updateRxAuxViewForStart(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (!views.containsKey(PlaybackStartFunction.ATTR_TYPE) || mActivityRef.get() == null)
            return;

        LinearLayout container = getRxAuxViewContainer();
        if (container == null)
            return;

        container.removeAllViews();
        container.setOrientation(LinearLayout.VERTICAL);
        final ArrayList<String> selections = new ArrayList<>();
        selections.add("");
        selections.add(PlaybackFunction.TASK_OFFLOAD);
        selections.add(PlaybackFunction.TASK_NONOFFLOAD);
        Spinner spinner = ViewUtils.getSimpleSpinner(mActivityRef.get(), selections, new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;

                WorkerFunctionView.ParameterView pv = views.get(PlaybackStartFunction.ATTR_TYPE);
                if (pv == null)
                    return;
                pv.requestValue.setText(selections.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));

        container.addView(spinner);
        container.invalidate();
    }

    private void updateRxAuxViewForStop(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (!views.containsKey(PlaybackStartFunction.ATTR_TYPE) ||
                !views.containsKey(PlaybackStartFunction.ATTR_PLAYBACK_ID) || mActivityRef.get() == null)
            return;

        LinearLayout container = getRxAuxViewContainer();
        if (container == null)
            return;

        container.removeAllViews();
        container.setOrientation(LinearLayout.VERTICAL);
        final ArrayList<String> selections = new ArrayList<>();
        selections.add("");
        for (String type : mRunningUsecases.keySet()) {
            SparseArray<JSONObject> usecases = mRunningUsecases.get(type);
            if (usecases == null || usecases.size() == 0)
                continue;

            for (int i = 0; i < usecases.size(); i++)
                selections.add(type + "@" + usecases.keyAt(i));
        }
        Spinner spinner = ViewUtils.getSimpleSpinner(mActivityRef.get(), selections, new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;

                WorkerFunctionView.ParameterView pv1 = views.get(PlaybackStartFunction.ATTR_TYPE);
                WorkerFunctionView.ParameterView pv2 = views.get(PlaybackStartFunction.ATTR_PLAYBACK_ID);
                if (pv1 == null || pv2 == null)
                    return;

                String s = selections.get(position);
                String[] patterns = s.split("@");
                pv1.requestValue.setText(patterns[0]);
                pv2.requestValue.setText(patterns[1]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));

        container.addView(spinner);
        container.invalidate();
    }
}
