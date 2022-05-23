LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := native-fft
LOCAL_SRC_FILES := native-fft.cpp FFT.cpp

LOCAL_LDLIBS    += -llog -ldl

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE    := native-peak-detector
LOCAL_SRC_FILES := native-peak-detector.cpp Matrix.cpp PeakDetector.cpp

LOCAL_LDLIBS    += -llog -ldl

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := native-lib
LOCAL_SRC_FILES := native-lib.cpp mmap-audio.cpp buffer.cpp opensl-audio.cpp

LOCAL_LDLIBS    += -llog -ldl -laaudio -lOpenSLES

include $(BUILD_SHARED_LIBRARY)
