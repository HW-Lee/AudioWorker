package com.google.audioworker.utils.signalproc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.audioworker.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioConverter {
    private final static String TAG = Constants.packageTag("AudioConverter");

    private Context mContext;
    private String mSource;
    private String mDestination;

    public interface LoadListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface ConvertListener {
        void onSuccess(String msg);
        void onFailure(Exception e);
    }

    public static class Config {
        private Integer bitrate;
        private Integer samplerate;
        private Integer quality;

        private Config(Integer br, Integer fs, Integer q) {
            bitrate = br;
            samplerate = fs;
            quality = q;
        }

        public static class Builder {
            private Integer bitrate;
            private Integer samplerate;
            private Integer quality;

            public Builder withBitrate(int bitrate) {
                this.bitrate = bitrate;
                return this;
            }

            public Builder withSamplingRate(int fs) {
                this.samplerate = fs;
                return this;
            }

            public Builder withQuality(int q) {
                this.quality = q;
                return this;
            }

            public Config build() {
                return new Config(bitrate, samplerate, quality);
            }
        }
    }

    public static class Builder {
        private Context context;
        private String source;
        private String destination;

        public Builder with(@NonNull Context ctx) {
            context = ctx;
            return this;
        }

        public Builder withSource(@NonNull String src) {
            source = src;
            return this;
        }

        public Builder convertTo(@NonNull String dst) {
            destination = dst;
            return this;
        }

        public AudioConverter build() {
            return new AudioConverter(context, source, destination);
        }
    }

    private AudioConverter(Context ctx, String src, String dst) {
        mContext = ctx;
        mSource = src;
        mDestination = dst;
    }

    public boolean load(final LoadListener l, final boolean blocking) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(true);
        try {
            FFmpeg.getInstance(mContext).loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.w(TAG, "load failed");
                    success.set(false);
                    done.set(true);
                    if (blocking) {
                        synchronized (AudioConverter.this) {
                            AudioConverter.this.notify();
                        }
                    } else if (l != null) {
                        l.onFailure(new Exception("Failed to loaded FFmpeg lib"));
                    }
                }

                @Override
                public void onSuccess() {
                    done.set(true);
                    success.set(true);
                    if (blocking) {
                        synchronized (AudioConverter.this) {
                            AudioConverter.this.notify();
                        }
                    } else if (l != null) {
                        l.onSuccess();
                    }
                }

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
            if (blocking)
                return false;
            if (l != null)
                l.onFailure(e);
        }

        while (blocking && !done.get()) {
            success.set(false);
            try {
                synchronized (AudioConverter.this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return success.get();
    }

    public boolean convert(final ConvertListener l, final boolean blocking, Config config) {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(mSource);

        cmd.add("-map");
        cmd.add("0:a:0");

        cmd.add("-codec:a");
        cmd.add("libmp3lame");

        if (config != null) {
            if (config.bitrate != null) {
                cmd.add("-b:a");
                cmd.add(config.bitrate + "k");
            }
            if (config.samplerate != null) {
                cmd.add("-ar");
                cmd.add(config.samplerate.toString());
            }
            if (config.quality != null) {
                cmd.add("-q:a");
                cmd.add(config.quality.toString());
            }
        }

        cmd.add(mDestination);

        Log.d(TAG, "cmd: " + TextUtils.join(" ", cmd));

        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(true);

        try {
            FFmpeg.getInstance(mContext).execute(cmd.toArray(new String[0]), new FFmpegExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    done.set(true);
                    success.set(true);
                    if (blocking) {
                        synchronized (AudioConverter.this) {
                            AudioConverter.this.notify();
                        }
                    } else if (l != null) {
                        l.onSuccess(message);
                    }
                }

                @Override
                public void onProgress(String message) {
                }

                @Override
                public void onFailure(String message) {
                    Log.d(TAG, "onFailure(" + message + ")");
                    done.set(true);
                    success.set(false);
                    if (blocking) {
                        synchronized (AudioConverter.this) {
                            AudioConverter.this.notify();
                        }
                    } else if (l != null) {
                        l.onFailure(new IOException(message));
                    }
                }

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
            if (blocking)
                return false;
            if (l != null)
                l.onFailure(e);
        }

        while (blocking && !done.get()) {
            success.set(false);
            try {
                synchronized (AudioConverter.this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return success.get();
    }
}
