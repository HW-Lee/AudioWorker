package com.google.audioworker.utils;

import android.Manifest;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;

import com.google.audioworker.fragments.AudioFragment;
import com.google.audioworker.fragments.AudioPlaybackFragment;
import com.google.audioworker.fragments.AudioRecordFragment;
import com.google.audioworker.fragments.AudioVoIPFragment;
import com.google.audioworker.fragments.ConnectFragment;
import com.google.audioworker.fragments.GeneralInfoFragment;
import com.google.audioworker.fragments.ShellFragment;
import com.google.audioworker.utils.Constants.Controllers.Config.AudioApi;
import com.google.audioworker.utils.Constants.Controllers.Config.PerformanceMode;
import com.google.audioworker.utils.Constants.Controllers.Config.RecordTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class Constants {
    private static final String APP_NAME_TAG = "Google-AudioWorker";

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
        return new File(
                        EnvironmentPaths.SDCARD_PATH,
                        EnvironmentPaths.PACKAGE_ROOT_FOLDER + "/" + tag)
                .getAbsolutePath();
    }

    public static String dataDirectory(String tag) {
        return new File(
                        EnvironmentPaths.DATA_PATH,
                        EnvironmentPaths.PACKAGE_ROOT_FOLDER + "/" + tag)
                .getAbsolutePath();
    }

    public static class EnvironmentPaths {
        public static final String PACKAGE_ROOT_FOLDER = APP_NAME_TAG + "-data";
        public static final String SDCARD_PATH =
                Environment.getExternalStorageDirectory().getAbsolutePath();
        public static final String DATA_PATH = Environment.getDataDirectory().getAbsolutePath();
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

        private static final FragmentInfo GENERAL_FRAGMENT_INFO =
                new FragmentInfo("GeneralFragment", "Main", GeneralInfoFragment.class);
        private static final FragmentInfo CONNECT_FRAGMENT_INFO =
                new FragmentInfo("ConnectFragment", "Comm", ConnectFragment.class);
        private static final FragmentInfo AUDIO_FRAGMENT_INFO =
                new FragmentInfo("AudioFragment", "Audio", AudioFragment.class);
        private static final FragmentInfo SHELL_FRAGMENT_INFO =
                new FragmentInfo("ShellFragment", "Shell", ShellFragment.class);

        public static final FragmentInfo[] FRAGMENT_INFOS = {
            GENERAL_FRAGMENT_INFO, CONNECT_FRAGMENT_INFO, AUDIO_FRAGMENT_INFO, SHELL_FRAGMENT_INFO
        };

        public static class Audio {
            private static final FragmentInfo PLAYBACK_FRAGMENT_INFO =
                    new FragmentInfo(
                            "AudioPlaybackFragment", "Playback", AudioPlaybackFragment.class);
            private static final FragmentInfo RECORD_FRAGMENT_INFO =
                    new FragmentInfo("AudioRecordFragment", "Record", AudioRecordFragment.class);
            private static final FragmentInfo VOIP_FRAGMENT_INFO =
                    new FragmentInfo("AudioVoIPFragment", "VoIP", AudioVoIPFragment.class);

            public static final FragmentInfo[] FRAGMENT_INFOS = {
                PLAYBACK_FRAGMENT_INFO, RECORD_FRAGMENT_INFO, VOIP_FRAGMENT_INFO
            };
        }
    }

    public static class WIFIP2PConstants {
        public static final int SERVER_PORT = 8888;
        public static final int MAX_THREAD_COUNT = 10;
        public static final int KEEP_ALIVE_TIME_SECONDS = 30;
        public static final int MSG_BUFFER_SIZE = 1024;
    }

    public static class MessageSpecification {
        public static final String COMMAND_ID = "command-id";
        public static final String COMMAND_TAG_NAME = "SN";

        public static final String COMMAND_ACK_RETURN = "return";
        public static final String COMMAND_ACK_DESC = "desc";
        public static final String COMMAND_ACK_RETURN_CODE = "code";
        public static final String COMMAND_ACK_TARGET = "target";

        public static final String COMMAND_SHELL_TARGET = "shell";
        public static final String COMMAND_BROADCAST_INTENT = "intent";
        public static final String COMMAND_BROADCAST_PARAMS = "params";
    }

    private static final String INTENT_PREFIX = "com.google.audioworker.intent";
    public static final String INTENT_OWNER_PLAYBACK = "playback";
    public static final String INTENT_OWNER_RECORD = "record";
    public static final String INTENT_OWNER_VOIP = "voip";
    public static final String INTENT_OWNER_UNKNOWN = "unknown";

    public static String getIntentOwner(Intent intent) {
        return getIntentOwner(intent.getAction());
    }

    public static String getIntentOwner(String intent) {
        if (intent == null || !intent.startsWith(INTENT_PREFIX)) return INTENT_OWNER_UNKNOWN;

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
        public static final String INTENT_PLAYBACK_START = INTENT_PREFIX + ".playback.start";
        public static final String INTENT_PLAYBACK_STOP = INTENT_PREFIX + ".playback.stop";
        public static final String INTENT_PLAYBACK_SEEK = INTENT_PREFIX + ".playback.seek";
        public static final String INTENT_PLAYBACK_INFO = INTENT_PREFIX + ".playback.info";

        public static final String INTENT_RECORD_START = INTENT_PREFIX + ".record.start";
        public static final String INTENT_RECORD_STOP = INTENT_PREFIX + ".record.stop";
        public static final String INTENT_RECORD_DETECT_REGISTER =
                INTENT_PREFIX + ".record.detect.register";
        public static final String INTENT_RECORD_DETECT_UNREGISTER =
                INTENT_PREFIX + ".record.detect.unregister";
        public static final String INTENT_RECORD_DETECT_SETPARAMS =
                INTENT_PREFIX + ".record.detect.setparams";
        public static final String INTENT_RECORD_INFO = INTENT_PREFIX + ".record.info";
        public static final String INTENT_RECORD_DUMP = INTENT_PREFIX + ".record.dump";

        public static final String INTENT_VOIP_START = INTENT_PREFIX + ".voip.start";
        public static final String INTENT_VOIP_STOP = INTENT_PREFIX + ".voip.stop";
        public static final String INTENT_VOIP_DETECT_REGISTER =
                INTENT_PREFIX + ".voip.detect.register";
        public static final String INTENT_VOIP_DETECT_UNREGISTER =
                INTENT_PREFIX + ".voip.detect.unregister";
        public static final String INTENT_VOIP_DETECT_SETPARAMS =
                INTENT_PREFIX + ".voip.detect.setparams";
        public static final String INTENT_VOIP_INFO = INTENT_PREFIX + ".voip.info";
        public static final String INTENT_VOIP_CONFIG = INTENT_PREFIX + ".voip.config";
        public static final String INTENT_VOIP_TX_DUMP = INTENT_PREFIX + ".voip.tx.dump";

        public static final String[] INTENT_NAMES = {
            INTENT_PLAYBACK_START,
            INTENT_PLAYBACK_STOP,
            INTENT_PLAYBACK_SEEK,
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
        public static final String INTENT_RECORD_EVENT = INTENT_PREFIX + ".record.event";

        public static final String INTENT_VOIP_EVENT = INTENT_PREFIX + ".voip.event";

        public static final String[] INTENT_NAMES = {INTENT_RECORD_EVENT, INTENT_VOIP_EVENT};
    }

    public static class DebugInterface {
        public static final String INTENT_RECEIVE_FUNCTION =
                INTENT_PREFIX + ".debug.receive.function";
        public static final String INTNET_SEND_FUNCTION = INTENT_PREFIX + ".debug.send.function";

        public static final String INTENT_KEY_FUNCTION_CONTENT = "json";
        public static final String INTENT_KEY_RECEIVER_ID = "receiver";

        public static final String[] INTENT_NAMES = {INTENT_RECEIVE_FUNCTION, INTNET_SEND_FUNCTION};
    }

    public static class Controllers {
        public static final String NAME_AUDIO = "Audio";
        public static final String NAME_PLAYBACK = "Playback";
        public static final String NAME_RECORD = "Record";
        public static final String NAME_VOIP = "VoIP";
        public static final String NAME_SHELL = "Shell";

        public static class Config {
            public static class Common {
                public static final int MAX_THREAD_COUNT = 10;
                public static final int KEEP_ALIVE_TIME_SECONDS = 30;
                public static final int BYTE_BUFFER_SIZE = 1024;
            }

            public static class Playback {
                public static final int TONE_FILE_DURATION_SECONDS = 60;

                public static class MP3_ENCODE {
                    public static final int MODE_CBR = 0;
                    public static final int MODE_VBR = 1;
                    public static final int MODE_ABR = 2;
                    public static final int COMPRESSION_RATIO_KHZ =
                            96; // 8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256,
                    // or 320
                    public static final int QUALITY =
                            7; // quality=0..9. 0=best (very slow). 9=worst
                    // 2 near-best quality, not too slow
                    // 5 good quality, fast
                    // 7 ok quality, really fast
                }
            }

            public static class Record {
                public static final float TIMEOUT_MULTIPLIER = 5.0f;
            }

            public static class PerformanceMode {
                public static final int NONE = 10; // AAUDIO_PERFORMANCE_MODE_NONE,
                public static final int POWERSAVING = 11; // AAUDIO_PERFORMANCE_MODE_POWER_SAVING,
                public static final int LOWLATENCY = 12; // AAUDIO_PERFORMANCE_MODE_LOW_LATENCY
            }

            public static class AudioApi {
                public static final int NONE = 0;
                public static final int OPENSLES = 1;
                public static final int AAUDIO = 2;
            }

            public static class RecordTask {
                public static final int INDEX_DEFAULT = 0;
                public static final int MAX_NUM = 10;
                public static final int TASK_ALL = -1;
            }
        }
    }

    public static class PlaybackDefaultConfig {
        public static final float AMPLITUDE = 0.6f;
        public static final int SAMPLING_FREQ = 44100;
        public static final int NUM_CHANNELS = 2;
        public static final int BIT_PER_SAMPLE = 16;
        public static final String FILE_NAME = "null";
        public static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
        public static final int USAGE = AudioAttributes.USAGE_MEDIA;
        public static final int CONTENT_TYPE = AudioAttributes.CONTENT_TYPE_MUSIC;
        public static final int PERF_MODE = -1;
    }

    public static class RecordDefaultConfig {
        public static final int SAMPLING_FREQ = 44100;
        public static final int NUM_CHANNELS = 2;
        public static final int BIT_PER_SAMPLE = 16;
        public static final int BUFFER_SIZE_MILLIS = 0;
        public static final int INPUT_SRC = MediaRecorder.AudioSource.MIC;
        public static final int AUDIO_PERF = PerformanceMode.NONE;
        public static final int AUDIO_API = AudioApi.NONE;
        public static final int INDEX = RecordTask.INDEX_DEFAULT;
    }

    public static class VoIPDefaultConfig {
        public static class Rx {
            public static final float AMPLITUDE = 0.6f;
            public static final int SAMPLING_FREQ = 8000;
            public static final int NUM_CHANNELS = 1;
            public static final int BIT_PER_SAMPLE = 16;
        }

        public static class Tx {
            public static final int SAMPLING_FREQ = 8000;
            public static final int NUM_CHANNELS = 1;
            public static final int BIT_PER_SAMPLE = 16;
            public static final int BUFFER_SIZE_MILLIS = 0;
        }
    }

    public static class Detectors {
        public static class ToneDetector {
            private static final Class<?> CLASS_REF =
                    com.google.audioworker.functions.audio.record.detectors.ToneDetector.class;

            public static final String PARAM_FS = "sampling-freq";
            public static final String PARAM_PROCESS_FRAME_MILLIS = "process-frame-ms";
            public static final String PARAM_TOL_DIFF_SEMI = "tolerance-semitone";
            public static final String PARAM_TARGET_FREQ = "target-freq";
            public static final String PARAM_CLEAR_TARGETS = "clear-target";
            public static final String PARAM_DUMP_HISTORY = "dump-history";

            public static class Config {
                public static final int PROCESS_FRAME_MILLIS = 50;
                public static final int TOL_DIFF_SEMI = 1;
            }
        }

        public static final Class[] CLASSES = {ToneDetector.CLASS_REF};

        public static String[] getDetectorClassNamesByTag(String tag) {
            if (tag == null || tag.equals("")) return new String[0];

            ArrayList<String> findings = new ArrayList<>();
            for (Class c : CLASSES) {
                if (c.getName().endsWith(tag)) findings.add(c.getName());
            }

            return findings.toArray(new String[0]);
        }
    }

    public static class Logging {
        public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS '(UTF+8)'";
        public static final Locale LOCALE = Locale.TAIWAN;
        public static final int MAX_NUM_ENTRIES = 1000;
        public static final int AUTO_SAVE_PERIOD_MILLIS = 30000;
    }
}
