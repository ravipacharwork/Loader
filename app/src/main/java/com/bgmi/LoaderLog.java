package com.bgmi;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight on-device logger used to diagnose black-screen / launch failures.
 * Writes a rolling plain-text log that the user can open, share or copy from the app.
 */
public final class LoaderLog {

    private static final String TAG = "LoaderLog";
    private static final long MAX_BYTES = 1024 * 1024;
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
    private static final ArrayDeque<String> MEMORY = new ArrayDeque<>();

    private static volatile File sLogFile;
    private static volatile Context sCtx;
    private static volatile boolean sReady;

    private LoaderLog() {
    }

    public static void init(Context ctx) {
        if (ctx == null) return;
        synchronized (LOCK) {
            if (sReady) return;
            sCtx = ctx.getApplicationContext();
            try {
                File dir = null;
                try {
                    File download = new File("/storage/emulated/0/Download/FeaturesticLogs");
                    if (download.isDirectory() || download.mkdirs()) {
                        dir = download;
                    }
                } catch (Throwable ignored) {
                }
                if (dir == null) {
                    File ext = sCtx.getExternalFilesDir(null);
                    if (ext != null) {
                        dir = new File(ext, "logs");
                    }
                }
                if (dir == null) {
                    dir = new File(sCtx.getFilesDir(), "logs");
                }
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, "loader.log");
                if (file.length() > MAX_BYTES) {
                    File old = new File(dir, "loader.old.log");
                    old.delete();
                    file.renameTo(old);
                }
                sLogFile = file;
                sReady = true;
            } catch (Throwable t) {
                Log.e(TAG, "init failed", t);
            }
        }
        i("SESSION", "===== new session =====");
        i("DEVICE", Build.MANUFACTURER + " " + Build.MODEL
                + " | Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                + " | " + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?"));
        i("PROCESS", currentProcessTag());
    }

    public static File getLogFile() {
        return sLogFile;
    }

    public static String getDirPath() {
        File f = sLogFile;
        return f != null ? f.getParent() : "(unavailable)";
    }

    public static void d(String tag, String msg) {
        line("D", tag, msg, null);
    }

    public static void i(String tag, String msg) {
        line("I", tag, msg, null);
    }

    public static void w(String tag, String msg) {
        line("W", tag, msg, null);
    }

    public static void e(String tag, String msg) {
        line("E", tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable t) {
        line("E", tag, msg, t);
    }

    public static void crash(Throwable t) {
        line("F", "CRASH", "uncaught exception", t);
    }

    private static void line(String level, String tag, String msg, Throwable t) {
        String time;
        synchronized (FMT) {
            time = FMT.format(new Date());
        }
        StringBuilder sb = new StringBuilder(160);
        sb.append(time).append(' ').append(level).append('/').append(tag).append(": ").append(msg);
        if (t != null) {
            sb.append('\n').append(Log.getStackTraceString(t));
        }
        String entry = sb.toString();
        synchronized (MEMORY) {
            MEMORY.addLast(entry);
            while (MEMORY.size() > 800) {
                MEMORY.removeFirst();
            }
        }
        synchronized (LOCK) {
            File f = sLogFile;
            if (f == null) return;
            try (FileOutputStream fos = new FileOutputStream(f, true)) {
                fos.write((entry + "\n").getBytes(StandardCharsets.UTF_8));
                fos.flush();
            } catch (Throwable ignored) {
            }
        }
    }

    public static String readTail(int maxChars) {
        File f = sLogFile;
        StringBuilder out = new StringBuilder();
        if (f != null && f.isFile()) {
            try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                long len = raf.length();
                long start = Math.max(0, len - maxChars);
                raf.seek(start);
                byte[] buf = new byte[(int) (len - start)];
                raf.readFully(buf);
                out.append(new String(buf, StandardCharsets.UTF_8));
            } catch (Throwable t) {
                out.append("read error: ").append(t);
            }
        }
        if (out.length() == 0) {
            synchronized (MEMORY) {
                for (String s : MEMORY) {
                    out.append(s).append('\n');
                }
            }
        }
        return out.toString();
    }

    public static void clear() {
        synchronized (MEMORY) {
            MEMORY.clear();
        }
        synchronized (LOCK) {
            File f = sLogFile;
            if (f != null) {
                try (FileOutputStream fos = new FileOutputStream(f, false)) {
                    fos.write(new byte[0]);
                } catch (Throwable ignored) {
                }
            }
        }
        i("SESSION", "log cleared");
    }

    /** Best-effort logcat snapshot (may be empty on unrooted Android 10+). */
    public static void captureLogcat() {
        i("LOGCAT", "capture requested");
        new Thread(() -> {
            Process p = null;
            try {
                p = new ProcessBuilder("logcat", "-d", "-v", "time", "-t", "1200").redirectErrorStream(true).start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String l;
                    int lines = 0;
                    while ((l = br.readLine()) != null && lines < 1600) {
                        sb.append(l).append('\n');
                        lines++;
                    }
                }
                i("LOGCAT", sb.length() == 0 ? "(empty - READ_LOGS not granted)" : "captured " + sb.length() + " chars");
                if (sb.length() > 0) {
                    appendBlock("----- logcat snapshot -----\n" + sb + "----- end logcat -----");
                }
            } catch (Throwable t) {
                e("LOGCAT", "capture failed: " + t);
            } finally {
                if (p != null) {
                    try {
                        p.destroy();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, "loader-logcat").start();
    }

    private static void appendBlock(String block) {
        synchronized (LOCK) {
            File f = sLogFile;
            if (f == null) return;
            try (FileOutputStream fos = new FileOutputStream(f, true)) {
                fos.write((block + "\n").getBytes(StandardCharsets.UTF_8));
                fos.flush();
            } catch (Throwable ignored) {
            }
        }
        synchronized (MEMORY) {
            MEMORY.addLast(block);
        }
    }

    private static String currentProcessTag() {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return android.app.Application.getProcessName();
            }
            BufferedReader br = new BufferedReader(new java.io.FileReader("/proc/self/cmdline"));
            try {
                String line = br.readLine();
                return line != null ? line.replace("\u0000", "") : "?";
            } finally {
                br.close();
            }
        } catch (Throwable t) {
            return "?";
        }
    }

    public static Context ctx() {
        return sCtx;
    }
}
