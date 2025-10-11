#!/usr/bin/env bash

set -eo pipefail

sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y build-essential git pkg-config cmake nasm clang curl python3-pip python3-wheel ninja-build
sudo apt-get install -y libfontconfig-dev libva-dev libdrm-dev libuchardet-dev libpipewire-0.3-dev libasound-dev libpulse-dev libgl-dev libglx-dev libegl-dev libssl-dev hwdata libsystemd-dev

sudo pip3 install meson

curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sudo -i sh -s -- -y
sudo -i rustup default stable
sudo -i cargo install cargo-c

sudo rm -rf dovi_tool && git clone -b 2.3.1 https://github.com/quietvoid/dovi_tool.git
pushd dovi_tool/dolby_vision
sudo -i bash -c "cd $(pwd) && cargo cinstall --release --prefix /usr"
popd

rm -rf mpv && git clone https://github.com/mpv-player/mpv.git --depth=1 mpv
pushd mpv

mkdir -p subprojects
meson wrap update-db
meson wrap install expat
meson wrap install harfbuzz
meson wrap install libpng
meson wrap install zlib
meson wrap install libjpeg-turbo
meson wrap install openal-soft

git clone -b VER-2-14-1 https://gitlab.freedesktop.org/freetype/freetype.git --depth=1 --recursive subprojects/freetype2
git clone -b v1.0.16 https://github.com/fribidi/fribidi.git --depth=1 --recursive subprojects/fribidi
git clone -b 0.3.0 https://gitlab.freedesktop.org/emersion/libdisplay-info.git --depth=1 subprojects/libdisplay-info
cp -r ../subprojects/* subprojects/

export CFLAGS="-fPIC"
export CXXFLAGS="-fPIC"
meson setup build -Dlibmpv=true -Ddrm=enabled -Ddefault_library=static -Dopenal=enabled -Dprefer_static=true --force-fallback-for=ffmpeg,libjpeg,openal
meson compile -C build
