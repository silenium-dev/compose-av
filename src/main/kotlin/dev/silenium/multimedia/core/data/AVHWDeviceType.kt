package dev.silenium.multimedia.core.data

enum class AVHWDeviceType(override val id: Int, val hwFormat: AVPixelFormat) : FFmpegEnum {
    AV_HWDEVICE_TYPE_NONE(0, AVPixelFormat.AV_PIX_FMT_NONE),
    AV_HWDEVICE_TYPE_VDPAU(1, AVPixelFormat.AV_PIX_FMT_VDPAU),
    AV_HWDEVICE_TYPE_CUDA(2, AVPixelFormat.AV_PIX_FMT_CUDA),
    AV_HWDEVICE_TYPE_VAAPI(3, AVPixelFormat.AV_PIX_FMT_VAAPI),
    AV_HWDEVICE_TYPE_DXVA2(4, AVPixelFormat.AV_PIX_FMT_DXVA2_VLD),
    AV_HWDEVICE_TYPE_QSV(5, AVPixelFormat.AV_PIX_FMT_QSV),
    AV_HWDEVICE_TYPE_VIDEOTOOLBOX(6, AVPixelFormat.AV_PIX_FMT_VIDEOTOOLBOX),
    AV_HWDEVICE_TYPE_D3D11VA(7, AVPixelFormat.AV_PIX_FMT_D3D11),
    AV_HWDEVICE_TYPE_DRM(8, AVPixelFormat.AV_PIX_FMT_DRM_PRIME),
    AV_HWDEVICE_TYPE_OPENCL(9, AVPixelFormat.AV_PIX_FMT_OPENCL),
    AV_HWDEVICE_TYPE_MEDIACODEC(10, AVPixelFormat.AV_PIX_FMT_MEDIACODEC),
    AV_HWDEVICE_TYPE_VULKAN(11, AVPixelFormat.AV_PIX_FMT_VULKAN),
    AV_HWDEVICE_TYPE_D3D12VA(12, AVPixelFormat.AV_PIX_FMT_D3D12);

    companion object {
        fun fromFormat(format: AVPixelFormat) = entries.first { it.hwFormat == format }
    }
}
