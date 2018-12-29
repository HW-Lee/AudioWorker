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

public class WorkerFunctionView extends LinearLayout {
    private final static String TAG = Constants.packageTag("WorkerFunctionView");

    private ControllerBase mController;

    private Spinner mSpinner;
    private String mSelectedAction;
    private LinearLayout mParameterViewContainer;
    private ArrayList<ParameterView> mParameterViews;
    private Button mSendFunctionBtn;

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

    public void setSupportedIntentActions(Collection<? extends String> actions) {
        final ArrayList<String> selections = new ArrayList<>();
        selections.add("");
        selections.addAll(actions);
        mSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, selections));

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

        mParameterViews = new ArrayList<>();
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

        if (mSelectedAction == null)
            return;

        WorkerFunction function = CommandHelper.getFunction(new Intent(mSelectedAction));
        if (function == null)
            return;

        ArrayList<WorkerFunction.Parameter> parameters = new ArrayList<>();
        Collections.addAll(parameters, function.getParameters());
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
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, getPxByDp(50)));
            mParameterViewContainer.addView(v);
            mParameterViews.add(v);
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
    }

    private void sendFunction() {
        if (mController == null)
            return;

        WorkerFunction function = CommandHelper.getFunction(new Intent(mSelectedAction));
        if (function == null)
            return;

        for (ParameterView pv : mParameterViews) {
            function.setParameter(pv.getAttributeLabel(), pv.getRequestValue());
        }

        String timestamp = new CommandHelper.Command().getCommandId().split("::")[1];
        function.setCommandId(TAG + "-" + timestamp);
        Toast.makeText(getContext(), "Send function: " + function, Toast.LENGTH_LONG).show();
        mController.execute(function, new WorkerFunction.WorkerFunctionListener() {
            @Override
            public void onAckReceived(final WorkerFunction.Ack ack) {
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAck(ack);
                    }
                });
            }
        });
    }

    private void showAck(WorkerFunction.Ack ack) {
        Toast.makeText(getContext(), "receive ack: " + ack, Toast.LENGTH_LONG).show();
        mSendFunctionBtn.setText("Send Function");
        mSendFunctionBtn.setEnabled(true);
    }

    private class ParameterView extends LinearLayout {
        TextView attrLabel;
        EditText currentValue;
        EditText requestValue;

        public ParameterView(Context context, WorkerFunction.Parameter parameter) {
            super(context);
            init(parameter);
        }

        private void init(WorkerFunction.Parameter parameter) {
            setOrientation(LinearLayout.HORIZONTAL);

            attrLabel = new TextView(getContext());
            currentValue = new EditText(getContext());
            currentValue.setEnabled(false);
            requestValue = new EditText(getContext());
            requestValue.setEnabled(true);

            attrLabel.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            currentValue.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            requestValue.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));

            attrLabel.setGravity(Gravity.CENTER);
            currentValue.setGravity(Gravity.CENTER);
            requestValue.setGravity(Gravity.CENTER);

            attrLabel.setPadding(getPxByDp(1), getPxByDp(1), getPxByDp(1), getPxByDp(1));

            attrLabel.setText(parameter.getAttribute());
            if (parameter.getValue() != null)
                currentValue.setText(parameter.getValue().toString());
            else
                currentValue.setText("NULL");

            addView(attrLabel);
            addView(currentValue);
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
