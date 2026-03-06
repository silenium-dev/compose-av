{ pkgs
}:
let
  ffmpeg = { arch, hash }: rec {
    source_url = "https://repoflow.silenium.dev/api/universal/personal/github-releases/BtbN/FFmpeg-Builds/autobuild-2026-02-25-13-05/ffmpeg-n8.0.1-64-g15504610b0-${arch}-gpl-shared-8.0.tar.xz";
    source_hash = hash;
    source_filename = pkgs.lib.lists.last (pkgs.lib.strings.split "/" source_url);
    directory = builtins.elemAt (pkgs.lib.strings.split "." source_filename) 0;
  };
in
{
  "x86_64-linux" = ffmpeg {
    arch = "linux64";
    hash = "sha256-uwnRytgBbJLjzjI0gRRPGL70H/1d6B76QaHQcdvLtS8=";
  };
  "aarch64-linux" = ffmpeg {
    arch = "linuxarm64";
    hash = "sha256-tK1z9Uenfae52UZhliptXTEqlF9Lyz+Cx5CycwzixS8=";
  };
}
