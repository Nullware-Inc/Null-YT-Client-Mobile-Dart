# Null YT — Mobile (Flutter)

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
