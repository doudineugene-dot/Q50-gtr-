#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${Q50_NDK:?Set Q50_NDK to official Android NDK r10e}"
test -d "$Q50_NDK/platforms/android-9/arch-x86"
test -d "$Q50_NDK/platforms/android-9/arch-arm"
mkdir -p .cache build/classes build/gen artifact
JAR=../codex-phase1/.cache/android-2.3.3.jar
mkdir -p ../codex-phase1/.cache
if [ ! -f "$JAR" ]; then curl -fsSL --max-time 120 https://repo.maven.apache.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar -o "$JAR"; fi
echo "638a4f8a6f31cdfd40699cc0a7e2fe90057f52dab88a8224585aae08ca055d5f  $JAR" | sha256sum -c -
"$Q50_NDK/ndk-build" NDK_TOOLCHAIN_VERSION=4.9 NDK_PROJECT_PATH="$PWD" APP_BUILD_SCRIPT="$PWD/native/Android.mk" NDK_APPLICATION_MK="$PWD/native/Application.mk"
test -f libs/x86/libq50btprobe.so
test -f libs/armeabi-v7a/libq50btprobe.so
aapt package -f -M AndroidManifest.xml -S res -I "$JAR" -J build/gen -F build/unsigned.apk
find src build/gen -name '*.java' | sort > build/java-sources.txt
javac --release 7 -encoding UTF-8 -classpath "$JAR" -d build/classes @build/java-sources.txt
find build/classes -name '*.class' | sort > build/dex-sources.txt
d8 --min-api 10 --lib "$JAR" --output build @build/dex-sources.txt
(cd build && aapt add -f unsigned.apk classes.dex)
mkdir -p build/lib
cp -r libs/x86 libs/armeabi-v7a build/lib/
(cd build && aapt add -f unsigned.apk lib/x86/libq50btprobe.so lib/armeabi-v7a/libq50btprobe.so)
zipalign -f 4 build/unsigned.apk build/aligned.apk
if [ -n "${Q50_KEYSTORE:-}" ]; then
  KEYSTORE="$Q50_KEYSTORE"; MODE=persistent
  : "${Q50_KEY_ALIAS:?}" "${Q50_KEY_PASS:?}"
else
  KEYSTORE=.cache/probe.keystore; MODE=temporary
  export Q50_KEY_ALIAS=prototype Q50_KEY_PASS=prototype-only
  if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -noprompt -keystore "$KEYSTORE" -alias "$Q50_KEY_ALIAS" -storepass "$Q50_KEY_PASS" -keypass "$Q50_KEY_PASS" -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Q50 Passive Probe'
  fi
fi
APK=artifact/Q50-Codex-EcuTek-Probe.apk
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$Q50_KEY_ALIAS" --ks-pass env:Q50_KEY_PASS --key-pass env:Q50_KEY_PASS --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --out "$APK" build/aligned.apk
apksigner verify --min-sdk-version 10 --verbose --print-certs "$APK" | tee artifact/signature.txt
grep -q 'Verified using v1 scheme (JAR signing): true' artifact/signature.txt
grep -q 'Verified using v2 scheme (APK Signature Scheme v2): false' artifact/signature.txt
grep -q 'Verified using v3 scheme (APK Signature Scheme v3): false' artifact/signature.txt
zipalign -c 4 "$APK"
aapt dump badging "$APK" | tee artifact/badging.txt
grep -q "package: name='ru.q50.ecutekprobe'" artifact/badging.txt
grep -q "^sdkVersion:'10'" artifact/badging.txt
grep -q "^targetSdkVersion:'10'" artifact/badging.txt
python3 - <<'PY'
import re, zipfile
data=open('artifact/badging.txt').read()
assert set(re.findall(r"uses-permission: name='([^']+)'",data))=={'android.permission.BLUETOOTH','android.permission.WRITE_EXTERNAL_STORAGE'}
with zipfile.ZipFile('artifact/Q50-Codex-EcuTek-Probe.apk') as z:
    assert z.read('classes.dex')[:8]==b'dex\n035\x00'
    for abi,machine in [('x86',3),('armeabi-v7a',40)]:
        lib=z.read('lib/'+abi+'/libq50btprobe.so')
        assert lib[:5]==b'\x7fELF\x01' and int.from_bytes(lib[18:20],'little')==machine
print('PASS: exact permissions, DEX035 and ELF32 x86/ARM')
PY
python3 ../tools/epktool.py build "$APK" -o artifact/Q50-Codex-EcuTek-Probe.epk --cert ../keys/obu_cert.pem --type 2 --name q50probe.apk
python3 ../tools/epktool.py verify artifact/Q50-Codex-EcuTek-Probe.epk --expect-name q50probe.apk --expect-type 2 --expect-blocks 1 --expect-key-size 128 | tee artifact/epk-verification.txt
# Legacy Linux fallback: statically link API9 bionic rather than modern glibc,
# whose minimum kernel can exceed the reported DCU 2.6.37. Never auto-run su.
"$Q50_NDK/toolchains/x86-4.9/prebuilt/linux-x86_64/bin/i686-linux-android-gcc" \
  --sysroot="$Q50_NDK/platforms/android-9/arch-x86" -static -DSTANDALONE -std=c99 \
  -O2 -Wall -Wextra -Werror native/btprobe.c -o artifact/Q50-btprobe-linux-x86
file artifact/Q50-btprobe-linux-x86 | tee artifact/native-abi.txt
if readelf -l artifact/Q50-btprobe-linux-x86 | grep -q INTERP; then exit 1; fi
mkdir -p artifact/legacy-host-native
set +e
artifact/Q50-btprobe-linux-x86 artifact/legacy-host-native 1
HOST_RESULT=$?
set -e
test "$HOST_RESULT" -eq 0 -o "$HOST_RESULT" -eq 5 -o "$HOST_RESULT" -eq 6
grep -q 'finished result=' artifact/legacy-host-native/native.txt
readelf -h libs/x86/libq50btprobe.so >> artifact/native-abi.txt
readelf -d libs/x86/libq50btprobe.so >> artifact/native-abi.txt
nm -D libs/x86/libq50btprobe.so > artifact/native-symbols.txt
# Reject transmit/connect symbols, including accidental imports added later.
if grep -E '\b(send|sendto|sendmsg|connect|hci_send_cmd|hci_send_req)$' artifact/native-symbols.txt; then exit 1; fi
printf '%s\n' "$MODE" > artifact/signing-mode.txt
{
  echo "commit=$(git rev-parse HEAD)"
  echo "branch=${GITHUB_REF_NAME:-$(git branch --show-current)}"
  echo 'package=ru.q50.ecutekprobe minSdk=10 targetSdk=10 version=1.0-recon'
  echo 'scope=passive reconnaissance; ECUTEK RPM/BOOST UNAVAILABLE'
  echo "signing=$MODE"
  echo "run=${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-local}/actions/runs/${GITHUB_RUN_ID:-local}"
  java -version 2>&1
  cat "$Q50_NDK/RELEASE.TXT"
  aapt version; d8 --version; apksigner --version
} > artifact/BUILD-PROVENANCE.txt
(cd artifact && sha256sum Q50-Codex-EcuTek-Probe.apk Q50-Codex-EcuTek-Probe.epk Q50-btprobe-linux-x86 > SHA256SUMS && sha256sum -c SHA256SUMS)
cp ../docs/BLUETOOTH-PROBE-NEXT-TEST.md artifact/NEXT_TEST.md
cp tools/analyze_hci.py artifact/
