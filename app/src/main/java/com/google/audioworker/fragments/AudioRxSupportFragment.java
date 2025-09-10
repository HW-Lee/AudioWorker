package com.google.audioworker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.activities.MainActivity;
import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStartFunction;
import com.google.audioworker.functions.common.ParameterizedWorkerFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.ViewUtils;
import com.google.audioworker.views.WorkerFunctionView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class AudioRxSupportFragment extends WorkerFragment
        implements AudioFragment.RxSupport,
                AudioFragment.WorkerFunctionAuxSupport,
                WorkerFunctionView.ActionSelectedListener,
                MainActivity.ControllerReadyListener {
    private static final String TAG = Constants.packageTag("AudioRxSupportFragment");

    private final Factory.Bundle mBundle = new Factory.Bundle();

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
        Factory.init(this, mBundle);
        View container = getContainerView();
        if (container != null) {
            container.setVisibility(View.VISIBLE);
        }
    }

    @CallSuper
    @Override
    public void onActionSelected(
            String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        Factory.onActionSelected(this, mBundle, action, views);
    }

    @CallSuper
    @Override
    public void onFunctionSent(WorkerFunction function) {}

    @CallSuper
    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        Factory.onFunctionAckReceived(this, mBundle, ack);
    }

    static class Factory {
        static class Bundle {
            FrameLayout mRxInfoCollapseToggleView;
            LinearLayout mRxInfoContentView;
            HashMap<String, SparseArray<JSONObject>> mRunningUsecases;
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void init(
                final T fragment, Bundle bundle) {
            if (fragment.mActivityRef.get() == null) return;

            fragment.initRxSupport();

            if (fragment instanceof AudioFragment.WorkerFunctionAuxSupport
                    && fragment instanceof WorkerFunctionView.ActionSelectedListener) {
                WorkerFunctionView workerFunctionView =
                        ((AudioFragment.WorkerFunctionAuxSupport) fragment).getWorkerFunctionView();
                workerFunctionView.setSupportedIntentActions(
                        ((AudioFragment.WorkerFunctionAuxSupport) fragment).getSupportedIntents(),
                        (WorkerFunctionView.ActionSelectedListener) fragment);
                workerFunctionView.setController(
                        fragment.mActivityRef
                                .get()
                                .getMainController()
                                .getSubControllerByName(fragment.getControllerName()));
            }

            Factory.initInfoView(fragment, bundle, fragment.getRxInfoTitle());
            LinearLayout rxInfoContainer = fragment.getRxInfoContainer();
            rxInfoContainer.removeAllViews();
            rxInfoContainer.addView(bundle.mRxInfoCollapseToggleView);
            rxInfoContainer.addView(bundle.mRxInfoContentView);

            bundle.mRunningUsecases = new HashMap<>();
            bundle.mRunningUsecases.put(
                    PlaybackFunction.TASK_OFFLOAD, new SparseArray<JSONObject>());
            bundle.mRunningUsecases.put(
                    PlaybackFunction.TASK_NONOFFLOAD, new SparseArray<JSONObject>());

            if (fragment instanceof WorkerFunctionView.ActionSelectedListener) {
                fragment.mActivityRef
                        .get()
                        .getMainController()
                        .execute(
                                fragment.getInfoRequestFunction(),
                                new WorkerFunction.WorkerFunctionListener() {
                                    @Override
                                    public void onAckReceived(WorkerFunction.Ack ack) {
                                        ((WorkerFunctionView.ActionSelectedListener) fragment)
                                                .onFunctionAckReceived(ack);
                                    }
                                },
                                true);
            }
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void initInfoView(
                T fragment, final Bundle bundle, String title) {
            bundle.mRxInfoCollapseToggleView = new FrameLayout(fragment.mActivityRef.get());
            bundle.mRxInfoCollapseToggleView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));
            bundle.mRxInfoContentView = new LinearLayout(fragment.mActivityRef.get());
            bundle.mRxInfoContentView.setOrientation(LinearLayout.VERTICAL);
            bundle.mRxInfoContentView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            bundle.mRxInfoContentView.setVisibility(View.VISIBLE);

            TextView tv = new TextView(fragment.mActivityRef.get());
            tv.setText(title);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(20);
            tv.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
            final TextView toggle = new TextView(fragment.mActivityRef.get());
            toggle.setText("v");
            toggle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            toggle.setTextSize(20);
            toggle.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
            toggle.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (bundle.mRxInfoContentView.getVisibility() == View.GONE) {
                                toggle.setText("v");
                                bundle.mRxInfoContentView.setVisibility(View.VISIBLE);
                            } else {
                                toggle.setText(">");
                                bundle.mRxInfoContentView.setVisibility(View.GONE);
                            }
                        }
                    });

            bundle.mRxInfoCollapseToggleView.addView(tv);
            bundle.mRxInfoCollapseToggleView.addView(toggle);
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void onActionSelected(
                final T fragment,
                final Bundle bundle,
                String action,
                HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (action == null
                    || !(fragment instanceof AudioFragment.WorkerFunctionAuxSupport)
                    || !((AudioFragment.WorkerFunctionAuxSupport) fragment)
                            .needToShowAuxView(action)) Factory.hideRxAuxView(fragment);
            else Factory.showRxAuxView(fragment, bundle, action, views);
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void onFunctionAckReceived(
                final T fragment, final Bundle bundle, WorkerFunction.Ack ack) {
            if (fragment.mActivityRef.get() == null) return;

            ControllerBase controller = fragment.mActivityRef.get().getMainController();
            controller.execute(
                    fragment.getInfoRequestFunction(),
                    new WorkerFunction.WorkerFunctionListener() {
                        @Override
                        public void onAckReceived(WorkerFunction.Ack ack) {
                            if (fragment.mActivityRef.get() == null) return;

                            final Object[] returns = fragment.getRxReturns(ack);

                            fragment.mActivityRef
                                    .get()
                                    .runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    Factory.updateRxInfoContent(
                                                            fragment, bundle, returns);
                                                    if (fragment
                                                                    instanceof
                                                                    AudioFragment
                                                                            .WorkerFunctionAuxSupport
                                                            && ((AudioFragment
                                                                                            .WorkerFunctionAuxSupport)
                                                                                    fragment)
                                                                            .getWorkerFunctionView()
                                                                    != null) {
                                                        ((AudioFragment.WorkerFunctionAuxSupport)
                                                                        fragment)
                                                                .getWorkerFunctionView()
                                                                .updateParameterView();
                                                    }
                                                }
                                            });
                        }
                    },
                    true);
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void updateRxInfoContent(
                T fragment, Bundle bundle, Object[] returns) {
            bundle.mRxInfoContentView.removeAllViews();
            if (returns.length < 1 || fragment.mActivityRef.get() == null) return;

            try {
                JSONObject rxInfo = new JSONObject(returns[0].toString());
                for (String type : bundle.mRunningUsecases.keySet()) {
                    SparseArray<JSONObject> array = bundle.mRunningUsecases.get(type);
                    if (array == null) continue;

                    array.clear();

                    if (!rxInfo.has(type) || rxInfo.getJSONObject(type) == null) continue;

                    JSONObject jsonOffload = rxInfo.getJSONObject(type);
                    Iterator<String> iterator = jsonOffload.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        int id;
                        try {
                            id = Integer.valueOf(key);
                            JSONObject usecase = jsonOffload.getJSONObject(key);
                            if (usecase != null) array.put(id, usecase);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            if (Factory.getNumRunningTracks(bundle) == 0) {
                {
                    TextView tv = new TextView(fragment.mActivityRef.get());
                    tv.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    fragment.getPxByDp(40)));
                    tv.setGravity(Gravity.CENTER_VERTICAL);
                    tv.setTextSize(16);
                    tv.setText("no running tracks");
                    bundle.mRxInfoContentView.addView(tv);
                }
            } else {
                for (String type : bundle.mRunningUsecases.keySet()) {
                    SparseArray<JSONObject> usecases = bundle.mRunningUsecases.get(type);
                    if (usecases == null || usecases.size() == 0) continue;

                    {
                        TextView tv = new TextView(fragment.mActivityRef.get());
                        tv.setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        fragment.getPxByDp(40)));
                        tv.setGravity(Gravity.CENTER_VERTICAL);
                        tv.setTextSize(16);
                        tv.setText(usecases.size() + " running " + type + " tracks");
                        bundle.mRxInfoContentView.addView(tv);
                    }

                    LinearLayout configContainer = new LinearLayout(fragment.mActivityRef.get());
                    configContainer.setOrientation(LinearLayout.VERTICAL);
                    configContainer.setPadding(
                            fragment.getPxByDp(6),
                            fragment.getPxByDp(6),
                            fragment.getPxByDp(6),
                            fragment.getPxByDp(6));
                    configContainer.setBackgroundResource(R.drawable.border2);
                    configContainer.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));

                    for (int i = 0; i < usecases.size(); i++) {
                        if (i > 0) {
                            View border =
                                    ViewUtils.getHorizontalBorder(
                                            fragment.mActivityRef.get(), fragment.getPxByDp(2));
                            border.setBackgroundColor(Color.argb(64, 0, 0, 0));
                            configContainer.addView(border);
                        }
                        int idx = usecases.keyAt(i);
                        Factory.appendTrackInfoView(fragment, configContainer, usecases.get(idx));
                    }

                    bundle.mRxInfoContentView.addView(configContainer);
                }
            }

            bundle.mRxInfoContentView.invalidate();
        }

        private static int getNumRunningTracks(Bundle bundle) {
            int cnt = 0;
            for (String type : bundle.mRunningUsecases.keySet()) {
                SparseArray<JSONObject> dummy = bundle.mRunningUsecases.get(type);
                if (dummy == null) continue;

                cnt += dummy.size();
            }

            return cnt;
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void appendTrackInfoView(
                T fragment, LinearLayout configContainer, JSONObject jsonInfo) {
            try {
                if (jsonInfo == null
                        || jsonInfo.getJSONObject(ParameterizedWorkerFunction.KEY_PARAMS) == null)
                    return;

                JSONObject jsonConfig =
                        jsonInfo.getJSONObject(ParameterizedWorkerFunction.KEY_PARAMS);
                Iterator<String> iterator = jsonConfig.keys();

                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (key.equals(PlaybackStartFunction.ATTR_TYPE)) continue;

                    LinearLayout container = new LinearLayout(fragment.mActivityRef.get());
                    container.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                    container.setOrientation(LinearLayout.HORIZONTAL);

                    TextView tv = new TextView(fragment.mActivityRef.get());
                    tv.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextSize(16);
                    tv.setText(key);
                    container.addView(tv);

                    EditText et = new EditText(fragment.mActivityRef.get());
                    et.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
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

        static <T extends WorkerFragment & AudioFragment.RxSupport> void hideRxAuxView(T fragment) {
            LinearLayout container = fragment.getRxAuxViewContainer();
            if (container == null) return;

            container.setVisibility(View.GONE);
            container.removeAllViews();
            container.invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void showRxAuxView(
                T fragment,
                Bundle bundle,
                String action,
                HashMap<String, WorkerFunctionView.ParameterView> views) {
            LinearLayout container = fragment.getRxAuxViewContainer();
            if (container == null) return;

            switch (action) {
                case Constants.MasterInterface.INTENT_PLAYBACK_START:
                    container.setVisibility(View.VISIBLE);
                    Factory.updateRxAuxViewForStart(fragment, views);
                    return;
                case Constants.MasterInterface.INTENT_PLAYBACK_STOP:
                    container.setVisibility(View.VISIBLE);
                    Factory.updateRxAuxViewForStop(fragment, bundle, views);
                    return;

                default:
                    break;
            }
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void updateRxAuxViewForStart(
                T fragment, final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (!views.containsKey(PlaybackStartFunction.ATTR_TYPE)
                    || fragment.mActivityRef.get() == null) return;

            LinearLayout container = fragment.getRxAuxViewContainer();
            if (container == null) return;

            container.removeAllViews();
            container.setOrientation(LinearLayout.VERTICAL);
            final ArrayList<String> selections = new ArrayList<>();
            selections.add("");
            selections.add(PlaybackFunction.TASK_OFFLOAD);
            selections.add(PlaybackFunction.TASK_NONOFFLOAD);
            Spinner spinner =
                    ViewUtils.getSimpleSpinner(
                            fragment.mActivityRef.get(),
                            selections,
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent, View view, int position, long id) {
                                    if (position == 0) return;

                                    WorkerFunctionView.ParameterView pv =
                                            views.get(PlaybackStartFunction.ATTR_TYPE);
                                    if (pv == null) return;
                                    pv.requestValue.setText(selections.get(position));
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
            spinner.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));

            container.addView(spinner);
            container.invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.RxSupport> void updateRxAuxViewForStop(
                T fragment,
                Bundle bundle,
                final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (!views.containsKey(PlaybackStartFunction.ATTR_TYPE)
                    || !views.containsKey(PlaybackStartFunction.ATTR_PLAYBACK_ID)
                    || fragment.mActivityRef.get() == null) return;

            LinearLayout container = fragment.getRxAuxViewContainer();
            if (container == null) return;

            container.removeAllViews();
            container.setOrientation(LinearLayout.VERTICAL);
            final ArrayList<String> selections = new ArrayList<>();
            selections.add("");
            for (String type : bundle.mRunningUsecases.keySet()) {
                SparseArray<JSONObject> usecases = bundle.mRunningUsecases.get(type);
                if (usecases == null || usecases.size() == 0) continue;

                for (int i = 0; i < usecases.size(); i++)
                    selections.add(type + "@" + usecases.keyAt(i));
            }
            Spinner spinner =
                    ViewUtils.getSimpleSpinner(
                            fragment.mActivityRef.get(),
                            selections,
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent, View view, int position, long id) {
                                    if (position == 0) return;

                                    WorkerFunctionView.ParameterView pv1 =
                                            views.get(PlaybackStartFunction.ATTR_TYPE);
                                    WorkerFunctionView.ParameterView pv2 =
                                            views.get(PlaybackStartFunction.ATTR_PLAYBACK_ID);
                                    if (pv1 == null || pv2 == null) return;

                                    String s = selections.get(position);
                                    String[] patterns = s.split("@");
                                    pv1.requestValue.setText(patterns[0]);
                                    pv2.requestValue.setText(patterns[1]);
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });

            spinner.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));

            container.addView(spinner);
            container.invalidate();
        }
    }
}
