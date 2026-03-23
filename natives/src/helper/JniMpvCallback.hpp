#ifndef NATIVES_JNIMPVCALLBACK_HPP
#define NATIVES_JNIMPVCALLBACK_HPP

#include "JniCallRef.hpp"
#include "MpvCallback.hpp"

#include <jni.h>
#include <string>


class JniMpvCallback : public MpvCallback {
public:
    explicit JniMpvCallback(JNIEnv *env, jobject thiz);

    void onPropertyChanged(const std::string &name, jobject result) override;

    void onPropertyGet(long subscriptionId, jobject result) override;

    void onPropertySet(long subscriptionId, jobject result) override;

    void onCommandReply(long subscriptionId, jobject result) override;

private:
    JniCallRef<void, jstring, jobject> propertyChanged;
    JniCallRef<void, jlong, jobject> propertyGet;
    JniCallRef<void, jlong, jobject> propertySet;
    JniCallRef<void, jlong, jobject> commandReply;
};


#endif //NATIVES_JNIMPVCALLBACK_HPP
