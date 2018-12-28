package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.util.Log;

import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.shell.ShellFunction;
import com.google.audioworker.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ShellController extends ControllerBase {
    private final static String TAG = Constants.packageTag("ShellController");

    private WeakReference<Context> mContextRef;
    private ThreadPoolExecutor mPoolExecuter;

    @Override
    public void activate(Context ctx) {
        mContextRef = new WeakReference<>(ctx);
        mPoolExecuter = new ThreadPoolExecutor(
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.KEEP_ALIVE_TIME_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    @Override
    public void destroy() {
        mPoolExecuter.shutdown();
        mPoolExecuter = null;
    }

    @Override
    public void execute(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        if (function instanceof ShellFunction) {
            mPoolExecuter.execute(new ShellRunnable((ShellFunction) function, l));
        } else {
            Log.e(TAG, "The function: " + function + " is not shell function");
            if (l != null) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                ack.setDescription("invalid argument");
                ack.setReturnCode(-1);
                l.onAckReceived(ack);
            }
        }
    }

    private class ShellRunnable implements Runnable {
        private ShellFunction mFunction;
        private WorkerFunction.WorkerFunctionListener mListener;

        ShellRunnable(ShellFunction function, WorkerFunction.WorkerFunctionListener l) {
            mFunction = function;
            mListener = l;
        }

        @Override
        public void run() {
            if (mFunction.isBroadcastFunction()) {
                run_broadcast();
                return;
            }
            run_legacy();
        }

        private void run_broadcast() {
            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(mFunction);
            ArrayList<Object> returns = new ArrayList<>(2);

            if (mContextRef.get() == null) {
                if (mListener != null) {
                    ack.setReturnCode(-1);
                    ack.setDescription("No context to send broadcast");
                    mListener.onAckReceived(ack);
                }
                return;
            }

            mContextRef.get().sendBroadcast(mFunction.getBroadcastIntent());
            if (mListener != null) {
                returns.add(mFunction.getCommand());
                returns.add("" + CommandHelper.getFunction(mFunction.getBroadcastIntent()));
                ack.setReturnCode(0);
                ack.setDescription("send broadcast successfully");
                ack.setReturns(returns);
                mListener.onAckReceived(ack);
            }
        }

        private void run_legacy() {
            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(mFunction);
            ArrayList<Object> returns = new ArrayList<>(2);
            try {
                Process proc = Runtime.getRuntime().exec(mFunction.getCommand());
                BufferedReader stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                StringBuilder stdOutMsg = new StringBuilder();
                StringBuilder stdErrMsg = new StringBuilder();

                int outRead = 0;
                int errRead = 0;
                char[] buffer = new char[Constants.Controllers.Config.Common.BYTE_BUFFER_SIZE];
                do {
                    outRead = stdOut.read(buffer);
                    if (outRead > 0)
                        stdOutMsg.append(buffer, 0, outRead);

                    errRead = stdErr.read(buffer);
                    if (errRead > 0)
                        stdErrMsg.append(buffer, 0, errRead);
                } while (outRead > 0 || errRead > 0);

                proc.waitFor();

                if (mListener != null) {
                    returns.add(stdOutMsg.toString());
                    returns.add(stdErrMsg.toString());
                    ack.setReturnCode(proc.exitValue());
                    ack.setDescription("process returned");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();

                if (mListener != null) {
                    returns.add("");
                    returns.add(e.getMessage());
                    ack.setReturnCode(-1);
                    ack.setDescription("got exception on App");
                }
            } finally {
                if (mListener != null) {
                    ack.setReturns(returns);
                    mListener.onAckReceived(ack);
                }
            }
        }
    }
}
