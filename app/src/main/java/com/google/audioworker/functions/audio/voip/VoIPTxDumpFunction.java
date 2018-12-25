package com.google.audioworker.functions.audio.voip;

import com.google.audioworker.functions.audio.record.RecordDumpFunction;
import com.google.audioworker.utils.Constants;

public class VoIPTxDumpFunction extends VoIPFunction {
    private final static String TAG = Constants.packageTag("VoIPTxDumpFunction");

    private RecordDumpFunction mReference = new RecordDumpFunction();

    @Override
    public Parameter[] getParameters() {
        return mReference.getParameters();
    }

    @Override
    public boolean isValueAccepted(String attr, Object value) {
        return mReference.isValueAccepted(attr, value);
    }

    @Override
    public void setParameter(String attr, Object value) {
        mReference.setParameter(attr, value);
    }

    public String getFileName() {
        return mReference.getFileName();
    }
}
