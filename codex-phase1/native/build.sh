#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .cache build/native artifact
if [ -z "${Q50_NDK:-}" ]; then
  ZIP=.cache/android-ndk-r10e-linux-x86_64.zip
  [ -f "$ZIP" ] || curl -fL --retry 3 https://dl.google.com/android/repository/android-ndk-r10e-linux-x86_64.zip -o "$ZIP"
  echo "ee5f405f3b57c4f5c3b3b8b5d495ae12b660e03d2112e4ed5c728d349f1e520c  $ZIP" | sha256sum -c -
  [ -d .cache/android-ndk-r10e ] || unzip -q "$ZIP" -d .cache
  Q50_NDK="$PWD/.cache/android-ndk-r10e"
fi
test -d "$Q50_NDK/platforms/android-9/arch-x86"
test -d "$Q50_NDK/platforms/android-9/arch-arm"
"$Q50_NDK/ndk-build" NDK_TOOLCHAIN_VERSION=4.9 NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=native/Android.mk \
  NDK_APPLICATION_MK=native/Application.mk NDK_OUT=build/native/obj NDK_LIBS_OUT=build/native/lib
find build/native/lib -name '*.so' -exec sha256sum '{}' \; > artifact/native-checksums.txt
echo 'NDK r10e GCC4.9; APP_PLATFORM android-9; x86 and ARM API9; x86_64 platform floor API21' > artifact/native-provenance.txt
