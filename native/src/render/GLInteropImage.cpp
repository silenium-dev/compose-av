//
// Created by silenium-dev on 7/23/24.
//

#include "GLInteropImage.hpp"
#include "helper/errors.hpp"

#include <cassert>
#include <jni.h>
#include <vector>

extern "C" {
JNIEXPORT jobjectArray JNICALL
Java_dev_silenium_multimedia_render_GLInteropImageKt_planeTexturesN(JNIEnv *env, jobject thiz, const jlong surface) {
    const auto interopImage = reinterpret_cast<GLInteropImage *>(surface);
    const auto textures = interopImage->planeTextures();
    const auto texturesArray = env->NewObjectArray(static_cast<int>(textures.size()),
                                                   env->FindClass("java/lang/Integer"),
                                                   boxedInt(env, 0));
    for (int i = 0; i < textures.size(); ++i) {
        env->SetObjectArrayElement(texturesArray, i, boxedInt(env, static_cast<int>(textures[i])));
    }
    return texturesArray;
}

JNIEXPORT jobjectArray JNICALL
Java_dev_silenium_multimedia_render_GLInteropImageKt_planeSwizzlesN(JNIEnv *env, jobject thiz, const jlong surface) {
    const auto interopImage = reinterpret_cast<GLInteropImage *>(surface);
    const auto planeSwizzles = interopImage->planeSwizzles();
    const auto swizzlesArray = env->NewObjectArray(static_cast<int>(planeSwizzles.size()),
                                                   env->FindClass("dev/silenium/multimedia/render/Swizzles"),
                                                   nullptr);
    const auto swizzleEnumClass = env->FindClass("dev/silenium/multimedia/render/Swizzle");
    const auto swizzlesClass = env->FindClass("dev/silenium/multimedia/render/Swizzles");
    const auto swizzlesConstructor = env->GetMethodID(swizzlesClass, "<init>",
                                                      "(Ldev/silenium/multimedia/render/Swizzle;Ldev/silenium/multimedia/render/Swizzle;Ldev/silenium/multimedia/render/Swizzle;Ldev/silenium/multimedia/render/Swizzle;)V");
    for (int i = 0; i < planeSwizzles.size(); ++i) {
        const auto [rSwizzle, gSwizzle, bSwizzle, aSwizzle] = planeSwizzles[i];
        const auto rValue = env->GetStaticObjectField(swizzleEnumClass,
                                                      env->GetStaticFieldID(swizzleEnumClass, "USE_RED",
                                                                            "Ldev/silenium/multimedia/render/Swizzle;"));
        const auto gValue = env->GetStaticObjectField(swizzleEnumClass,
                                                      env->GetStaticFieldID(swizzleEnumClass, "USE_GREEN",
                                                                            "Ldev/silenium/multimedia/render/Swizzle;"));
        const auto bValue = env->GetStaticObjectField(swizzleEnumClass,
                                                      env->GetStaticFieldID(swizzleEnumClass, "USE_BLUE",
                                                                            "Ldev/silenium/multimedia/render/Swizzle;"));
        const auto aValue = env->GetStaticObjectField(swizzleEnumClass,
                                                      env->GetStaticFieldID(swizzleEnumClass, "USE_ALPHA",
                                                                            "Ldev/silenium/multimedia/render/Swizzle;"));

        const auto resolveSwizzle = [&](const Swizzle &channel) -> jobject {
            switch (channel) {
                case Swizzle::USE_RED:
                    return rValue;
                case Swizzle::USE_GREEN:
                    return gValue;
                case Swizzle::USE_BLUE:
                    return bValue;
                case Swizzle::USE_ALPHA:
                    return aValue;
            }
            assert(false);
        };

        const auto swizzlesObj = env->NewObject(swizzlesClass, swizzlesConstructor,
                                                resolveSwizzle(rSwizzle),
                                                resolveSwizzle(gSwizzle),
                                                resolveSwizzle(bSwizzle),
                                                resolveSwizzle(aSwizzle));
        env->SetObjectArrayElement(swizzlesArray, i, swizzlesObj);
    }
    return swizzlesArray;
}

JNIEXPORT void JNICALL
Java_dev_silenium_multimedia_render_GLInteropImageKt_destroyN(
    JNIEnv *env, jobject thiz, const jlong surface
) {
    if (surface == 0L) return;
    const auto interopImage = reinterpret_cast<GLInteropImage *>(surface);
    delete interopImage;
}
}
