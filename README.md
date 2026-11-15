# Null YT — Mobile (Flutter)

[Get it on Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/Nullware-Inc/Null-YT-Client-Mobile-Dart) · [Latest APKs](https://github.com/Nullware-Inc/Null-YT-Client-Mobile-Dart/releases/latest)

## Install

- **Obtainium (recommended):** tap the link above, or add `https://github.com/Nullware-Inc/Null-YT-Client-Mobile-Dart` in Obtainium. It installs the APK for your phone's CPU and keeps it updated from GitHub Releases.
- **Manual:** download `NullYT-android-*-arm64-v8a.apk` from Releases. Almost every phone from the last 8 years is arm64. Use `armeabi-v7a` for very old phones and `x86_64` for emulators and Chromebooks.

Every release is signed with the same Nullware key, so updates install over the top.

Android shell for Null YT. All UI and logic lives in
[`Null-YT-Core-Dart`](../Null-YT-Core-Dart) (path dependency, checked out next
to this repo).

yt-dlp runs **inside the APK**: [youtubedl-android](https://github.com/JunkFood02/youtubedl-android)
bundles Python + yt-dlp, and `MainActivity.kt` exposes it to Dart on the
`nullyt/ytdlp` method channel. So the mobile app uses the exact same yt-dlp
command lines (and the same YouTube sync) as desktop.

iOS: not supported yet. iOS can't spawn processes or embed yt-dlp the same way;
it needs a native-Dart extractor backend. The `ios/` folder is a placeholder.

```bash
flutter run -d <android-device>
```
