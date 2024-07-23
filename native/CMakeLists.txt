cmake_minimum_required(VERSION 3.21)
project(gl-demo LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(SOURCES
        src/data/AVCodecID.cpp
        src/data/AVPixelFormat.cpp
        src/data/Frame.cpp
        src/data/Packet.cpp
        src/decode/Decoder.cpp
        src/decode/SoftwareDecoder.cpp
        src/demux/FileDemuxer.cpp
        src/demux/Stream.cpp
        src/helper/errors.cpp
        src/helper/errors.hpp
        src/helper/rationals.cpp
        src/helper/rationals.hpp
        src/render/GLInteropImage.cpp
        src/render/GLInteropImage.hpp
        src/render/Swizzles.hpp
        src/util/Errors.cpp
)
add_library(gl-demo SHARED ${SOURCES})

if (LINUX)
    if (NOT DEFINED JAVA_HOME)
        set(JAVA_HOME "/usr/lib/jvm/java-21-openjdk")
    endif ()

    list(APPEND SOURCES
            src/platform/linux/drm_mapping.cpp
            src/platform/linux/drm_mapping.hpp
            src/platform/linux/EGL.cpp
            src/platform/linux/EGL.hpp
            src/platform/linux/Errors.cpp
            src/platform/linux/VAGLInteropImage.cpp
            src/platform/linux/VAGLInteropImage.hpp
            src/platform/linux/VAGLRenderInterop.cpp
            src/platform/linux/VaapiDecoder.cpp
    )

    target_compile_options(gl-demo PRIVATE -fPIC)
    target_compile_definitions(gl-demo PRIVATE -D_LINUX)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GL REQUIRED IMPORTED_TARGET gl egl libva libva-drm libdrm)
    pkg_check_modules(ffmpeg REQUIRED IMPORTED_TARGET libavcodec libavformat libavutil libswscale)
    target_link_libraries(gl-demo PRIVATE PkgConfig::GL PkgConfig::ffmpeg)
    target_include_directories(gl-demo PRIVATE "${JAVA_HOME}/include/linux")
elseif (WIN32)
    if (NOT DEFINED JAVA_HOME)
        message(FATAL_ERROR "JAVA_HOME must be defined")
    endif ()
    if (NOT DEFINED FFMPEG_PREFIX)
        message(FATAL_ERROR "FFMPEG_PREFIX must be defined")
    endif ()

    target_include_directories(gl-demo PRIVATE "${FFMPEG_PREFIX}\\include")
    target_link_libraries(gl-demo PRIVATE "${FFMPEG_PREFIX}\\lib\\avcodec.lib" "${FFMPEG_PREFIX}\\lib\\avformat.lib" "${FFMPEG_PREFIX}\\lib\\avutil.lib" "${FFMPEG_PREFIX}\\lib\\swscale.lib")

    target_compile_definitions(gl-demo PRIVATE -D_WIN32)
    target_link_libraries(gl-demo PRIVATE opengl32 dxgi d3d11 d3dcompiler)
    target_include_directories(gl-demo PRIVATE "${JAVA_HOME}/include/win32")
endif ()

target_include_directories(gl-demo PRIVATE "src")
message(STATUS "JAVA_HOME: ${JAVA_HOME}")
target_include_directories(gl-demo PRIVATE "${JAVA_HOME}/include")
