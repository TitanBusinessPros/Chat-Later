package com.titanbusinesspros.chatlater

import android.content.Context
import java.io.File

private const val CRASH_FILE_NAME = "last_crash.txt"

// Installs a handler that saves crash details to a file right before the app
// closes, so the *next* launch can show exactly what went wrong on screen -
// no computer, cable, or developer tools needed to see it.
fun installCrashReporter(context: Context) {
    val appContext = context.applicationContext
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            File(appContext.filesDir, CRASH_FILE_NAME).writeText(
                "${throwable.javaClass.name}: ${throwable.message}\n\n" +
                    throwable.stackTraceToString()
            )
        } catch (e: Exception) {
            // If saving the crash itself fails, there's nothing more we can do here.
        }
        previousHandler?.uncaughtException(thread, throwable)
    }
}

// Reads and clears whatever crash was saved on the previous launch, if any.
fun readAndClearLastCrash(context: Context): String? {
    val file = File(context.applicationContext.filesDir, CRASH_FILE_NAME)
    if (!file.exists()) return null
    val text = file.readText()
    file.delete()
    return text
}
