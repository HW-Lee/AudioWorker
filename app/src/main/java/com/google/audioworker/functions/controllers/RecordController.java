package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.functions.audio.record.RecordDumpFunction;
import com.google.audioworker.functions.audio.record.RecordFunction;
import com.google.audioworker.functions.audio.record.RecordInfoFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.RecordStopFunction;
import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.signalproc.WavUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordController extends AudioController.AudioTxController {
    private final static String TAG = Constants.packageTag("RecordController");

    private WeakReference<Context> mContextRef;
    private HashMap<String, DetectorBase> mDetectors;
    private HashMap<String, DetectorBase.DetectionListener> mDetectionListeners;
    private final ArrayList<RecordRunnable.RecordDataListener> mDataListeners = new ArrayList<>();
    private RecordRunnable mMainRunningTask;
    private ThreadPoolExecutor mPoolExecuter;

    @Override
    public void activate(Context ctx) {
        mDetectors = new HashMap<>();
        mContextRef = new WeakReference<>(ctx);
        mDetectionListeners = new HashMap<>();
        mPoolExecuter = new ThreadPoolExecutor(
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.KEEP_ALIVE_TIME_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        String name = "RecordController";
        if (createFolder(name))
            _dataPath = Constants.externalDirectory(name);
        else
            _dataPath = Constants.EnvironmentPaths.SDCARD_PATH;

        Log.i(TAG, "create data folder: " + _dataPath);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mMainRunningTask != null && !mMainRunningTask.hasDone()) {
            mMainRunningTask.tryStop();
            mMainRunningTask = null;
        }
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
        if (function instanceof RecordFunction && function.isValid()) {
            if (function instanceof RecordStartFunction) {
                if (isTxRunning()) {
                    for (RecordRunnable.RecordDataListener dl : mDataListeners)
                        mMainRunningTask.unregisterDataListener(dl);
                    mMainRunningTask.tryStop(new RecordStopFunction());

                    while (isTxRunning()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                pushFunctionBeingExecuted(function);
                mMainRunningTask = new RecordRunnable((RecordStartFunction) function, l, this);
                mMainRunningTask.setRecordRunner(new RecordInternalRunnable(mMainRunningTask));
                for (RecordRunnable.RecordDataListener dl : mDataListeners)
                    mMainRunningTask.registerDataListener(dl);
                for (DetectorBase detector : mDetectors.values())
                    mMainRunningTask.registerDetector(detector);

                mPoolExecuter.execute(mMainRunningTask);
                mPoolExecuter.execute(mMainRunningTask.slave);
            } else if (function instanceof RecordStopFunction) {
                if (isTxRunning()) {
                    pushFunctionBeingExecuted(function);
                    mMainRunningTask.tryStop((RecordStopFunction) function, l);
                    mMainRunningTask = null;
                } else if (l != null) {
                    WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                    ack.setReturnCode(-1);
                    ack.setDescription("no recording process running");
                    l.onAckReceived(ack);
                }
                mDetectors.clear();
            } else if (function instanceof RecordInfoFunction) {
                if (l == null)
                    return;
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (isTxRunning()) {
                    ArrayList<Object> returns = new ArrayList<>();
                    returns.addAll(RecordController.getRecordInfoAckStrings(mMainRunningTask.getStartFunction(), mDetectors));
                    ack.setReturns(returns);
                }

                if (((RecordInfoFunction) function).getFileName().length() > 0) {
                    String path = new File(getDataDir(), ((RecordInfoFunction) function).getFileName()).getAbsolutePath();
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
            } else if (function instanceof RecordDetectFunction) {
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (!isTxRunning()) {
                    if (l != null) {
                        ack.setReturnCode(-1);
                        ack.setDescription("no recording process running");
                        l.onAckReceived(ack);
                    }
                    return;
                }

                switch (((RecordDetectFunction) function).getOperationType()) {
                    case RecordDetectFunction.OP_REGISTER: {
                        String className = ((RecordDetectFunction) function).getDetectorClassName();
                        String[] findings = Constants.Detectors.getDetectorClassNamesByTag(className);

                        if (findings.length > 0)
                            className = findings[0];

                        String params = processDetectorParams(mMainRunningTask, className, ((RecordDetectFunction) function).getDetectorParams());
                        DetectorBase detector = DetectorBase.getDetectorByClassName(className, new DetectorBase.DetectionListener() {
                            @Override
                            public void onTargetDetected(DetectorBase detector, SparseArray<? extends DetectorBase.Target> targets) {
                                RecordController.this.onTargetDetected(detector.getHandle(), targets);
                            }
                        }, mMainRunningTask.getStartFunction(), params);

                        if (detector == null) {
                            notifyFunctionHasBeenExecuted(function);
                            if (l != null) {
                                ack.setReturnCode(-1);
                                ack.setDescription("invalid detector class name");
                                l.onAckReceived(ack);
                            }
                            return;
                        }

                        mDetectors.put(detector.getHandle(), detector);
                        Log.d(TAG, "register the detector " + detector.getHandle());
                        mMainRunningTask.registerDetector(detector);
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
                        if (mDetectors.containsKey(((RecordDetectFunction) function).getClassHandle())) {
                            mMainRunningTask.unregisterDetector(mDetectors.get(((RecordDetectFunction) function).getClassHandle()));
                            mDetectors.remove(((RecordDetectFunction) function).getClassHandle());
                            mDetectionListeners.remove(((RecordDetectFunction) function).getClassHandle());
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
                        if (mDetectors.containsKey(((RecordDetectFunction) function).getClassHandle())) {
                            DetectorBase detector = mDetectors.get(((RecordDetectFunction) function).getClassHandle());
                            boolean success = true;
                            if (detector != null && ((RecordDetectFunction) function).getDetectorParams() != null)
                                success = detector.setDetectorParameters(((RecordDetectFunction) function).getDetectorParams());

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
            } else if (function instanceof RecordDumpFunction) {
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
                String path = ((RecordDumpFunction) function).getFileName();
                if (!path.startsWith("/"))
                    path = new File(getDataDir(), path).getAbsolutePath();
                mMainRunningTask.dumpBufferTo(path, function);
            }
        } else {
            if (function.isValid())
                Log.e(TAG, "The function: " + function + " is not record function");
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

    static Collection<? extends String> getRecordInfoAckStrings(RecordStartFunction function, HashMap<String, DetectorBase> detectors) {
        JSONObject detectionInfo = new JSONObject();

        for (String handle : detectors.keySet()) {
            DetectorBase detector = detectors.get(handle);
            if (detector == null)
                continue;

            try {
                detectionInfo.put(handle, detector.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> returns = new ArrayList<>();
        returns.add(function.toString());
        returns.add(detectionInfo.toString());
        return returns;
    }

    @Override
    public DetectorBase getDetectorByHandle(String handle) {
        return mDetectors.get(handle);
    }

    @Override
    public boolean isTxRunning() {
        return mMainRunningTask != null && !mMainRunningTask.hasDone();
    }

    @Override
    public void registerDataListener(@NonNull RecordRunnable.RecordDataListener l) {
        synchronized (mDataListeners) {
            if (!mDataListeners.contains(l))
                mDataListeners.add(l);
        }

        if (!isTxRunning())
            return;

        mMainRunningTask.registerDataListener(l);
    }

    @Override
    public void unregisterDataListener(@NonNull RecordRunnable.RecordDataListener l) {
        synchronized (mDataListeners) {
            mDataListeners.remove(l);
        }

        if (!isTxRunning())
            return;

        mMainRunningTask.unregisterDataListener(l);
    }

    static public String processDetectorParams(RecordRunnable runnable, String className, String params) {
        if (params == null)
            params = "{}";

        return params;
    }

    @Override
    public void setDetectionListener(final String detectorHandle, DetectorBase.DetectionListener l) {
        if (!mDetectors.containsKey(detectorHandle))
            return;

        mDetectionListeners.put(detectorHandle, l);
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

    public static class RecordSharedBuffer {
        private byte[] raw;
        private boolean hasRefreshed;

        RecordSharedBuffer(int buffsize) {
            hasRefreshed = false;
            raw = new byte[buffsize];
        }

        boolean dataAvailable() {
            return hasRefreshed;
        }

        void put(byte[] src) {
            synchronized (this) {
                System.arraycopy(src, 0, raw, 0, src.length);
                hasRefreshed = true;
            }
        }

        void fetch(byte[] dest) {
            synchronized (this) {
                System.arraycopy(raw, 0, dest, 0, dest.length);
                hasRefreshed = false;
            }
        }
    }

    static class RecordCircularBuffer {
        private byte[] raw;
        private int head = 0;
        private int len = 0;

        RecordCircularBuffer(int buffsize) {
            raw = new byte[buffsize];
        }

        void push(byte[] src) {
            synchronized (this) {
                for (byte v : src) {
                    raw[(head + len) % raw.length] = v;
                    if (len < raw.length) {
                        len++;
                    } else {
                        head++;
                        head %= raw.length;
                    }
                }
            }
        }

        void fetch(byte[] dest) {
            synchronized (this) {
                for (int i = 0; i < dest.length; i++) {
                    dest[i] = raw[(head + i) % raw.length];
                }
            }
        }
    }

    public static class RecordRunnable implements Runnable {
        private RecordStartFunction mStartFunction;
        private RecordStopFunction mStopFunction;
        private WorkerFunction.WorkerFunctionListener mListener;

        private final RecordSharedBuffer sharedBuffer;
        private final AtomicBoolean needPush;

        private final RecordCircularBuffer dumpBuffer;
        private final int dumpBufferSize;
        private RecordInternalRunnable slave;

        private ControllerBase mController;
        private final ArrayList<DetectorBase> mDetectors;
        private final ArrayList<RecordDataListener> mDataListeners;

        private boolean exitPending;
        private boolean hasDone;

        public interface RecordDataListener {
            void onDataUpdated(List<? extends Double>[] signal, RecordStartFunction function);
        }

        RecordRunnable(RecordStartFunction function, WorkerFunction.WorkerFunctionListener l, ControllerBase controller) {
            mStartFunction = function;
            mListener = l;
            mController = controller;

            int minBuffsize = AudioRecord.getMinBufferSize(
                    mStartFunction.getSamplingFreq(), parseChannelMask(mStartFunction.getNumChannels()), parseEncodingFormat(mStartFunction.getBitWidth()));
            sharedBuffer = new RecordSharedBuffer(minBuffsize);
            dumpBufferSize = (int) (mStartFunction.getBitWidth() / 8. * mStartFunction.getNumChannels() * mStartFunction.getSamplingFreq() * mStartFunction.getDumpBufferSizeMs() / 1000.);
            dumpBuffer = new RecordCircularBuffer(dumpBufferSize);
            needPush = new AtomicBoolean(true);
            mDetectors = new ArrayList<>();
            mDataListeners = new ArrayList<>();
        }

        public RecordStartFunction getStartFunction() {
            return mStartFunction;
        }

        public RecordInternalRunnable getSlave() {
            return slave;
        }

        public boolean hasDone() {
            return hasDone;
        }

        public void tryStop() {
            tryStop(null);
        }

        public void tryStop(RecordStopFunction function) {
            tryStop(function, null);
        }

        public void tryStop(RecordStopFunction function, WorkerFunction.WorkerFunctionListener l) {
            if (l != null)
                mListener = l;
            mStopFunction = function;
            exitPending = true;
        }

        private int parseEncodingFormat(int bits) {
            switch (bits) {
                case 8:
                    return AudioFormat.ENCODING_PCM_8BIT;
                case 16:
                    return AudioFormat.ENCODING_PCM_16BIT;
                default:
                    return AudioFormat.ENCODING_PCM_16BIT;
            }
        }

        private int parseChannelMask(int nch) {
            switch (nch) {
                case 1:
                    return AudioFormat.CHANNEL_IN_MONO;
                case 2:
                    return AudioFormat.CHANNEL_IN_STEREO;
                default:
                    return AudioFormat.CHANNEL_IN_MONO;
            }
        }

        public void setRecordRunner(RecordInternalRunnable runner) {
            slave = runner;
        }

        public void registerDataListener(@NonNull RecordDataListener l) {
            synchronized (mDataListeners) {
                if (mDataListeners.contains(l))
                    return;
                mDataListeners.add(l);
            }
        }

        public void unregisterDataListener(@NonNull RecordDataListener l) {
            synchronized (mDataListeners) {
                mDataListeners.remove(l);
            }
        }

        public void registerDetector(DetectorBase detector) {
            if (detector.getStartFunction() != mStartFunction)
                detector.updateStartFunction(mStartFunction);

            synchronized (mDetectors) {
                mDetectors.add(detector);
            }
        }

        public void unregisterDetector(DetectorBase detector) {
            synchronized (mDetectors) {
                mDetectors.remove(detector);
            }
        }

        private void returnAck(RecordFunction function, int ret) {
            mController.notifyFunctionHasBeenExecuted(function);
            if (mListener == null)
                return;

            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
            if (function instanceof RecordStartFunction) {
                ack.setReturnCode(ret);
                if (ret < 0)
                    ack.setDescription("unexpected stop");
                else
                    ack.setDescription("record start");
            } else if (function instanceof RecordStopFunction) {
                ack.setReturnCode(ret);
                ack.setDescription("stop command received");
            } else {
                ack.setReturnCode(-1);
                ack.setDescription("invalid argument");
            }

            mListener.onAckReceived(ack);
        }

        @Override
        public void run() {
            int minBuffsize = sharedBuffer.raw.length;
            long minBuffsizeMillis = minBuffsize*1000 / mStartFunction.getSamplingFreq() / mStartFunction.getNumChannels() / (mStartFunction.getBitWidth() / 8);
            final byte[] buffer = new byte[minBuffsize];
            Log.d(TAG, "RecordRunnable: start running");
            returnAck(mStartFunction, 0);

            if (mController != null)
                mController.broadcastStateChange(mController);

            exitPending = false;
            hasDone = false;
            while (!exitPending) {
                try {
                    if (!sharedBuffer.dataAvailable()) {
                        synchronized (this) {
                            wait((long) (minBuffsizeMillis * Constants.Controllers.Config.Record.TIMEOUT_MULTIPLIER));
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (sharedBuffer.dataAvailable()) {
                    sharedBuffer.fetch(buffer);
                } else {
                    Log.w(TAG, "Timed out for the thread waiting the buffer from the framework. (buffer size: "
                            + minBuffsizeMillis + " ms, timeout: "
                            + (minBuffsizeMillis * Constants.Controllers.Config.Record.TIMEOUT_MULTIPLIER) + " ms.");
                    Arrays.fill(buffer, (byte) 0);
                }

                pushDumpBuffer(buffer);

                @SuppressWarnings("unchecked")
                final ArrayList<Double>[] values = new ArrayList[mStartFunction.getNumChannels()];
                for (int c = 0; c < mStartFunction.getNumChannels(); c++)
                    values[c] = new ArrayList<>();

                switch (mStartFunction.getBitWidth()) {
                    case 8: {
                        for (int i = 0; i < buffer.length; i++) {
                            byte v = buffer[i];
                            values[i % mStartFunction.getNumChannels()].add(v * 1.0 / (1 << 7));
                        }
                    }
                        break;

                    default:
                    case 16: {
                        short[] frame = new short[buffer.length / 2];
                        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(frame);

                        for (int i = 0; i < frame.length; i++) {
                            short v = frame[i];
                            values[i % mStartFunction.getNumChannels()].add(v * 1.0 / (1 << 15));
                        }
                    }
                        break;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (RecordDataListener l : mDataListeners)
                            l.onDataUpdated(values, mStartFunction);
                    }
                }).start();

                synchronized (mDetectors) {
                    for (DetectorBase detector : mDetectors)
                        detector.feed(values);
                }
            }

            if (mController != null)
                mController.broadcastStateChange(mController);

            if (mStopFunction != null) {
                if (mStopFunction.getCommandId() == null)
                    mStopFunction.setCommandId(mStartFunction.getCommandId());
                returnAck(mStopFunction, 0);
            } else {
                returnAck(mStartFunction, -1);
            }
            Log.d(TAG, "RecordRunnable: terminated");
            if (slave != null) {
                slave.tryStop();
                slave = null;
            }

            mDataListeners.clear();
            hasDone = true;
        }

        private void pushDumpBuffer(final byte[] buffer) {
            if (dumpBufferSize <= 0 || !needPush.get())
                return;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    dumpBuffer.push(buffer);
                }
            }).start();
        }

        public void dumpBufferTo(final String path, final WorkerFunction function) {
            if (dumpBufferSize <= 0) {
                mController.notifyFunctionHasBeenExecuted(function);
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    _dumpBufferTo(path, function);
                }
            }).start();
        }

        private void _dumpBufferTo(String path, WorkerFunction function) {
            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
            ArrayList<Object> returns = new ArrayList<>();
            WavUtils.WavConfig config = new WavUtils.WavConfig.Builder()
                    .withDurationMillis(mStartFunction.getDumpBufferSizeMs())
                    .withBitPerSample(mStartFunction.getBitWidth())
                    .withNumChannels(mStartFunction.getNumChannels())
                    .withSamplingFrequency(mStartFunction.getSamplingFreq()).build();

            try {
                DataOutputStream dump = WavUtils.obtainWavFile(config, path);
                byte[] buffer = new byte[dumpBufferSize];

                needPush.set(false);
                dumpBuffer.fetch(buffer);
                needPush.set(true);

                Log.d(TAG, "dump buffer: " + buffer.length + " bytes");
                dump.write(buffer);
                dump.close();

                returns.add(path);
                ack.setReturnCode(0);
                ack.setDescription("Record dump successfully");
                ack.setReturns(returns);
            } catch (IOException e) {
                e.printStackTrace();

                returns.add(e.getMessage());
                ack.setReturnCode(-1);
                ack.setDescription("Record dump failed: IO Exception");
                ack.setReturns(returns);
            }

            mController.notifyFunctionHasBeenExecuted(function);
            if (mListener != null) {
                mListener.onAckReceived(ack);
            }
        }
    }

    public static class RecordInternalRunnable implements Runnable {
        private final RecordRunnable master;
        private int inputSource;

        private boolean exitPending;

        public RecordInternalRunnable(RecordRunnable masterTask) {
            this(masterTask, MediaRecorder.AudioSource.MIC);
        }

        public RecordInternalRunnable(RecordRunnable masterTask, int input) {
            master = masterTask;
            inputSource = input;
        }

        public void tryStop() {
            exitPending = true;
        }

        @Override
        public void run() {
            RecordStartFunction startFunction = master.mStartFunction;
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(startFunction.getSamplingFreq())
                    .setEncoding(master.parseEncodingFormat(startFunction.getBitWidth()))
                    .setChannelMask(master.parseChannelMask(startFunction.getNumChannels()))
                    .build();
            int minBuffsize;
            while (master.sharedBuffer == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (master.sharedBuffer) {
                minBuffsize = master.sharedBuffer.raw.length;
            }


            AudioRecord record = new AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setAudioSource(inputSource)
                    .setBufferSizeInBytes(minBuffsize).build();

            if (master.mController instanceof RecordController &&
                ((RecordController) master.mController).mContextRef.get() != null) {
                AudioManager audioManager =
                        (AudioManager) ((RecordController) master.mController).mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setBluetoothScoOn(master.mStartFunction.bluetoothScoOn());
                if (audioManager.isBluetoothScoOn()) {
                    audioManager.startBluetoothSco();
                }
            }
            record.startRecording();
            byte[] buffer = new byte[master.sharedBuffer.raw.length];

            Log.d(TAG, "RecordInternalRunnable: start running");
            while (!exitPending) {
                record.read(buffer, 0, minBuffsize);
                master.sharedBuffer.put(buffer);
                synchronized (master) {
                    master.notify();
                }
            }
            record.stop();
            record.release();

            if (master.mController instanceof RecordController &&
                    ((RecordController) master.mController).mContextRef.get() != null) {
                AudioManager audioManager =
                        (AudioManager) ((RecordController) master.mController).mContextRef.get().getSystemService(Context.AUDIO_SERVICE);
                if (audioManager.isBluetoothScoOn()) {
                    audioManager.setBluetoothScoOn(false);
                    audioManager.stopBluetoothSco();
                }
            }
            Log.d(TAG, "RecordInternalRunnable: terminated");
        }
    }
}
