package dev.silenium.multimedia.data

enum class AVMediaType(override val id: Int) : FFmpegEnum {
    /**
     * Usually treated as AVMEDIA_TYPE_DATA
     */
    AVMEDIA_TYPE_UNKNOWN(-1),
    AVMEDIA_TYPE_VIDEO(0),
    AVMEDIA_TYPE_AUDIO(1),

    /**
     * Opaque data information usually continuous
     */
    AVMEDIA_TYPE_DATA(2),
    AVMEDIA_TYPE_SUBTITLE(3),

    /**
     * Opaque data information usually sparse
     */
    AVMEDIA_TYPE_ATTACHMENT(4),
    AVMEDIA_TYPE_NB(5);
};
