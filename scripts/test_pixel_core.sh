#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .work/pixels
cc -O2 -Wall -Wextra -DPNG_ARM_NEON_OPT=0 -I ios/Core/Vendor/libpng tests/pixel_roundtrip.c ios/Core/PixelCore.c ios/Core/Vendor/libpng/*.c -lz -lm -o .work/pixels/test-core
(ulimit -v 65536; .work/pixels/test-core .work/pixels)
