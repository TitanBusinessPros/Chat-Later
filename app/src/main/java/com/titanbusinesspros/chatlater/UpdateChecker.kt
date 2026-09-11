package com.titanbusinesspros.chatlater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionName: String, val apkUrl: String, val notes: String)

// Hosted on the same GitHub Pages site as the download page. Bump "versionCode" here
// every time a new APK is released so installed apps know a newer one exists.
private const val VERSION_CHECK_URL = "https://titanbusinesspros.github.io/Chat-Later/version.json"

// Checks version.json for a versionCode newer than the one installed on this device.
// Calls onResult(null) if already up to date or the check fails (e.g. no internet) -
// this never blocks or breaks the app, it just silently skips the update banner.
fun checkForUpdate(context: Context, onResult: (UpdateInfo?) -> Unit) {
    Thread {
        val result = try {
            val installedCode = PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0)
            )
            val body = (URL(VERSION_CHECK_URL).openConnection() as HttpURLConnection).run {
                connectTimeout = 8000
                readTimeout = 8000
                inputStream.bufferedReader().use { it.readText() }
            }
            val json = JSONObject(body)
            val latestCode = json.getLong("versionCode")
            if (latestCode > installedCode) {
                UpdateInfo(
                    versionName = json.optString("versionName", ""),
                    apkUrl = json.getString("apkUrl"),
                    notes = json.optString("notes", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        Handler(Looper.getMainLooper()).post { onResult(result) }
    }.start()
}

// Shown when version.json on the download site reports a newer build than what's installed.
@Composable
fun UpdateBanner(update: UpdateInfo) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Update available" + if (update.versionName.isNotBlank()) " (v${update.versionName})" else "")
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
        }) {
            Text("Download")
        }
    }
}
