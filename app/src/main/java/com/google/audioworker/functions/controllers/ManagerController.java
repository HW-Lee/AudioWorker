package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.support.annotation.CallSuper;

import java.util.HashMap;

public abstract class ManagerController extends ControllerBase
        implements ControllerBase.ControllerStateListener {
    protected HashMap<String, ControllerBase> mControllers;

    public ManagerController() {
        mControllers = new HashMap<>();
    }

    public ControllerBase getSubControllerByName(String name) {
        if (mControllers.containsKey(name)) return mControllers.get(name);

        for (ControllerBase controller : mControllers.values()) {
            if (controller instanceof ManagerController)
                return ((ManagerController) controller).getSubControllerByName(name);
        }

        return null;
    }

    @CallSuper
    @Override
    public void activate(Context ctx) {
        for (ControllerBase controller : mControllers.values()) {
            controller.activate(ctx);
            controller.setManager(this);
        }
    }

    @CallSuper
    @Override
    public void destroy() {
        super.destroy();

        for (ControllerBase controller : mControllers.values()) controller.destroy();

        mControllers.clear();
    }

    @CallSuper
    @Override
    public void onStateChanged(ControllerBase controller) {
        broadcastStateChange(controller);
    }
}
