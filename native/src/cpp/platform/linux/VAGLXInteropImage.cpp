//
// Created by silenium-dev on 7/23/24.
//

#include "VAGLXInteropImage.hpp"

#include <GL/glx.h>
#include <va/va_glx.h>

VAGLXInteropImage::VAGLXInteropImage(
        unsigned int texture,
        Swizzles swizzles)
    : texture({texture}), swizzles({swizzles}) {
}

VAGLXInteropImage::~VAGLXInteropImage() {
    glDeleteTextures(1, &texture[0]);
}

const std::vector<GLuint> &VAGLXInteropImage::planeTextures() const {
    return texture;
}

const std::vector<Swizzles> &VAGLXInteropImage::planeSwizzles() const {
    return swizzles;
}
