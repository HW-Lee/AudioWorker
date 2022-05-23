#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "buffer.h"

namespace audio_record {
class OpenSLRecord {
 public:
  int OpenInput(FORMAT_T format, int ch, int sr, int source, int perf,
                int bufferSize);
  int StartRecord(void);
  int StopRecord(void);
  void ReleaseRecord(void);

  int SaveFile(const char* filename);
  void BufferCallback(SLAndroidSimpleBufferQueueItf bq);

 private:
  SLObjectItf mEngineObject = nullptr;
  SLEngineItf mEngineInterface = nullptr;
  SLRecordItf mRecInterface = nullptr;
  SLAndroidSimpleBufferQueueItf mSLBuffer = nullptr;
  audio_record::RingBuffer* mBuffer = nullptr;
  wav_header mWavHDR = {};
  char* mData = nullptr;
  int mDataSize = 0;
  int mFrameCount = 10;
};
}  // namespace audio_record
