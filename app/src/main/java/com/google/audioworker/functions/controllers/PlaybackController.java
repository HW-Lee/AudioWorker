package com.google.audioworker.functions.controllers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.Log;
import android.util.SparseArray;

import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.playback.PlaybackInfoFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStartFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStopFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.signalproc.SinusoidalGenerator;
import com.google.audioworker.utils.signalproc.WavUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jaygoo.library.converter.Mp3Converter;

public class PlaybackController extends AudioController.AudioRxController {
    private final static String TAG = Constants.packageTag("PlaybackController");

    private ThreadPoolExecutor mPoolExecuter;
    private HashMap<String, SparseArray<PlaybackRunnable>> mRunningPlaybackTasks;

    @Override
    public void activate(Context ctx) {
        mPoolExecuter = new ThreadPoolExecutor(
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.MAX_THREAD_COUNT,
                Constants.Controllers.Config.Common.KEEP_ALIVE_TIME_SECONDS,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        mRunningPlaybackTasks = new HashMap<>();
        mRunningPlaybackTasks.put(PlaybackStartFunction.TASK_NONOFFLOAD, new SparseArray<PlaybackRunnable>());
        mRunningPlaybackTasks.put(PlaybackStartFunction.TASK_OFFLOAD, new SparseArray<PlaybackRunnable>());

        String name = "PlaybackController";
        if (createFolder(name))
            _dataPath = Constants.externalDirectory(name);
        else
            _dataPath = Constants.EnvironmentPaths.SDCARD_PATH;

        Log.i(TAG, "create data folder: " + _dataPath);
    }

    @Override
    public void destroy() {
        super.destroy();

        for (SparseArray<PlaybackRunnable> tasks : mRunningPlaybackTasks.values()) {
            for (int i = 0; i < tasks.size(); i++) {
                PlaybackRunnable task = tasks.get(i);
                if (task != null) {
                    task.tryStop();
                    tasks.delete(i);
                }
            }
        }
        mPoolExecuter.shutdown();
        mPoolExecuter = null;
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

    private void executeBackground(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        if (function instanceof PlaybackFunction && function.isValid()) {
            if (function instanceof PlaybackStartFunction) {
                int playbackId = ((PlaybackStartFunction) function).getPlaybackId();
                String playbackType = ((PlaybackStartFunction) function).getPlaybackType();
                SparseArray<PlaybackRunnable> tasks = mRunningPlaybackTasks.get(playbackType);
                if (tasks == null) {
                    return;
                }
                PlaybackRunnable playbackRunnable = tasks.get(playbackId);
                if (playbackRunnable != null) {
                    Log.w(TAG, "The playback-id " + playbackId + " is running, stop it");
                    PlaybackStopFunction stopFunction = new PlaybackStopFunction();
                    stopFunction.setPlaybackId(((PlaybackStartFunction) function).getPlaybackId());
                    playbackRunnable.tryStop(stopFunction);
                    tasks.delete(playbackId);
                }
                playbackRunnable = new PlaybackRunnable((PlaybackStartFunction) function, l, this);
                tasks.put(playbackId, playbackRunnable);
                mPoolExecuter.execute(playbackRunnable);
            } else if (function instanceof PlaybackStopFunction) {
                int playbackId = ((PlaybackStopFunction) function).getPlaybackId();
                String playbackType = ((PlaybackStopFunction) function).getPlaybackType();
                SparseArray<PlaybackRunnable> tasks = mRunningPlaybackTasks.get(playbackType);
                if (tasks == null) {
                    return;
                }
                PlaybackRunnable playbackRunnable = tasks.get(playbackId);
                if (playbackRunnable != null) {
                    playbackRunnable.tryStop((PlaybackStopFunction) function, l);
                    tasks.delete(playbackId);
                } else if (l != null) {
                    WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
                    ack.setReturnCode(-1);
                    ack.setDescription("invalid argument: the task[" + playbackType + ": " + playbackId + "] does not exist");
                    l.onAckReceived(ack);
                }
            } else if (function instanceof PlaybackInfoFunction) {
                mPoolExecuter.execute(new PlaybackInfoRunnable((PlaybackInfoFunction) function, l));
            }
        } else {
            if (function.isValid())
                Log.e(TAG, "The function: " + function + " is not playback function");
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

    @Override
    public boolean isRxRunning() {
        for (SparseArray<PlaybackRunnable> tasks : mRunningPlaybackTasks.values()) {
            for (int i = 0; i < tasks.size(); i++) {
                PlaybackRunnable task = tasks.get(tasks.keyAt(i));
                if (task != null && !task.hasDone())
                    return true;
            }
        }

        return false;
    }

    @Override
    public int getNumRxRunning() {
        int cnt = 0;
        for (SparseArray<PlaybackRunnable> tasks : mRunningPlaybackTasks.values()) {
            for (int i = 0; i < tasks.size(); i++) {
                PlaybackRunnable task = tasks.get(tasks.keyAt(i));
                if (task != null && !task.hasDone())
                    cnt++;
            }
        }

        return cnt;
    }

    public static class PlaybackRunnable implements Runnable {
        private PlaybackStartFunction mStartFunction;
        private PlaybackStopFunction mStopFunction;
        private AudioAttributes mAttributes;
        private WorkerFunction.WorkerFunctionListener mListener;
        private ControllerBase mController;
        private AudioTrack mTrack;
        private boolean exitPending;

        public PlaybackRunnable(PlaybackStartFunction function, WorkerFunction.WorkerFunctionListener l, ControllerBase controller) {
            this(function, l, controller, null);
        }

        public PlaybackRunnable(PlaybackStartFunction function, WorkerFunction.WorkerFunctionListener l, ControllerBase controller, AudioAttributes attributes) {
            mStartFunction = function;
            mListener = l;
            mController = controller;
            mAttributes = attributes;

            if (mAttributes == null)
                mAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
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
                    return AudioFormat.CHANNEL_OUT_MONO;
                case 2:
                    return AudioFormat.CHANNEL_OUT_STEREO;
                default:
                    return AudioFormat.CHANNEL_OUT_MONO;
            }
        }

        public WorkerFunction setSignalConfig(float amp, float freq) {
            if (mStartFunction != null) {
                mStartFunction.setAmplitude(amp);
                mStartFunction.setTargetFrequency(freq);
            }
            return mStartFunction;
        }

        public PlaybackStartFunction getStartFunction() {
            return mStartFunction;
        }

        public boolean hasDone() {
            return exitPending;
        }

        public void tryStop() {
            tryStop(null);
        }

        public void tryStop(PlaybackStopFunction function) {
            tryStop(function, null);
        }

        public void tryStop(PlaybackStopFunction function, WorkerFunction.WorkerFunctionListener l) {
            if (l != null)
                mListener = l;
            exitPending = true;
            mStopFunction = function;
        }

        @Override
        public void run() {
            if (mStartFunction.getPlaybackType().equals(PlaybackStartFunction.TASK_NONOFFLOAD))
                run_nonoffload();
            else
                run_offload();
        }

        private void run_nonoffload() {
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(mStartFunction.getSamplingFreq())
                    .setEncoding(parseEncodingFormat(mStartFunction.getBitWidth()))
                    .setChannelMask(parseChannelMask(mStartFunction.getNumChannels()))
                    .build();
            int minBuffsize = AudioTrack.getMinBufferSize(
                    mStartFunction.getSamplingFreq(), parseChannelMask(mStartFunction.getNumChannels()), parseEncodingFormat(mStartFunction.getBitWidth()));

            mTrack = new AudioTrack.Builder()
                    .setAudioAttributes(mAttributes).setAudioFormat(format)
                    .setBufferSizeInBytes(minBuffsize).setTransferMode(AudioTrack.MODE_STREAM).build();
            mTrack.play();

            SinusoidalGenerator signalGenerator = new SinusoidalGenerator();
            double[] signal = new double[minBuffsize / mStartFunction.getNumChannels()];
            SparseArray<SinusoidalGenerator.ModelInfo> info = new SparseArray<>();
            exitPending = false;
            Log.d(TAG, "run_nonoffload: start running (id: " + mStartFunction.getPlaybackId() + ")");
            returnAck(mStartFunction, 0);
            switch (mStartFunction.getBitWidth()) {
                case 8: {
                    byte[] buffer = new byte[minBuffsize];

                    while (!exitPending) {
                        info.put(0, new SinusoidalGenerator.ModelInfo(mStartFunction.getAmplitude(), mStartFunction.getTargetFrequency()));
                        signalGenerator.render(signal, info, mStartFunction.getSamplingFreq());
                        for (int i = 0; i < signal.length; i++) {
                            byte v = (byte) (signal[i] * 127);
                            for (int c = 0; c < mStartFunction.getNumChannels(); c++)
                                buffer[i*mStartFunction.getNumChannels() + c] = v;
                        }

                        mTrack.write(buffer, 0, minBuffsize);
                    }
                }
                break;
                case 16: {
                    short[] buffer = new short[minBuffsize];

                    while (!exitPending) {
                        info.put(0, new SinusoidalGenerator.ModelInfo(mStartFunction.getAmplitude(), mStartFunction.getTargetFrequency()));
                        signalGenerator.render(signal, info, mStartFunction.getSamplingFreq());
                        for (int i = 0; i < signal.length; i++) {
                            short v = (short) (signal[i] * 32767);
                            for (int c = 0; c < mStartFunction.getNumChannels(); c++)
                                buffer[i*mStartFunction.getNumChannels() + c] = v;
                        }

                        mTrack.write(buffer, 0, minBuffsize);
                    }
                }
                break;
                default:
                    break;
            }
            if (mStopFunction != null) {
                Log.d(TAG, "run_nonoffload: terminated (id: " + mStopFunction.getPlaybackId() + ")");
                if (mStopFunction.getCommandId() == null)
                    mStopFunction.setCommandId(mStartFunction.getCommandId());
                returnAck(mStopFunction, 0);
            } else {
                returnAck(mStartFunction, -1);
            }
        }

        private void run_offload() {
            Log.d(TAG, "Generating wav file...");
            int retry = 5;
            while (!genAudioFile()) {
                Log.w(TAG, "Failed to generate wav file");
                if (--retry < 0) {
                    Log.e(TAG, "retry count has been reached, abort the request");
                    return;
                }
            }
            Log.d(TAG, "Generate " + Constants.Controllers.Config.Playback.TONE_FILE_DURATION_SECONDS + " sec " + mStartFunction.getTargetFrequency() + "Hz tone wav");

            String wavPath = new File(mController.getDataDir(), getTempName() + ".wav").getAbsolutePath();
            String mp3Path = new File(mController.getDataDir(), getTempName() + ".mp3").getAbsolutePath();
            convertToMp3(wavPath, mp3Path);

            exitPending = false;
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(mp3Path);
                player.setLooping(true);
                player.prepare();
                player.start();

                Log.d(TAG, "run_offload: start running (id: " + mStartFunction.getPlaybackId() + ")");
                returnAck(mStartFunction, 0);
            } catch (IOException e) {
                exitPending = true;
                e.printStackTrace();
            }
            while (!exitPending) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    exitPending = true;
                    e.printStackTrace();
                }
            }
            player.stop();
            player.release();
            if (mStopFunction != null) {
                Log.d(TAG, "run_offload: terminated (id: " + mStopFunction.getPlaybackId() + ")");
                returnAck(mStopFunction, 0);
            } else {
                returnAck(mStartFunction, -1);
            }

            File[] files = {new File(wavPath), new File(mp3Path)};
            for (File f : files) {
                if (f.exists() && !f.delete()) {
                    Log.w(TAG, "Delete " + wavPath + " failed");
                }
            }
        }

        private void convertToMp3(String wavPath, String mp3Path) {
            synchronized (Mp3Converter.class) {
                Mp3Converter.init(mStartFunction.getSamplingFreq(),
                        mStartFunction.getNumChannels(),
                        Constants.Controllers.Config.Playback.MP3_ENCODE.MODE_CBR,
                        mStartFunction.getSamplingFreq(),
                        Constants.Controllers.Config.Playback.MP3_ENCODE.COMPRESSION_RATIO_KHZ,
                        Constants.Controllers.Config.Playback.MP3_ENCODE.QUALITY);

                Log.d(TAG, "Convert " + wavPath + " to " + mp3Path + "...");
                Mp3Converter.convertMp3(wavPath, mp3Path);
                Log.d(TAG, "Convert successfully");
            }
        }

        private boolean genAudioFile() {
            try {
                WavUtils.WavConfig config = new WavUtils.WavConfig.Builder()
                        .withSamplingFrequency(mStartFunction.getSamplingFreq())
                        .withNumChannels(mStartFunction.getNumChannels())
                        .withBitPerSample(mStartFunction.getBitWidth())
                        .withDurationMillis(Constants.Controllers.Config.Playback.TONE_FILE_DURATION_SECONDS * 1000).build();
                DataOutputStream data = WavUtils.obtainWavFile(config, new File(mController.getDataDir(), getTempName() + ".wav").getAbsolutePath());
                double[] signal = new double[mStartFunction.getSamplingFreq()];
                SinusoidalGenerator signalGenerator = new SinusoidalGenerator();
                SparseArray<SinusoidalGenerator.ModelInfo> info = new SparseArray<>();
                double amp = mStartFunction.getAmplitude();
                double freq = mStartFunction.getTargetFrequency();
                info.put(0, new SinusoidalGenerator.ModelInfo(amp, freq));

                for (int dummy = 0; dummy < Constants.Controllers.Config.Playback.TONE_FILE_DURATION_SECONDS; dummy++) {
                    signalGenerator.render(signal, info, mStartFunction.getSamplingFreq());
                    switch ((mStartFunction.getBitWidth())) {
                        case 8: {
                            byte[] raw = new byte[signal.length];
                            for (int i = 0; i < signal.length; i++)
                                raw[i] = (byte) ((1 << 7 - 1) * signal[i]);
                            data.write(raw);
                        }
                        break;
                        case 32: {
                            int[] raw = new int[signal.length * mStartFunction.getNumChannels()];
                            byte[] buffer = new byte[raw.length * 4];
                            for (int i = 0; i < signal.length; i++)
                                for (int c = 0; c < mStartFunction.getNumChannels(); c++)
                                    raw[i*mStartFunction.getNumChannels() + c] = (int) ((1 << 31 - 1) * signal[i]);
                            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(raw);
                            data.write(buffer);
                        }
                            break;
                        case 16:
                        default: {
                            short[] raw = new short[signal.length * mStartFunction.getNumChannels()];
                            byte[] buffer = new byte[raw.length * 2];
                            for (int i = 0; i < signal.length; i++)
                                for (int c = 0; c < mStartFunction.getNumChannels(); c++)
                                    raw[i*mStartFunction.getNumChannels() + c] = (short) ((1 << 15 - 1) * signal[i]);
                            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(raw);
                            data.write(buffer);
                        }
                            break;
                    }
                }

                data.close();
                Log.d(TAG, "write data to: " + new File(mController.getDataDir(), getTempName() + ".wav").getAbsolutePath());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private String getTempName() {
            return "tmp-" + mStartFunction.getPlaybackId() + "-" + this;
        }

        private void returnAck(PlaybackFunction function, int ret) {
            if (mListener == null)
                return;

            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(function);
            if (function instanceof PlaybackStartFunction) {
                ack.setReturnCode(ret);
                if (ret < 0)
                    ack.setDescription("unexpected stop");
                else
                    ack.setDescription("playback start");
            } else if (function instanceof PlaybackStopFunction) {
                ack.setReturnCode(ret);
                ack.setDescription("stop command received");
            } else {
                ack.setReturnCode(-1);
                ack.setDescription("invalid argument");
            }

            mListener.onAckReceived(ack);
        }
    }

    static String getPlaybackInfoAckString(Collection<? extends PlaybackStartFunction> functions) {
        JSONObject obj = new JSONObject();
        try {
            for (PlaybackStartFunction function : functions) {
                String type = function.getPlaybackType();
                if (!obj.has(type)) {
                    obj.put(type, new JSONObject());
                }
                obj.getJSONObject(type).put(String.valueOf(function.getPlaybackId()), function.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject().toString();
        }

        return obj.toString();
    }

    class PlaybackInfoRunnable implements Runnable {
        private PlaybackInfoFunction mFunction;
        private WorkerFunction.WorkerFunctionListener mListener;

        public PlaybackInfoRunnable(PlaybackInfoFunction function, WorkerFunction.WorkerFunctionListener l) {
            mFunction = function;
            mListener = l;
        }

        @Override
        public void run() {
            if (mListener == null)
                return;

            ArrayList<Object> returns = new ArrayList<>();
            ArrayList<PlaybackStartFunction> functions = new ArrayList<>();
            for (String type : mRunningPlaybackTasks.keySet()) {
                SparseArray<PlaybackRunnable> tasks = mRunningPlaybackTasks.get(type);
                if (tasks == null)
                    continue;
                for (int i = 0; i < tasks.size(); i++) {
                    int idx = tasks.keyAt(i);
                    PlaybackRunnable task = tasks.get(idx);
                    if (task == null) {
                        continue;
                    }
                    functions.add(task.mStartFunction);
                }
            }
            returns.add(PlaybackController.getPlaybackInfoAckString(functions));

            WorkerFunction.Ack ack = WorkerFunction.Ack.ackToFunction(mFunction);
            ack.setReturnCode(0);
            ack.setDescription("info returned");
            ack.setReturns(returns);
            mListener.onAckReceived(ack);
        }

        private void save_put(JSONObject obj, String key, Object value) {
            try {
                obj.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
