package com.google.audioworker.utils.logging;

import com.google.audioworker.utils.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class WorkerLogger extends LoggerBase<LoggerBase.LogUnit> {
    private static final String TAG = Constants.packageTag("WorkerLogger");

    private String mDir;
    private String mFileName;

    public WorkerLogger(int nentries, int periodMs, String dirname, String filename) {
        super(nentries, periodMs);
        mFileName = filename;
        if (createFolder(dirname)) {
            mDir = Constants.externalDirectory(dirname);
        } else {
            mDir = Constants.externalDirectory("");
        }
    }

    @Override
    protected boolean saveLog(List<String> logs) {
        try {
            FileWriter writer = new FileWriter(new File(mDir, mFileName), true);
            for (String log : logs) {
                writer.write(log + "\n");
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean createFolder(String name) {
        File folder = new File(Constants.externalDirectory(name));
        return folder.exists() || folder.mkdirs();
    }
}
