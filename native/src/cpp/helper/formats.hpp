//
// Created by silenium-dev on 7/26/24.
//

#ifndef COMPOSE_AV_FORMATS_HPP
#define COMPOSE_AV_FORMATS_HPP

#include <GL/gl.h>
#include <map>
extern "C" {
#include <libavutil/pixfmt.h>
}
// @formatter:off
// clang-format off
static std::map<AVPixelFormat, std::map<int, std::pair<int, int> > > planeFractions{
        {AV_PIX_FMT_RGB0,        {{0, {1, 1}}}},
        {AV_PIX_FMT_NV12,        {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_P010LE,      {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_P010BE,      {{0, {1, 1}}, {1, {2, 2}}}},
        {AV_PIX_FMT_YUV420P,     {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV420P10LE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV420P10BE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
        {AV_PIX_FMT_YUV422P,     {{0, {1, 1}}, {1, {2, 1}}, {2, {2, 1}}}},
        {AV_PIX_FMT_YUV444P,     {{0, {1, 1}}, {1, {1, 1}}, {2, {1, 1}}}},
};

static std::map<AVPixelFormat, std::map<int, std::pair<int, int>>> planeTextureFormats{
        {AV_PIX_FMT_RGB0,        {{0, {GL_RGBA8, GL_RGBA}}}},
        {AV_PIX_FMT_NV12,        {{0, {GL_R8,    GL_RED}}, {1, {GL_RG8, GL_RG}}}},
        {AV_PIX_FMT_P010LE,      {{0, {GL_R8,    GL_RED}}, {1, {GL_RG8, GL_RG}}}},
        {AV_PIX_FMT_P010BE,      {{0, {GL_R8,    GL_RED}}, {1, {GL_RG8, GL_RG}}}},
        {AV_PIX_FMT_YUV420P,     {{0, {GL_R8,    GL_RED}}, {0, {GL_R8,  GL_RED}}, {0, {GL_R8, GL_RED}}}},
        {AV_PIX_FMT_YUV420P10LE, {{0, {GL_R8,    GL_RED}}, {0, {GL_R8,  GL_RED}}, {0, {GL_R8, GL_RED}}}},
        {AV_PIX_FMT_YUV420P10BE, {{0, {GL_R8,    GL_RED}}, {0, {GL_R8,  GL_RED}}, {0, {GL_R8, GL_RED}}}},
        {AV_PIX_FMT_YUV422P,     {{0, {GL_R8,    GL_RED}}, {0, {GL_R8,  GL_RED}}, {0, {GL_R8, GL_RED}}}},
        {AV_PIX_FMT_YUV444P,     {{0, {GL_R8,    GL_RED}}, {0, {GL_R8,  GL_RED}}, {0, {GL_R8, GL_RED}}}},
};

static std::map<AVPixelFormat, std::map<int, int>> planeComponents{
        {AV_PIX_FMT_RGB0,        {{0, 4}}},
        {AV_PIX_FMT_NV12,        {{0, 1}, {1, 2}}},
        {AV_PIX_FMT_P010LE,      {{0, 1}, {1, 2}}},
        {AV_PIX_FMT_P010BE,      {{0, 1}, {1, 2}}},
        {AV_PIX_FMT_YUV420P,     {{0, 1}, {0, 1}, {0, 1}}},
        {AV_PIX_FMT_YUV420P10LE, {{0, 1}, {0, 1}, {0, 1}}},
        {AV_PIX_FMT_YUV420P10BE, {{0, 1}, {0, 1}, {0, 1}}},
        {AV_PIX_FMT_YUV422P,     {{0, 1}, {0, 1}, {0, 1}}},
        {AV_PIX_FMT_YUV444P,     {{0, 1}, {0, 1}, {0, 1}}},
};
// clang-format on
// @formatter:on

#endif//COMPOSE_AV_FORMATS_HPP
