#include "JniMpvCallback.hpp"

/*
private interface MPVListener {
    fun onPropertyChanged(name: String, value: Any?)
    fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
    fun onCommandReply(subscriptionId: Long, result: Result<Unit>)
}
*/
template<typename R, typename... Args>
JniCallRef<R, Args...> refTo(JNIEnv *env, const jobject obj, const std::string &method, const std::string &signature) {
    const auto methodId = env->GetMethodID(env->GetObjectClass(obj), method.c_str(), signature.c_str());
    if (methodId == nullptr) {
        throw std::runtime_error("Method not found: " + method);
    }
    return {env, obj, methodId};
}

JniMpvCallback::JniMpvCallback(JNIEnv *env, const jobject thiz)
    : propertyChanged{
          refTo<void, jstring, jobject>(
              env, thiz, "onPropertyChanged", "(Ljava/lang/String;Ljava/lang/Object;)V")
      },
      propertyGet{
          refTo<void, jlong, jobject>(
              env, thiz, "onPropertyGet", "(JLjava/lang/Object;)V")
      },
      propertySet{
          refTo<void, jlong, jobject>(
              env, thiz, "onPropertySet", "(JLjava/lang/Object;)V")
      },
      commandReply{
          refTo<void, jlong, jobject>(
              env, thiz, "onCommandReply", "(JLjava/lang/Object;)V")
      } {
}

void JniMpvCallback::onPropertyChanged(const std::string &name, jobject result) {
    const auto attached = propertyChanged.attach();
    const auto jni = attached.get()->get();
    const auto nameStr = jni->NewStringUTF(name.c_str());
    propertyChanged(nameStr, result);
}

void JniMpvCallback::onCommandReply(long subscriptionId, jobject result) {
    commandReply(subscriptionId, result);
}

void JniMpvCallback::onPropertyGet(long subscriptionId, jobject result) {
    propertyGet(subscriptionId, result);
}

void JniMpvCallback::onPropertySet(long subscriptionId, jobject result) {
    propertySet(subscriptionId, result);
}
