//
// Created by silenium-dev on 7/23/24.
//

#ifndef VAINTEROPIMAGE_HPP
#define VAINTEROPIMAGE_HPP

#include "render/GLInteropImage.hpp"
#include "util/EGL.hpp"
#include <EGL/eglext.h>

class VAGLInteropImage final : public GLInteropImage {
public:
    VAGLInteropImage(EGLDisplay display, const std::vector<EGLImageKHR> &images,
                     const std::vector<unsigned int> &textures,
                     const std::vector<Swizzles> &swizzles);

    VAGLInteropImage(VAGLInteropImage &&) noexcept = default;

    VAGLInteropImage &operator=(VAGLInteropImage &&) noexcept = default;

    VAGLInteropImage(const VAGLInteropImage &) = delete;

    VAGLInteropImage &operator=(const VAGLInteropImage &) = delete;

    ~VAGLInteropImage() override;

    [[nodiscard]] const std::vector<unsigned int> &planeTextures() const override;

    [[nodiscard]] const std::vector<Swizzles> &planeSwizzles() const override;

private:
    EGLDisplay eglDisplay{EGL_NO_DISPLAY};
    std::vector<EGLImageKHR> eglImages{};
    std::vector<unsigned int> textures{};
    std::vector<Swizzles> swizzles{};
};


#endif //VAINTEROPIMAGE_HPP
