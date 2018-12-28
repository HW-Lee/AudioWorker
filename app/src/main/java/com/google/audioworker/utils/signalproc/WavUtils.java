package com.google.audioworker.utils.signalproc;

import com.google.audioworker.utils.Constants;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WavUtils {
    public static class WavConfig {
        private int samplingRate;
        private int numChannels;
        private int bitPerSample;
        private int dataLength;

        public WavConfig(int freq, int nch, int bps, int length) {
            samplingRate = freq;
            numChannels = nch;
            bitPerSample = bps;
            dataLength = length;
        }

        public static class Builder {
            private int samplingRate = Constants.PlaybackDefaultConfig.SAMPLINGFREQ;
            private int numChannels = Constants.PlaybackDefaultConfig.NUMCHANNELS;
            private int bitPerSample = Constants.PlaybackDefaultConfig.BITPERSAMPLE;
            private int durationMillis = Constants.Controllers.Config.Playback.TONE_FILE_DURATION_SECONDS;

            public Builder withSamplingFrequency(int freq) {
                this.samplingRate = freq;
                return this;
            }

            public Builder withNumChannels(int nch) {
                this.numChannels = nch;
                return this;
            }

            public Builder withBitPerSample(int bps) {
                this.bitPerSample = bps;
                return this;
            }

            public Builder withDurationMillis(int millis) {
                this.durationMillis = millis;
                return this;
            }

            public WavConfig build() {
                int length = (int) (samplingRate * numChannels * bitPerSample/8 * durationMillis/1000.0);
                return new WavConfig(samplingRate, numChannels, bitPerSample, length);
            }
        }
    }

    static public DataOutputStream obtainWavFile(WavConfig config, final String filePath) throws IOException {
        File waveFile = new File(filePath);

        DataOutputStream output = new DataOutputStream(new FileOutputStream(waveFile));
        // WAVE header
        // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
        writeString(output, "RIFF"); // chunk id
        writeInt(output, 36 + config.dataLength); // chunk size
        writeString(output, "WAVE"); // format
        writeString(output, "fmt "); // subchunk 1 id
        writeInt(output, 16); // subchunk 1 size
        writeShort(output, (short) 1); // audio format (1 = PCM)
        writeShort(output, (short) config.numChannels); // number of channels
        writeInt(output, config.samplingRate); // sample rate
        writeInt(output, config.samplingRate * config.bitPerSample/8 * config.numChannels); // byte rate: SampleRate * NumChannels * BitsPerSample/8
        writeShort(output, (short) (config.bitPerSample/8 * config.numChannels)); // block align: NumChannels * BitsPerSample/8
        writeShort(output, (short) config.bitPerSample); // bits per sample
        writeString(output, "data"); // subchunk 2 id
        writeInt(output, config.dataLength); // subchunk 2 size

        return output;
    }

    static public File rawToWave(byte[] rawData, WavConfig config, final String filePath) throws IOException {

        File waveFile = new File(filePath);

        DataOutputStream output = new DataOutputStream(new FileOutputStream(waveFile));
        // WAVE header
        // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
        writeString(output, "RIFF"); // chunk id
        writeInt(output, 36 + rawData.length); // chunk size
        writeString(output, "WAVE"); // format
        writeString(output, "fmt "); // subchunk 1 id
        writeInt(output, 16); // subchunk 1 size
        writeShort(output, (short) 1); // audio format (1 = PCM)
        writeShort(output, (short) config.numChannels); // number of channels
        writeInt(output, config.samplingRate); // sample rate
        writeInt(output, config.samplingRate * config.bitPerSample/8 * config.numChannels); // byte rate: SampleRate * NumChannels * BitsPerSample/8
        writeShort(output, (short) (config.bitPerSample/8 * config.numChannels)); // block align: NumChannels * BitsPerSample/8
        writeShort(output, (short) config.bitPerSample); // bits per sample
        writeString(output, "data"); // subchunk 2 id
        writeInt(output, rawData.length); // subchunk 2 size
        output.write(rawData);
        output.close();

        return waveFile;

    }

    static private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    static private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value);
        output.write(value >> 8);
    }

    static private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}
