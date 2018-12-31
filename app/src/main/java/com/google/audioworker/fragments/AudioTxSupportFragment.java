package com.google.audioworker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.common.ParameterizedWorkerFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.AudioController;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.functions.controllers.RecordController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.ViewUtils;
import com.google.audioworker.views.WorkerFunctionView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class AudioTxSupportFragment<T extends AudioController.AudioTxController> extends WorkerFragment
        implements RecordController.RecordRunnable.RecordDataListener, AudioFragment.TxSupport,
            AudioFragment.WorkerFunctionAuxSupport, WorkerFunctionView.ActionSelectedListener {
    private final static String TAG = Constants.packageTag("AudioInputSupportFragment");

    private String mContollerName;
    private WorkerFunction mInitInfoRequestFunction;

    protected HashMap<String, WorkerFunctionView.ParameterView> mDetectorParameterViews;

    protected FrameLayout mTxInfoCollapseToggleView;
    protected LinearLayout mTxInfoContentView;

    protected final HashMap<String, String> mDetectorHandles = new HashMap<>();

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initTxSupport();
    }

    @Override
    public void onResume() {
        super.onResume();
        initTxSupport();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(mContollerName);
        if (controller instanceof AudioController.TxCallback)
            ((AudioController.TxCallback) controller).unregisterDataListener(this);

        mDetectorParameterViews.clear();
    }

    @Override
    public void initTxSupport() {
        if (mActivityRef.get() == null)
            return;

        mContollerName = getControllerName();
        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(mContollerName);
        if (!(controller instanceof AudioController.TxCallback))
            return;

        ((AudioController.TxCallback) controller).registerDataListener(this);
        mInitInfoRequestFunction = getInfoRequestFunction();

        Collection<? extends String> supportIntents = getSupportedIntents();

        WorkerFunctionView workerFunctionView = getWorkerFunctionView();
        workerFunctionView.setSupportedIntentActions(supportIntents, this);
        workerFunctionView.setController(controller);

        for (DataView v : getTxDataViews()) {
            v.setGridSlotsY(4);
            v.setGridSlotsX(10);
        }

        mDetectorParameterViews = new HashMap<>();

        initInfoView(getTxInfoTitle());
        LinearLayout txInfoContainer = getTxInfoContainer();
        txInfoContainer.removeAllViews();
        txInfoContainer.addView(mTxInfoCollapseToggleView);
        txInfoContainer.addView(mTxInfoContentView);

        controller.execute(mInitInfoRequestFunction, new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(WorkerFunction.Ack ack) {
                onFunctionAckReceived(ack);
            }
        });
    }

    protected void initInfoView(String title) {
        mTxInfoCollapseToggleView = new FrameLayout(mActivityRef.get());
        mTxInfoCollapseToggleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
        mTxInfoContentView = new LinearLayout(mActivityRef.get());
        mTxInfoContentView.setOrientation(LinearLayout.VERTICAL);
        mTxInfoContentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mTxInfoContentView.setVisibility(View.GONE);

        TextView tv = new TextView(mActivityRef.get());
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final TextView toggle = new TextView(mActivityRef.get());
        toggle.setText(">");
        toggle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        toggle.setTextSize(20);
        toggle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTxInfoContentView.getVisibility() == View.GONE) {
                    toggle.setText("v");
                    mTxInfoContentView.setVisibility(View.VISIBLE);
                } else {
                    toggle.setText(">");
                    mTxInfoContentView.setVisibility(View.GONE);
                }
            }
        });

        mTxInfoCollapseToggleView.addView(tv);
        mTxInfoCollapseToggleView.addView(toggle);
    }

    @Override
    public void onDataUpdated(List<? extends Double>[] signal, RecordStartFunction function) {
        for (int i = 0; i < signal.length; i++) {
            getTxDataViews()[i].plot(signal[i]);
        }
    }

    @Override
    public void onActionSelected(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (action == null || !needToShowAuxView(action)) {
            hideAuxView();
        } else {
            updateAuxView(action, views);
        }
    }

    private void updateAuxView(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        switch (action) {
            case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
            case Constants.MasterInterface.INTENT_VOIP_DETECT_REGISTER:
                updateAuxViewForRegisterOperation(views);
                return;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
            case Constants.MasterInterface.INTENT_VOIP_DETECT_UNREGISTER:
                updateAuxViewForUnregisterOperation(views);
                return;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
            case Constants.MasterInterface.INTENT_VOIP_DETECT_SETPARAMS:
                updateAuxViewForSetParametersOperation(views);
                return;

            default:
                break;
        }
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
        controller.execute(mInitInfoRequestFunction, new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(WorkerFunction.Ack ack) {
                final Object[] returns = getTxReturns(ack);
                updateDetectors(returns);
                mActivityRef.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateInfoContent(returns);
                    }
                });
            }
        });
    }

    private void updateDetectors(Object[] returns) {
        if (returns.length < 2)
            return;

        try {
            JSONObject detectorInfo = new JSONObject(returns[1].toString());
            synchronized (mDetectorHandles) {
                mDetectorHandles.clear();
                Iterator<String> iterator = detectorInfo.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    mDetectorHandles.put(key, detectorInfo.getString(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateInfoContent(Object[] returns) {
        mTxInfoContentView.removeAllViews();
        if (returns.length < 2)
            return;

        HashMap<String, Pair<String, String>> kvpairs = new HashMap<>();
        try {
            JSONObject recordConfig = new JSONObject(returns[0].toString());
            JSONObject recordParams = recordConfig.getJSONObject(ParameterizedWorkerFunction.KEY_PARAMS);
            if (recordParams == null)
                return;

            Iterator<String> iterator = recordParams.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = recordParams.get(key);
                String[] type = value.getClass().asSubclass(value.getClass()).getName().split("\\.");
                kvpairs.put(key, new Pair<>(value.toString(), type[type.length-1]));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        LinearLayout recordConfigContainer = new LinearLayout(mActivityRef.get());
        recordConfigContainer.setOrientation(LinearLayout.VERTICAL);
        recordConfigContainer.setPadding(getPxByDp(6), getPxByDp(6), getPxByDp(6), getPxByDp(6));
        recordConfigContainer.setBackgroundResource(R.drawable.border2);
        recordConfigContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (String key : kvpairs.keySet()) {
            LinearLayout container = new LinearLayout(mActivityRef.get());
            container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            container.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv = new TextView(mActivityRef.get());
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16);
            tv.setText(key);

            container.addView(tv);

            Pair<String, String> value = kvpairs.get(key);
            if (value != null) {
                for (String s : new String[]{value.first, value.second}) {
                    EditText et = new EditText(mActivityRef.get());
                    et.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    et.setGravity(Gravity.CENTER);
                    et.setTextSize(16);
                    et.setEnabled(false);
                    et.setText(s);
                    container.addView(et);
                }
            }

            recordConfigContainer.addView(container);
        }

        mTxInfoContentView.addView(recordConfigContainer);

        {
            View v = new View(mActivityRef.get());
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(5)));
            mTxInfoContentView.addView(v);
        }

        ControllerBase controller = mActivityRef.get().getMainController().getSubControllerByName(mContollerName);
        if (controller instanceof AudioController.TxCallback) {
            try {
                JSONObject jsonDetectors = new JSONObject(returns[1].toString());
                Iterator<String> iterator = jsonDetectors.keys();
                LinearLayout container = new LinearLayout(mActivityRef.get());
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    DetectorBase detector = ((AudioController.TxCallback) controller).getDetectorByHandle(key);
                    if (!(detector instanceof DetectorBase.Visualizable))
                        continue;

                    View v = ((DetectorBase.Visualizable) detector).getVisualizedView(mActivityRef.get(), jsonDetectors.getString(key), detector);
                    v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    container.addView(v);
                    if (iterator.hasNext()) {
                        View border = new View(mActivityRef.get());
                        border.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(4)));
                        border.setBackgroundColor(Color.argb(200, 0, 0, 0));
                        container.addView(border);
                    }
                }

                {
                    TextView v = new TextView(mActivityRef.get());
                    v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
                    v.setGravity(Gravity.CENTER);
                    v.setTextSize(20);
                    v.setText("Running Detectors");
                    mTxInfoContentView.addView(v);
                }
                container.setBackgroundResource(R.drawable.border2);
                mTxInfoContentView.addView(container);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mTxInfoContentView.invalidate();
    }

    private String genClassNameAbbr(String className, int np) {
        StringBuilder b = new StringBuilder();
        String[] patterns = className.split("\\.");
        for (int i = 0; i < np; i++) {
            b.insert(0, patterns[patterns.length - 1 - i]);
        }

        return b.toString();
    }

    private void updateAuxViewForRegisterOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        getTxAuxViewContainer().removeAllViews();

        final ArrayList<String> detectorClassNames = new ArrayList<>();
        final HashMap<String, String> classNameTable = new HashMap<>();
        detectorClassNames.add("");
        for (Class c : Constants.Detectors.CLASSES) {
            int np = 1;
            while (true) {
                String className = genClassNameAbbr(c.getName(), np);
                if (classNameTable.containsKey(className)) {
                    String s = classNameTable.get(className);
                    if (s == null)
                        continue;
                    classNameTable.remove(className);
                    classNameTable.put(genClassNameAbbr(s, ++np), s);
                    detectorClassNames.set(detectorClassNames.indexOf(className), genClassNameAbbr(s, np));
                    continue;
                }
                classNameTable.put(className, c.getName());
                detectorClassNames.add(className);
                break;
            }
        }

        Spinner spinner = ViewUtils.getSimpleSpinner(mActivityRef.get(), detectorClassNames, new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;

                onDetectorChosen(classNameTable.get(detectorClassNames.get(position)), views);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));

        getTxAuxViewContainer().addView(spinner);
        getTxAuxViewContainer().invalidate();
    }

    private void updateAuxViewForUnregisterOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        getTxAuxViewContainer().removeAllViews();

        final ArrayList<String> handles = new ArrayList<>();
        handles.add("");
        synchronized (mDetectorHandles) {
            handles.addAll(mDetectorHandles.keySet());
        }

        Spinner spinner = ViewUtils.getSimpleSpinner(mActivityRef.get(), handles, new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WorkerFunctionView.ParameterView pv = views.get(RecordDetectFunction.ATTR_CLASS_HANDLE);
                if (pv == null)
                    return;

                pv.requestValue.setText(handles.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));

        getTxAuxViewContainer().addView(spinner);
        getTxAuxViewContainer().invalidate();
    }

    private void updateAuxViewForSetParametersOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        getTxAuxViewContainer().removeAllViews();

        final ArrayList<String> handles = new ArrayList<>();
        handles.add("");
        synchronized (mDetectorHandles) {
            handles.addAll(mDetectorHandles.keySet());
        }

        Spinner spinner = ViewUtils.getSimpleSpinner(mActivityRef.get(), handles, new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;

                onDetectorChosen(handles.get(position), views);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));

        getTxAuxViewContainer().addView(spinner);
        getTxAuxViewContainer().invalidate();
    }

    private void hideAuxView() {
        if (mActivityRef.get() == null)
            return;

        mDetectorParameterViews.clear();
        getTxAuxViewContainer().removeAllViews();
        getTxAuxViewContainer().invalidate();
    }

    private void onDetectorChosen(String classNameOrHandle, final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        final String className;
        final String classHandle;
        final boolean useClassName;
        if (classNameOrHandle.split("@").length > 1) {
            className = classNameOrHandle.split("@")[0];
            classHandle = classNameOrHandle;
            useClassName = false;
        } else {
            className = classNameOrHandle;
            classHandle = null;
            useClassName = true;
        }

        final DetectorBase detector = DetectorBase.getDetectorByClassName(className);
        final WorkerFunction.Parameterizable pdetector;
        if (!(detector instanceof WorkerFunction.Parameterizable))
            return;

        pdetector = (WorkerFunction.Parameterizable) detector;
        for (WorkerFunction.Parameter p : pdetector.getParameters()) {
            WorkerFunctionView.ParameterView v = new WorkerFunctionView.ParameterView(mActivityRef.get(), p);
            v.attrLabel.setPadding(getPxByDp(1), getPxByDp(1), getPxByDp(1), getPxByDp(1));
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(50)));
            getTxAuxViewContainer().addView(v);
            mDetectorParameterViews.put(p.getAttribute(), v);
        }

        Button b = new Button(mActivityRef.get());
        b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(50)));
        b.setText("Fill the detector settings");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkerFunctionView.ParameterView classPv =
                        views.get(useClassName ? RecordDetectFunction.ATTR_CLASS_NAME : RecordDetectFunction.ATTR_CLASS_HANDLE);
                WorkerFunctionView.ParameterView paramPv = views.get(RecordDetectFunction.ATTR_PARAMS);

                for (WorkerFunctionView.ParameterView pv : mDetectorParameterViews.values()) {
                    pdetector.setParameter(pv.getAttributeLabel(), pv.getRequestValue());
                }

                if (paramPv != null)
                    paramPv.requestValue.setText(detector.getDetectorParameters().toString());

                if (classPv != null)
                    classPv.requestValue.setText(useClassName ? className : classHandle);
            }
        });

        getTxAuxViewContainer().addView(b);
        getTxAuxViewContainer().invalidate();
    }

    private int getPxByDp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
