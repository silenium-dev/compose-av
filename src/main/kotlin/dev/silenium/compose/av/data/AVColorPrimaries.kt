package dev.silenium.compose.av.data


/**
 * Chromaticity coordinates of the source primaries.
 * These values match the ones defined by ISO/IEC 23091-2_2019 subclause 8.1 and ITU-T H.273.
 */
@Suppress("unused")
enum class AVColorPrimaries(override val id: Int) : FFmpegEnum {
    AVCOL_PRI_RESERVED0(0),

    /**
     * also ITU-R BT1361 / IEC 61966-2-4 / SMPTE RP 177 Annex B
     */
    AVCOL_PRI_BT709(1),
    AVCOL_PRI_UNSPECIFIED(2),
    AVCOL_PRI_RESERVED(3),

    /**
     * also FCC Title 47 Code of Federal Regulations 73.682 (a)(20)
     */
    AVCOL_PRI_BT470M(4),

    /**
     * also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM
     */
    AVCOL_PRI_BT470BG(5),

    /**
     * also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC
     */
    AVCOL_PRI_SMPTE170M(6),

    /**
     * identical to above, also called "SMPTE C" even though it uses D65
     */
    AVCOL_PRI_SMPTE240M(7),

    /**
     * colour filters using Illuminant C
     */
    AVCOL_PRI_FILM(8),

    /**
     * ITU-R BT2020
     */
    AVCOL_PRI_BT2020(9),

    /**
     * SMPTE ST 428-1 (CIE 1931 XYZ)
     */
    AVCOL_PRI_SMPTE428(10),

    /**
     * SMPTE ST 428-1 (CIE 1931 XYZ)
     */
    AVCOL_PRI_SMPTEST428_1(AVCOL_PRI_SMPTE428.id),

    /**
     * SMPTE ST 431-2 (2011) / DCI P3
     */
    AVCOL_PRI_SMPTE431(11),

    /**
     * SMPTE ST 432-1 (2010) / P3 D65 / Display P3
     */
    AVCOL_PRI_SMPTE432(12),

    /**
     * EBU Tech. 3213-E (nothing there) / one of JEDEC P22 group phosphors
     */
    AVCOL_PRI_EBU3213(22),

    /**
     * EBU Tech. 3213-E (nothing there) / one of JEDEC P22 group phosphors
     */
    AVCOL_PRI_JEDEC_P22(AVCOL_PRI_EBU3213.id),

    /**
     * Not part of ABI
     */
    AVCOL_PRI_NB(23);
}
