LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := vulnerablemedia
LOCAL_SRC_FILES := vulnerablemedia.c
include $(BUILD_SHARED_LIBRARY)