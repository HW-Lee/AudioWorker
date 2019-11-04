package com.google.audioworker.utils.logging;

import android.util.Log;

import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.ds.CircularArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class BufferLogger extends LoggerBase<LoggerBase.LogUnit> {
    private static final String TAG = Constants.packageTag("BufferLogger");

    private final CircularArray<String> mLogBuffer;

    public BufferLogger(int nentries, int periodMs) {
        super(nentries, periodMs);
        mLogBuffer = new CircularArray<>(nentries);
    }

    @Override
    protected boolean saveLog(List<String> logs) {
        boolean success;
        synchronized (mLogBuffer) {
            success = mLogBuffer.addAll(logs);
        }
        return success;
    }

    public String[] getDump() {
        String[] logs;

        synchronized (mLogBuffer) {
            logs = new String[mLogBuffer.size()];
            mLogBuffer.toArray(logs);
        }

        return logs;
    }

    public boolean dumpTo(String filename) {
        synchronized (mLogBuffer) {
            try {
                FileWriter writer = new FileWriter(new File(filename), false);
                for (String log : mLogBuffer) {
                    writer.write(log + "\n");
                }

                writer.close();
                mLogBuffer.clear();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public void dumpToSystemLog() {
        synchronized (mLogBuffer) {
            for (String log : mLogBuffer)
                Log.d(TAG, log);
        }
    }
}
