package com.google.audioworker.utils.logging;

import android.support.annotation.NonNull;

import com.google.audioworker.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LoggerBase<T> extends Thread {
    private int maxNumEntries;
    private int autoStorePeriodMs;

    private AtomicBoolean exitPending;

    protected final ArrayList<LogEntry<T>> logEntris;

    abstract protected boolean saveLog(List<String> logs);

    public static class LogUnit {
        private String tag;
        private String msg;

        public LogUnit(String tag, String msg) {
            this.tag = tag;
            this.msg = msg;
        }

        @NonNull
        @Override
        public String toString() {
            return tag + ": " + msg;
        }
    }

    static class LogEntry<T> {
        private String createAt;
        private T content;

        LogEntry(T c) {
            createAt = new SimpleDateFormat(Constants.Logging.TIME_FORMAT, Constants.Logging.LOCALE).format(Calendar.getInstance().getTime());
            content = c;
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + createAt + "] " + content;
        }
    }

    protected LoggerBase(int nentries, int periodMs) {
        maxNumEntries = nentries;
        autoStorePeriodMs = periodMs;
        logEntris = new ArrayList<>();
        exitPending = new AtomicBoolean(true);
    }

    public void pushLog(T content) {
        synchronized (logEntris) {
            logEntris.add(new LogEntry<T>(content));
            if (logEntris.size() >= maxNumEntries) {
                synchronized (this) {
                    notify();
                }
            }
        }
    }

    public void activate() {
        start();
    }

    public void shutdown() {
        exitPending.set(true);
    }

    @Override
    final public void run() {
        exitPending.set(false);
        while (!exitPending.get()) {
            ArrayList<String> logs = new ArrayList<>();
            synchronized (logEntris) {
                for (LogEntry<T> entry : logEntris)
                    logs.add(entry.toString());

                if (logs.size() > 0 && saveLog(logs)) {
                    logEntris.clear();
                }
            }

            try {
                synchronized (this) {
                    wait(autoStorePeriodMs);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
