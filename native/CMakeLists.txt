cmake_minimum_required(VERSION 3.21)
project(gl-demo LANGUAGES CXX)

file(GLOB_RECURSE SOURCES CONFIGURE_DEPENDS "src/*.cpp" "src/*.h" "src/*.hpp")
add_library(gl-demo SHARED ${SOURCES})

find_package(PkgConfig REQUIRED)
pkg_check_modules(GL REQUIRED IMPORTED_TARGET gl egl libva libva-drm libdrm)
pkg_check_modules(ffmpeg REQUIRED IMPORTED_TARGET libavcodec libavformat libavutil libswscale)
target_link_libraries(gl-demo PRIVATE PkgConfig::GL PkgConfig::ffmpeg)
target_include_directories(gl-demo PRIVATE "/usr/lib/jvm/java-21-openjdk/include" "/usr/lib/jvm/java-21-openjdk/include/linux")
