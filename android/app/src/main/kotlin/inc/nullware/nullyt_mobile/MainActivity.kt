package inc.nullware.nullyt_mobile

import android.os.Handler
import android.os.Looper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.Executors

/**
 * Hosts yt-dlp on Android. youtubedl-android ships a Python runtime + yt-dlp
 * inside the APK; we expose it to Dart on the `nullyt/ytdlp` channel so the
 * shared core can use the exact same command lines as on desktop.
 */
class MainActivity : FlutterActivity() {
    private val pool = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var initialized = false

    private fun ensureInit() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                YoutubeDL.init(applicationContext)
                initialized = true
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "nullyt/ytdlp").setMethodCallHandler { call, result ->
            pool.execute {
                try {
                    ensureInit()
                    val value: Any? = when (call.method) {
                        "init" -> null
                        "version" -> YoutubeDL.version(applicationContext)
                        "update" -> YoutubeDL.updateYoutubeDL(applicationContext)?.name
                        "run" -> {
                            val args = call.argument<List<String>>("args") ?: emptyList()
                            val req = YoutubeDLRequest(emptyList<String>()).addCommands(args)
                            YoutubeDL.execute(req).out
                        }
                        else -> {
                            main.post { result.notImplemented() }
                            return@execute
                        }
                    }
                    main.post { result.success(value) }
                } catch (e: Exception) {
                    main.post { result.error("YTDLP", e.message ?: e.toString(), null) }
                }
            }
        }
    }
}
