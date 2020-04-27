package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
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
import com.google.audioworker.functions.audio.voip.VoIPTxDumpFunction;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VoIPController extends AudioController.AudioRxTxController {
    private final static String TAG = Constants.packageTag("VoIPController");

    private WeakReference<Context> mContextRef;

    private HashMap<String, DetectorBase> mDetectors;
    private HashMap<String, DetectorBase.DetectionListener> mDetectionListeners;
    private final ArrayList<RecordController.RecordRunnable.RecordDataListener> mDataListeners = new ArrayList<>();

    private ThreadPoolExecutor mPoolExecuter;
    private PlaybackController.PlaybackRunnable mRxRunnable;
    private RecordController.RecordRunnable mTxRunnable;

    @Override
    public void activate(Context ctx) {
        mDetectors = new HashMap<>();
        mDetectionListeners = new HashMap<>();
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
        super.destroy();
        mPoolExecuter.shutdown();
        mPoolExecuter = null;
        mDetectors.clear();
        mDetectionListeners.clear();
        mDataListeners.clear();
    }

    @Override
    public void execute(final WorkerFunction function, final WorkerFunction.WorkerFunctionListener l) {
        mPoolExecuter.execute(new Runnable() {
            @Override
            public void run() {
                executeBackground(function, l);
            }
        });
    }

    private void executeBackground(final WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        if (function instanceof VoIPFunction && function.isValid()) {
            if (function instanceof VoIPStartFunction) {
                if (isRxRunning()) {
                    PlaybackStopFunction rxStopFunction = new PlaybackStopFunction();
                    initRxStopFunction(function, rxStopFunction);
                    mRxRunnable.tryStop(rxStopFunction);
                    mRxRunnable = null;
                }
                if (isTxRunning()) {
                    RecordStopFunction txStopFunction = new RecordStopFunction();
                    initTxStopFunction(function, txStopFunction);
                    for (RecordController.RecordRunnable.RecordDataListener dl : mDataListeners)
                        mTxRunnable.unregisterDataListener(dl);
                    mTxRunnable.tryStop(txStopFunction);

                    while (!mTxRunnable.hasDone()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mTxRunnable = null;
                }

                PlaybackStartFunction rxStartFunction = new PlaybackStartFunction();
                RecordStartFunction txStartFunction = new RecordStartFunction();

                initRxStartFunction((VoIPStartFunction) function, rxStartFunction);
                initTxStartFunction((VoIPStartFunction) function, txStartFunction);

                ProxyListener listener = new ProxyListener(function, l);
                AudioAttributes attr = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                        .build();
                pushFunctionBeingExecuted(rxStartFunction);
                pushFunctionBeingExecuted(txStartFunction);
                mRxRunnable = new PlaybackController.PlaybackRunnable(rxStartFunction, listener, this, attr);
                mTxRunnable = new RecordController.RecordRunnable(txStartFunction, listener, this);
                mTxRunnable.setRecordRunner(new RecordController.RecordInternalRunnable(mTxRunnable, MediaRecorder.AudioSource.VOICE_COMMUNICATION));

                if (mContextRef.get() != null) {
                    AudioManager audioManager = (AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                    if (((VoIPStartFunction) function).bluetoothScoOn()) {
                        audioManager.setBluetoothScoOn(true);
                    }

                    if (((VoIPStartFunction) function).switchSpeakerPhone() != audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(((VoIPStartFunction) function).switchSpeakerPhone());
                    }

                    if (!audioManager.isSpeakerphoneOn() && audioManager.isBluetoothScoOn()) {
                        audioManager.startBluetoothSco();
                    }
                }
                for (RecordController.RecordRunnable.RecordDataListener dl : mDataListeners)
                    mTxRunnable.registerDataListener(dl);
                for (DetectorBase detector : mDetectors.values())
                    mTxRunnable.registerDetector(detector);
                mPoolExecuter.execute(mRxRunnable);
                mPoolExecuter.execute(mTxRunnable);
                mPoolExecuter.execute(mTxRunnable.getSlave());
            } else if (function instanceof VoIPStopFunction) {
                PlaybackStopFunction rxStopFunction = new PlaybackStopFunction();
                RecordStopFunction txStopFunction = new RecordStopFunction();

                initRxStopFunction(function, rxStopFunction);
                initTxStopFunction(function, txStopFunction);
                ProxyListener listener = new ProxyListener(function, l, true);
                if (!isRxRunning() && !isTxRunning()) {
                    if (l != null) {
                        WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                        ack.setReturnCode(-1);
                        ack.setDescription("no VoIP process running");
                        l.onAckReceived(ack);
                    }
                    return;
                }

                if (isRxRunning()) {
                    mRxRunnable.tryStop(rxStopFunction, listener);
                    mRxRunnable = null;
                }
                if (isTxRunning()) {
                    mTxRunnable.tryStop(txStopFunction, listener);
                    mTxRunnable = null;
                }
                mDetectors.clear();
            } else if (function instanceof VoIPConfigFunction) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (isRxRunning()) {
                    WorkerFunction f = mRxRunnable.setSignalConfig(((VoIPConfigFunction) function).getRxAmplitude(), ((VoIPConfigFunction) function).getTargetFrequency());
                    ArrayList<Object> returns = new ArrayList<>();
                    returns.add(f.toString());

                    AudioManager audioManager = (AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                    if (((VoIPConfigFunction) function).switchSpeakerPhone() != audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(((VoIPConfigFunction) function).switchSpeakerPhone());
                        returns.add("change to " + (audioManager.isSpeakerphoneOn() ? "speakerphone" : "default"));
                    }

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
                if (!isTxRunning()) {
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
                        String[] findings = Constants.Detectors.getDetectorClassNamesByTag(className);

                        if (findings.length > 0)
                            className = findings[0];

                        String params = RecordController.processDetectorParams(mTxRunnable, className, ((VoIPDetectFunction) function).getDetectorParams());
                        DetectorBase detector = DetectorBase.getDetectorByClassName(className, new DetectorBase.DetectionListener() {
                            @Override
                            public void onTargetDetected(DetectorBase detector, SparseArray<? extends DetectorBase.Target> targets) {
                                VoIPController.this.onTargetDetected(detector.getHandle(), targets);
                            }
                        }, mTxRunnable.getStartFunction(), params);

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
                            mDetectionListeners.remove(((VoIPDetectFunction) function).getClassHandle());
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
                                success = detector.setDetectorParameters(((VoIPDetectFunction) function).getDetectorParams());

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
                if (isVoIPRunning()) {
                    ArrayList<Object> returns = new ArrayList<>();
                    ArrayList<PlaybackStartFunction> functions = new ArrayList<>();
                    functions.add(mRxRunnable.getStartFunction());

                    String rxInfoStr = PlaybackController.getPlaybackInfoAckString(functions);
                    AudioManager audioManager = (AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                    try {
                        JSONObject rxInfoObj = new JSONObject(rxInfoStr);
                        rxInfoObj.put("use-speakerphone", audioManager.isSpeakerphoneOn());
                        rxInfoStr = rxInfoObj.toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    returns.add(rxInfoStr);
                    returns.addAll(RecordController.getRecordInfoAckStrings(mTxRunnable.getStartFunction(), mDetectors));
                    ack.setReturns(returns);
                }

                if (((VoIPInfoFunction) function).getFileName().length() > 0) {
                    String path = new File(getDataDir(), ((VoIPInfoFunction) function).getFileName()).getAbsolutePath();
                    ArrayList<String> returnStrs = new ArrayList<>();
                    StringBuilder infoStr = new StringBuilder();
                    for (Object obj : ack.getReturns())
                        returnStrs.add(obj.toString());
                    if (returnStrs.size() > 0) {
                        infoStr.append("[");
                        infoStr.append(String.join(",", returnStrs));
                        infoStr.append("]\n");
                    }
                    try {
                        PrintWriter pw = new PrintWriter(path);
                        pw.write(CommandHelper.Command.genId() + "\n");
                        pw.write(infoStr.toString());
                        pw.close();
                        Log.d(TAG, "successfully dump the info to " + path);
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "failed to dump the info to " + path);
                        e.printStackTrace();
                    }
                }

                ack.setReturnCode(0);
                ack.setDescription("info returned");
                l.onAckReceived(ack);
            } else if (function instanceof VoIPTxDumpFunction) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (!isTxRunning()) {
                    if (l != null) {
                        ack.setReturnCode(-1);
                        ack.setDescription("no recording process running");
                        l.onAckReceived(ack);
                    }
                    return;
                }

                pushFunctionBeingExecuted(function);
                String path = new File(getDataDir(), ((VoIPTxDumpFunction) function).getFileName()).getAbsolutePath();
                mTxRunnable.dumpBufferTo(path, function);
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

    private void onTargetDetected(String detectorHandle, SparseArray<? extends DetectorBase.Target> targets) {
        if (!mDetectionListeners.containsKey(detectorHandle))
            return;

        DetectorBase detector = mDetectors.get(detectorHandle);
        DetectorBase.DetectionListener l = mDetectionListeners.get(detectorHandle);
        if (detector == null || l == null)
            return;

        l.onTargetDetected(detector, targets);
    }

    @Override
    public DetectorBase getDetectorByHandle(String handle) {
        return mDetectors.get(handle);
    }

    @Override
    public void setDetectionListener(String detectorHandle, DetectorBase.DetectionListener l) {
        if (!mDetectors.containsKey(detectorHandle))
            return;

        mDetectionListeners.put(detectorHandle, l);
    }

    public boolean isTxRunning() {
        return mTxRunnable != null && !mTxRunnable.hasDone();
    }

    public boolean isRxRunning() {
        return mRxRunnable != null && !mRxRunnable.hasDone();
    }

    public int getNumRxRunning() {
        return isRxRunning() ? 1 : 0;
    }

    public boolean isVoIPRunning() {
        return isRxRunning() && isTxRunning();
    }

    @Override
    public void registerDataListener(@NonNull RecordController.RecordRunnable.RecordDataListener l) {
        synchronized (mDataListeners) {
            if (!mDataListeners.contains(l))
                mDataListeners.add(l);
        }

        if (!isTxRunning())
            return;

        mTxRunnable.registerDataListener(l);
    }

    @Override
    public void unregisterDataListener(@NonNull RecordController.RecordRunnable.RecordDataListener l) {
        synchronized (mDataListeners) {
            mDataListeners.remove(l);
        }

        if (!isTxRunning())
            return;

        mTxRunnable.unregisterDataListener(l);
    }

    class ProxyListener implements WorkerFunction.WorkerFunctionListener {
        private WorkerFunction.WorkerFunctionListener mListener;
        private WorkerFunction mFunction;

        private boolean isRunning;

        private boolean isRxRunning;
        private boolean isTxRunning;

        ProxyListener(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
            this(function, l, false);
        }

        ProxyListener(WorkerFunction function, WorkerFunction.WorkerFunctionListener l, boolean initRunning) {
            mFunction = function;
            mListener = l;
            isRxRunning = initRunning;
            isTxRunning = initRunning;
            isRunning = initRunning;
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

            if (isRxRunning && isTxRunning && mListener != null && !isRunning) {
                isRunning = true;
                WorkerFunction.Ack oack = WorkerFunction.Ack.ackToFunction(mFunction);
                oack.setReturnCode(0);
                oack.setDescription("VoIP starts");
                mListener.onAckReceived(oack);
            } else if (!isRxRunning && !isTxRunning && mListener != null && isRunning) {
                isRunning = false;
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

                if (mContextRef.get() != null) {
                    AudioManager audioManager = (AudioManager) mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    if (audioManager.isBluetoothScoOn()) {
                        audioManager.stopBluetoothSco();
                        audioManager.setBluetoothScoOn(false);
                    }

                    if (audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(false);
                    }
                }

                mListener.onAckReceived(oack);
            }
        }
    }
}
