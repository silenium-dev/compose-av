#ifndef NATIVES_MPVCALLBACK_HPP
#define NATIVES_MPVCALLBACK_HPP

#include <jni.h>
#include <string>


class MpvCallback {
public:
    virtual ~MpvCallback() = default;

    virtual void onPropertyChanged(const std::string &name, jobject result) = 0;

    virtual void onPropertyGet(long subscriptionId, jobject result) = 0;

    virtual void onPropertySet(long subscriptionId, jobject result) = 0;

    virtual void onCommandReply(long subscriptionId, jobject result) = 0;
};


#endif //NATIVES_MPVCALLBACK_HPP
