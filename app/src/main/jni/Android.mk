LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := myJni
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := myJni.cpp

include $(BUILD_SHARED_LIBRARY)