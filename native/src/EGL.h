//
// Created by silenium-dev on 7/15/24.
//

#ifndef GL_DEMO_EGL_H
#define GL_DEMO_EGL_H

#include <EGL/egl.h>
#include <iostream>

template<typename T>
T getFunc(const char *name) {
    auto proc = reinterpret_cast<T>(eglGetProcAddress(name));
    if (!proc) {
        std::cerr << "Failed to get function " << name << std::endl;
        return nullptr;
    }
    return proc;
}

#endif //GL_DEMO_EGL_H
