package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.audioworker.functions.audio.record.RecordFunction;
import com.google.audioworker.functions.audio.record.RecordInfoFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.RecordStopFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RecordController extends ControllerBase {
    private final static String TAG = Constants.packageTag("RecordController");

    private RecordRunnable mMainRunningTask;
    private ThreadPoolExecutor mPoolExecuter;

    @Override
    public void activate(Context ctx) {
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
        if (mMainRunningTask != null && !mMainRunningTask.hasDone()) {
            mMainRunningTask.tryStop();
            mMainRunningTask = null;
        }
        mPoolExecuter.shutdown();
        mPoolExecuter = null;
    }

    @Override
    public void execute(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        if (function instanceof RecordFunction && function.isValid()) {
            if (function instanceof RecordStartFunction) {
                if (mMainRunningTask != null && !mMainRunningTask.hasDone()) {
                    RecordStopFunction f = new RecordStopFunction();
                    f.setCommandId(function.getCommandId());
                    mMainRunningTask.tryStop(f);
                }
                mMainRunningTask = new RecordRunnable((RecordStartFunction) function, l);
                mMainRunningTask.setRecordRunner(new RecordInternalRunnable(mMainRunningTask));
                mPoolExecuter.execute(mMainRunningTask);
                mPoolExecuter.execute(mMainRunningTask.slave);
            } else if (function instanceof RecordStopFunction) {
                if (mMainRunningTask != null && !mMainRunningTask.hasDone()) {
                    mMainRunningTask.tryStop((RecordStopFunction) function);
                    mMainRunningTask = null;
                }
            } else if (function instanceof RecordInfoFunction) {
                if (l == null)
                    return;
                WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                if (mMainRunningTask != null && !mMainRunningTask.hasDone()) {
                    ArrayList<Object> returns = new ArrayList<>();
                    returns.add(mMainRunningTask.mStartFunction.toString());
                    ack.setReturns(returns);
                }
                ack.setReturnCode(0);
                ack.setDescription("info returned");
                l.onAckReceived(ack);
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

    public static class RecordRunnable implements Runnable {
        private RecordStartFunction mStartFunction;
        private RecordStopFunction mStopFunction;
        private WorkerFunction.WorkerFunctionListener mListener;

        private final RecordSharedBuffer sharedBuffer;
        private RecordInternalRunnable slave;

        private boolean exitPending;

        RecordRunnable(RecordStartFunction function, WorkerFunction.WorkerFunctionListener l) {
            mStartFunction = function;
            mListener = l;

            int minBuffsize = AudioRecord.getMinBufferSize(
                    mStartFunction.getSamplingFreq(), parseChannelMask(mStartFunction.getNumChannels()), parseEncodingFormat(mStartFunction.getBitWidth()));
            sharedBuffer = new RecordSharedBuffer(minBuffsize);
        }

        public RecordInternalRunnable getSlave() {
            return slave;
        }

        public boolean hasDone() {
            return exitPending;
        }

        public void tryStop() {
            tryStop(null);
        }

        public void tryStop(RecordStopFunction function) {
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

        private void returnAck(RecordFunction function, int ret) {
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
            byte[] buffer = new byte[minBuffsize];
            int cnt = 0;
            Log.d(TAG, "RecordRunnable: start running");
            returnAck(mStartFunction, 0);
            while (!exitPending) {
                try {
                    if (!sharedBuffer.dataAvailable()) {
                        synchronized (this) {
                            wait((long) (minBuffsizeMillis * 1.1));
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (sharedBuffer.dataAvailable()) {
                    sharedBuffer.fetch(buffer);
                } else {
                    Arrays.fill(buffer, (byte) 0);
                }

                short[] frame = new short[buffer.length / 2];
                ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(frame);
                double[] values = new double[frame.length];
                for (int i = 0; i < values.length; i++)
                    values[i] = frame[i] * 1.0 / (1 << 15);
            }
            if (mStopFunction != null) {
                returnAck(mStopFunction, 0);
            } else {
                returnAck(mStartFunction, -1);
            }
            Log.d(TAG, "RecordRunnable: terminated");
            if (slave != null) {
                slave.tryStop();
                slave = null;
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
            Log.d(TAG, "RecordInternalRunnable: terminated");
        }
    }
}
