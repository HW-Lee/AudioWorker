#ifndef RING_BUFFER
#define RING_BUFFER

#include "wav_header.h"
namespace audio_record {
class RingBuffer {
 public:
  RingBuffer(int SizePerSample, int channel)
      : mSizePerSample(SizePerSample), mChannel(channel), mAvail(true) {}

  ~RingBuffer() { delete[] mData; }

  int init(int size);
  int push(void* buffer, int frames);
  int pop(char* dest, int size);
  int SaveWav(const char* filename, wav_header WavHDR);

 private:
  bool mAvail = false;
  int mHead = 0;
  int mLen = 0;
  int mSizePerSample = 0;
  int mChannel = 0;
  char* mData = nullptr;
};
}  // namespace audio_record

#endif /* RING_BUFFER */
