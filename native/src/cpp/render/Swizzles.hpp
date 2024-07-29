//
// Created by silenium-dev on 7/23/24.
//

#ifndef SWIZZLES_HPP
#define SWIZZLES_HPP

enum class Swizzle {
    USE_RED = 0,
    USE_GREEN = 1,
    USE_BLUE = 2,
    USE_ALPHA = 3,
};

struct Swizzles {
    Swizzle r = Swizzle::USE_RED;
    Swizzle g = Swizzle::USE_GREEN;
    Swizzle b = Swizzle::USE_BLUE;
    Swizzle a = Swizzle::USE_ALPHA;

    static Swizzles Identity;
};

#endif //SWIZZLES_HPP
