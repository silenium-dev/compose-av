//
// Created by silenium-dev on 7/26/24.
//

#ifndef COMPOSE_AV_FORMATS_HPP
#define COMPOSE_AV_FORMATS_HPP

#include <map>
extern "C" {
#include <libavutil/pixfmt.h>
}

static std::map<AVPixelFormat, std::map<int, std::pair<int, int> > > planeFractions{
        {AV_PIX_FMT_NV12,        {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_P010LE,      {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_P010BE,      {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_YUV420P,     {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV420P10LE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV420P10BE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV422P,     {{0, {1, 1}}, {1, {2, 1}}, {2, {2, 1}}}},
        {AV_PIX_FMT_YUV444P,     {{0, {1, 1}}, {1, {1, 1}}, {2, {1, 1}}}},
};

#endif //COMPOSE_AV_FORMATS_HPP
