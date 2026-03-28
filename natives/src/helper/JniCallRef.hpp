#ifndef NATIVES_JNICALLREF_HPP
#define NATIVES_JNICALLREF_HPP

#include <jni.h>
#include <memory>
#include <stdexcept>
#include <type_traits>

namespace detail {
    class AttachedEnv {
    public:
        explicit AttachedEnv(JavaVM *jvm) : m_jvm(jvm), m_env(nullptr) {
            if (m_jvm->AttachCurrentThread(reinterpret_cast<void **>(&m_env), nullptr) != JNI_OK) {
                throw std::runtime_error("Failed to attach current thread");
            }
        }

        ~AttachedEnv() {
            if (m_jvm != nullptr) {
                m_jvm->DetachCurrentThread();
            }
        }

        JNIEnv *get() const {
            return m_env;
        }

    private:
        JavaVM *m_jvm;
        JNIEnv *m_env;
    };

    template<typename Return>
    struct JniCallTraits;

    template<>
    struct JniCallTraits<void> {
        template<typename... Args>
        static void call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            env->CallVoidMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jboolean> {
        template<typename... Args>
        static jboolean call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallBooleanMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jbyte> {
        template<typename... Args>
        static jbyte call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallByteMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jchar> {
        template<typename... Args>
        static jchar call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallCharMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jshort> {
        template<typename... Args>
        static jshort call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallShortMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jint> {
        template<typename... Args>
        static jint call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallIntMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jlong> {
        template<typename... Args>
        static jlong call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallLongMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jfloat> {
        template<typename... Args>
        static jfloat call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallFloatMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jdouble> {
        template<typename... Args>
        static jdouble call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallDoubleMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jobject> {
        template<typename... Args>
        static jobject call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallObjectMethod(obj, method, args...);
        }
    };
} // namespace detail

template<typename Return, typename... Args>
class JniCallRef {
public:
    JniCallRef(JNIEnv *env, const jobject obj, const jmethodID method)
        : m_jvm(nullptr), m_obj(nullptr), m_method(method) {
        if (env->GetJavaVM(&m_jvm) != JNI_OK) {
            throw std::runtime_error("Failed to get JavaVM");
        }
        m_obj = env->NewGlobalRef(obj);
    }

    ~JniCallRef() {
        const detail::AttachedEnv attached(m_jvm);
        JNIEnv *env = attached.get();
        env->DeleteGlobalRef(m_obj);
        m_jvm = nullptr;
    }

    Return operator()(JNIEnv *env, Args... args) {
        if constexpr (std::is_void_v<Return>) {
            detail::JniCallTraits<void>::call(env, m_obj, m_method, args...);
            return;
        } else {
            return detail::JniCallTraits<Return>::call(env, m_obj, m_method, args...);
        }
    }

    std::unique_ptr<detail::AttachedEnv> attach() const {
        return std::make_unique<detail::AttachedEnv>(m_jvm);
    }

private:
    JavaVM *m_jvm;
    jobject m_obj;
    jmethodID m_method;
};

#endif // NATIVES_JNICALLREF_HPP
