package com.bgmi;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Local companion-tool compatibility layer.
 * Mirrors the small subset of FeaturesticCompanionToolManager that this app relies on,
 * so the loader can build against SDK releases where those helpers were removed.
 */
public final class FboxCompat {

    private static volatile String sNotifiedCompanionPkg = "";

    private FboxCompat() {
    }

    public static final class DownloadApkCandidate {
        public final File file;
        public final String packageName;
        public final String label;
        public final boolean has64BitLibs;
        public final boolean has32BitLibs;
        public final boolean hasGgInternalSignature;
        public final int priorityScore;
        public final long lastModified;

        public DownloadApkCandidate(File file, String packageName, String label,
                                    boolean has64BitLibs, boolean has32BitLibs,
                                    boolean hasGgInternalSignature, int priorityScore,
                                    long lastModified) {
            this.file = file;
            this.packageName = packageName != null ? packageName : "";
            this.label = label != null ? label : (file != null ? file.getName() : "");
            this.has64BitLibs = has64BitLibs;
            this.has32BitLibs = has32BitLibs;
            this.hasGgInternalSignature = hasGgInternalSignature;
            this.priorityScore = priorityScore;
            this.lastModified = lastModified;
        }

        public String getArchBadge() {
            if (has64BitLibs && has32BitLibs) return "32/64-Bit";
            if (has64BitLibs) return "64-Bit";
            if (has32BitLibs) return "32-Bit";
            return "32/64-Bit Universal";
        }
    }

    public static void notifyCompanionPackageInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        sNotifiedCompanionPkg = packageName;
        try {
            Context ctx = com.featurestic.box.FeaturesticBoxCore.getContext();
            if (ctx != null) {
                ctx.getSharedPreferences("companion_tool", Context.MODE_PRIVATE)
                        .edit().putString("companion_active_u", packageName).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isKnownCompanionPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (packageName.equals(sNotifiedCompanionPkg)) return true;
        String low = packageName.toLowerCase(Locale.US);
        return low.contains("catch_.me_")
                || low.contains("gameguardian")
                || low.startsWith("com.gg.");
    }

    public static boolean isExcludedGameOrSystemPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return true;
        try {
            String host = com.featurestic.box.FeaturesticBoxCore.getHostPkg();
            if (host != null && host.equalsIgnoreCase(packageName)) return true;
        } catch (Throwable ignored) {
        }
        String low = packageName.toLowerCase(Locale.US);
        return low.startsWith("com.google.")
                || low.startsWith("com.android.")
                || low.equals("android")
                || low.startsWith("com.pubg.")
                || low.startsWith("com.tencent.")
                || low.startsWith("com.dts.freefire")
                || low.startsWith("com.activision.")
                || low.startsWith("com.garena.")
                || low.startsWith("com.mobile.legends")
                || low.startsWith("com.krafton.")
                || low.startsWith("com.proxima.")
                || low.startsWith("com.facebook.")
                || low.startsWith("com.twitter.")
                || low.startsWith("com.whatsapp")
                || low.startsWith("com.instagram.")
                || low.startsWith("org.telegram.");
    }

    public static File findBestApkFileInDownloads() {
        DownloadApkCandidate best = findBestApkInDownloads();
        return best != null ? best.file : null;
    }

    public static DownloadApkCandidate findBestApkInDownloads() {
        List<DownloadApkCandidate> all = scanAllCandidateApksInDownloads();
        return all.isEmpty() ? null : all.get(0);
    }

    public static List<DownloadApkCandidate> scanAllCandidateApksInDownloads() {
        List<DownloadApkCandidate> out = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null) dirs.add(downloads);
        } catch (Throwable ignored) {
        }
        dirs.add(new File("/sdcard/Download"));
        dirs.add(new File("/storage/emulated/0/Download"));
        dirs.add(new File("/sdcard/Download/Telegram"));
        dirs.add(new File("/storage/emulated/0/Download/Telegram"));
        dirs.add(new File("/sdcard/APK"));
        dirs.add(new File("/storage/emulated/0/APK"));

        Set<String> seen = new HashSet<>();
        for (File dir : dirs) {
            if (dir == null || !dir.exists() || !dir.isDirectory()) continue;
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f == null) continue;
                if (f.isDirectory()) {
                    File[] nested = f.listFiles();
                    if (nested == null) continue;
                    for (File n : nested) inspect(n, seen, out);
                } else {
                    inspect(f, seen, out);
                }
            }
        }

        Collections.sort(out, new Comparator<DownloadApkCandidate>() {
            @Override
            public int compare(DownloadApkCandidate a, DownloadApkCandidate b) {
                if (a.priorityScore != b.priorityScore) {
                    return b.priorityScore - a.priorityScore;
                }
                return Long.compare(b.lastModified, a.lastModified);
            }
        });
        return out;
    }

    private static void inspect(File f, Set<String> seen, List<DownloadApkCandidate> out) {
        if (f == null || !f.isFile()) return;
        String name = f.getName().toLowerCase(Locale.US);
        if (!name.endsWith(".apk")) return;
        String key;
        try {
            key = f.getCanonicalPath();
        } catch (Throwable t) {
            key = f.getAbsolutePath();
        }
        if (!seen.add(key)) return;

        boolean has64 = false;
        boolean has32 = false;
        try (ZipFile zip = new ZipFile(f)) {
            java.util.Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                String entry = en.nextElement().getName();
                if (entry.startsWith("lib/arm64-v8a/")) has64 = true;
                else if (entry.startsWith("lib/armeabi-v7a/")) has32 = true;
                if (has64 && has32) break;
            }
        } catch (Throwable ignored) {
        }

        int score = 0;
        if (name.contains("gameguardian") || name.contains("gg")) score += 10;
        if (has64 && has32) score += 2;
        if (name.contains("virtual") || name.contains("clone")) score += 1;

        out.add(new DownloadApkCandidate(f, "", f.getName(), has64, has32,
                false, score, f.lastModified()));
    }
}
