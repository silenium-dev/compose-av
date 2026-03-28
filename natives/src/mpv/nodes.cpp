#include "nodes.hpp"

#include <iostream>

#include <jni.h>
#include <unordered_map>
#include <vector>

static std::unordered_map<std::string, jclass> classCache;
static std::unordered_map<ClassConstructorRef, NodeClass> nodeClassCache;

jclass nodeClass(JNIEnv *env, const std::string &name) {
    if (classCache.contains(name)) {
        return classCache[name];
    }
    const auto clazz = env->FindClass(name.c_str());
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found: " + name);
    }
    const auto globalRef = static_cast<jclass>(env->NewGlobalRef(clazz));
    env->DeleteLocalRef(clazz);
    classCache[name] = globalRef;
    return globalRef;
}

NodeClass nodeClass(JNIEnv *env, const ClassConstructorRef &ref) {
    if (nodeClassCache.contains(ref)) {
        return nodeClassCache.at(ref);
    }
    const NodeClass clazz{env, ref};
    nodeClassCache.emplace(ref, clazz);
    return clazz;
}

ClassConstructorRef::ClassConstructorRef(const std::string &name, const std::string &signature) : m_name(name),
    m_signature(signature) {
}

std::string ClassConstructorRef::name() const {
    return m_name;
}

std::string ClassConstructorRef::signature() const {
    return m_signature;
}

bool ClassConstructorRef::operator==(const ClassConstructorRef &other) const {
    return m_name == other.m_name && m_signature == other.m_signature;
}

NodeClass::NodeClass(JNIEnv *env, const ClassConstructorRef &ref) {
    const auto clazz = env->FindClass(ref.name().c_str());
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found: " + ref.name());
    }
    m_clazz = static_cast<jclass>(env->NewGlobalRef(clazz));
    env->DeleteLocalRef(clazz);
    m_ctor = env->GetMethodID(m_clazz, "<init>", ref.signature().c_str());
    if (m_ctor == nullptr) {
        throw std::runtime_error("Constructor not found: <init> " + ref.signature());
    }
}

jobject NodeClass::newInstance(JNIEnv *env, ...) const {
    va_list args;
    va_start(args, env);
    const auto result = env->NewObjectV(m_clazz, m_ctor, args);
    va_end(args);
    return result;
}

jclass NodeClass::clazz() const {
    return m_clazz;
}

jobject mapNode(JNIEnv *env, const mpv_node node) {
    switch (node.format) {
        case MPV_FORMAT_STRING:
            [[fallthrough]];
        case MPV_FORMAT_OSD_STRING: {
            const std::string str{node.u.string};
            return nodeClass(env, NODE_STRING_CLASS).newInstance(env, env->NewStringUTF(str.c_str()));
        }
        case MPV_FORMAT_FLAG: {
            return nodeClass(env, NODE_FLAG_CLASS).newInstance(env, static_cast<jboolean>(node.u.flag));
        }
        case MPV_FORMAT_INT64: {
            return nodeClass(env, NODE_LONG_CLASS).newInstance(env, static_cast<jlong>(node.u.int64));
        }
        case MPV_FORMAT_DOUBLE: {
            return nodeClass(env, NODE_DOUBLE_CLASS).newInstance(env, node.u.double_);
        }
        case MPV_FORMAT_NODE_ARRAY: {
            const auto result = env->NewObjectArray(node.u.list->num, nodeClass(env, NODE_BASE_CLASS), nullptr);
            for (int i = 0; i < node.u.list->num; i++) {
                const auto mapped = mapNode(env, node.u.list->values[i]);
                env->SetObjectArrayElement(result, i, mapped);
            }
            return nodeClass(env, NODE_LIST_CLASS).newInstance(env, result);
        }
        case MPV_FORMAT_NODE_MAP: {
            const auto entries = env->NewObjectArray(
                node.u.list->num,
                nodeClass(env, NODE_MAP_ENTRY_CLASS).clazz(),
                nullptr
            );
            for (int i = 0; i < node.u.list->num; i++) {
                const auto value = mapNode(env, node.u.list->values[i]);
                const auto key = env->NewStringUTF(node.u.list->keys[i]);
                const auto entry = nodeClass(env, NODE_MAP_ENTRY_CLASS).newInstance(env, key, value);
                env->SetObjectArrayElement(entries, i, entry);
            }
            return nodeClass(env, NODE_MAP_CLASS).newInstance(env, entries);
        }
        case MPV_FORMAT_BYTE_ARRAY: {
            const auto result = env->NewByteArray(node.u.ba->size);
            if (result == nullptr || env->ExceptionCheck()) {
                return nullptr;
            }
            env->SetByteArrayRegion(result, 0, node.u.ba->size, static_cast<const jbyte *>(node.u.ba->data));
            if (env->ExceptionCheck()) {
                return nullptr;
            }
            const auto instance = nodeClass(env, NODE_BYTEARRAY_CLASS).newInstance(env, result);
            if (instance == nullptr || env->ExceptionCheck()) {
                return nullptr;
            }
            return instance;
        }
        case MPV_FORMAT_NONE: {
            const auto clazz = env->FindClass(NODE_NONE_CLASS);
            if (clazz == nullptr) {
                throw std::runtime_error("Class not found: " + std::string(NODE_NONE_CLASS));
            }
            const auto instanceField = env->GetStaticFieldID(clazz, "INSTANCE",
                                                             std::format("L{};", NODE_NONE_CLASS).c_str());
            if (instanceField == nullptr) {
                throw std::runtime_error("Static field not found: INSTANCE");
            }
            return env->GetStaticObjectField(clazz, instanceField);
        }
        case MPV_FORMAT_NODE: // Not possible
            [[fallthrough]];
        default:
            throw std::runtime_error("Unsupported format: " + formatName(node.format));
    }
}

std::string formatName(const mpv_format fmt) {
    switch (fmt) {
        case MPV_FORMAT_STRING:
            return "MPV_FORMAT_STRING";
        case MPV_FORMAT_FLAG:
            return "MPV_FORMAT_FLAG";
        case MPV_FORMAT_INT64:
            return "MPV_FORMAT_INT64";
        case MPV_FORMAT_DOUBLE:
            return "MPV_FORMAT_DOUBLE";
        case MPV_FORMAT_NODE_ARRAY:
            return "MPV_FORMAT_NODE_ARRAY";
        case MPV_FORMAT_NODE_MAP:
            return "MPV_FORMAT_NODE_MAP";
        case MPV_FORMAT_BYTE_ARRAY:
            return "MPV_FORMAT_BYTE_ARRAY";
        case MPV_FORMAT_NONE:
            return "MPV_FORMAT_NONE";
        default:
            return "Unknown";
    }
}
