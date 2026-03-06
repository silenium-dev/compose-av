{ pkgs
}:
let
  mpv = { arch, hash }: rec {
    source_url = "https://repoflow.silenium.dev/api/universal/personal/github-releases/shinchiro/mpv-winbuild-cmake/20260225/mpv-dev-${arch}-20260225-git-92ed2d2.7z";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "x86_64-windows" = mpv {
    arch = "x86_64";
    hash = "sha256-XSZqaJm4uxdaaFfJPFNnnwQJUxZ6K+1luD7bA+C0i2U=";
  };
  "aarch64-windows" = {
    arch = "aarch64";
    hash = "sha256-eDtgyMqU7adllvKzVfhg813/5vVqouN2Ly/H/T9zBNI=";
  };
}
