package com.google.audioworker.functions.controllers;

import android.content.Context;

import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import java.io.File;

public abstract class ControllerBase {
    abstract public void activate(Context ctx);
    abstract public void destroy();
    abstract public void execute(WorkerFunction function, WorkerFunction.WorkerFunctionListener l);

    protected String _dataPath;

    public void receiveAck(WorkerFunction.Ack ack) {
    }

    protected boolean createFolder(String name) {
        File folder = new File(Constants.externalDirectory(name));
        return folder.exists() || folder.mkdirs();
    }

    public String getDataDir() {
        return _dataPath;
    }
}
