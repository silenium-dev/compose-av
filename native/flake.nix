{
  description = "jni build environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    jni-utils.url = "github:silenium-dev/jni-utils?ref=nix-base";
  };

  outputs = { nixpkgs, jni-utils, ... }:
    let
      pkgs = nixpkgs.legacyPackages."x86_64-linux";
    in
    {
      packages."x86_64-linux" = jni-utils.lib.buildJNILib {
        name = "compose-av";
        mesonTarget = "compose-av";
        libName = "compose-av";
        libDir = "src";

        additionalNativeInputs = [ pkgs.p7zip ];
        sources = targetSystem:
          let
            mpv = import ./mpv-config.nix { inherit pkgs; };
          in
          [
            (builtins.fetchurl {
              url = mpv.${targetSystem}.mpv_source_url;
              sha256 = mpv.${targetSystem}.mpv_source_hash;
              name = "mpv.7z";
            })
            (builtins.path {
              path = ./.;
              name = "compose-av";
            })
          ];

        unpack = targetSystem: ''
          runHook preUnpack

          # Process each source
          for src in $srcs; do
            srcName=$(stripHash "$src")

            case "$src" in
              *.7z)
              dirName="''${srcName%.7z}"
              echo "Extracting 7z archive into: $dirName"
              mkdir -p "$dirName"
              7z x -o"$dirName" "$src"
              ;;
            *)
              echo "Copying: $srcName"
              cp -r "$src" "$srcName"
              chmod -R +w "$srcName"
              ;;
            esac
          done

          runHook postUnpack
        '';

        patch = targetSystem: ''
          ls -la compose-av
          case "${targetSystem}" in
            *-linux)
              echo "todo"
              exit 1
              ;;
            *-windows)
              mkdir -p compose-av/subprojects
              cp -r mpv compose-av/subprojects/mpv
              cp compose-av/subprojects.tpl/windows/packagefiles/mpv/* compose-av/subprojects/mpv/
              ;;
          esac

          sourceRoot="compose-av"
          cd $sourceRoot
        '';
      };
    };
}
