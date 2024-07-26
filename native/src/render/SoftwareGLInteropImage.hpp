//
// Created by silenium-dev on 7/26/24.
//

#ifndef COMPOSE_AV_SOFTWAREGLINTEROPIMAGE_HPP
#define COMPOSE_AV_SOFTWAREGLINTEROPIMAGE_HPP

#include "GLInteropImage.hpp"

class SoftwareGLInteropImage: public GLInteropImage {
public:
    SoftwareGLInteropImage(const std::vector<unsigned int> &textures,
                           const std::vector<Swizzles> &swizzles);
    ~SoftwareGLInteropImage() override;

    const std::vector<unsigned int> &planeTextures() const override;
    const std::vector<Swizzles> &planeSwizzles() const override;

private:
    const std::vector<unsigned int> textures{};
    const std::vector<Swizzles> swizzles{};
};


#endif//COMPOSE_AV_SOFTWAREGLINTEROPIMAGE_HPP
