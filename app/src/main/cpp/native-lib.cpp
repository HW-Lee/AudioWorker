#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#define TAG "AudioWorkerJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#include "mmap-audio.h"
#include "opensl-audio.h"

static audio_record::AAudioRecord *mAAudioRecord =
    new audio_record::AAudioRecord();
static audio_record::OpenSLRecord *mOpenSLRecord =
    new audio_record::OpenSLRecord();

/*********************************************************************************/
/**********************  JNI  Prototypes *****************************************/
/*********************************************************************************/
extern "C" {

JNIEXPORT void JNICALL
Java_com_google_audioworker_functions_controllers_RecordController_openInput(
    JNIEnv *env, jobject instance, jint format, jint channel, jint sample_rate,
    jint input_source, jint perf, jint dumpBufferSize, jint api) {
  switch (api) {
    case AAUDIO:
      mAAudioRecord->OpenInput((FORMAT_T)format, channel, sample_rate,
                               input_source, perf, dumpBufferSize);
      break;
    case OPENSLES:
      mOpenSLRecord->OpenInput((FORMAT_T)format, channel, sample_rate,
                               input_source, perf, dumpBufferSize);
      break;
    default:
      LOGE("Unsupported API %d", api);
  }
}

JNIEXPORT void JNICALL
Java_com_google_audioworker_functions_controllers_RecordController_startRecording(
    JNIEnv *env, jobject instance, jint api) {
  switch (api) {
    case AAUDIO:
      mAAudioRecord->StartRecord();
      break;
    case OPENSLES:
      mOpenSLRecord->StartRecord();
      break;
    default:
      LOGE("Unsupported API %d", api);
  }
}

JNIEXPORT void JNICALL
Java_com_google_audioworker_functions_controllers_RecordController_stopRecording(
    JNIEnv *env, jobject instance, jint api) {
  switch (api) {
    case AAUDIO:
      mAAudioRecord->StopRecord();
      break;
    case OPENSLES:
      mOpenSLRecord->StopRecord();
      break;
    default:
      LOGE("Unsupported API %d", api);
  }
}

JNIEXPORT void JNICALL
Java_com_google_audioworker_functions_controllers_RecordController_releaseRecording(
    JNIEnv *env, jobject instance, jint api) {
  switch (api) {
    case AAUDIO:
      mAAudioRecord->ReleaseRecord();
      break;
    case OPENSLES:
      mOpenSLRecord->ReleaseRecord();
      break;
    default:
      LOGE("Unsupported API %d", api);
  }
}

JNIEXPORT void JNICALL
Java_com_google_audioworker_functions_controllers_RecordController_saveWav(
    JNIEnv *env, jobject instance, jstring filename, jint api) {
  const char *str = env->GetStringUTFChars(filename, 0);
  LOGD("Dump Save to %s", str);
  switch (api) {
    case AAUDIO:
      mAAudioRecord->SaveFile(str);
      break;
    case OPENSLES:
      mOpenSLRecord->SaveFile(str);
      break;
    default:
      LOGE("Unsupported API %d", api);
  }
  env->ReleaseStringUTFChars(filename, str);
}
}
