package com.lucky9.app.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Persists uncaught crash traces to a file in app-internal storage so they
 * can be displayed back to the user on the next launch (helpful when no
 * adb/logcat is available).
 */
public final class CrashHandler {

    private static final String TAG = "Lucky9";
    private static final String FILE_NAME = "lucky9_last_crash.txt";

    private CrashHandler() {}

    public static void install(Context appContext) {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Context ctx = appContext.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                writeTrace(ctx, thread, throwable);
            } catch (Throwable ignored) {
                // Don't let logging itself crash the crash handler.
            }
            Log.e(TAG, "uncaught", throwable);
            if (previous != null) previous.uncaughtException(thread, throwable);
        });
    }

    public static String readLastCrash(Context ctx) {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (!f.exists()) return null;
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Throwable ignored) {
            return null;
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public static void clear(Context ctx) {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (f.exists() && !f.delete()) {
            Log.w(TAG, "could not delete crash file");
        }
    }

    private static void writeTrace(Context ctx, Thread thread, Throwable t) throws Exception {
        File dir = ctx.getFilesDir();
        if (dir == null) return;
        File f = new File(dir, FILE_NAME);
        try (FileWriter fw = new FileWriter(f, false); PrintWriter pw = new PrintWriter(fw)) {
            pw.println("Lucky 9 crash @ " + new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            pw.println("Thread: " + thread.getName());
            pw.println();
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            pw.println(sw.toString());
        }
    }
}
