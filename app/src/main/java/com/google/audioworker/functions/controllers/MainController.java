package com.google.audioworker.functions.controllers;

import android.util.Log;

import com.google.audioworker.functions.audio.AudioFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import org.json.JSONException;

import java.util.HashMap;

public class MainController extends ManagerController {
    private static final String TAG = Constants.packageTag("MainController");

    private HashMap<String, WorkerFunction> mRequestedFunctions;

    public MainController() {
        super();
        mControllers.put(Constants.Controllers.NAME_AUDIO, new AudioController());
        // mControllers.put(Constants.Controllers.NAME_SHELL, new ShellController());

        mRequestedFunctions = new HashMap<>();
    }

    @Override
    public void execute(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        ControllerBase selectedController = selectController(function);
        if (selectedController != null) {
            selectedController.execute(function, l);
        }
    }

    @Override
    public void receiveAck(WorkerFunction.Ack ack) {
        String targetId;
        try {
            targetId = ack.getString(Constants.MessageSpecification.COMMAND_ACK_TARGET);
            if (mRequestedFunctions.containsKey(targetId)
                    && mRequestedFunctions.get(targetId) != null) {
                mRequestedFunctions.get(targetId).pushAck(ack);
                listFunctions();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addRequestedFunction(WorkerFunction function) {
        Log.d(TAG, "addRequestedFunction: " + function);
        mRequestedFunctions.put(function.getCommandId(), function);
        listFunctions();
    }

    private ControllerBase selectController(WorkerFunction function) {
        ControllerBase selectedController = null;
        if (function instanceof AudioFunction)
            selectedController = mControllers.get(Constants.Controllers.NAME_AUDIO);

        return selectedController;
    }

    private void listFunctions() {
        for (String targetId : mRequestedFunctions.keySet()) {
            if (mRequestedFunctions.get(targetId) == null) continue;
            Log.d(TAG, "Requested WorkerFunction#" + targetId);
            Log.d(TAG, mRequestedFunctions.get(targetId).toString());
        }
    }
}
