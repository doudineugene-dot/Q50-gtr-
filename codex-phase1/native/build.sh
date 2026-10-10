#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .cache build/native artifact
if [ -z "${Q50_NDK:-}" ]; then
  ZIP=.cache/android-ndk-r16b-linux-x86_64.zip
  [ -f "$ZIP" ] || curl -fL --retry 3 https://dl.google.com/android/repository/android-ndk-r16b-linux-x86_64.zip -o "$ZIP"
  echo "42aa43aae89a50d1c66c3f9fdecd676936da6128  $ZIP" | sha1sum -c -
  [ -d .cache/android-ndk-r16b ] || unzip -q "$ZIP" -d .cache
  Q50_NDK="$PWD/.cache/android-ndk-r16b"
fi
"$Q50_NDK/ndk-build" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=native/Android.mk \
  NDK_APPLICATION_MK=native/Application.mk NDK_OUT=build/native/obj NDK_LIBS_OUT=build/native/lib
find build/native/lib -name '*.so' -exec sha256sum '{}' \; > artifact/native-checksums.txt
echo 'NDK r16b; APP_PLATFORM android-9; x86 and ARM API9; x86_64 platform floor API21' > artifact/native-provenance.txt
