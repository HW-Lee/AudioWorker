package com.google.audioworker.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.ControllerBase;
import com.google.audioworker.utils.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class WorkerFunctionView extends LinearLayout {
    private final static String TAG = Constants.packageTag("WorkerFunctionView");

    private ControllerBase mController;

    private Spinner mSpinner;
    private String mSelectedAction;
    private ActionSelectedListener mListener;
    private LinearLayout mParameterViewContainer;
    private HashMap<String, ParameterView> mParameterViews;
    private Button mSendFunctionBtn;
    private String mListenedFunctionId;

    public interface ActionSelectedListener {
        void onActionSelected(String action, HashMap<String, ParameterView> views);
        void onFunctionSent(WorkerFunction function);
        void onFunctionAckReceived(WorkerFunction.Ack ack);
    }

    public WorkerFunctionView(Context context) {
        super(context);
        init();
    }

    public WorkerFunctionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WorkerFunctionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setController(@NonNull ControllerBase controller) {
        mController = controller;
    }

    public void setSupportedIntentActions(Collection<? extends String> action) {
        setSupportedIntentActions(action, null);
    }

    public void setSupportedIntentActions(Collection<? extends String> actions, ActionSelectedListener l) {
        final ArrayList<String> selections = new ArrayList<>();
        selections.add("");
        selections.addAll(actions);
        mListener = l;
        mSpinner.setAdapter(ViewUtils.getSimpleAdapter(getContext(), selections));

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedAction = position > 0 ? selections.get(position) : null;
                updateParameterView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ((ArrayAdapter) mSpinner.getAdapter()).notifyDataSetChanged();
    }

    private void init() {
        this.setOrientation(LinearLayout.VERTICAL);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, getPxByDp(50));
        mSpinner = new Spinner(getContext());
        mSpinner.setLayoutParams(params);

        mParameterViewContainer = new LinearLayout(getContext());
        mParameterViewContainer.setOrientation(LinearLayout.VERTICAL);
        mParameterViewContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mParameterViews = new HashMap<>();
        mSendFunctionBtn = new Button(getContext());
        mSendFunctionBtn.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPxByDp(50)));
        mSendFunctionBtn.setText("Send Function");
        mSendFunctionBtn.setEnabled(true);

        addView(mSpinner);
        addView(mParameterViewContainer);
    }

    private int getPxByDp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void updateParameterView() {
        mParameterViewContainer.removeAllViews();
        mParameterViewContainer.invalidate();
        mParameterViews.clear();

        if (mSelectedAction == null) {
            if (mListener != null)
                mListener.onActionSelected(mSelectedAction, mParameterViews);
            return;
        }

        WorkerFunction function = CommandHelper.getFunction(new Intent(mSelectedAction));
        if (function == null) {
            if (mListener != null)
                mListener.onActionSelected(mSelectedAction, mParameterViews);
            return;
        }

        ArrayList<WorkerFunction.Parameter> parameters = new ArrayList<>();
        if (function instanceof WorkerFunction.Parameterizable) {
            Collections.addAll(parameters, ((WorkerFunction.Parameterizable) function).getParameters());
        }
        Collections.sort(parameters, new Comparator<WorkerFunction.Parameter>() {
            @Override
            public int compare(WorkerFunction.Parameter p1, WorkerFunction.Parameter p2) {
                if (p1.isRequired() == p2.isRequired())
                    return 0;
                if (p1.isRequired())
                    return -1;
                return 1;
            }
        });

        for (WorkerFunction.Parameter p : parameters) {
            ParameterView v = new ParameterView(getContext(), p);
            v.attrLabel.setPadding(getPxByDp(1), getPxByDp(1), getPxByDp(1), getPxByDp(1));
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            mParameterViewContainer.addView(v);
            mParameterViews.put(p.getAttribute(), v);
        }

        mSendFunctionBtn.setText("Send Function");
        mSendFunctionBtn.setEnabled(true);
        mSendFunctionBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEnabled()) {
                    mSendFunctionBtn.setText("Sending...");
                    mSendFunctionBtn.setEnabled(false);
                    sendFunction();
                }
            }
        });
        mParameterViewContainer.addView(mSendFunctionBtn);

        mParameterViewContainer.invalidate();

        if (mListener != null)
            mListener.onActionSelected(mSelectedAction, mParameterViews);
    }

    private void sendFunction() {
        if (mController == null) {
            showAck(null);
            return;
        }

        WorkerFunction function = CommandHelper.getFunction(new Intent(mSelectedAction));
        if (function == null) {
            showAck(null);
            return;
        }

        if (function instanceof WorkerFunction.Parameterizable) {
            for (ParameterView pv : mParameterViews.values()) {
                ((WorkerFunction.Parameterizable) function).setParameter(pv.getAttributeLabel(), pv.getRequestValue());
            }
        }

        String timestamp = new CommandHelper.Command().getCommandId().split("::")[1];
        function.setCommandId(TAG + "-" + timestamp);
        Toast.makeText(getContext(), "Send function: " + function, Toast.LENGTH_LONG).show();
        mListenedFunctionId = function.getCommandId();
        mController.execute(function, new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(final WorkerFunction.Ack ack) {
                if (mListener != null)
                    mListener.onFunctionAckReceived(ack);
                if (!ack.getTarget().equals(mListenedFunctionId))
                    return;

                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAck(ack);
                    }
                });
            }
        });
        if (mListener != null)
            mListener.onFunctionSent(function);
    }

    private void showAck(WorkerFunction.Ack ack) {
        if (ack != null)
            Toast.makeText(getContext(), "receive ack: " + ack, Toast.LENGTH_LONG).show();
        mSendFunctionBtn.setText("Send Function");
        mSendFunctionBtn.setEnabled(true);
    }

    public static class ParameterView extends LinearLayout {
        public TextView attrLabel;
        public EditText defaultValue;
        public EditText requestValue;

        public ParameterView(Context context, WorkerFunction.Parameter parameter) {
            super(context);
            init(parameter);
        }

        private void init(WorkerFunction.Parameter parameter) {
            setOrientation(LinearLayout.HORIZONTAL);

            attrLabel = new TextView(getContext());
            defaultValue = new EditText(getContext());
            defaultValue.setEnabled(false);
            requestValue = new EditText(getContext());
            requestValue.setEnabled(true);

            LayoutParams params;

            params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            params.gravity = Gravity.BOTTOM;
            attrLabel.setLayoutParams(params);
            defaultValue.setLayoutParams(params);

            params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
            params.gravity = Gravity.BOTTOM;
            requestValue.setLayoutParams(params);

            attrLabel.setGravity(Gravity.CENTER);
            defaultValue.setGravity(Gravity.CENTER);
            requestValue.setGravity(Gravity.CENTER);

            attrLabel.setText(parameter.getAttribute());
            if (!parameter.isRequired()) {
                if (parameter.getValue() != null)
                    defaultValue.setText(parameter.getValue().toString());
                else
                    defaultValue.setText("NULL");
            } else {
                defaultValue.setText("Required");
            }

            addView(attrLabel);
            addView(defaultValue);
            addView(requestValue);
        }

        public String getAttributeLabel() {
            return attrLabel.getText().toString();
        }

        public Object getRequestValue() {
            try {
                return Integer.valueOf(requestValue.getText().toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            try {
                return Float.valueOf(requestValue.getText().toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            return requestValue.getText().toString();
        }
    }
}
