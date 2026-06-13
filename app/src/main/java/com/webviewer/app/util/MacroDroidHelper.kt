package com.webviewer.app.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object MacroDroidHelper {

    private const val TAG = "MacroDroidHelper"
    private const val PKG = "com.arlosoft.macrodroid"

    /**
     * Apre MacroDroid — la bolla parte automaticamente all'avvio.
     * Usato al 1° long press della ⚙.
     */
    fun openApp(context: Context, packageName: String = PKG): Boolean {
        if (!isInstalled(context, packageName)) {
            Log.w(TAG, "MacroDroid non installato")
            return false
        }
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "MacroDroid aperto")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore apertura MacroDroid", e)
            false
        }
    }

    /**
     * Chiude MacroDroid — la bolla scompare.
     * Usato al 2° long press della ⚙.
     * killBackgroundProcesses richiede KILL_BACKGROUND_PROCESSES nel manifest.
     */
    fun closeApp(context: Context, packageName: String = PKG): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            Log.d(TAG, "MacroDroid chiuso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore chiusura MacroDroid", e)
            false
        }
    }

    fun isInstalled(context: Context, packageName: String = PKG): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Mantenuti per compatibilità con SettingsActivity
    fun triggerMacro(context: Context, packageName: String, macroName: String) =
        openApp(context, packageName)

    fun stopMacro(context: Context, packageName: String, macroName: String) =
        closeApp(context, packageName)
}
