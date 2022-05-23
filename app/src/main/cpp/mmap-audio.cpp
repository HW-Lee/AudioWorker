#include "mmap-audio.h"

#include <android/log.h>

#define TAG "AudioWorkerAAudio"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

aaudio_data_callback_result_t RecordCallback(AAudioStream *stream,
                                             void *userData, void *audioData,
                                             int32_t numFrames) {
  if (((audio_record::AAudioRecord *)userData)
          ->GetRingBuffer()
          ->push(audioData, numFrames))
    LOGE("Recording push buffer error");

  return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

int audio_record::AAudioRecord::OpenInput(FORMAT_T format, int ch, int sr,
                                          int source, int perf,
                                          int bufferSize) {
  if (mRecordingStream != nullptr) {
    LOGE("Recording stream exist!");
  }

  AAudioStreamBuilder *inputStreamBuilder;
  AAudio_createStreamBuilder(&inputStreamBuilder);

  AAudioStreamBuilder_setDirection(inputStreamBuilder, AAUDIO_DIRECTION_INPUT);
  AAudioStreamBuilder_setFormat(inputStreamBuilder,
                                convertAAudioFormat(format));
  AAudioStreamBuilder_setChannelCount(inputStreamBuilder, ch);
  AAudioStreamBuilder_setSampleRate(inputStreamBuilder, sr);
  AAudioStreamBuilder_setPerformanceMode(inputStreamBuilder, perf);
  AAudioStreamBuilder_setInputPreset(inputStreamBuilder, source);
  AAudioStreamBuilder_setSharingMode(inputStreamBuilder,
                                     AAUDIO_SHARING_MODE_EXCLUSIVE);
  AAudioStreamBuilder_setDataCallback(inputStreamBuilder, RecordCallback, this);

  // TODO: ErrorCallback
  // AAudioStreamBuilder_setErrorCallback(inputStreamBuilder,
  // AAudioErrorCallback, NULL);

  if (((AAudioStreamBuilder_openStream(inputStreamBuilder,
                                        &mRecordingStream) != AAUDIO_OK) &&
        (mRecordingStream != nullptr))) {
    AAudioStreamBuilder_delete(inputStreamBuilder);
    LOGE("AAduio Recording open fail");
    return -1;
  }

  AAudioStreamBuilder_delete(inputStreamBuilder);
  mBuffer = new audio_record::RingBuffer(bytePerSample(format), ch);
  if (mBuffer->init(bufferSize)) {
    LOGE("Buffer init fail!");
    return -1;
  }

  InitHeaderWAV(&mWavHDR, sr, ch, bytePerSample(format), bufferSize,
                WaveFormat(format));
  LOGD("%s: channel %d, sample rate %d", __func__, ch, sr);
  return 0;
}

int audio_record::AAudioRecord::StartRecord(void) {
  if (mRecordingStream != nullptr) {
    if (AAudioStream_requestStart(mRecordingStream) != AAUDIO_OK) {
      LOGE("AAduio Input starting fail");
      return -1;
    }
  } else {
    LOGE("AAduio Input invalid");
    return -1;
  }
  return 0;
}

int audio_record::AAudioRecord::StopRecord(void) {
  if (mRecordingStream != nullptr) {
    if (AAudioStream_requestStop(mRecordingStream) != AAUDIO_OK) {
      LOGE("AAduio Input Stop fail");
      return -1;
    }
  }
  LOGD("%s: Stop ", __func__);
  return 0;
}

void audio_record::AAudioRecord::ReleaseRecord(void) {
  if (mRecordingStream != nullptr) {
    AAudioStream_close(mRecordingStream);
    mRecordingStream = nullptr;
  }
  if (mBuffer != nullptr) {
    delete mBuffer;
    mBuffer = nullptr;
  }
}

int audio_record::AAudioRecord::SaveFile(const char *filename) {
  return mBuffer->SaveWav(filename, mWavHDR);
}
