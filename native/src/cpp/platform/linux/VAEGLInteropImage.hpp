//
// Created by silenium-dev on 7/23/24.
//

#ifndef VAINTEROPIMAGE_HPP
#define VAINTEROPIMAGE_HPP

#include "render/GLInteropImage.hpp"
#include "helper/EGL.hpp"
#include <EGL/eglext.h>

class VAEGLInteropImage final : public GLInteropImage {
public:
    VAEGLInteropImage(EGLDisplay display, const std::vector<EGLImageKHR> &images,
                     const std::vector<unsigned int> &textures,
                     const std::vector<Swizzles> &swizzles);

    VAEGLInteropImage(VAEGLInteropImage &&) noexcept = default;

    VAEGLInteropImage &operator=(VAEGLInteropImage &&) noexcept = default;

    VAEGLInteropImage(const VAEGLInteropImage &) = delete;

    VAEGLInteropImage &operator=(const VAEGLInteropImage &) = delete;

    ~VAEGLInteropImage() override;

    [[nodiscard]] const std::vector<unsigned int> &planeTextures() const override;

    [[nodiscard]] const std::vector<Swizzles> &planeSwizzles() const override;

private:
    EGLDisplay eglDisplay{EGL_NO_DISPLAY};
    std::vector<EGLImageKHR> eglImages{};
    std::vector<unsigned int> textures{};
    std::vector<Swizzles> swizzles{};
};


#endif //VAINTEROPIMAGE_HPP
