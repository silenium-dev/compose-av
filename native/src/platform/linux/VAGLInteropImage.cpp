//
// Created by silenium-dev on 7/23/24.
//

#include "VAGLInteropImage.hpp"

#include <GLES3/gl3.h>

VAGLInteropImage::VAGLInteropImage(const EGLDisplay display,
                                   const std::vector<EGLImageKHR> &images,
                                   const std::vector<unsigned int> &textures,
                                   const std::vector<Swizzles> &swizzles)
    : eglDisplay(display), eglImages(images), textures(textures), swizzles(swizzles) {
}

VAGLInteropImage::~VAGLInteropImage() {
    const auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    for (const auto &eglImage: eglImages) {
        if (eglImage != EGL_NO_IMAGE_KHR) {
            eglDestroyImageKHR(eglDisplay, eglImage);
        }
    }
    for (auto &texture: textures) {
        glDeleteTextures(1, &texture);
    }
}

const std::vector<GLuint> &VAGLInteropImage::planeTextures() const {
    return textures;
}

const std::vector<Swizzles> &VAGLInteropImage::planeSwizzles() const {
    return swizzles;
}
