LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := q50probe
LOCAL_SRC_FILES := q50probe.c
LOCAL_CFLAGS := -std=c99 -Wall -Wextra -Werror
include $(BUILD_SHARED_LIBRARY)
