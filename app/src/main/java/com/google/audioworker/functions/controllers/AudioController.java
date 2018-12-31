package com.google.audioworker.functions.controllers;

import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.record.RecordFunction;
import com.google.audioworker.functions.audio.record.detectors.DetectorBase;
import com.google.audioworker.functions.audio.voip.VoIPFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.utils.Constants;

public class AudioController extends ManagerController {
    private final static String TAG = Constants.packageTag("AudioController");

    public interface TxCallback {
        void registerDataListener(RecordController.RecordRunnable.RecordDataListener l);
        void unregisterDataListener(RecordController.RecordRunnable.RecordDataListener l);
        DetectorBase getDetectorByHandle(String handle);
    }

    public static abstract class AudioTxController extends ControllerBase implements TxCallback {
    }

    public AudioController() {
        super();
        mControllers.put(Constants.Controllers.NAME_PLAYBACK, new PlaybackController());
        mControllers.put(Constants.Controllers.NAME_RECORD, new RecordController());
        mControllers.put(Constants.Controllers.NAME_VOIP, new VoIPController());
    }

    @Override
    public void execute(WorkerFunction function, WorkerFunction.WorkerFunctionListener l) {
        ControllerBase selectedController = null;
        if (function instanceof PlaybackFunction)
            selectedController = mControllers.get(Constants.Controllers.NAME_PLAYBACK);
        else if (function instanceof RecordFunction)
            selectedController = mControllers.get(Constants.Controllers.NAME_RECORD);
        else if (function instanceof VoIPFunction)
            selectedController = mControllers.get(Constants.Controllers.NAME_VOIP);

        if (selectedController != null)
            selectedController.execute(function, l);
    }
}
