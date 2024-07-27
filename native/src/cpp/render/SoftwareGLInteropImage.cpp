//
// Created by silenium-dev on 7/26/24.
//

#include "SoftwareGLInteropImage.hpp"
#include <GLES3/gl3.h>

SoftwareGLInteropImage::SoftwareGLInteropImage(const std::vector<unsigned int> &textures, const std::vector<Swizzles> &swizzles)
    : textures(textures), swizzles(swizzles) {}

SoftwareGLInteropImage::~SoftwareGLInteropImage() {
    for (auto texture : textures) {
        glDeleteTextures(1, &texture);
    }
}
const std::vector<unsigned int> &SoftwareGLInteropImage::planeTextures() const {
    return textures;
}
const std::vector<Swizzles> &SoftwareGLInteropImage::planeSwizzles() const {
    return swizzles;
}
