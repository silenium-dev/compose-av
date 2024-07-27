//
// Created by silenium-dev on 7/23/24.
//

#ifndef INTEROPIMAGE_HPP
#define INTEROPIMAGE_HPP

#include "Swizzles.hpp"

#include <vector>

class GLInteropImage {
public:
    virtual ~GLInteropImage() = default;

    [[nodiscard]] virtual const std::vector<unsigned int> &planeTextures() const = 0;

    [[nodiscard]] virtual const std::vector<Swizzles> &planeSwizzles() const = 0;
};

#endif //INTEROPIMAGE_HPP
