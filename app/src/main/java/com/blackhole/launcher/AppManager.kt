package com.blackhole.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object AppManager {

    fun loadInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<AppInfo>()

        for (ri in resolveInfos) {
            val packageName = ri.activityInfo.packageName
            // Excluirse a sí mismo de la lista de apps flotantes
            if (packageName == context.packageName) continue

            val label = ri.loadLabel(pm).toString()
            val drawable = ri.loadIcon(pm)
            val bitmap = drawableToBitmap(drawable)

            appList.add(
                AppInfo(
                    label = label,
                    packageName = packageName,
                    activityName = ri.activityInfo.name,
                    icon = bitmap
                )
            )
        }

        return appList.sortedBy { it.label.lowercase() }
    }

    fun launchApp(context: Context, app: AppInfo) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "No se pudo abrir ${app.label}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error abriendo ${app.label}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openHomeSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Abre Ajustes > Apps > Apps Predeterminadas", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
