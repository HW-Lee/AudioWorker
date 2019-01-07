package com.google.audioworker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.util.Pair;
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
import com.google.audioworker.views.ViewUtils;
import com.google.audioworker.views.WorkerFunctionView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class AudioTxSupportFragment extends WorkerFragment
        implements RecordController.RecordRunnable.RecordDataListener, AudioFragment.TxSupport,
            AudioFragment.WorkerFunctionAuxSupport, WorkerFunctionView.ActionSelectedListener {
    private final static String TAG = Constants.packageTag("AudioTxSupportFragment");

    private final Factory.Bundle mBundle = new Factory.Bundle();

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);
    }

    @CallSuper
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Factory.init(this, mBundle);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        Factory.init(this, mBundle);
    }

    @CallSuper
    @Override
    public void onDestroy() {
        super.onDestroy();
        Factory.onDestroy(this, mBundle);
    }

    @Override
    public void onDataUpdated(List<? extends Double>[] signal, RecordStartFunction function) {
        getTxDataView().plot(signal);
    }

    @CallSuper
    @Override
    public void onActionSelected(String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
        Factory.onActionSelected(this, mBundle, action, views);
    }

    @Override
    public void onFunctionSent(WorkerFunction function) {
    }

    @CallSuper
    @Override
    public void onFunctionAckReceived(WorkerFunction.Ack ack) {
        Factory.onFunctionAckReceived(this, mBundle, ack);
    }

    static class Factory {
        static class Bundle {
            String mContollerName;
            HashMap<String, WorkerFunctionView.ParameterView> mDetectorParameterViews;
            FrameLayout mTxInfoCollapseToggleView;
            LinearLayout mTxInfoContentView;
            final HashMap<String, String> mDetectorHandles = new HashMap<>();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void onDestroy(T fragment, Bundle bundle) {
            if (fragment.mActivityRef.get() == null)
                return;

            ControllerBase controller = fragment.mActivityRef.get().getMainController().getSubControllerByName(bundle.mContollerName);
            if (controller instanceof AudioController.TxCallback && fragment instanceof RecordController.RecordRunnable.RecordDataListener)
                ((AudioController.TxCallback) controller).unregisterDataListener((RecordController.RecordRunnable.RecordDataListener) fragment);

            bundle.mDetectorParameterViews.clear();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void init(final T fragment, Bundle bundle) {
            if (fragment == null || fragment.mActivityRef.get() == null)
                return;

            fragment.initTxSupport();

            bundle.mContollerName = fragment.getControllerName();
            ControllerBase controller = fragment.mActivityRef.get().getMainController().getSubControllerByName(bundle.mContollerName);
            if (!(controller instanceof AudioController.TxCallback))
                return;

            if (fragment instanceof RecordController.RecordRunnable.RecordDataListener)
                ((AudioController.TxCallback) controller).registerDataListener((RecordController.RecordRunnable.RecordDataListener) fragment);

            if (fragment instanceof AudioFragment.WorkerFunctionAuxSupport && fragment instanceof WorkerFunctionView.ActionSelectedListener) {
                WorkerFunctionView workerFunctionView = ((AudioFragment.WorkerFunctionAuxSupport) fragment).getWorkerFunctionView();
                workerFunctionView.setSupportedIntentActions(
                        ((AudioFragment.WorkerFunctionAuxSupport) fragment).getSupportedIntents(), (WorkerFunctionView.ActionSelectedListener) fragment);
                workerFunctionView.setController(fragment.mActivityRef.get().getMainController().getSubControllerByName(fragment.getControllerName()));
            }

            if (fragment.getTxDataView() != null) {
                fragment.getTxDataView().setGridSlotsY(4);
                fragment.getTxDataView().setGridSlotsX(10);
            }

            bundle.mDetectorParameterViews = new HashMap<>();

            Factory.initInfoView(fragment, bundle, fragment.getTxInfoTitle());
            LinearLayout txInfoContainer = fragment.getTxInfoContainer();
            txInfoContainer.removeAllViews();
            txInfoContainer.addView(bundle.mTxInfoCollapseToggleView);
            txInfoContainer.addView(bundle.mTxInfoContentView);

            if (fragment instanceof WorkerFunctionView.ActionSelectedListener) {
                controller.execute(fragment.getInfoRequestFunction(), new WorkerFunction.WorkerFunctionListener() {
                    @Override
                    public void onAckReceived(WorkerFunction.Ack ack) {
                        ((WorkerFunctionView.ActionSelectedListener) fragment).onFunctionAckReceived(ack);
                    }
                }, true);
            }
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void initInfoView(T fragment, final Bundle bundle, String title) {
            bundle.mTxInfoCollapseToggleView = new FrameLayout(fragment.mActivityRef.get());
            bundle.mTxInfoCollapseToggleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));
            bundle.mTxInfoContentView = new LinearLayout(fragment.mActivityRef.get());
            bundle.mTxInfoContentView.setOrientation(LinearLayout.VERTICAL);
            bundle.mTxInfoContentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            bundle.mTxInfoContentView.setVisibility(View.VISIBLE);

            TextView tv = new TextView(fragment.mActivityRef.get());
            tv.setText(title);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(20);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            final TextView toggle = new TextView(fragment.mActivityRef.get());
            toggle.setText("v");
            toggle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            toggle.setTextSize(20);
            toggle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (bundle.mTxInfoContentView.getVisibility() == View.GONE) {
                        toggle.setText("v");
                        bundle.mTxInfoContentView.setVisibility(View.VISIBLE);
                    } else {
                        toggle.setText(">");
                        bundle.mTxInfoContentView.setVisibility(View.GONE);
                    }
                }
            });

            bundle.mTxInfoCollapseToggleView.addView(tv);
            bundle.mTxInfoCollapseToggleView.addView(toggle);
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void updateTxInfoContent(T fragment, Bundle bundle, Object[] returns) {
            bundle.mTxInfoContentView.removeAllViews();
            if (returns.length < 2 || fragment.mActivityRef.get() == null)
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

            LinearLayout recordConfigContainer = new LinearLayout(fragment.mActivityRef.get());
            recordConfigContainer.setOrientation(LinearLayout.VERTICAL);
            recordConfigContainer.setPadding(fragment.getPxByDp(6), fragment.getPxByDp(6), fragment.getPxByDp(6), fragment.getPxByDp(6));
            recordConfigContainer.setBackgroundResource(R.drawable.border2);
            recordConfigContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            for (String key : kvpairs.keySet()) {
                LinearLayout container = new LinearLayout(fragment.mActivityRef.get());
                container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                container.setOrientation(LinearLayout.HORIZONTAL);

                TextView tv = new TextView(fragment.mActivityRef.get());
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(16);
                tv.setText(key);

                container.addView(tv);

                Pair<String, String> value = kvpairs.get(key);
                if (value != null) {
                    for (String s : new String[]{value.first, value.second}) {
                        EditText et = new EditText(fragment.mActivityRef.get());
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

            bundle.mTxInfoContentView.addView(recordConfigContainer);

            {
                View v = new View(fragment.mActivityRef.get());
                v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(5)));
                bundle.mTxInfoContentView.addView(v);
            }

            ControllerBase controller = fragment.mActivityRef.get().getMainController().getSubControllerByName(fragment.getControllerName());
            if (controller instanceof AudioController.TxCallback) {
                try {
                    JSONObject jsonDetectors = new JSONObject(returns[1].toString());
                    Iterator<String> iterator = jsonDetectors.keys();
                    LinearLayout container = new LinearLayout(fragment.mActivityRef.get());
                    container.setOrientation(LinearLayout.VERTICAL);
                    container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        DetectorBase detector = ((AudioController.TxCallback) controller).getDetectorByHandle(key);
                        if (!(detector instanceof DetectorBase.Visualizable))
                            continue;

                        View v = ((DetectorBase.Visualizable) detector).getVisualizedView(fragment.mActivityRef.get(), jsonDetectors.getString(key), detector);
                        if (v != null) {
                            ((AudioController.TxCallback) controller).setDetectionListener(key, (DetectorBase.DetectionListener) v);
                            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            container.addView(v);
                        }
                        if (iterator.hasNext()) {
                            View border = ViewUtils.getHorizontalBorder(fragment.mActivityRef.get(), fragment.getPxByDp(4));
                            border.setBackgroundColor(Color.argb(200, 0, 0, 0));
                            container.addView(border);
                        }
                    }

                    {
                        TextView v = new TextView(fragment.mActivityRef.get());
                        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));
                        v.setGravity(Gravity.CENTER);
                        v.setTextSize(20);
                        v.setText("Running Detectors");
                        bundle.mTxInfoContentView.addView(v);
                    }
                    container.setBackgroundResource(R.drawable.border2);
                    bundle.mTxInfoContentView.addView(container);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            bundle.mTxInfoContentView.invalidate();
        }

        static private String genClassNameAbbr(String className, int np) {
            StringBuilder b = new StringBuilder();
            String[] patterns = className.split("\\.");
            for (int i = 0; i < np; i++) {
                b.insert(0, patterns[patterns.length - 1 - i]);
            }

            return b.toString();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void updateAuxViewForRegisterOperation(final T fragment, final Bundle bundle,
                                               final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (fragment.mActivityRef.get() == null)
                return;

            fragment.getTxAuxViewContainer().removeAllViews();

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

            Spinner spinner = ViewUtils.getSimpleSpinner(fragment.mActivityRef.get(), detectorClassNames, new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0)
                        return;

                    Factory.onDetectorChosen(fragment, bundle, classNameTable.get(detectorClassNames.get(position)), views);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));

            fragment.getTxAuxViewContainer().addView(spinner);
            fragment.getTxAuxViewContainer().invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void onDetectorChosen(T fragment, Bundle bundle,
                              String classNameOrHandle, final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (fragment.mActivityRef.get() == null)
                return;

            final HashMap<String, WorkerFunctionView.ParameterView> detectorParameterViews = new HashMap<>(bundle.mDetectorParameterViews);
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
                WorkerFunctionView.ParameterView v = new WorkerFunctionView.ParameterView(fragment.mActivityRef.get(), p);
                v.attrLabel.setPadding(fragment.getPxByDp(1), fragment.getPxByDp(1), fragment.getPxByDp(1), fragment.getPxByDp(1));
                v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(50)));
                fragment.getTxAuxViewContainer().addView(v);
                detectorParameterViews.put(p.getAttribute(), v);
            }

            Button b = new Button(fragment.mActivityRef.get());
            b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(50)));
            b.setText("Fill the detector settings");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WorkerFunctionView.ParameterView classPv =
                            views.get(useClassName ? RecordDetectFunction.ATTR_CLASS_NAME : RecordDetectFunction.ATTR_CLASS_HANDLE);
                    WorkerFunctionView.ParameterView paramPv = views.get(RecordDetectFunction.ATTR_PARAMS);

                    for (WorkerFunctionView.ParameterView pv : detectorParameterViews.values()) {
                        pdetector.setParameter(pv.getAttributeLabel(), pv.getRequestValue());
                    }

                    if (paramPv != null)
                        paramPv.requestValue.setText(detector.getDetectorParameters().toString());

                    if (classPv != null)
                        classPv.requestValue.setText(useClassName ? className : classHandle);
                }
            });

            fragment.getTxAuxViewContainer().addView(b);
            fragment.getTxAuxViewContainer().invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void updateAuxViewForUnregisterOperation(T fragment, Bundle bundle, final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (fragment.mActivityRef.get() == null)
                return;

            fragment.getTxAuxViewContainer().removeAllViews();

            final ArrayList<String> handles = new ArrayList<>();
            handles.add("");
            synchronized (bundle.mDetectorHandles) {
                handles.addAll(bundle.mDetectorHandles.keySet());
            }

            Spinner spinner = ViewUtils.getSimpleSpinner(fragment.mActivityRef.get(), handles, new AdapterView.OnItemSelectedListener() {
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
            spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));

            fragment.getTxAuxViewContainer().addView(spinner);
            fragment.getTxAuxViewContainer().invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void updateAuxViewForSetParametersOperation(final T fragment, final Bundle bundle,
                                                    final HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (fragment.mActivityRef.get() == null)
                return;

            fragment.getTxAuxViewContainer().removeAllViews();

            final ArrayList<String> handles = new ArrayList<>();
            handles.add("");
            synchronized (bundle.mDetectorHandles) {
                handles.addAll(bundle.mDetectorHandles.keySet());
            }

            Spinner spinner = ViewUtils.getSimpleSpinner(fragment.mActivityRef.get(), handles, new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0)
                        return;

                    Factory.onDetectorChosen(fragment, bundle, handles.get(position), views);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fragment.getPxByDp(40)));

            fragment.getTxAuxViewContainer().addView(spinner);
            fragment.getTxAuxViewContainer().invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void onFunctionAckReceived(final T fragment, final Bundle bundle, WorkerFunction.Ack ack) {
            if (fragment.mActivityRef.get() == null)
                return;

            ControllerBase controller = fragment.mActivityRef.get().getMainController();
            controller.execute(fragment.getInfoRequestFunction(), new WorkerFunction.WorkerFunctionListener() {
                @Override
                public void onAckReceived(WorkerFunction.Ack ack) {
                    if (fragment.mActivityRef.get() == null)
                        return;

                    final Object[] returns = fragment.getTxReturns(ack);

                    Factory.updateDetectors(bundle, returns);
                    fragment.mActivityRef.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Factory.updateTxInfoContent(fragment, bundle, returns);
                            if (fragment instanceof AudioFragment.WorkerFunctionAuxSupport &&
                                    ((AudioFragment.WorkerFunctionAuxSupport) fragment).getWorkerFunctionView() != null) {
                                ((AudioFragment.WorkerFunctionAuxSupport) fragment).getWorkerFunctionView().updateParameterView();
                            }
                        }
                    });
                }
            }, true);
        }

        static private void updateDetectors(Bundle bundle, Object[] returns) {
            if (returns.length < 2)
                return;

            try {
                JSONObject detectorInfo = new JSONObject(returns[1].toString());
                synchronized (bundle.mDetectorHandles) {
                    bundle.mDetectorHandles.clear();
                    Iterator<String> iterator = detectorInfo.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        bundle.mDetectorHandles.put(key, detectorInfo.getString(key));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        static private <T extends WorkerFragment & AudioFragment.TxSupport>
        void updateTxAuxView(T fragment, Bundle bundle, String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
            switch (action) {
                case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
                case Constants.MasterInterface.INTENT_VOIP_DETECT_REGISTER:
                    Factory.updateAuxViewForRegisterOperation(fragment, bundle, views);
                    return;
                case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
                case Constants.MasterInterface.INTENT_VOIP_DETECT_UNREGISTER:
                    Factory.updateAuxViewForUnregisterOperation(fragment, bundle, views);
                    return;
                case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
                case Constants.MasterInterface.INTENT_VOIP_DETECT_SETPARAMS:
                    Factory.updateAuxViewForSetParametersOperation(fragment, bundle, views);
                    return;

                default:
                    break;
            }
        }

        static private <T extends WorkerFragment & AudioFragment.TxSupport>
        void hideTxAuxView(T fragment, Bundle bundle) {
            if (fragment.mActivityRef.get() == null)
                return;

            bundle.mDetectorParameterViews.clear();
            fragment.getTxAuxViewContainer().removeAllViews();
            fragment.getTxAuxViewContainer().invalidate();
        }

        static <T extends WorkerFragment & AudioFragment.TxSupport>
        void onActionSelected(T fragment, Bundle bundle, String action, HashMap<String, WorkerFunctionView.ParameterView> views) {
            if (action == null || !(fragment instanceof AudioFragment.WorkerFunctionAuxSupport) ||
                    !((AudioFragment.WorkerFunctionAuxSupport) fragment).needToShowAuxView(action)) {
                Factory.hideTxAuxView(fragment, bundle);
            } else {
                Factory.updateTxAuxView(fragment, bundle, action, views);
            }
        }
    }
}
