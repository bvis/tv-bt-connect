#!/usr/bin/env bash
# Builds bt-connect.apk without Gradle: aapt2 + javac + d8 + zipalign + apksigner.
# Needs a JDK 17 and an Android SDK with "platforms;android-29" and "build-tools;34.0.0".
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$SDK/build-tools/${BUILD_TOOLS:-34.0.0}
JAR=$SDK/platforms/android-29/android.jar
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17 ]; then export JAVA_HOME=/opt/homebrew/opt/openjdk@17; fi
[ -n "${JAVA_HOME:-}" ] && export PATH=$JAVA_HOME/bin:$PATH

rm -rf build && mkdir -p build/classes build/dex
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 28 --target-sdk-version 28
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$JAR" -classpath "$JAR" -d build/classes src/es/bvis/btconnect/*.java
"$BT/d8" --lib "$JAR" --min-api 28 --output build/dex build/classes/es/bvis/btconnect/*.class
(cd build/dex && zip -q -u ../base.apk classes.dex)
"$BT/zipalign" -f 4 build/base.apk build/aligned.apk

# Debug key, created on first build and kept out of git. Updating an installed copy needs the same key.
if [ ! -f debug.keystore ]; then
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias debug \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=bt-connect" >/dev/null
fi
"$BT/apksigner" sign --ks debug.keystore --ks-pass pass:android --out bt-connect.apk build/aligned.apk
rm -f bt-connect.apk.idsig
ls -l bt-connect.apk
