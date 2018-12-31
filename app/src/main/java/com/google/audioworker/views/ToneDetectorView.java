package com.google.audioworker.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.audio.record.detectors.ToneDetector;
import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class ToneDetectorView extends LinearLayout {
    private final static String TAG = Constants.packageTag("ToneDetectorView");

    private SparseArray<View> mIsDetectedOrNotViews;
    private SparseBooleanArray mIsDetectedOrNot;
    private ToneDetectorViewHandler mHandler;

    public ToneDetectorView(Context context) {
        super(context);
        init();
    }

    public ToneDetectorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToneDetectorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        mIsDetectedOrNotViews = new SparseArray<>();
        mIsDetectedOrNot = new SparseBooleanArray();
        mHandler = new ToneDetectorViewHandler(this);
    }

    static public ToneDetectorView createView(Context ctx, String token, DetectorBase detector) {
        if (ctx == null)
            return null;

        Log.d(TAG, "createView called");

        ToneDetectorView view = new ToneDetectorView(ctx);
        try {
            JSONObject jsonDetector = new JSONObject(token);
            {
                Iterator<String> iterator = jsonDetector.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (key.equals(ToneDetector.INFO_KEY_UNIT) || key.equals(ToneDetector.INFO_KEY_TARGETS))
                        continue;

                    LinearLayout container = new LinearLayout(ctx);
                    container.setOrientation(HORIZONTAL);
                    container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                    LayoutParams params;

                    TextView tv = new TextView(ctx);
                    params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 4);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    tv.setLayoutParams(params);
                    tv.setTextSize(16);
                    tv.setGravity(Gravity.CENTER);
                    tv.setText(key);

                    EditText et = new EditText(ctx);
                    params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 4);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    et.setLayoutParams(params);
                    et.setTextSize(16);
                    et.setGravity(Gravity.CENTER);
                    et.setText(jsonDetector.getString(key));
                    et.setEnabled(false);

                    TextView unitView = new TextView(ctx);
                    params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    unitView.setLayoutParams(params);
                    unitView.setTextSize(16);
                    unitView.setGravity(Gravity.CENTER);
                    if (jsonDetector.has(ToneDetector.INFO_KEY_UNIT) && jsonDetector.getJSONObject(ToneDetector.INFO_KEY_UNIT).has(key)) {
                        unitView.setText(jsonDetector.getJSONObject(ToneDetector.INFO_KEY_UNIT).getString(key));
                    }

                    container.addView(tv);
                    container.addView(et);
                    container.addView(unitView);
                    if (key.equals(ToneDetector.INFO_KEY_HANDLE)) {
                        params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
                        params.gravity = Gravity.CENTER_VERTICAL;
                        tv.setLayoutParams(params);
                        container.removeView(unitView);
                    }

                    view.addView(container);
                }
            }

            view.addView(view.getBorder());

            JSONArray jsonTargets;
            {
                TextView tv = new TextView(ctx);
                tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, view.getPxByDp(40)));
                tv.setTextSize(16);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                if (jsonDetector.has(ToneDetector.INFO_KEY_TARGETS) &&
                        jsonDetector.getJSONArray(ToneDetector.INFO_KEY_TARGETS) != null &&
                            jsonDetector.getJSONArray(ToneDetector.INFO_KEY_TARGETS).length() > 0) {
                    jsonTargets = jsonDetector.getJSONArray(ToneDetector.INFO_KEY_TARGETS);
                    tv.setText("Targets (" + jsonTargets.length() + ")");
                } else {
                    jsonTargets = new JSONArray();
                    tv.setText("No Targets");
                }

                view.addView(tv);
            }

            for (int i = 0; i < jsonTargets.length(); i++) {
                LinearLayout parameterContainer = new LinearLayout(ctx);
                parameterContainer.setOrientation(VERTICAL);
                parameterContainer.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 7));
                JSONObject jsonTarget = jsonTargets.getJSONObject(i);
                if (jsonTarget != null) {
                    Iterator<String> iterator = jsonTarget.keys();
                    while (iterator.hasNext()) {
                        String key = iterator.next();

                        LinearLayout container = new LinearLayout(ctx);
                        container.setOrientation(HORIZONTAL);
                        container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                        {
                            TextView tv = new TextView(ctx);
                            tv.setLayoutParams(new LayoutParams(0, view.getPxByDp(40), 1));
                            tv.setTextSize(16);
                            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                            tv.setText(String.valueOf(i));

                            container.addView(tv);
                        }

                        {
                            TextView tv = new TextView(ctx);
                            tv.setLayoutParams(new LayoutParams(0, view.getPxByDp(40), 5));
                            tv.setTextSize(16);
                            tv.setGravity(Gravity.CENTER);
                            tv.setText(key);

                            container.addView(tv);
                        }

                        {
                            EditText et = new EditText(ctx);
                            et.setLayoutParams(new LayoutParams(0, view.getPxByDp(40), 5));
                            et.setTextSize(16);
                            et.setGravity(Gravity.CENTER);
                            et.setText(jsonTarget.getInt(key) + " Hz");
                            et.setEnabled(false);

                            container.addView(et);
                        }

                        parameterContainer.addView(container);
                    }
                }

                LinearLayout container = new LinearLayout(ctx);
                container.setOrientation(HORIZONTAL);
                container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                container.addView(parameterContainer);

                {
                    View v = new View(ctx);
                    v.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
                    container.addView(v);
                    view.addView(container);
                    view.mIsDetectedOrNotViews.put(i, v);
                    view.mIsDetectedOrNot.put(i, true);
                    view.setDetected(i, false);
                }

                {
                    View border = view.getBorder();
                    border.setBackgroundColor(Color.argb(0, 0, 0, 0));
                    view.addView(border);
                }
            }

            detector.registerDetectionListener(view.mHandler);
            view.setPadding(view.getPxByDp(6), view.getPxByDp(6), view.getPxByDp(6), view.getPxByDp(6));
            return view;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getPxByDp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private View getBorder() {
        View v = new View(getContext());
        v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, getPxByDp(1)));
        v.setBackgroundColor(Color.argb(80, 0, 0, 0));
        return v;
    }

    private void setDetected(int id, boolean detected) {
        if (mIsDetectedOrNotViews.get(id) == null || mIsDetectedOrNot.get(id) == detected)
            return;

        if (detected)
            mIsDetectedOrNotViews.get(id).setBackgroundColor(Color.argb(255, 0, 255, 0));
        else
            mIsDetectedOrNotViews.get(id).setBackgroundColor(Color.argb(100, 0, 0, 0));
    }

    private static class ToneDetectorViewHandler extends Handler implements DetectorBase.DetectionListener {
        private WeakReference<ToneDetectorView> mRef;

        ToneDetectorViewHandler(ToneDetectorView v) {
            mRef = new WeakReference<>(v);
        }

        @Override
        public void onTargetDetected(SparseArray<? extends DetectorBase.Target> targets) {
            SparseBooleanArray results = new SparseBooleanArray();
            for (int i = 0; i < targets.size(); i++)
                results.put(targets.keyAt(i), true);

            sendMessage(obtainMessage(0, results));
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef.get() == null || !(msg.obj instanceof SparseBooleanArray))
                return;

            SparseBooleanArray results = (SparseBooleanArray) msg.obj;
            for (int i = 0; i < mRef.get().mIsDetectedOrNotViews.size(); i++) {
                int id = mRef.get().mIsDetectedOrNotViews.keyAt(i);
                mRef.get().setDetected(id, results.get(id, false));
            }
        }
    }
}
