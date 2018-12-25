package com.google.audioworker.functions.commands;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.audioworker.functions.audio.playback.PlaybackFunction;
import com.google.audioworker.functions.audio.playback.PlaybackInfoFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStartFunction;
import com.google.audioworker.functions.audio.playback.PlaybackStopFunction;
import com.google.audioworker.functions.audio.record.RecordDetectFunction;
import com.google.audioworker.functions.audio.record.RecordDumpFunction;
import com.google.audioworker.functions.audio.record.RecordEventFunction;
import com.google.audioworker.functions.audio.record.RecordFunction;
import com.google.audioworker.functions.audio.record.RecordInfoFunction;
import com.google.audioworker.functions.audio.record.RecordStartFunction;
import com.google.audioworker.functions.audio.record.RecordStopFunction;
import com.google.audioworker.functions.audio.voip.VoIPConfigFunction;
import com.google.audioworker.functions.audio.voip.VoIPDetectFunction;
import com.google.audioworker.functions.audio.voip.VoIPEventFunction;
import com.google.audioworker.functions.audio.voip.VoIPFunction;
import com.google.audioworker.functions.audio.voip.VoIPInfoFunction;
import com.google.audioworker.functions.audio.voip.VoIPStartFunction;
import com.google.audioworker.functions.audio.voip.VoIPStopFunction;
import com.google.audioworker.functions.audio.voip.VoIPTxDumpFunction;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.shell.ShellFunction;
import com.google.audioworker.utils.Constants;
import com.google.audioworker.utils.communicate.base.Communicable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class CommandHelper {
    private final static String TAG = Constants.packageTag("CommandHelper");

    public static class Command extends JSONObject {
        public Command() {
            super();
            try {
                put(Constants.MessageSpecification.COMMAND_ID, genId());
            } catch (JSONException e) {
                Log.e(TAG + "::Command", "put command id failed");
            }
        }

        public Command(String s) throws JSONException {
            super(s);
            if (!has(Constants.MessageSpecification.COMMAND_ID)) {
                put(Constants.MessageSpecification.COMMAND_ID, genId());
            }
        }

        @SuppressLint("MissingPermission")
        private String genId() {
            return Build.getSerial() + "::" + System.currentTimeMillis();
        }
    }

    static public WorkerFunction getFunction(String cmd) {
        try {
            JSONObject jsonCmd = new JSONObject(cmd);
            return getFunction(jsonCmd);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    static public WorkerFunction getFunction(Intent intent) {
        switch (Constants.getIntentOwner(intent)) {
            case Constants.INTENT_OWNER_PLAYBACK:
                return getPlaybackFunction(intent);
            case Constants.INTENT_OWNER_RECORD:
                return getRecordFunction(intent);
            case Constants.INTENT_OWNER_VOIP:
                return getVoIPFunction(intent);

            default:
                break;
        }

        switch (Objects.requireNonNull(intent.getAction())) {
            case Constants.DebugInterface.INTENT_RECEIVE_FUNCTION:
                String cmd = intent.getStringExtra(Constants.DebugInterface.INTENT_KEY_FUNCTION_CONTENT);
                return getFunction(cmd);

            default:
                break;
        }
        return null;
    }

    static private WorkerFunction getFunction(JSONObject obj) throws JSONException {
        WorkerFunction function;
        function = getShellFunction(obj);
        if (function != null)
            return function;

        return null;
    }

    static private ShellFunction getShellFunction(JSONObject obj) throws JSONException {
        ShellFunction function = null;
        if (obj.has(Constants.MessageSpecification.COMMAND_SHELL_TARGET)) {
            function = new ShellFunction(obj.getString(Constants.MessageSpecification.COMMAND_SHELL_TARGET));
        }

        if (obj.has(Constants.MessageSpecification.COMMAND_BROADCAST_INTENT)) {
            function = new ShellFunction(obj);
        }

        if (obj.has(Constants.MessageSpecification.COMMAND_ID) && obj.getString(Constants.MessageSpecification.COMMAND_ID) != null && function != null) {
            function.setCommandId(obj.getString(Constants.MessageSpecification.COMMAND_ID));
        }

        return function;
    }

    static private void checkParameters(WorkerFunction function, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (WorkerFunction.Parameter parameter : function.getParameters()) {
                String attr = parameter.getAttribute();
                if (extras.containsKey(attr)) {
                    function.setParameter(attr, extras.get(attr));
                }
            }

            if (extras.containsKey(Constants.MessageSpecification.COMMAND_ID)) {
                function.setCommandId(extras.getString(Constants.MessageSpecification.COMMAND_ID));
            } else {
                function.setCommandId(null);
            }
        }
    }

    static private PlaybackFunction getPlaybackFunction(Intent intent) {
        PlaybackFunction function;
        switch (Objects.requireNonNull(intent.getAction())) {
            case Constants.MasterInterface.INTENT_PLAYBACK_INFO:
                function = new PlaybackInfoFunction();
                break;
            case Constants.MasterInterface.INTENT_PLAYBACK_START:
                function = new PlaybackStartFunction();
                break;
            case Constants.MasterInterface.INTENT_PLAYBACK_STOP:
                function = new PlaybackStopFunction();
                break;

            default:
                return null;
        }

        checkParameters(function, intent);
        return function;
    }

    static private RecordFunction getRecordFunction(Intent intent) {
        RecordFunction function;
        switch (Objects.requireNonNull(intent.getAction())) {
            case Constants.MasterInterface.INTENT_RECORD_INFO:
                function = new RecordInfoFunction();
                break;
            case Constants.MasterInterface.INTENT_RECORD_START:
                function = new RecordStartFunction();
                break;
            case Constants.MasterInterface.INTENT_RECORD_STOP:
                function = new RecordStopFunction();
                break;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_REGISTER:
                function = new RecordDetectFunction(RecordDetectFunction.OP_REGISTER);
                break;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_UNREGISTER:
                function = new RecordDetectFunction(RecordDetectFunction.OP_UNREGISTER);
                break;
            case Constants.MasterInterface.INTENT_RECORD_DETECT_SETPARAMS:
                function = new RecordDetectFunction(RecordDetectFunction.OP_SETPARAMS);
                break;
            case Constants.MasterInterface.INTENT_RECORD_DUMP:
                function = new RecordDumpFunction();
                break;
            case Constants.SlaveInterface.INTENT_RECORD_EVENT:
                function = new RecordEventFunction();
                break;

            default:
                return null;
        }

        checkParameters(function, intent);
        return function;
    }

    static private VoIPFunction getVoIPFunction(Intent intent) {
        VoIPFunction function;
        switch (Objects.requireNonNull(intent.getAction())) {
            case Constants.MasterInterface.INTENT_VOIP_CONFIG:
                function = new VoIPConfigFunction();
                break;
            case Constants.MasterInterface.INTENT_VOIP_INFO:
                function = new VoIPInfoFunction();
                break;
            case Constants.MasterInterface.INTENT_VOIP_START:
                function = new VoIPStartFunction();
                break;
            case Constants.MasterInterface.INTENT_VOIP_STOP:
                function = new VoIPStopFunction();
                break;
            case Constants.MasterInterface.INTENT_VOIP_DETECT_REGISTER:
                function = new VoIPDetectFunction(RecordDetectFunction.OP_REGISTER);
                break;
            case Constants.MasterInterface.INTENT_VOIP_DETECT_UNREGISTER:
                function = new VoIPDetectFunction(RecordDetectFunction.OP_UNREGISTER);
                break;
            case Constants.MasterInterface.INTENT_VOIP_DETECT_SETPARAMS:
                function = new VoIPDetectFunction(RecordDetectFunction.OP_SETPARAMS);
                break;
            case Constants.MasterInterface.INTENT_VOIP_TX_DUMP:
                function = new VoIPTxDumpFunction();
                break;
            case Constants.SlaveInterface.INTENT_VOIP_EVENT:
                function = new VoIPEventFunction();
                break;

            default:
                return null;
        }

        checkParameters(function, intent);
        return function;
    }

    static private ShellFunction getShellFunction(String cmd) {
        return null;
    }

    static private ShellFunction getShellFunction(Intent intent) {
        return null;
    }

    public static class BroadcastHandler extends BroadcastReceiver {
        public interface FunctionReceivedListener {
            public void onFunctionReceived(WorkerFunction function);
        }

        private FunctionReceivedListener mListener;
        private Communicable<String, String> mCommunicator;

        public BroadcastHandler(FunctionReceivedListener l) {
            mListener = l;
        }

        public void registerCommuncator(Communicable<String, String> communicator) {
            mCommunicator = communicator;
        }

        static public BroadcastHandler registerReceiver(Context ctx, FunctionReceivedListener l) {
            BroadcastHandler handler = new BroadcastHandler(l);
            IntentFilter intentFilter = new IntentFilter("android.intent.action.MAIN");
            ctx.registerReceiver(handler, intentFilter);

            ArrayList<String[]> interfaces = new ArrayList<>();
            interfaces.add(Constants.MasterInterface.INTENT_NAMES);
            interfaces.add(Constants.SlaveInterface.INTENT_NAMES);
            interfaces.add(Constants.DebugInterface.INTENT_NAMES);

            for (String[] intents : interfaces) {
                for (String intent : intents) {
                    intentFilter = new IntentFilter(intent);
                    ctx.registerReceiver(handler, intentFilter);
                }
            }

            return handler;
        }

        static public void unregisterReceiver(Context ctx, BroadcastReceiver handler) {
            ctx.unregisterReceiver(handler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListener == null)
                return;

            switch (Constants.getIntentOwner(intent)) {
                case Constants.INTENT_OWNER_PLAYBACK:
                    mListener.onFunctionReceived(CommandHelper.getPlaybackFunction(intent));
                    break;
                case Constants.INTENT_OWNER_RECORD:
                    mListener.onFunctionReceived(CommandHelper.getRecordFunction(intent));
                    break;
                case Constants.INTENT_OWNER_VOIP:
                    mListener.onFunctionReceived(CommandHelper.getVoIPFunction(intent));
                    break;

                case Constants.DebugInterface.INTENT_RECEIVE_FUNCTION:
                    mListener.onFunctionReceived(CommandHelper.getFunction(intent));
                    break;
                case Constants.DebugInterface.INTNET_SEND_FUNCTION:
                    String recv = intent.getStringExtra(Constants.DebugInterface.INTENT_KEY_RECEIVER_ID);
                    String func = intent.getStringExtra(Constants.DebugInterface.INTENT_KEY_FUNCTION_CONTENT);
                    if (mCommunicator != null && recv != null && func != null)
                        mCommunicator.send(recv, func);
                    break;

                default:
                    Log.w(TAG, "no handler for intent: " + intent.getAction());
                    break;
            }
        }
    }
}
