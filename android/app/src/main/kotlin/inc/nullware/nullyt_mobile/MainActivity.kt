package inc.nullware.nullyt_mobile

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
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
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "nullyt/update").setMethodCallHandler { call, result ->
            when (call.method) {
                "abis" -> result.success(Build.SUPPORTED_ABIS.toList())
                "installApk" -> {
                    val path = call.argument<String>("path")
                    if (path == null) { result.error("UPDATE", "no path", null); return@setMethodCallHandler }
                    pool.execute {
                        try {
                            installApk(File(path))
                            main.post { result.success(null) }
                        } catch (e: Exception) {
                            main.post { result.error("UPDATE", e.message ?: e.toString(), null) }
                        }
                    }
                }
                else -> result.notImplemented()
            }
        }
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

    /**
     * Streams the APK into a PackageInstaller session. Android then asks the
     * user to confirm (and, the first time, to allow installs from Null YT);
     * the result comes back to [onNewIntent].
     */
    private fun installApk(apk: File) {
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("update.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(this, MainActivity::class.java).setAction(ACTION_INSTALL_STATUS)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getActivity(this, id, intent, flags).intentSender)
        }
        apk.delete()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != ACTION_INSTALL_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            else intent.getParcelableExtra(Intent.EXTRA_INTENT)
            confirm?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    companion object {
        private const val ACTION_INSTALL_STATUS = "inc.nullware.nullyt.INSTALL_STATUS"
    }
}
