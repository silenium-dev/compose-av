//
// Created by silenium-dev on 8/10/24.
//

#include "buffers.hpp"

AVBufferRef *bufferFromJava(JNIEnv *env, jobject buffer) {
    const auto nativePointerField = env->GetFieldID(env->GetObjectClass(buffer), "nativePointer", "Ldev/silenium/multimedia/core/data/NativePointer;");
    if (nativePointerField == nullptr) {
        return nullptr;
    }
    const auto nativePointer = env->GetObjectField(buffer, nativePointerField);
    if (nativePointer == nullptr) {
        return nullptr;
    }
    const auto nativePointerClass = env->GetObjectClass(nativePointer);
    if (nativePointerClass == nullptr) {
        return nullptr;
    }
    const auto pointerField = env->GetFieldID(nativePointerClass, "address", "J");
    if (pointerField == nullptr) {
        return nullptr;
    }
    const auto pointer = env->GetLongField(nativePointer, pointerField);
    return reinterpret_cast<AVBufferRef *>(pointer);
}

jobject bufferToJava(JNIEnv *env, AVBufferRef *buffer) {
    const auto clazz = env->FindClass("dev/silenium/multimedia/core/data/AVBufferRef");
    if (clazz == nullptr) {
        return nullptr;
    }
    const auto constructor = env->GetMethodID(clazz, "<init>", "(J)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    return env->NewObject(clazz, constructor, reinterpret_cast<jlong>(buffer));
}
