//
// Created by silenium-dev on 7/29/24.
//

#include "va.hpp"

#include <unistd.h>

void closeDrm(const VADRMPRIMESurfaceDescriptor &drm) {
    for (int i = 0; i < drm.num_objects; ++i) {
        close(drm.objects[i].fd);
    }
}
