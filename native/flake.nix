{
  description = "compose-gl build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          buildDeps = pkgBase: with pkgBase; [
            # Libraries
            libGL
            mesa-gl-headers
            libdrm
            libx11
            libva
            libdovi
            libdrm
            libva
            systemdLibs
            hwdata
          ];
          buildTools = pkgBase: with pkgBase; [
            gcc
            binutils
            pkg-config
          ];
        in
        {
          devShells.linux-arm64 = import ./shell-linux-arm64.nix { inherit pkgs; inherit buildTools; inherit buildDeps; };
          devShells.linux-x86_64 = import ./shell-linux-x86_64.nix { inherit pkgs; inherit buildTools; inherit buildDeps; };
          devShells.windows-x86_64 = import ./shell-windows-x86_64.nix { inherit pkgs; inherit buildTools; inherit buildDeps; };
        }
      );
}
