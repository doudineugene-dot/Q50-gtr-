#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p .cache build/classes build/gen artifact
JAR=.cache/android-2.3.3.jar
if [ ! -f "$JAR" ]; then
  curl -fsSL https://repo.maven.apache.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar -o "$JAR"
fi
echo '638a4f8a6f31cdfd40699cc0a7e2fe90057f52dab88a8224585aae08ca055d5f  .cache/android-2.3.3.jar' | sha256sum -c -
aapt package -f -M AndroidManifest.xml -S res -I "$JAR" -J build/gen -F build/unsigned.apk
find src build/gen -name '*.java' | sort > build/java-sources.txt
javac --release 7 -encoding UTF-8 -classpath "$JAR" -d build/classes @build/java-sources.txt
find build/classes -name '*.class' | sort > build/dex-sources.txt
d8 --min-api 10 --lib "$JAR" --output build @build/dex-sources.txt
(cd build && aapt add -f unsigned.apk classes.dex)
zipalign -f 4 build/unsigned.apk build/aligned.apk
if [ -n "${Q50_KEYSTORE:-}" ]; then
  KEYSTORE="$Q50_KEYSTORE"; MODE=persistent
  : "${Q50_KEY_ALIAS:?Signing alias required}" "${Q50_KEY_PASS:?Signing password required}"
else
  KEYSTORE=.cache/prototype.keystore; MODE=temporary
  export Q50_KEY_ALIAS=prototype Q50_KEY_PASS=prototype-only
  if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -noprompt -keystore "$KEYSTORE" -alias "$Q50_KEY_ALIAS" \
      -storepass "$Q50_KEY_PASS" -keypass "$Q50_KEY_PASS" -keyalg RSA -keysize 2048 \
      -validity 10000 -dname 'CN=Q50 Codex Phase1 Prototype'
  fi
fi
APK=artifact/Q50-Codex-Phase1.apk
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$Q50_KEY_ALIAS" --ks-pass env:Q50_KEY_PASS \
  --key-pass env:Q50_KEY_PASS --min-sdk-version 10 --v1-signing-enabled true \
  --v2-signing-enabled false --v3-signing-enabled false --out "$APK" build/aligned.apk
apksigner verify --min-sdk-version 10 --verbose --print-certs "$APK" | tee artifact/signature.txt
grep -q 'Verified using v1 scheme (JAR signing): true' artifact/signature.txt
zipalign -c 4 "$APK"
aapt dump badging "$APK" | tee artifact/badging.txt
grep -q "package: name='ru.q50.codex'" artifact/badging.txt
grep -q "^sdkVersion:'10'" artifact/badging.txt
grep -q "^targetSdkVersion:'10'" artifact/badging.txt
# Reject accidental vehicle/network permissions in this UI-only phase.
if grep -q '^uses-permission:' artifact/badging.txt; then exit 1; fi
python3 - <<'PY'
import zipfile
data=zipfile.ZipFile('artifact/Q50-Codex-Phase1.apk').read('classes.dex')
assert data[:8]==b'dex\n035\x00', 'API 10 requires legacy DEX 035'
print('DEX 035 verified')
PY
python3 ../tools/epktool.py build "$APK" -o artifact/Q50-Codex-Phase1.epk \
  --cert ../keys/obu_cert.pem --type 2 --name q50codex.apk
python3 ../tools/epktool.py info artifact/Q50-Codex-Phase1.epk > artifact/epk-info.txt
python3 ../tools/epktool.py verify artifact/Q50-Codex-Phase1.epk --expect-name q50codex.apk \
  --expect-type 2 --expect-blocks 1 --expect-key-size 128 | tee artifact/epk-verification.txt
printf '%s\n' "$MODE" > artifact/signing-mode.txt
{
  echo "commit=$(git rev-parse HEAD)"
  echo "branch=${GITHUB_REF_NAME:-$(git branch --show-current)}"
  echo 'package=ru.q50.codex minSdk=10 targetSdk=10 version=1.0-phase1'
  echo "signing=$MODE"
  echo "run=${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-local}/actions/runs/${GITHUB_RUN_ID:-local}"
  java -version 2>&1
  aapt version; d8 --version; apksigner --version
} > artifact/BUILD-PROVENANCE.txt
(cd artifact && sha256sum Q50-Codex-Phase1.apk Q50-Codex-Phase1.epk > SHA256SUMS && sha256sum -c SHA256SUMS)
