package dev.silenium.compose.av.data

/**
 * YUV colorspace type.
 * These values match the ones defined by ISO/IEC 23091-2_2019 subclause 8.3.
 */
enum class AVColorSpace(override val id: Int) : FFmpegEnum {
    /**
     * order of coefficients is actually GBR, also IEC 61966-2-1 (sRGB), YZX and ST 428-1
     */
    AVCOL_SPC_RGB(0),

    /**
     * also ITU-R BT1361 / IEC 61966-2-4 xvYCC709 / derived in SMPTE RP 177 Annex B
     */
    AVCOL_SPC_BT709(1),
    AVCOL_SPC_UNSPECIFIED(2),

    /**
     * reserved for future use by ITU-T and ISO/IEC just like 15-255 are
     */
    AVCOL_SPC_RESERVED(3),

    /**
     * FCC Title 47 Code of Federal Regulations 73.682 (a)(20)
     */
    AVCOL_SPC_FCC(4),

    /**
     * also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM / IEC 61966-2-4 xvYCC601
     */
    AVCOL_SPC_BT470BG(5),

    /**
     * also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC / functionally identical to above
     */
    AVCOL_SPC_SMPTE170M(6),

    /**
     * derived from 170M primaries and D65 white point, 170M is derived from BT470 System M's primaries
     */
    AVCOL_SPC_SMPTE240M(7),

    /**
     * used by Dirac / VC-2 and H.264 FRext, see ITU-T SG16
     */
    AVCOL_SPC_YCGCO(8),

    /**
     * used by Dirac / VC-2 and H.264 FRext, see ITU-T SG16
     */
    AVCOL_SPC_YCOCG(AVCOL_SPC_YCGCO.id),

    /**
     * ITU-R BT2020 non-constant luminance system
     */
    AVCOL_SPC_BT2020_NCL(9),

    /**
     * ITU-R BT2020 constant luminance system
     */
    AVCOL_SPC_BT2020_CL(10),

    /**
     * SMPTE 2085, Y'D'zD'x
     */
    AVCOL_SPC_SMPTE2085(11),

    /**
     * Chromaticity-derived non-constant luminance system
     */
    AVCOL_SPC_CHROMA_DERIVED_NCL(12),

    /**
     * Chromaticity-derived constant luminance system
     */
    AVCOL_SPC_CHROMA_DERIVED_CL(13),

    /**
     * ITU-R BT.2100-0, ICtCp
     */
    AVCOL_SPC_ICTCP(14),

    /**
     * Not part of ABI
     */
    AVCOL_SPC_NB(15);
}
