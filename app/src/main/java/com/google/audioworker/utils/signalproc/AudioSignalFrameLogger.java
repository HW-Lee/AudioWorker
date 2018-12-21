package com.google.audioworker.utils.signalproc;

import android.util.Log;

import com.google.audioworker.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by hw_lee on 2018/3/9.
 */

public class AudioSignalFrameLogger {
    private static final String TAG = Constants.packageTag("AudioSignalFrameLogger");
    private static final String DUMP_STREAM_NAME = "stream.bin";
    private static final String DUMP_INFO_NAME = "info.json";

    private final ArrayList<AudioSignalFrame> mFrames;

    public class AudioSignalFrame {
        private String createAt;
        private String mName;
        private int mFs;
        private double[] mValues;

        public AudioSignalFrame(String name, int fs, double[] value) {
            mName = name;
            mFs = fs;
            mValues = Arrays.copyOf(value, value.length);
            createAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS '(UTF+8)'", Locale.TAIWAN).format(Calendar.getInstance().getTime());
        }

        public String getCreateAt() {
            return createAt;
        }

        public String getName() {
            return mName;
        }

        public int getSampleRate() {
            return mFs;
        }

        public double[] getValues() {
            return mValues;
        }
    }

    public AudioSignalFrameLogger() {
        mFrames = new ArrayList<>(100);
    }

    public void push(String name, double[] value) {
        push(name, -1, value);
    }

    public void push(String name, int fs, double[] value) {
        synchronized (mFrames) {
            mFrames.add(new AudioSignalFrame(name, fs, value));
        }
    }

    public void clear() {
        synchronized (mFrames) {
            mFrames.clear();
        }
    }

    public void dumpTo(String path) {
        File folder = new File(path);

        boolean success;
        if (!folder.exists()) {
            Log.d(TAG, "create the folder \"" + path + "\"");
            success = folder.mkdirs();
            if (!success) {
                Log.w(TAG, "failed to create the folder, skip the command");
                clear();
                return;
            }
        }

        try {
            FileOutputStream os = new FileOutputStream(new File(path, DUMP_STREAM_NAME));
            JSONArray info = new JSONArray();

            synchronized (mFrames) {
                for (AudioSignalFrame frame : mFrames) {
                    info.put(toJson(frame));

                    ByteBuffer buf = ByteBuffer.allocate(frame.getValues().length * 8);
                    buf.clear();
                    buf.asDoubleBuffer().put(frame.getValues());
                    os.write(buf.array());
                }
            }

            os.close();

            PrintWriter pw = new PrintWriter(new File(path, DUMP_INFO_NAME));
            pw.write(info.toString());
            pw.close();
        } catch (Exception e) {
            Log.w(TAG, "failed to dump the buffer");
        }

        clear();
    }

    private JSONObject toJson(AudioSignalFrame frame) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", frame.getName());
            obj.put("fs", frame.getSampleRate());
            obj.put("createAt", frame.getCreateAt());
            obj.put("datasize-in-double", frame.getValues().length);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
