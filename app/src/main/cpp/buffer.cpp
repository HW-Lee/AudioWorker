#include "buffer.h"

#include <fstream>
#include <iostream>
int audio_record::RingBuffer::push(void* buffer, int frames) {
  if (!mAvail) return 0;

  int size_in_byte = frames * mChannel * mSizePerSample;
  int avail_size = mLen - mHead;

  if (avail_size >= size_in_byte) {
    memcpy(mData + mHead, buffer, size_in_byte);
    mHead += size_in_byte;
  } else {
    memcpy(mData + mHead, buffer, avail_size);
    memcpy(mData, (char*)buffer + avail_size, size_in_byte - avail_size);
    mHead = size_in_byte - avail_size;
  }

  return 0;
}

int audio_record::RingBuffer::pop(char* dest, int size_in_byte) {
  int avail_size = mLen - mHead;

  if (avail_size >= size_in_byte) {
    memcpy(dest, mData + mHead, size_in_byte);
    mHead += size_in_byte;
  } else {
    memcpy(dest, mData + mHead, avail_size);
    memcpy(dest + avail_size, mData, size_in_byte - avail_size);
    mHead = size_in_byte - avail_size;
  }
  return 0;
}

int audio_record::RingBuffer::init(int size) {
  if (size > 0) {
    mLen = size;
    mData = new char[mLen];
    if (mData) return 0;
  }

  return -1;
}

int audio_record::RingBuffer::SaveWav(const char* filename, wav_header WavHDR) {
  auto Dumpfile = std::ofstream(filename, std::ios::out | std::ios::binary);
  char value;

  // block mBuffer updating
  mAvail = false;

  Dumpfile.write((char*)&WavHDR, sizeof(wav_header));
  for (int i = 0; i < mLen; i++) {
    pop(&value, 1);
    Dumpfile.write(&value, 1);
  }
  Dumpfile.close();

  // unblock mBuffer updating
  mAvail = true;
  return 0;
}
