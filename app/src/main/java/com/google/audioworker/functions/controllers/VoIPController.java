package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStartFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStopFunction;
import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.RecordStopFunction;
import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.audio.voip.VoIPConfigFunction;
import com.google.audioworker.functions.audio.voip.VoIPDetectFunction;
import com.google.audioworker.functions.audio.voip.VoIPFunction;
import com.google.audioworker.functions.audio.voip.VoIPInfoFunction;
import com.google.audioworker.functions.audio.voip.VoIPStartFunction;
import com.google.audioworker.functions.audio.voip.VoIPStopFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VoIPController extends ControllerBase {
    private final static String TAG = Constants.packageTag("VoIPController");

    private WeakReference<Context> mContextRef;

    private HashMap<String, DetectorBase> mDetectors;

    private ThreadPoolExecutor mPoolExecuter;
    private PlaybackController.PlaybackRunnable mRxRunnable;
    private RecordController.RecordRunnable mTxRunnable;

    @Override
    public void activate(Context ctx) {
        mDetectors = new HashMap<>();
        mContextRef = new WeakReference<>(ctx);
        mPoolExecuter = new ThreadPoolExecutor(
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.KEEP_ALIVE_TIME_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        String name = "VoIPController";
        if (createFolder(name))
            _dataPath = Constants.externalDirectory(name);
        else
            _dataPath = Constants.EnvironmentPaths.SDCARD_PATH;

        Log.i(TAG, "create data folder: " + _dataPath);

        if (mContextRef.get() != null) {
            ((AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
        }
    }

    @Override
    public void destroy() {
        mPoolExecuter.shutdown();
        mPoolExecuter = null;
    }

    @Override
    public void execute(final WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        if (function instanceof VoIPFunction && function.isValid()) {
            if (function instanceof VoIPStartFunction) {
                if (mRxRunnable != null && !mRxRunnable.hasDone()) {
                    PlaybackStopFunction rxStopFunction = new PlaybackStopFunction();
                    initRxStopFunction(function, rxStopFunction);
                    mRxRunnable.tryStop(rxStopFunction);
                    mRxRunnable = null;
                }
                if (mTxRunnable != null && !mTxRunnable.hasDone()) {
                    RecordStopFunction txStopFunction = new RecordStopFunction();
                    initTxStopFunction(function, txStopFunction);
                    mTxRunnable.tryStop(txStopFunction);
                    mTxRunnable = null;
                }

                PlaybackStartFunction rxStartFunction = new PlaybackStartFunction();
                RecordStartFunction txStartFunction = new RecordStartFunction();

                initRxStartFunction((VoIPStartFunction) function, rxStartFunction);
                initTxStartFunction((VoIPStartFunction) function, txStartFunction);

                ProxyListener listener = new ProxyListener(function, l);
                mRxRunnable = new PlaybackController.PlaybackRunnable(rxStartFunction, listener, this);
                mTxRunnable = new RecordController.RecordRunnable(txStartFunction, listener);
                mTxRunnable.setRecordRunner(new RecordController.RecordInternalRunnable(mTxRunnable, MediaRecorder.AudioSource.VOICE_COMMUNICATION));
                if (mContextRef.get() != null) {
                    ((AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
                mPoolExecuter.execute(mRxRunnable);
                mPoolExecuter.execute(mTxRunnable);
                mPoolExecuter.execute(mTxRunnable.getSlave());
            } else if (function instanceof VoIPStopFunction) {
                PlaybackStopFunction rxStopFunction = new PlaybackStopFunction();
                RecordStopFunction txStopFunction = new RecordStopFunction();

                initRxStopFunction(function, rxStopFunction);
                initTxStopFunction(function, txStopFunction);
                if (mRxRunnable != null && !mRxRunnable.hasDone()) {
                    mRxRunnable.tryStop(rxStopFunction);
                    mRxRunnable = null;
                }
                if (mTxRunnable != null && !mTxRunnable.hasDone()) {
                    mTxRunnable.tryStop(txStopFunction);
                    mTxRunnable = null;
                }
                if (mContextRef.get() != null) {
                    ((AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
                }
                mDetectors.clear();
            } else if (function instanceof VoIPConfigFunction) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (mRxRunnable != null && !mRxRunnable.hasDone()) {
                    WorkerFunction f = mRxRunnable.setSignalConfig(((VoIPConfigFunction) function).getRxAmplitude(), ((VoIPConfigFunction) function).getTargetFrequency());
                    ArrayList<Object> returns = new ArrayList<>();
                    returns.add(f.toString());

                    ack.setReturnCode(0);
                    ack.setDescription("VoIP config sent");
                    ack.setReturns(returns);
                } else {
                    ack.setReturnCode(-1);
                    ack.setDescription("VoIP is not running");
                }

                if (l != null) {
                    l.onAckReceived(ack);
                }
            } else if (function instanceof VoIPDetectFunction) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (mTxRunnable == null || mTxRunnable.hasDone()) {
                    if (l != null) {
                        ack.setReturnCode(-1);
                        ack.setDescription("no recording process running");
                        l.onAckReceived(ack);
                    }
                    return;
                }

                switch (((VoIPDetectFunction) function).getOperationType()) {
                    case RecordDetectFunction.OP_REGISTER: {
                        String className = ((VoIPDetectFunction) function).getDetectorClassName();
                        DetectorBase detector = null;
                        try {
                            Class[] types = {DetectorBase.DetectionListener.class, String.class};
                            Object c;
                            String params = RecordController.processDetectorParams(mTxRunnable, className, ((VoIPDetectFunction) function).getDetectorParams());

                            c = Class.forName(className).getConstructor(types).newInstance(new DetectorBase.DetectionListener() {
                                @Override
                                public void onTargetDetected(SparseArray<? extends DetectorBase.Target> targets) {
                                    VoIPController.this.onTargetDetected(function.getCommandId(), targets);
                                }
                            }, params);
                            if (c instanceof DetectorBase)
                                detector = (DetectorBase) c;
                        } catch (ClassNotFoundException | NoSuchMethodException |
                                IllegalAccessException | InstantiationException | InvocationTargetException e) {
                            e.printStackTrace();
                            return;
                        }

                        if (detector == null) {
                            if (l != null) {
                                ack.setReturnCode(-1);
                                ack.setDescription("invalid detector class name");
                                l.onAckReceived(ack);
                            }
                            return;
                        }

                        mDetectors.put(detector.getHandle(), detector);
                        Log.d(TAG, "register the detector " + detector.getHandle());
                        mTxRunnable.registerDetector(detector);
                        if (l != null) {
                            ArrayList<Object> returns = new ArrayList<>();
                            returns.add(detector.getHandle());
                            ack.setReturnCode(0);
                            ack.setDescription("detector has been registered");
                            ack.setReturns(returns);
                            l.onAckReceived(ack);
                        }
                    }
                        break;
                    case RecordDetectFunction.OP_UNREGISTER: {
                        if (mDetectors.containsKey(((VoIPDetectFunction) function).getClassHandle())) {
                            mTxRunnable.unregisterDetector(mDetectors.get(((VoIPDetectFunction) function).getClassHandle()));
                            mDetectors.remove(((VoIPDetectFunction) function).getClassHandle());
                            if (l != null) {
                                ack.setReturnCode(0);
                                ack.setDescription("detector has been unregistered");
                                l.onAckReceived(ack);
                            }
                        } else if (l != null) {
                            ack.setReturnCode(-1);
                            ack.setDescription("invalid class handle");
                            l.onAckReceived(ack);
                        }
                    }
                        break;
                    case RecordDetectFunction.OP_SETPARAMS: {
                        if (mDetectors.containsKey(((VoIPDetectFunction) function).getClassHandle())) {
                            DetectorBase detector = mDetectors.get(((VoIPDetectFunction) function).getClassHandle());
                            boolean success = true;
                            if (detector != null && ((VoIPDetectFunction) function).getDetectorParams() != null)
                                success = detector.setParameters(((VoIPDetectFunction) function).getDetectorParams());

                            if (l != null) {
                                ack.setReturnCode(success ? 0 : -1);
                                ack.setDescription(success ? "set parameters successfully" : "set parameters failed");
                                l.onAckReceived(ack);
                            }
                        } else if (l != null) {
                            ack.setReturnCode(-1);
                            ack.setDescription("invalid class handle");
                            l.onAckReceived(ack);
                        }
                    }
                        break;
                    default:
                        break;
                }
            } else if (function instanceof VoIPInfoFunction) {
                if (l == null)
                    return;
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (mTxRunnable != null && !mTxRunnable.hasDone() &&
                        mRxRunnable != null && !mRxRunnable.hasDone()) {
                    ArrayList<Object> returns = new ArrayList<>();
                    JSONObject detectionInfo = new JSONObject();

                    for (String handle : mDetectors.keySet()) {
                        DetectorBase detector = mDetectors.get(handle);
                        if (detector == null)
                            continue;

                        try {
                            detectionInfo.put(handle, detector.getInfo());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    returns.add(mRxRunnable.getStartFunction().toString());
                    returns.add(mTxRunnable.getStartFunction().toString());
                    returns.add(detectionInfo.toString());
                    ack.setReturns(returns);
                }
                ack.setReturnCode(0);
                ack.setDescription("info returned");
                l.onAckReceived(ack);
            }
        } else {
            if (function.isValid())
                Log.e(TAG, "The function: " + function + " is not VoIP function");
            else
                Log.e(TAG, "The function: " + function + " is invalid");
            if (l != null) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                ack.setDescription("invalid argument");
                ack.setReturnCode(-1);
                l.onAckReceived(ack);
            }
        }
    }

    private void initRxStartFunction(VoIPStartFunction function, PlaybackStartFunction rxFunction) {
        rxFunction.setCommandId(function.getCommandId() + "p-start");
        rxFunction.setAmplitude(function.getRxAmplitude());
        rxFunction.setBitWidth(function.getRxBitWidth());
        rxFunction.setNumChannels(function.getRxNumChannels());
        rxFunction.setPlaybackId(0);
        rxFunction.setPlaybackType(PlaybackFunction.TASK_NONOFFLOAD);
        rxFunction.setSamplingFreq(function.getRxSamplingFreq());
        rxFunction.setTargetFrequency(function.getTargetFrequency());
    }

    private void initTxStartFunction(VoIPStartFunction function, RecordStartFunction txFunction) {
        txFunction.setCommandId(function.getCommandId() + "c-start");
        txFunction.setBitWidth(function.getTxBitWidth());
        txFunction.setNumChannels(function.getTxNumChannels());
        txFunction.setSamplingFreq(function.getTxSamplingFreq());
        txFunction.setDumpBufferSizeMs(function.getTxDumpBufferSizeMs());
    }

    private void initRxStopFunction(WorkerFunction function, PlaybackStopFunction rxFunction) {
        rxFunction.setCommandId(function.getCommandId() + "p-stop");
        rxFunction.setPlaybackId(0);
        rxFunction.setPlaybackType(PlaybackFunction.TASK_NONOFFLOAD);
    }

    private void initTxStopFunction(WorkerFunction function, RecordStopFunction txFunction) {
        txFunction.setCommandId(function.getCommandId() + "c-stop");
    }

    public void onTargetDetected(final String commandId, SparseArray<? extends DetectorBase.Target> targets) {
    }

    class ProxyListener implements WorkerFunction.WorkerFunctionListener {
        private WorkerFunction.WorkerFunctionListener mListener;
        private WorkerFunction mFunction;

        private boolean isRxRunning;
        private boolean isTxRunning;

        ProxyListener(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
            mFunction = function;
            mListener = l;
            isRxRunning = false;
            isTxRunning = false;
        }

        @Override
        public void onAckReceived(WorkerFunction.Ack ack) {
            Log.d(TAG, "onAckReceived(" + ack + ")");
            if (ack.getTarget().endsWith("p-start") && ack.getReturnCode() >= 0) {
                isRxRunning = true;
            } else if (ack.getTarget().endsWith("p-stop") && ack.getReturnCode() >= 0) {
                isRxRunning = false;
            } else if (ack.getTarget().endsWith("p-start") || ack.getTarget().endsWith("p-stop")) {
                isRxRunning = ack.getReturnCode() >= 0;
            }
            if (ack.getTarget().endsWith("c-start") && ack.getReturnCode() >= 0) {
                isTxRunning = true;
            } else if (ack.getTarget().endsWith("c-stop") && ack.getReturnCode() >= 0) {
                isTxRunning = false;
            } else if (ack.getTarget().endsWith("c-start") || ack.getTarget().endsWith("c-stop")) {
                isTxRunning = ack.getReturnCode() >= 0;
            }

            if (isRxRunning && isTxRunning && mListener != null) {
                WorkerFunction.Ack oack = WorkerFunction.Ack.ackToFunction(mFunction);
                oack.setReturnCode(0);
                oack.setDescription("VoIP starts");
                mListener.onAckReceived(oack);
            } else if (!isRxRunning && !isTxRunning && mListener != null) {
                WorkerFunction function = new VoIPStopFunction();
                String cmdId = ack.getTarget();
                String[] tags = {"p-start", "p-stop", "c-start", "c-stop"};
                for (String tag : tags) {
                    cmdId = cmdId.replace(tag, "");
                }
                function.setCommandId(cmdId);
                WorkerFunction.Ack oack = WorkerFunction.Ack.ackToFunction(function);
                oack.setReturnCode(ack.getReturnCode());
                if (ack.getReturnCode() >= 0)
                    oack.setDescription("VoIP terminated");
                else
                    oack.setDescription("VoIP unexpected failed");
                mListener.onAckReceived(oack);
            }
        }
    }
}
