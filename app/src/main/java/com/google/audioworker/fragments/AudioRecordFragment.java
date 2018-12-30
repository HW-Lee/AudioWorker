package com.google.audioworker.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.audioworker.R;
import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.functions.audio.record.RecordInfoFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.functions.controllers.RecordController;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.views.DataView;
import com.google.audioworker.views.WorkerFunctionView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AudioRecordFragment extends WorkerFragment
        implements RecordController.RecordRunnable.RecordDataListener, ControllerBase.ControllerStateListener,
            WorkerFunctionView.ActionSelectedListener {
    private final static String TAG = Constants.packageTag("AudioRecordFragment");

    private ArrayList<String> mSupportIntents;
    private DataView[] mSignalViews;

    private LinearLayout mAuxViewContainer;
    private HashMap<String, WorkerFunctionView.ParameterView> mDetectorParameterViews;

    private final HashMap<String, String> mDetectorHandles = new HashMap<>();

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

        mDetectorParameterViews.clear();
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

        WorkerFunctionView workerFunctionView = mActivityRef.get().findViewById(R.id.record_func_attr_container);
        workerFunctionView.setSupportedIntentActions(mSupportIntents, this);
        workerFunctionView.setController(controller);

        mSignalViews = new DataView[2];
        mSignalViews[0] = mActivityRef.get().findViewById(R.id.record_signal_1);
        mSignalViews[1] = mActivityRef.get().findViewById(R.id.record_signal_2);
        for (DataView v : mSignalViews) {
            v.setGridSlotsY(4);
            v.setGridSlotsX(10);
        }

        mDetectorParameterViews = new HashMap<>();
        mAuxViewContainer = mActivityRef.get().findViewById(R.id.record_aux_view_container);
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

    @Override
    public void onActionSelected(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (action == null) {
            hideAuxView();
            return;
        }

        switch (action) {
            case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
            case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
            case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
                updateAuxView(action, views);
                break;

            default:
                hideAuxView();
                break;
        }
    }

    @Override
    public void onFunctionSent(WorkerFunction function) {
    }

    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        if (mActivityRef.get() == null)
            return;

        ControllerBase controller = mActivityRef.get().getMainController();
        controller.execute(new RecordInfoFunction(), new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(WorkerFunction.Ack ack) {
                Object[] returns = ack.getReturns();
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
        });
    }

    private String genClassNameAbbr(String className, int np) {
        StringBuilder b = new StringBuilder();
        String[] patterns = className.split("\\.");
        for (int i = 0; i < np; i++) {
            b.insert(0, patterns[patterns.length - 1 - i]);
        }

        return b.toString();
    }

    private void updateAuxView(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        switch (action) {
            case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
                updateAuxViewForRegisterOperation(views);
                return;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
                updateAuxViewForUnregisterOperation(views);
                return;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
                updateAuxViewForSetParametersOperation(views);
                return;

            default:
                break;
        }
    }

    private void updateAuxViewForRegisterOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        mAuxViewContainer.removeAllViews();

        final ArrayList<String> detectorClassNames = new ArrayList<>();
        final HashMap<String, String> classNameTable = new HashMap<>();
        detectorClassNames.add("");
        for (Class c : Constants.Detectors.CLASSES) {
            int np = 1;
            while (true) {
                String className = genClassNameAbbr(c.getName(), np);
                if (classNameTable.containsKey(className)) {
                    String s = classNameTable.get(className);
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

        Spinner spinner = new Spinner(mActivityRef.get());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
        spinner.setAdapter(new ArrayAdapter<>(mActivityRef.get(), android.R.layout.simple_list_item_1, android.R.id.text1, detectorClassNames));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        mAuxViewContainer.addView(spinner);
        mAuxViewContainer.invalidate();
    }

    private void updateAuxViewForUnregisterOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        mAuxViewContainer.removeAllViews();

        final ArrayList<String> handles = new ArrayList<>();
        handles.add("");
        synchronized (mDetectorHandles) {
            handles.addAll(mDetectorHandles.keySet());
        }

        Spinner spinner = new Spinner(mActivityRef.get());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
        spinner.setAdapter(new ArrayAdapter<>(mActivityRef.get(), android.R.layout.simple_list_item_1, android.R.id.text1, handles));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        mAuxViewContainer.addView(spinner);
        mAuxViewContainer.invalidate();
    }

    private void updateAuxViewForSetParametersOperation(final HashMap<String, WorkerFunctionView.ParameterView> views) {
        if (mActivityRef.get() == null)
            return;

        mAuxViewContainer.removeAllViews();

        final ArrayList<String> handles = new ArrayList<>();
        handles.add("");
        synchronized (mDetectorHandles) {
            handles.addAll(mDetectorHandles.keySet());
        }

        Spinner spinner = new Spinner(mActivityRef.get());
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(40)));
        spinner.setAdapter(new ArrayAdapter<>(mActivityRef.get(), android.R.layout.simple_list_item_1, android.R.id.text1, handles));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        mAuxViewContainer.addView(spinner);
        mAuxViewContainer.invalidate();
    }

    private void hideAuxView() {
        if (mActivityRef.get() == null)
            return;

        mDetectorParameterViews.clear();
        mAuxViewContainer.removeAllViews();
        mAuxViewContainer.invalidate();
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

        Class[] types = {DetectorBase.DetectionListener.class, String.class};
        try {
            Object obj = Class.forName(className).getConstructor(types).newInstance(new DetectorBase.DetectionListener() {
                @Override
                public void onTargetDetected(SparseArray<? extends DetectorBase.Target> targets) {
                }
            }, null);
            final DetectorBase detector;
            if (!(obj instanceof DetectorBase))
                return;

            detector = (DetectorBase) obj;
            final WorkerFunction.Parameterizable pdetector;
            if (!(detector instanceof WorkerFunction.Parameterizable))
                return;

            pdetector = (WorkerFunction.Parameterizable) detector;
            for (WorkerFunction.Parameter p : pdetector.getParameters()) {
                WorkerFunctionView.ParameterView v = new WorkerFunctionView.ParameterView(mActivityRef.get(), p);
                v.attrLabel.setPadding(getPxByDp(1), getPxByDp(1), getPxByDp(1), getPxByDp(1));
                v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getPxByDp(50)));
                mAuxViewContainer.addView(v);
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

            mAuxViewContainer.addView(b);
            mAuxViewContainer.invalidate();
        } catch (ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | java.lang.InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            Toast.makeText(mActivityRef.get(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int getPxByDp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
