{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  strictDeps = true;
  nativeBuildInputs = with pkgs; [
    # Java Development
    jdk21
    gradle_9

    # C/C++ Build Toolchain
    cmake
    gcc
    gdb
    binutils-unwrapped
    pkgsCross.mingwW64.gcc
    pkgsCross.mingwW64.gdb
    pkgsCross.mingwW64.binutils-unwrapped
    pkgsCross.aarch64-multiplatform.gcc
    pkgsCross.aarch64-multiplatform.binutils-unwrapped
    ninja
    pkg-config
    meson

    # Additional utilities
    git
    python3
    wineWow64Packages.staging
    qemu-user
    perl
  ];
  buildInputs = with pkgs; [
    # Libraries
    libGL
    mesa-gl-headers
    libdrm
    libx11
    libva
    dovi-tool
    libdovi
    libdrm
    libva
    pipewire
    alsa-lib
    libpulseaudio
    libGL
    egl-wayland
    openssl
    systemdLibs
    hwdata
  ];

  shellHook = ''
    echo "Development environment loaded"
    echo "Java version: $(java -version 2>&1 | head -n 1)"
    echo "Gradle version: $(gradle --version | grep Gradle)"
    echo "CMake version: $(cmake --version | head -n 1)"
    echo "GCC version: $(gcc --version | head -n 1)"
    echo "Ninja version: $(ninja --version)"
  '';

  # Set environment variables
  JAVA_HOME = "${pkgs.jdk21}";
  HWDATA_PATH = "${pkgs.hwdata}";
  CMAKE_GENERATOR = "Ninja";
}
