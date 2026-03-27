#include "instance.hpp"

#include <format>
#include <iostream>
#include <mutex>

#include "util/MPVException.hpp"
#include "helper/JniMpvCallback.hpp"
#include "helper/results.hpp"


jobject eventDataToJava(JNIEnv *env, const mpv_event_property *prop) {
    jobject value = nullptr;
    switch (prop->format) {
        case MPV_FORMAT_INT64: {
            const auto clazz = env->FindClass("java/lang/Long");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Long" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(J)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<long *>(prop->data));
            break;
        }
        case MPV_FORMAT_DOUBLE: {
            const auto clazz = env->FindClass("java/lang/Double");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Double" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(D)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<double *>(prop->data));
            break;
        }
        case MPV_FORMAT_STRING:
            value = env->NewStringUTF(*static_cast<char **>(prop->data));
            break;
        case MPV_FORMAT_FLAG: {
            const auto clazz = env->FindClass("java/lang/Boolean");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Boolean" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(Z)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<bool *>(prop->data));
            break;
        }
        default:
            // std::cerr << "Unsupported format: " << prop->format << std::endl;
            break;
    }
    return value;
}

void MPVInstance::wakeupCallback(void *ctx) {
    if (ctx == nullptr) {
        return;
    }
    const auto instance = static_cast<MPVInstance *>(ctx);
    instance->onWakeup();
}

void MPVInstance::onWakeup() {
    m_wakeup = true;
    m_cv.notify_one();
}

void MPVInstance::eventLoop() {
    detail::AttachedEnv attached{m_jvm};
    while (m_running) {
        {
            std::unique_lock lock(m_mtx);
            m_cv.wait(lock, [&] { return !m_running || m_wakeup; });
            m_wakeup = false;
        }
        dispatchEvents(attached.get());
    }
}

void MPVInstance::dispatchEvents(JNIEnv *env) const {
    while (m_running) {
        const auto event = mpv_wait_event(m_handle, 0);
        switch (event->event_id) {
            case MPV_EVENT_NONE:
                return;
            case MPV_EVENT_PROPERTY_CHANGE: {
                const auto prop = static_cast<mpv_event_property *>(event->data);
                std::string name{prop->name};
                const auto value = eventDataToJava(env, prop);
                if (value == nullptr) {
                    return;
                }
                m_callback->onPropertyChanged(name, value);
                break;
            }
            case MPV_EVENT_GET_PROPERTY_REPLY: {
                const auto prop = static_cast<mpv_event_property *>(event->data);
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = eventDataToJava(env, prop);
                    if (value == nullptr) {
                        return;
                    }
                } else if (event->error == MPV_ERROR_PROPERTY_UNAVAILABLE) {
                    value = resultSuccessNull();
                } else {
                    value = mpvResultFailure(env, "mpv_get_propery reply", event->error);
                }
                m_callback->onCommandReply(event->reply_userdata, value);
                break;
            }
            case MPV_EVENT_SET_PROPERTY_REPLY: {
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = resultSuccess(env);
                } else {
                    value = mpvResultFailure(env, "mpv_set_propery reply", event->error);
                }
                m_callback->onPropertySet(event->reply_userdata, value);
                break;
            }
            case MPV_EVENT_COMMAND_REPLY: {
                // const auto reply = static_cast<mpv_event_command *>(event->data);
                // TODO: Proper node conversions
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = resultSuccess(env);
                } else {
                    value = mpvResultFailure(env, "mpv_event_command reply", event->error);
                }
                m_callback->onCommandReply(event->reply_userdata, value);
                break;
            }
            default:
                break;
        }
    }
}
