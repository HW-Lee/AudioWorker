#include <aaudio/AAudio.h>

#include "buffer.h"

aaudio_data_callback_result_t RecordCallback(AAudioStream* stream,
                                             void* userData, void* audioData,
                                             int32_t numFrames);

namespace audio_record {
class AAudioRecord {
public:
    int OpenInput(FORMAT_T format, int ch, int sr, int source, int perf, int bufferSize);
    int StartRecord(void );
    int StopRecord(void );
    void ReleaseRecord(void );

    int SaveFile(const char* filename);
    audio_record::RingBuffer* GetRingBuffer(){return mBuffer;}

private:
    AAudioStream* mRecordingStream = nullptr;
    audio_record::RingBuffer* mBuffer = nullptr;
    wav_header mWavHDR = {};
};
}  // namespace audio_record
