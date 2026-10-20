# Inferna Core - Biopunk JVM & Spring Developer Environment
# Copyright (C) 2026 Lucas Henrique Novais de Araújo Petrola <petrolalucas@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Optional, NixOS-only development shell.
#
# This project's build (build.gradle.kts, settings.gradle.kts) is fully
# portable and knows nothing about Nix. On Linux distros with a standard FHS
# layout (Ubuntu, Fedora, etc.), macOS, and Windows, `./gradlew runIde` just
# works out of the box.
#
# NixOS is the one exception: it has no /usr/lib, so the IntelliJ Platform's
# bundled JetBrains Runtime (a prebuilt binary) cannot dlopen() the X11/font
# libraries it expects at standard system paths, and `runIde` crashes during
# AWT/Swing init. This shell exists purely to hand those libraries to the
# sandboxed IDE process via LD_LIBRARY_PATH, as a local dev convenience.
#
# Usage (NixOS only):
#   nix-shell
#   ./gradlew runIde
{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [ pkgs.jdk21 ];

  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath (with pkgs; [
    xorg.libX11
    xorg.libXext
    xorg.libXrender
    xorg.libXtst
    xorg.libXi
    xorg.libXrandr
    xorg.libXcursor
    xorg.libXfixes
    xorg.libXcomposite
    xorg.libXdamage
    xorg.libxcb
    xorg.libXxf86vm
    libxkbcommon
    wayland
    freetype
    fontconfig
    expat
    zlib
    stdenv.cc.cc.lib
    glib
    cairo
    pango
    atk
    at-spi2-core
    at-spi2-atk
    dbus
    mesa
    alsa-lib
    cups
    nspr
    nss
  ]);
}
