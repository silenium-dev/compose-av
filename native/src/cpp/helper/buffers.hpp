//
// Created by silenium-dev on 8/10/24.
//

#ifndef BUFFERS_HPP
#define BUFFERS_HPP
#include <jni.h>
extern "C" {
#include <libavutil/buffer.h>
}

AVBufferRef *bufferFromJava(JNIEnv *env, jobject buffer);
jobject bufferToJava(JNIEnv *env, AVBufferRef *buffer);

#endif //BUFFERS_HPP
