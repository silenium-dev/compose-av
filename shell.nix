{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  strictDeps = true;
  nativeBuildInputs = with pkgs; [
    # Java Development
    jdk21
    gradle_9

    # C/C++ Build Toolchain
    cmake
    pkgsCross.aarch64-multiplatform.gcc
    pkgsCross.aarch64-multiplatform.binutils-unwrapped
    ninja
    pkg-config
    meson

    # Additional utilities
    git
    python3
  ];
  buildInputs = with pkgs; [
    # Libraries
    libGL
    mesa-gl-headers
    libdrm
    hwdata
    libx11
    libva
    mesa
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
  CMAKE_GENERATOR = "Ninja";
}
