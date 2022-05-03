package com.google.audioworker.utils;

import android.Manifest;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;

import com.google.audioworker.fragments.AudioFragment;
import com.google.audioworker.fragments.AudioPlaybackFragment;
import com.google.audioworker.fragments.AudioRecordFragment;
import com.google.audioworker.fragments.AudioVoIPFragment;
import com.google.audioworker.fragments.ConnectFragment;
import com.google.audioworker.fragments.GeneralInfoFragment;
import com.google.audioworker.fragments.ShellFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class Constants {
    private final static String APP_NAME_TAG = "Google-AudioWorker";

    public static String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.INTERNET,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            // WIFI P2P
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,

            // Telephony
            Manifest.permission.READ_PHONE_STATE,
    };

    public static String packageTag(String tag) {
        return APP_NAME_TAG + "::" + tag;
    }

    public static String externalDirectory(String tag) {
        return new File(EnvironmentPaths.SDCARD_PATH, EnvironmentPaths.PACKAGE_ROOT_FOLDER + "/" + tag).getAbsolutePath();
    }

    public static String dataDirectory(String tag) {
        return new File(EnvironmentPaths.DATA_PATH, EnvironmentPaths.PACKAGE_ROOT_FOLDER + "/" + tag).getAbsolutePath();
    }

    public static class EnvironmentPaths {
        public final static String PACKAGE_ROOT_FOLDER = APP_NAME_TAG + "-data";
        public final static String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
        public final static String DATA_PATH = Environment.getDataDirectory().getAbsolutePath();
    }

    public static class Fragments {
        public static class FragmentInfo {
            public String spec;
            public String label;
            public Class<?> classTarget;

            FragmentInfo(String spec, String label, Class<?> classTarget) {
                this.spec = spec;
                this.label = label;
                this.classTarget = classTarget;
            }
        }

        private final static FragmentInfo GENERAL_FRAGMENT_INFO = new FragmentInfo("GeneralFragment", "Main", GeneralInfoFragment.class);
        private final static FragmentInfo CONNECT_FRAGMENT_INFO = new FragmentInfo("ConnectFragment", "Comm", ConnectFragment.class);
        private final static FragmentInfo AUDIO_FRAGMENT_INFO = new FragmentInfo("AudioFragment", "Audio", AudioFragment.class);
        private final static FragmentInfo SHELL_FRAGMENT_INFO = new FragmentInfo("ShellFragment", "Shell", ShellFragment.class);

        public final static FragmentInfo[] FRAGMENT_INFOS = {
                GENERAL_FRAGMENT_INFO,
                CONNECT_FRAGMENT_INFO,
                AUDIO_FRAGMENT_INFO,
                SHELL_FRAGMENT_INFO
        };

        public static class Audio {
            private final static FragmentInfo PLAYBACK_FRAGMENT_INFO = new FragmentInfo("AudioPlaybackFragment", "Playback", AudioPlaybackFragment.class);
            private final static FragmentInfo RECORD_FRAGMENT_INFO = new FragmentInfo("AudioRecordFragment", "Record", AudioRecordFragment.class);
            private final static FragmentInfo VOIP_FRAGMENT_INFO = new FragmentInfo("AudioVoIPFragment", "VoIP", AudioVoIPFragment.class);

            public final static FragmentInfo[] FRAGMENT_INFOS = {
                    PLAYBACK_FRAGMENT_INFO,
                    RECORD_FRAGMENT_INFO,
                    VOIP_FRAGMENT_INFO
            };
        }
    }

    public static class WIFIP2PConstants {
        public final static int SERVER_PORT = 8888;
        public final static int MAX_THREAD_COUNT = 10;
        public final static int KEEP_ALIVE_TIME_SECONDS = 30;
        public final static int MSG_BUFFER_SIZE = 1024;
    }

    public static class MessageSpecification {
        public final static String COMMAND_ID = "command-id";
        public final static String COMMAND_TAG_NAME = "SN";

        public final static String COMMAND_ACK_RETURN = "return";
        public final static String COMMAND_ACK_DESC = "desc";
        public final static String COMMAND_ACK_RETURN_CODE = "code";
        public final static String COMMAND_ACK_TARGET = "target";

        public final static String COMMAND_SHELL_TARGET = "shell";
        public final static String COMMAND_BROADCAST_INTENT = "intent";
        public final static String COMMAND_BROADCAST_PARAMS = "params";
    }

    private final static String INTENT_PREFIX = "com.google.audioworker.intent";
    public final static String INTENT_OWNER_PLAYBACK = "playback";
    public final static String INTENT_OWNER_RECORD = "record";
    public final static String INTENT_OWNER_VOIP = "voip";
    public final static String INTENT_OWNER_UNKNOWN = "unknown";

    public static String getIntentOwner(Intent intent) {
        return getIntentOwner(intent.getAction());
    }

    public static String getIntentOwner(String intent) {
        if (intent == null || !intent.startsWith(INTENT_PREFIX))
            return INTENT_OWNER_UNKNOWN;

        try {
            String prefixPattern = INTENT_PREFIX.replace(".", "\\.");
            String separatorPatter = "\\.";
            String owner = intent.split(prefixPattern)[1].split(separatorPatter)[1];
            switch (owner) {
                case INTENT_OWNER_PLAYBACK:
                case INTENT_OWNER_RECORD:
                case INTENT_OWNER_VOIP:
                    return owner;

                default:
                    return intent;
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return INTENT_OWNER_UNKNOWN;
        }
    }

    public static class MasterInterface {
        public final static String INTENT_PLAYBACK_START = INTENT_PREFIX + ".playback.start";
        public final static String INTENT_PLAYBACK_STOP = INTENT_PREFIX + ".playback.stop";
        public final static String INTENT_PLAYBACK_INFO = INTENT_PREFIX + ".playback.info";

        public final static String INTENT_RECORD_START = INTENT_PREFIX + ".record.start";
        public final static String INTENT_RECORD_STOP = INTENT_PREFIX + ".record.stop";
        public final static String INTENT_RECORD_DETECT_REGISTER = INTENT_PREFIX + ".record.detect.register";
        public final static String INTENT_RECORD_DETECT_UNREGISTER = INTENT_PREFIX + ".record.detect.unregister";
        public final static String INTENT_RECORD_DETECT_SETPARAMS = INTENT_PREFIX + ".record.detect.setparams";
        public final static String INTENT_RECORD_INFO = INTENT_PREFIX + ".record.info";
        public final static String INTENT_RECORD_DUMP = INTENT_PREFIX + ".record.dump";

        public final static String INTENT_VOIP_START = INTENT_PREFIX + ".voip.start";
        public final static String INTENT_VOIP_STOP = INTENT_PREFIX + ".voip.stop";
        public final static String INTENT_VOIP_DETECT_REGISTER = INTENT_PREFIX + ".voip.detect.register";
        public final static String INTENT_VOIP_DETECT_UNREGISTER = INTENT_PREFIX + ".voip.detect.unregister";
        public final static String INTENT_VOIP_DETECT_SETPARAMS = INTENT_PREFIX + ".voip.detect.setparams";
        public final static String INTENT_VOIP_INFO = INTENT_PREFIX + ".voip.info";
        public final static String INTENT_VOIP_CONFIG = INTENT_PREFIX + ".voip.config";
        public final static String INTENT_VOIP_TX_DUMP = INTENT_PREFIX + ".voip.tx.dump";

        public final static String[] INTENT_NAMES = {
                INTENT_PLAYBACK_START,
                INTENT_PLAYBACK_STOP,
                INTENT_PLAYBACK_INFO,

                INTENT_RECORD_START,
                INTENT_RECORD_STOP,
                INTENT_RECORD_DETECT_REGISTER,
                INTENT_RECORD_DETECT_UNREGISTER,
                INTENT_RECORD_DETECT_SETPARAMS,
                INTENT_RECORD_INFO,
                INTENT_RECORD_DUMP,

                INTENT_VOIP_START,
                INTENT_VOIP_STOP,
                INTENT_VOIP_DETECT_REGISTER,
                INTENT_VOIP_DETECT_UNREGISTER,
                INTENT_VOIP_DETECT_SETPARAMS,
                INTENT_VOIP_INFO,
                INTENT_VOIP_CONFIG,
                INTENT_VOIP_TX_DUMP
        };
    }

    public static class SlaveInterface {
        public final static String INTENT_RECORD_EVENT = INTENT_PREFIX + ".record.event";

        public final static String INTENT_VOIP_EVENT = INTENT_PREFIX + ".voip.event";

        public final static String[] INTENT_NAMES = {
                INTENT_RECORD_EVENT,
                INTENT_VOIP_EVENT
        };
    }

    public static class DebugInterface {
        public final static String INTENT_RECEIVE_FUNCTION = INTENT_PREFIX + ".debug.receive.function";
        public final static String INTNET_SEND_FUNCTION = INTENT_PREFIX + ".debug.send.function";

        public final static String INTENT_KEY_FUNCTION_CONTENT = "json";
        public final static String INTENT_KEY_RECEIVER_ID = "receiver";

        public final static String[] INTENT_NAMES = {
                INTENT_RECEIVE_FUNCTION,
                INTNET_SEND_FUNCTION
        };
    }

    public static class Controllers {
        public final static String NAME_AUDIO = "Audio";
        public final static String NAME_PLAYBACK = "Playback";
        public final static String NAME_RECORD = "Record";
        public final static String NAME_VOIP = "VoIP";
        public final static String NAME_SHELL = "Shell";

        public static class Config {
            public static class Common {
                public final static int MAX_THREAD_COUNT = 10;
                public final static int KEEP_ALIVE_TIME_SECONDS = 30;
                public final static int BYTE_BUFFER_SIZE = 1024;
            }

            public static class Playback {
                public final static int TONE_FILE_DURATION_SECONDS = 60;

                public static class MP3_ENCODE {
                    public final static int MODE_CBR = 0;
                    public final static int MODE_VBR = 1;
                    public final static int MODE_ABR = 2;
                    public final static int COMPRESSION_RATIO_KHZ = 96; // 8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, or 320
                    public final static int QUALITY = 7; // quality=0..9. 0=best (very slow). 9=worst
                                                         // 2 near-best quality, not too slow
                                                         // 5 good quality, fast
                                                         // 7 ok quality, really fast
                }
            }

            public static class Record {
                public final static float TIMEOUT_MULTIPLIER = 5.0f;
            }
        }
    }

    public static class PlaybackDefaultConfig {
        public final static float AMPLITUDE = 0.6f;
        public final static int SAMPLING_FREQ = 44100;
        public final static int NUM_CHANNELS = 2;
        public final static int BIT_PER_SAMPLE = 16;
    }

    public static class RecordDefaultConfig {
        public final static int SAMPLING_FREQ = 44100;
        public final static int NUM_CHANNELS = 2;
        public final static int BIT_PER_SAMPLE = 16;
        public final static int BUFFER_SIZE_MILLIS = 0;
        public final static int INPUT_SRC = MediaRecorder.AudioSource.MIC;
    }

    public static class VoIPDefaultConfig {
        public static class Rx {
            public final static float AMPLITUDE = 0.6f;
            public final static int SAMPLING_FREQ = 8000;
            public final static int NUM_CHANNELS = 1;
            public final static int BIT_PER_SAMPLE = 16;
        }
        public static class Tx {
            public final static int SAMPLING_FREQ = 8000;
            public final static int NUM_CHANNELS = 1;
            public final static int BIT_PER_SAMPLE = 16;
            public final static int BUFFER_SIZE_MILLIS = 0;
        }
    }

    public static class Detectors {
        public static class ToneDetector {
            private final static Class<?> CLASS_REF = com.google.audioworker.functions.audio.record.detectors.ToneDetector.class;

            public final static String PARAM_FS = "sampling-freq";
            public final static String PARAM_PROCESS_FRAME_MILLIS = "process-frame-ms";
            public final static String PARAM_TOL_DIFF_SEMI = "tolerance-semitone";
            public final static String PARAM_TARGET_FREQ = "target-freq";
            public final static String PARAM_CLEAR_TARGETS = "clear-target";
            public final static String PARAM_DUMP_HISTORY = "dump-history";

            public static class Config {
                public final static int PROCESS_FRAME_MILLIS = 50;
                public final static int TOL_DIFF_SEMI = 1;
            }
        }

        public final static Class[] CLASSES = {
                ToneDetector.CLASS_REF
        };

        public static String[] getDetectorClassNamesByTag(String tag) {
            if (tag == null || tag.equals(""))
                return new String[0];

            ArrayList<String> findings = new ArrayList<>();
            for (Class c : CLASSES) {
                if (c.getName().endsWith(tag))
                    findings.add(c.getName());
            }

            return findings.toArray(new String[0]);
        }
    }

    public static class Logging {
        public final static String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS '(UTF+8)'";
        public final static Locale LOCALE = Locale.TAIWAN;
        public final static int MAX_NUM_ENTRIES = 1000;
        public final static int AUTO_SAVE_PERIOD_MILLIS = 30000;
    }
}
