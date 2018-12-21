package com.google.audioworker.functions.controllers;

import android.content.Context;

import java.util.HashMap;

public abstract class ManagerController extends ControllerBase {
    protected HashMap<String, ControllerBase> mControllers;

    public ManagerController() {
        mControllers = new HashMap<>();
    }

    @Override
    public void activate(Context ctx) {
        for (ControllerBase controller : mControllers.values())
            controller.activate(ctx);
    }

    @Override
    public void destroy() {
        for (ControllerBase controller : mControllers.values())
            controller.destroy();

        mControllers.clear();
    }
}
