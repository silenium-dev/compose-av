//
// Created by silenium-dev on 7/23/24.
//

#ifndef VAINTEROPIMAGE_HPP
#define VAINTEROPIMAGE_HPP

#include "helper/EGL.hpp"
#include "render/GLInteropImage.hpp"
#include <va/va.h>

class VAGLXInteropImage final : public GLInteropImage {
public:
    VAGLXInteropImage(
            unsigned int textures,
            Swizzles swizzles);

    VAGLXInteropImage(VAGLXInteropImage &&) noexcept = default;

    VAGLXInteropImage &operator=(VAGLXInteropImage &&) noexcept = default;

    VAGLXInteropImage(const VAGLXInteropImage &) = delete;

    VAGLXInteropImage &operator=(const VAGLXInteropImage &) = delete;

    ~VAGLXInteropImage() override;

    [[nodiscard]] const std::vector<unsigned int> &planeTextures() const override;

    [[nodiscard]] const std::vector<Swizzles> &planeSwizzles() const override;

private:
    std::vector<unsigned int> texture{};
    std::vector<Swizzles> swizzles{};
};


#endif//VAINTEROPIMAGE_HPP
