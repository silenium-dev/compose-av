cmake_minimum_required(VERSION 3.16)
if (NOT DEFINED PROJECT_NAME)
    set(PROJECT_NAME "compose-av")
endif ()
if (NOT DEFINED FFMPEG_PLATFORM)
    message(FATAL_ERROR "FFMPEG_PLATFORM must be defined")
else ()
    message(STATUS "FFMPEG_PLATFORM: ${FFMPEG_PLATFORM}")
endif ()
if (NOT DEFINED FFMPEG_VERSION)
    message(FATAL_ERROR "FFMPEG_VERSION must be defined")
else ()
    message(STATUS "FFMPEG_VERSION: ${FFMPEG_VERSION}")
endif ()
if (NOT DEFINED JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME must be defined")
else ()
    message(STATUS "JAVA_HOME: ${JAVA_HOME}")
endif ()

project(${PROJECT_NAME} LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
option(USE_SYSTEM_FFMPEG "Link dynamically against system ffmpeg instead of using static prebuilt libraries" OFF)

include(ExternalProject)
if (NOT USE_SYSTEM_FFMPEG)
    include(thirdparty/ffmpeg.cmake)
endif ()

set(SOURCES
        src/cpp/data/AVCodecID.cpp
        src/cpp/data/AVPixelFormat.cpp
        src/cpp/data/Frame.cpp
        src/cpp/data/Packet.cpp
        src/cpp/decode/Decoder.cpp
        src/cpp/decode/SoftwareDecoder.cpp
        src/cpp/demux/FileDemuxer.cpp
        src/cpp/demux/Stream.cpp
        src/cpp/helper/errors.cpp
        src/cpp/helper/errors.hpp
        src/cpp/helper/rationals.cpp
        src/cpp/helper/rationals.hpp
        src/cpp/render/GLInteropImage.cpp
        src/cpp/render/GLInteropImage.hpp
        src/cpp/render/SoftwareGLInteropImage.cpp
        src/cpp/render/SoftwareGLInteropImage.hpp
        src/cpp/render/SoftwareGLRenderInterop.cpp
        src/cpp/render/Swizzles.hpp
        src/cpp/render/Swizzles.cpp
        src/cpp/util/Errors.cpp
        src/cpp/util/AVBuffer.cpp
)

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    if (NOT DEFINED JAVA_HOME)
        set(JAVA_HOME "/usr/lib/jvm/java-11-openjdk")
    endif ()

    list(APPEND SOURCES
            src/cpp/platform/linux/helper/drm_mapping.cpp
            src/cpp/platform/linux/helper/drm_mapping.hpp
            src/cpp/platform/linux/helper/EGL.cpp
            src/cpp/platform/linux/helper/EGL.hpp
            src/cpp/platform/linux/helper/va.cpp
            src/cpp/platform/linux/helper/va.hpp
            src/cpp/platform/linux/Errors.cpp
            src/cpp/platform/linux/VaapiDeviceContext.cpp
            src/cpp/platform/linux/VaapiYuvToRgbConversion.cpp
            src/cpp/platform/linux/VAEGLInteropImage.cpp
            src/cpp/platform/linux/VAEGLInteropImage.hpp
            src/cpp/platform/linux/VAGLXInteropImage.cpp
            src/cpp/platform/linux/VAGLXInteropImage.hpp
            src/cpp/platform/linux/VAGLXRenderInterop.cpp
            src/cpp/platform/linux/VAEGLRenderInterop.cpp
            src/cpp/platform/linux/VaapiDecoder.cpp
    )
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
endif ()

add_library(${PROJECT_NAME} SHARED ${SOURCES})
target_include_directories(${PROJECT_NAME} PRIVATE "src/cpp/")

message(STATUS "JAVA_HOME: ${JAVA_HOME}")
target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include")
if (NOT USE_SYSTEM_FFMPEG)
    target_link_libraries(${PROJECT_NAME} PUBLIC -Wl,--push-state,--whole-archive,--allow-multiple-definition ffmpeg -Wl,--pop-state)
    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD COMMAND objcopy --localize-hidden --strip-all --strip-unneeded $<TARGET_FILE:${PROJECT_NAME}>)
endif ()

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_compile_definitions(${PROJECT_NAME} PRIVATE -D_LINUX)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GL REQUIRED IMPORTED_TARGET gl egl libva libva-drm libdrm glx libva-glx)
    target_link_libraries(${PROJECT_NAME} PUBLIC PkgConfig::GL)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/linux")
    if (USE_SYSTEM_FFMPEG)
        pkg_check_modules(FFMPEG REQUIRED IMPORTED_TARGET libavcodec libavformat libavutil libswscale libavfilter)
        target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::FFMPEG)
    endif ()
elseif (CMAKE_SYSTEM_NAME STREQUAL "Windows")
    target_compile_definitions(${PROJECT_NAME} PRIVATE -D_WINDOWS)
    target_link_libraries(${PROJECT_NAME} PUBLIC opengl32 dxgi d3d11 d3dcompiler)
    target_include_directories(${PROJECT_NAME} PUBLIC "${JAVA_HOME}/include/win32")
endif ()

#add_executable(test main.cpp)
#target_link_libraries(test PRIVATE PkgConfig::FFMPEG)
#target_link_libraries(test PRIVATE ${PROJECT_NAME})
#target_link_libraries(test PRIVATE X11 GLX xcb va va-glx va-x11)
#target_include_directories(test PRIVATE "src/cpp/")
