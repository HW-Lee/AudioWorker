package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.support.annotation.CallSuper;

import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class ControllerBase {
    abstract public void activate(Context ctx);
    abstract public void execute(final WorkerFunction function, final WorkerFunction.WorkerFunctionListener l);

    private boolean mSuppressStateChange;
    public void execute(final WorkerFunction function, final WorkerFunction.WorkerFunctionListener l, boolean suppressStateChange) {
        mSuppressStateChange = suppressStateChange;
        execute(function, l);
    }

    public interface ControllerStateListener {
        void onStateChanged(ControllerBase controller);
    }

    protected ManagerController mManager;
    protected String _dataPath;
    protected final ArrayList<WeakReference<ControllerStateListener>> _stateListeners = new ArrayList<>();
    protected final ArrayList<WorkerFunction> _functionsBeingExecuted = new ArrayList<>();

    @CallSuper
    public void destroy() {
        _stateListeners.clear();
        _functionsBeingExecuted.clear();
    }

    public void receiveAck(WorkerFunction.Ack ack) {
    }

    protected boolean createFolder(String name) {
        File folder = new File(Constants.externalDirectory(name));
        return folder.exists() || folder.mkdirs();
    }

    public String getDataDir() {
        return _dataPath;
    }

    public void registerStateListener(ControllerStateListener l) {
        if (l == null)
            return;

        synchronized (_stateListeners) {
            ArrayList<WeakReference<ControllerStateListener>> remove = new ArrayList<>();
            for (WeakReference<ControllerStateListener> ref : _stateListeners) {
                if (ref.get() == null) {
                    remove.add(ref);
                    continue;
                }
                if (ref.get() == l)
                    return;
            }

            if (remove.size() > 0)
                _stateListeners.removeAll(remove);

            _stateListeners.add(new WeakReference<>(l));
        }
    }

    public void unregisterStateListener(ControllerStateListener l) {
        if (l == null)
            return;

        synchronized (_stateListeners) {
            for (WeakReference<ControllerStateListener> ref : _stateListeners) {
                if (ref.get() == null)
                    continue;

                if (ref.get() == l) {
                    _stateListeners.remove(ref);
                    return;
                }
            }
        }
    }

    public void setManager(ManagerController manager) {
        mManager = manager;
    }

    protected void broadcastStateChange(final ControllerBase controller) {
        if (mSuppressStateChange)
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mManager != null)
                    mManager.onStateChanged(controller);

                ArrayList<WeakReference<ControllerStateListener>> listeners = new ArrayList<>(_stateListeners);

                for (WeakReference<ControllerStateListener> ref : listeners) {
                    if (ref.get() == null)
                        continue;

                    ref.get().onStateChanged(controller);
                }
            }
        }).start();
    }

    protected void pushFunctionBeingExecuted(WorkerFunction function) {
        synchronized (_functionsBeingExecuted) {
            _functionsBeingExecuted.add(function);
        }
    }

    protected void notifyFunctionHasBeenExecuted(WorkerFunction function) {
        synchronized (_functionsBeingExecuted) {
            _functionsBeingExecuted.remove(function);
        }
    }

    public boolean hasFunctionBeingExecuted() {
        int size;
        synchronized (_functionsBeingExecuted) {
            size = _functionsBeingExecuted.size();
        }

        return size > 0;
    }
}
