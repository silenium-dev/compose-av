//
// Created by silenium-dev on 8/10/24.
//

#ifndef DEMUXER_HPP
#define DEMUXER_HPP

extern "C" {
#include <libavformat/avformat.h>
}

class Demuxer {
public:
    explicit Demuxer(AVFormatContext *formatContext);
    virtual ~Demuxer();

    AVFormatContext *formatContext{nullptr};
};

#endif//DEMUXER_HPP
