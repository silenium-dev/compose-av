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
        "${FFMPEG_LIB_DIR}/libavcodec.so"
        "${FFMPEG_LIB_DIR}/libavdevice.so"
        "${FFMPEG_LIB_DIR}/libavfilter.so"
        "${FFMPEG_LIB_DIR}/libavformat.so"
        "${FFMPEG_LIB_DIR}/libavutil.so"
        "${FFMPEG_LIB_DIR}/libswresample.so"
        "${FFMPEG_LIB_DIR}/libswscale.so"
)
