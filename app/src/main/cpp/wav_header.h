#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <aaudio/AAudio.h>

#include <cstring>

enum WavFormat_t { WAVE_FORMAT_PCM = 0x1, WAVE_FORMAT_IEEE_FLOAT = 0x3 };

enum { OPENSLES = 1, AAUDIO = 2 };

/* From Java AudioFormat */
enum FORMAT_T {
  ENCODING_PCM_16BIT = 2,
  ENCODING_PCM_8BIT = 3,
  ENCODING_PCM_FLOAT = 4,
  ENCODING_PCM_32BIT = 22
};

struct wav_header {
  char chunkid[4];
  uint32_t chunkSize;
  char format[4];
  char subchunkid[4];
  uint32_t subchunksize;
  uint16_t audioFormat;
  uint16_t numChannels;
  uint32_t sampleRate;
  uint32_t byteRate;
  uint16_t blockAlign;
  uint16_t bitsPerSample;
  char subchunk2id[4];
  uint32_t subchunk2size;
} __attribute__((packed));

static void InitHeaderWAV(wav_header* hdr, int sample_rate, int num_chan,
                          int bytes_per_sample, uint64_t total_size,
                          WavFormat_t wav_format) {
  strncpy(hdr->chunkid, "RIFF", 4);
  hdr->chunkSize = sizeof(wav_header) - 8 + total_size;
  strncpy(hdr->format, "WAVE", 4);
  strncpy(hdr->subchunkid, "fmt ", 4);
  hdr->subchunksize = 16;
  hdr->audioFormat = wav_format;
  hdr->numChannels = num_chan;
  hdr->sampleRate = sample_rate;
  hdr->byteRate = sample_rate * num_chan * bytes_per_sample;
  hdr->blockAlign = num_chan * bytes_per_sample;
  hdr->bitsPerSample = bytes_per_sample * 8;
  strncpy(hdr->subchunk2id, "data", 4);
  hdr->subchunk2size = total_size;
}

static SLuint32 chMask(int ch) {
  switch (ch) {
    case 1:
      return SL_SPEAKER_FRONT_LEFT;
    case 2:
      return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    default:
      return (SLuint32)((1 << ch) - 1);
  }
}

static int bytePerSample(FORMAT_T format) {
  switch (format) {
    case ENCODING_PCM_8BIT:
    case ENCODING_PCM_16BIT:
      return 2;
    case ENCODING_PCM_32BIT:
    case ENCODING_PCM_FLOAT:
      return 4;
    default:
      return 2;
  }
}

static WavFormat_t WaveFormat(FORMAT_T format) {
  return (format != ENCODING_PCM_FLOAT ? WAVE_FORMAT_PCM
                                       : WAVE_FORMAT_IEEE_FLOAT);
}

static int convertAAudioFormat(FORMAT_T format) {
  switch (format) {
    case ENCODING_PCM_8BIT:
    case ENCODING_PCM_16BIT:
      return AAUDIO_FORMAT_PCM_I16;
    case ENCODING_PCM_32BIT:
      return AAUDIO_FORMAT_PCM_I32;
    case ENCODING_PCM_FLOAT:
      return AAUDIO_FORMAT_PCM_FLOAT;
    default:
      return AAUDIO_FORMAT_PCM_I16;
  }
}

static SLuint32 convertOpenSLFormat(FORMAT_T format) {
  switch (format) {
    case ENCODING_PCM_16BIT:
    case ENCODING_PCM_32BIT:
      return SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT;
    case ENCODING_PCM_FLOAT:
      return SL_ANDROID_PCM_REPRESENTATION_FLOAT;
    default:
      return 0;
  }
}

enum Source {
  Generic = 1,             // AAUDIO_INPUT_PRESET_GENERIC
  Camcorder = 5,           // AAUDIO_INPUT_PRESET_CAMCORDER
  VoiceRecognition = 6,    // AAUDIO_INPUT_PRESET_VOICE_RECOGNITION
  VoiceCommunication = 7,  // AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION
  Unprocessed = 9,         // AAUDIO_INPUT_PRESET_UNPROCESSED
  VoicePerformance = 10,   // AAUDIO_INPUT_PRESET_VOICE_PERFORMANCE
};

enum Perf {
  None = 10,         // AAUDIO_PERFORMANCE_MODE_NONE,
  PowerSaving = 11,  // AAUDIO_PERFORMANCE_MODE_POWER_SAVING,
  LowLatency = 12,   // AAUDIO_PERFORMANCE_MODE_LOW_LATENCY
};

static SLuint32 convertOpenSLSource(Source source) {
  SLuint32 preset = SL_ANDROID_RECORDING_PRESET_NONE;
  switch (source) {
    case Source::Generic:
      preset = SL_ANDROID_RECORDING_PRESET_GENERIC;
      break;
    case Source::Camcorder:
      preset = SL_ANDROID_RECORDING_PRESET_CAMCORDER;
      break;
    case Source::VoiceRecognition:
    case Source::VoicePerformance:
      preset = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
      break;
    case Source::VoiceCommunication:
      preset = SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION;
      break;
    case Source::Unprocessed:
      preset = SL_ANDROID_RECORDING_PRESET_UNPROCESSED;
      break;
    default:
      break;
  }
  return preset;
}

static SLuint32 convertOpenSLPerf(Perf perf) {
  SLuint32 openslperf = SL_ANDROID_PERFORMANCE_NONE;
  switch (perf) {
    case Perf::None:
      openslperf = SL_ANDROID_PERFORMANCE_NONE;
      break;
    case Perf::LowLatency:
      openslperf = SL_ANDROID_PERFORMANCE_LATENCY;
      break;
    case Perf::PowerSaving:
      openslperf = SL_ANDROID_PERFORMANCE_POWER_SAVING;
      break;
    default:
      break;
  }
  return openslperf;
}
