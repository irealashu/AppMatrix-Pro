package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class AppManagerRepository(private val context: Context, private val db: AppMatrixDatabase) {

    private val packageManager: PackageManager = context.packageManager
    private val dao = db.appNoteDao()
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    fun getAppNotesFlow(): Flow<Map<String, AppNoteEntity>> {
        return dao.getAllNotes().map { list ->
            list.associateBy { it.packageName }
        }
    }

    suspend fun saveAppNote(packageName: String, note: String, tags: String) {
        val existing = dao.getNoteForPackage(packageName)
        val updated = existing?.copy(note = note, customTags = tags)
            ?: AppNoteEntity(packageName = packageName, note = note, customTags = tags)
        dao.upsertNote(updated)
    }

    suspend fun loadInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
        } catch (e: Exception) {
            emptyList()
        }

        packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val appName = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val apkFile = File(appInfo.publicSourceDir ?: appInfo.sourceDir ?: "")
            val apkSize = if (apkFile.exists()) apkFile.length() else 0L

            val installTime = pkg.firstInstallTime
            val updateTime = pkg.lastUpdateTime

            val targetSdk = appInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21

            val category = when {
                isSystem -> AppCategory.SYSTEM
                else -> AppCategory.USER
            }

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val rawVersionName = pkg.versionName?.takeIf { it.isNotBlank() } ?: "$vCode"

            AppModel(
                packageName = pkg.packageName,
                appName = appName,
                versionName = rawVersionName,
                versionCode = vCode,
                isSystemApp = isSystem,
                installTimeMs = installTime,
                updateTimeMs = updateTime,
                apkSizeBytes = apkSize,
                dataSizeEstimateBytes = (apkSize * 0.35).toLong(),
                category = category,
                targetSdkVersion = targetSdk,
                minSdkVersion = minSdk,
                customNote = ""
            )
        }.sortedBy { it.appName.lowercase() }
    }

    suspend fun extractApk(packageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val srcFile = File(appInfo.publicSourceDir ?: appInfo.sourceDir ?: return@withContext null)
            if (!srcFile.exists()) return@withContext null

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputDir = File(downloadsDir, "AppMatrix_APKs")
            if (!outputDir.exists()) outputDir.mkdirs()

            val appLabel = try {
                packageManager.getApplicationLabel(appInfo).toString().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            } catch (e: Exception) {
                packageName
            }

            val destFile = File(outputDir, "${appLabel}_${packageName}.apk")

            FileInputStream(srcFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openAppDetailsSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun requestUninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                val fallbackIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.parse("package:$packageName")
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun getAppIconBitmap(packageName: String): ImageBitmap? {
        val cached = iconCache[packageName]
        if (cached != null) return cached

        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val drawable: Drawable = appInfo.loadIcon(packageManager)
            val targetSize = 144
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, targetSize, targetSize)
            drawable.draw(canvas)
            val imgBmp = bitmap.asImageBitmap()
            iconCache[packageName] = imgBmp
            imgBmp
        } catch (e: Exception) {
            try {
                val drawable = packageManager.getApplicationIcon(packageName)
                val targetSize = 144
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, targetSize, targetSize)
                drawable.draw(canvas)
                val imgBmp = bitmap.asImageBitmap()
                iconCache[packageName] = imgBmp
                imgBmp
            } catch (e2: Exception) {
                null
            }
        }
    }

    suspend fun calculateAppSha256(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext "N/A"
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error calculating hash"
        }
    }
}
