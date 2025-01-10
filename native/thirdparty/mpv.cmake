set(MPV_URL "https://reposilite.silenium.dev/releases/dev/silenium/libs/mpv/mpv-natives-${NATIVE_PLATFORM}/${MPV_VERSION}/mpv-natives-${NATIVE_PLATFORM}-${MPV_VERSION}.zip")
set(MPV_URL_SHA256 "https://reposilite.silenium.dev/releases/dev/silenium/libs/mpv/mpv-natives-${NATIVE_PLATFORM}/${MPV_VERSION}/mpv-natives-${NATIVE_PLATFORM}-${MPV_VERSION}.zip.sha256")
set(MPV_PREFIX "${CMAKE_BINARY_DIR}/mpv")
message(STATUS "Downloading mpv from ${MPV_URL}")

file(DOWNLOAD "${MPV_URL_SHA256}" "${CMAKE_BINARY_DIR}/mpv.zip.sha256")
file(READ "${CMAKE_BINARY_DIR}/mpv.zip.sha256" MPV_SHA256)
file(DOWNLOAD "${MPV_URL}" "${CMAKE_BINARY_DIR}/mpv.zip" EXPECTED_HASH SHA256=${MPV_SHA256} SHOW_PROGRESS)
file(ARCHIVE_EXTRACT INPUT "${CMAKE_BINARY_DIR}/mpv.zip" DESTINATION "${MPV_PREFIX}")

set(MPV_INCLUDE_DIR "${MPV_PREFIX}/include")
set(MPV_LIB_DIR "${MPV_PREFIX}/lib")
set(MPV_LIBRARIES
        mpv
        ass
        placebo)
add_library(mpv STATIC IMPORTED)

set(MPV_MRI "${CMAKE_CURRENT_BINARY_DIR}/mpv.mri")
file(WRITE "${MPV_MRI}" "CREATE libmpv.a\n")
message(STATUS "Checking for mpv libraries")
foreach (MPV_LIBRARY ${MPV_LIBRARIES})
    set(LIB_PATH "${MPV_LIB_DIR}/lib${MPV_LIBRARY}.a")
    if (NOT EXISTS ${LIB_PATH})
        message(STATUS "  ${MPV_LIBRARY} not found")
        continue()
    endif ()
    message(STATUS "  Found ${MPV_LIBRARY}")
    file(APPEND "${MPV_MRI}" "ADDLIB ${LIB_PATH}\n")
endforeach ()
file(APPEND "${CMAKE_CURRENT_BINARY_DIR}/mpv.mri" "SAVE\nEND\n")

add_custom_target(mpv_custom
        COMMAND ar -M < "${CMAKE_CURRENT_BINARY_DIR}/mpv.mri"
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/libmpv.a
)
add_dependencies(mpv mpv_custom)
set_target_properties(mpv PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_BINARY_DIR}/libmpv.a")

target_include_directories(mpv INTERFACE "${MPV_INCLUDE_DIR}")
target_link_options(mpv INTERFACE "-Wl,-Bsymbolic")

find_package(PkgConfig REQUIRED)
pkg_check_modules(MPV_deps REQUIRED IMPORTED_TARGET libva libva-drm libdrm libva-glx libva-x11 libpipewire-0.3)
target_link_libraries(mpv INTERFACE PkgConfig::MPV_deps)
