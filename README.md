# tv-bt-connect

Connect or disconnect a paired Bluetooth headset on an Android TV from the command line, without opening any menu. I use it to send my TV's sound to AirPods from a Home Assistant script, while whatever is on screen keeps playing.

It's a tiny APK with no UI, just a broadcast receiver. It asks the system A2DP service to connect the device whose MAC you pass in.

## Requirements

Android 10 or older. The receiver calls the hidden `BluetoothA2dp.connect()` / `disconnect()` methods, which Android 11 no longer lets regular apps use. Tested on a Sony Bravia KD-49XH9505 (Android TV 10).

The headset has to be paired with the TV already.

## Build

You need JDK 17 and an Android SDK with `platforms;android-29` and `build-tools;34.0.0`:

```sh
sdkmanager "platforms;android-29" "build-tools;34.0.0"
./build.sh
```

`ANDROID_HOME` defaults to `~/Library/Android/sdk`. The first build creates a debug signing key (`debug.keystore`) that isn't committed.

You can also grab a prebuilt APK from the releases page. GitHub Actions builds every push and attaches a signed APK to the release when a `v*` tag is pushed.

## Install

Turn on developer options and network debugging on the TV, then:

```sh
adb connect <tv-ip>:5555
adb install bt-connect.apk
```

Google Play Protect may warn that the app is unsafe or blocked. That's because it's sideloaded and targets an old SDK, which is what keeps the hidden API reachable. You can install it anyway from the same dialog.

## Use

```sh
# connect
adb shell am broadcast -f 0x20 -n es.bvis.btconnect/.ConnectReceiver --es mac AA:BB:CC:DD:EE:FF
# disconnect
adb shell am broadcast -f 0x20 -n es.bvis.btconnect/.ConnectReceiver --es mac AA:BB:CC:DD:EE:FF --es action disconnect
```

The broadcast result is `connect=true` (or `disconnect=true`) when the request was accepted, or `ERROR ...` otherwise. `true` means the stack took the request, not that the headset is already connected: if it's in its case nothing happens. Paired MACs are listed in `adb shell dumpsys bluetooth_manager` under "Bonded devices". `-f 0x20` lets the broadcast reach the app even if it has never been opened.

## Home Assistant

With the Android TV (ADB) integration set up for the TV:

```yaml
action: androidtv.adb_command
target:
  entity_id: media_player.android_tv
data:
  command: am broadcast -f 0x20 -n es.bvis.btconnect/.ConnectReceiver --es mac AA:BB:CC:DD:EE:FF
```

## License

MIT
