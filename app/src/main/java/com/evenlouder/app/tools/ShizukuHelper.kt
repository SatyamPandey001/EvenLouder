package com.evenlouder.app.tools

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    const val SHIZUKU_REQ_CODE = 1002

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int = SHIZUKU_REQ_CODE) {
        try {
            if (isShizukuAvailable() && !hasShizukuPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
        }
    }

    fun registerPermissionListener(onGranted: () -> Unit) {
        try {
            Shizuku.addRequestPermissionResultListener { _: Int, grantResult: Int ->
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error registering Shizuku listener", e)
        }
    }

    fun grantDumpPermissionViaShizuku(context: Context): Boolean {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            Log.w(TAG, "Shizuku is not running or permission is not granted")
            return false
        }
        return try {
            val pkg = context.packageName
            val cmd = arrayOf("sh", "-c", "pm grant $pkg android.permission.DUMP")

            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, cmd, null, null) as Process
            val exitCode = process.waitFor()
            Log.i(TAG, "Shizuku pm grant exit code: $exitCode")
            exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Failed executing pm grant via Shizuku", e)
            false
        }
    }
}
