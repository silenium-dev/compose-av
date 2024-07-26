set(FFMPEG_URL "https://reposilite.silenium.dev/releases/dev/silenium/libs/ffmpeg-natives-${FFMPEG_PLATFORM}/${FFMPEG_VERSION}/ffmpeg-natives-${FFMPEG_PLATFORM}-${FFMPEG_VERSION}.zip")
set(FFMPEG_URL_SHA256 "https://reposilite.silenium.dev/releases/dev/silenium/libs/ffmpeg-natives-${FFMPEG_PLATFORM}/${FFMPEG_VERSION}/ffmpeg-natives-${FFMPEG_PLATFORM}-${FFMPEG_VERSION}.zip.sha256")
set(FFMPEG_PREFIX "${CMAKE_BINARY_DIR}/ffmpeg")

file(DOWNLOAD "${FFMPEG_URL_SHA256}" "${CMAKE_BINARY_DIR}/ffmpeg.zip.sha256")
file(READ "${CMAKE_BINARY_DIR}/ffmpeg.zip.sha256" FFMPEG_SHA256)
file(DOWNLOAD "${FFMPEG_URL}" "${CMAKE_BINARY_DIR}/ffmpeg.zip" EXPECTED_HASH SHA256=${FFMPEG_SHA256} SHOW_PROGRESS)
file(ARCHIVE_EXTRACT INPUT "${CMAKE_BINARY_DIR}/ffmpeg.zip" DESTINATION "${FFMPEG_PREFIX}")

set(FFMPEG_INCLUDE_DIR "${FFMPEG_PREFIX}/include")
set(FFMPEG_LIB_DIR "${FFMPEG_PREFIX}/lib")
set(FFMPEG_LIBRARIES
        aom
        crypto
        freetype
        mp3lame
        opencore-amrnb
        opencore-amrwb
        openh264
        opus
        sharpyuv
        speex
        srt
        ssl
        SvtAv1Dec
        SvtAv1Enc
        vo-amrwbenc
        vpx
        webp
        webpdemux
        webpmux
        x264
        x265
        xml2
        z
        zimg
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale)
add_library(ffmpeg STATIC IMPORTED)

set(FFMPEG_MRI "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg.mri")
file(WRITE "${FFMPEG_MRI}" "CREATE libffmpeg.a\n")
message(STATUS "Checking for ffmpeg libraries")
foreach (FFMPEG_LIBRARY ${FFMPEG_LIBRARIES})
    set(LIB_PATH "${FFMPEG_LIB_DIR}/lib${FFMPEG_LIBRARY}.a")
    if (NOT EXISTS ${LIB_PATH})
        message(STATUS "  ${FFMPEG_LIBRARY} not found")
        continue()
    endif ()
    message(STATUS "  Found ${FFMPEG_LIBRARY}")
    file(APPEND "${FFMPEG_MRI}" "ADDLIB ${LIB_PATH}\n")
endforeach ()
file(APPEND "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg.mri" "SAVE\nEND\n")

add_custom_target(ffmpeg_custom
        COMMAND ar -M < "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg.mri"
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/libffmpeg.a
)
add_dependencies(ffmpeg ffmpeg_custom)
set_target_properties(ffmpeg PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_BINARY_DIR}/libffmpeg.a")

target_include_directories(ffmpeg INTERFACE "${FFMPEG_INCLUDE_DIR}")
target_link_options(ffmpeg INTERFACE "-Wl,-Bsymbolic")
