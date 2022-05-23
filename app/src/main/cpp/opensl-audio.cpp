#include "opensl-audio.h"

#include <android/log.h>

#define TAG "AudioWorkerOpenSLES"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static void SLCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
  (reinterpret_cast<audio_record::OpenSLRecord *>(context))->BufferCallback(bq);
}

void audio_record::OpenSLRecord::BufferCallback(
    SLAndroidSimpleBufferQueueItf bq) {
  if (mSLBuffer != bq) {
    LOGE("BufferCallback check bq fail");
    return;
  }

  // Enqueue buffer then OPENSL recorder will trigger callback again
  (*mSLBuffer)->Enqueue(mSLBuffer, mData, mDataSize);
  // Save mData from OPENSL to Ring buffer
  if (mBuffer->push(mData, mFrameCount)) LOGE("Recording push buffer error");
}

int audio_record::OpenSLRecord::OpenInput(FORMAT_T format, int ch, int sr,
                                          int source, int perf,
                                          int bufferSize) {
  SLresult result = SL_RESULT_SUCCESS;

  result = slCreateEngine(&mEngineObject, 0, NULL, 0, NULL, NULL);
  if (SL_RESULT_SUCCESS != result) {
    LOGE("slCreateEngine error %d", result);
    return -1;
  }

  result = (*mEngineObject)->Realize(mEngineObject, SL_BOOLEAN_FALSE);
  if (SL_RESULT_SUCCESS != result) {
    LOGE("Realize error %d", result);
    return -1;
  }

  result = (*mEngineObject)
               ->GetInterface(mEngineObject, SL_IID_ENGINE, &mEngineInterface);
  if (SL_RESULT_SUCCESS != result) {
    LOGE("GetInterface error %d", result);
    return -1;
  }

  SLuint32 bitsPerSample = static_cast<SLuint32>(bytePerSample(format) * 8);

  // configure audio sink
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
      SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,  // locatorType
      static_cast<SLuint32>(2)};                // numBuffers

  // Define the audio data format.
  SLAndroidDataFormat_PCM_EX format_pcm = {
      SL_ANDROID_DATAFORMAT_PCM_EX,      // formatType
      static_cast<SLuint32>(ch),         // num of channels
      static_cast<SLuint32>(sr * 1000),  // milliSamplesPerSec
      bitsPerSample,                     // bitsPerSample
      bitsPerSample,                     // containerSize;
      chMask(ch),                        // channelMask
      SL_BYTEORDER_LITTLEENDIAN,         // fix little endian
      convertOpenSLFormat(format),       // represent format
  };

  SLDataSink audioSink = {&loc_bufq, &format_pcm};
  SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE,
                                    SL_IODEVICE_AUDIOINPUT,
                                    SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
  SLDataSource audioSrc = {&loc_dev, NULL};
  const SLInterfaceID ids[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                               SL_IID_ANDROIDCONFIGURATION};
  const SLboolean reqs[] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

  result = (*mEngineInterface)
               ->CreateAudioRecorder(mEngineInterface, &mEngineObject,
                                     &audioSrc, &audioSink,
                                     sizeof(ids) / sizeof(ids[0]), ids, reqs);
  // Configure the stream.
  SLAndroidConfigurationItf configItf = nullptr;

  (*mEngineObject)
      ->GetInterface(mEngineObject, SL_IID_ANDROIDCONFIGURATION, &configItf);

  // Configure mic source.
  SLuint32 value = convertOpenSLSource((Source)source);
  (*configItf)
      ->SetConfiguration(configItf, SL_ANDROID_KEY_RECORDING_PRESET, &value,
                         sizeof(SLuint32));

  if (SL_RESULT_SUCCESS != result &&
      value != SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION) {
    value = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
    (*configItf)
        ->SetConfiguration(configItf, SL_ANDROID_KEY_RECORDING_PRESET, &value,
                           sizeof(SLuint32));
    LOGD("Setting Source %d failed. Using VOICE_RECOG.", source);
  }

  // Configure performance mode
  value = convertOpenSLPerf((Perf)perf);
  (*configItf)
      ->SetConfiguration(configItf, SL_ANDROID_KEY_PERFORMANCE_MODE, &value,
                         sizeof(SLuint32));

  (*mEngineObject)->Realize(mEngineObject, SL_BOOLEAN_FALSE);
  (*mEngineObject)->GetInterface(mEngineObject, SL_IID_RECORD, &mRecInterface);

  (*mEngineObject)
      ->GetInterface(mEngineObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                     &mSLBuffer);
  result = (*mSLBuffer)->RegisterCallback(mSLBuffer, SLCallback, this);
  if (SL_RESULT_SUCCESS != result)
    LOGE("%s: RegisterCallback error %d", __func__, result);

  mBuffer = new audio_record::RingBuffer(bytePerSample(format), ch);
  if (mBuffer->init(bufferSize)) {
    LOGE("Buffer init fail!");
    return -1;
  }

  mDataSize = (bytePerSample(format) * ch) *
              mFrameCount;  // read for mFrameCount per callback
  mData = new char[mDataSize];

  InitHeaderWAV(&mWavHDR, sr, ch, bytePerSample(format), bufferSize,
                WaveFormat(format));
  LOGD("%s: channel %d, sample rate %d", __func__, ch, sr);
  return 0;
}

int audio_record::OpenSLRecord::StartRecord(void) {
  if (mRecInterface != nullptr) {
    SLresult result =
        (*mRecInterface)
            ->SetRecordState(mRecInterface, SL_RECORDSTATE_RECORDING);
    if (SL_RESULT_SUCCESS != result) {
      LOGE("%s: error %d", __func__, result);
      return -1;
    }
    BufferCallback(mSLBuffer);
  }
  return 0;
}

int audio_record::OpenSLRecord::StopRecord(void) {
  if (mRecInterface != nullptr) {
    SLresult result =
        (*mRecInterface)->SetRecordState(mRecInterface, SL_RECORDSTATE_STOPPED);
    if (SL_RESULT_SUCCESS != result) {
      LOGE("%s: error %d", __func__, result);
      return -1;
    }
  }
  LOGD("%s: Stop ", __func__);
  return 0;
}

void audio_record::OpenSLRecord::ReleaseRecord(void) {
  if (mEngineObject != NULL) {
    (*mEngineObject)->Destroy(mEngineObject);
    mEngineObject = nullptr;
    mEngineInterface = nullptr;
    mRecInterface = nullptr;
    mSLBuffer = nullptr;
  }
  if (mBuffer != nullptr) {
    delete mBuffer;
    mBuffer = nullptr;
  }
  if (mData != nullptr) {
    delete mData;
    mData = nullptr;
  }
}

int audio_record::OpenSLRecord::SaveFile(const char *filename) {
  return mBuffer->SaveWav(filename, mWavHDR);
}
