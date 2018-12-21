LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := native-fft
LOCAL_SRC_FILES := native-fft.cpp FFT.cpp

LOCAL_LDLIBS    += -llog -ldl

include $(BUILD_SHARED_LIBRARY)