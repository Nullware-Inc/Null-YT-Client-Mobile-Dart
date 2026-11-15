import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:media_kit/media_kit.dart';
import 'package:nullyt_core/nullyt_core.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  MediaKit.ensureInitialized();

  // Edge-to-edge: the glass bars float over content, so let the app draw
  // behind the system status and navigation bars.
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    systemNavigationBarColor: Colors.transparent,
    systemNavigationBarContrastEnforced: false,
  ));

  // yt-dlp runs inside the APK (see MainActivity.kt).
  final services = await NullYtServices.init(ChannelYtDlpRunner());
  runApp(NullYtApp(services: services));
}
