package com.bgmi;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.bgmi.utils.Prefs;
import com.featurestic.box.FeaturesticAntiCrackVault;
import com.featurestic.box.FeaturesticBoxCore;
import com.featurestic.box.app.BActivityThread;
import com.featurestic.box.app.configuration.AppLifecycleCallback;
import com.featurestic.box.app.configuration.ClientConfiguration;
import com.featurestic.box.core.env.BEnvironment;
import com.featurestic.box.utils.compat.DexFileCompat;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Obfuscate
public class BoxApplication extends Application {

    static {
        try {
            System.loadLibrary("zenin");
        } catch (Throwable ignored) {
        }
    }

    public static native String getSdkKey();
    public static native void installNativeOffsetCrashShield(String crashLogPath);
    public static native void scrubLoadedPluginElfHeaderInRam(String soPath);

    public static File getSharedOffsetCrashLogFile() {
        try {
            File sysDir = BEnvironment.getSystemDir();
            if (sysDir != null) {
                if (!sysDir.exists()) sysDir.mkdirs();
                return new File(sysDir, "featurestic_offset_crash.log");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void installAutomaticCrashLogger() {
        try {
            File crashFile = getSharedOffsetCrashLogFile();
            if (crashFile == null) {
                crashFile = new File(getFilesDir(), "featurestic_offset_crash.log");
            }
            final File targetCrashFile = crashFile;
            try {
                installNativeOffsetCrashShield(targetCrashFile.getAbsolutePath());
            } catch (Throwable ignored) {
            }

            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try {
                    LoaderLog.crash(throwable);
                    boolean isMain = android.os.Looper.getMainLooper() != null
                            && android.os.Looper.getMainLooper().getThread() == thread;
                    String stack = Log.getStackTraceString(throwable);
                    String report = "Crash Type   : Java/JNI Exception (" + throwable.getClass().getSimpleName() + ")\n"
                            + "Thread Name  : " + (thread != null ? thread.getName() : "unknown") + "\n"
                            + "Message      : " + (throwable.getMessage() != null ? throwable.getMessage() : "N/A") + "\n"
                            + "Status       : " + (isMain ? "Main Thread Fault" : "Background Thread Isolated (Game Kept Alive)") + "\n"
                            + "Stack Trace  :\n" + (stack.length() > 900 ? stack.substring(0, 900) : stack);
                    try (FileOutputStream fos = new FileOutputStream(targetCrashFile, false)) {
                        fos.write(report.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        fos.flush();
                    }
                    // If a background thread threw an exception, absorb it so the game does NOT crash!
                    if (!isMain) {
                        Log.w(TAG, "Isolated background thread crash: " + throwable);
                        return;
                    }
                } catch (Throwable ignored) {
                }
                if (prev != null) {
                    prev.uncaughtException(thread, throwable);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static final String TAG = "BoxApplication";
    public static final String EXPECTED_SIGNATURE_HASH =
            "E545B00E31AF8FC008566DE0B87C0B17FC467A90AEED71B00755B98698AE58BC";

    private static final Set<String> sLoadedSoNamesInProcess =
            Collections.synchronizedSet(new HashSet<>());

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        Prefs prefs = new Prefs(base);
        try {
            base.getPackageManager();
        } catch (Throwable ignored) {
        }

        try {
            FeaturesticBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean setHideRoot() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LoaderLog.init(this);
        LoaderLog.i("App", "BoxApplication.onCreate pid=" + android.os.Process.myPid()
                + " process=" + LoaderLog.ctx());
        FeaturesticBoxCore.get().doCreate();
        FeaturesticBoxCore.setHideXposed(true);

        installAutomaticCrashLogger();

        if (FeaturesticBoxCore.get().isMainProcess()) {
            try {
                if (isValidSignature()) {
                    Log.d(TAG, "Valid signature verified, activating box...");
                    LoaderLog.i("App", "signature OK, box activated");
                } else {
                    Log.w(TAG, "Signature verification failed!");
                    LoaderLog.w("App", "signature verification FAILED");
                }
            } catch (Throwable t) {
                Log.w(TAG, "Signature check skipped", t);
            }
        }

        FeaturesticBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
            @Override
            public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                LoaderLog.i("VirtualApp", "beforeCreateApplication pkg=" + packageName
                        + " process=" + processName + " userId=" + userId);
            }

            @Override
            public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                // Do not load custom .so before Application.onCreate; wait until engine & native libs finish JNI_OnLoad
            }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                LoaderLog.i("VirtualApp", "afterApplicationOnCreate pkg=" + packageName
                        + " process=" + processName + " app=" + (application != null ? application.getClass().getName() : "null"));
                installAutomaticCrashLogger();
                if (isTargetGamePackage(packageName)
                        && (processName == null || processName.isEmpty() || packageName.equals(processName))) {
                    loadUserSelectedCustomSoIfEnabled(application);
                }
            }
        });
    }

    private static boolean isTargetGamePackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        String low = packageName.toLowerCase(Locale.US);
        if (FboxCompat.isKnownCompanionPackage(packageName)) {
            return false;
        }
        if (low.startsWith("com.google.android.")
                || low.startsWith("com.android.")
                || low.equals("com.featurestic.box")
                || low.equals("com.bgmi")) {
            return false;
        }
        return true;
    }

    private static final byte[] RAM_VAULT_MAGIC = new byte[]{
            'F', 'B', 'O', 'X', '_', 'R', 'A', 'M', '_', 'V', '1', 0
    };
    private static final byte[] RAM_VAULT_KEY = new byte[]{
            (byte) 0x7A, (byte) 0x4E, (byte) 0x91, (byte) 0x2C,
            (byte) 0xD5, (byte) 0x19, (byte) 0x63, (byte) 0xE8,
            (byte) 0x5B, (byte) 0xAF, (byte) 0x34, (byte) 0x82,
            (byte) 0xC6, (byte) 0x0D, (byte) 0xF7, (byte) 0x6E
    };

    /**
     * Returns 64 or 32 for both standard ELF .so files and FBOX_RAM_V1 encrypted online .so vaults.
     */
    public static int readSoOrVaultArchBit(File soFile) {
        if (soFile == null || !soFile.isFile() || soFile.length() < 20) {
            return 0;
        }
        try (FileInputStream fis = new FileInputStream(soFile)) {
            byte[] header = new byte[16];
            if (fis.read(header) < 16) return 0;
            if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                int cls = header[4] & 0xFF;
                return cls == 2 ? 64 : (cls == 1 ? 32 : 0);
            }
            boolean isVault = true;
            for (int i = 0; i < RAM_VAULT_MAGIC.length; i++) {
                if (header[i] != RAM_VAULT_MAGIC[i]) {
                    isVault = false;
                    break;
                }
            }
            if (isVault) {
                int cls = header[12] & 0xFF;
                return cls == 2 ? 64 : (cls == 1 ? 32 : 0);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * Encrypts an online .so file in place so no plaintext ELF .so sits on disk for users to extract.
     */
    public static void encryptOnlineSoInPlace(File soFile) {
        if (soFile == null || !soFile.isFile() || soFile.length() < 64) return;
        try {
            byte[] raw = new byte[(int) soFile.length()];
            try (FileInputStream fis = new FileInputStream(soFile)) {
                int off = 0;
                while (off < raw.length) {
                    int r = fis.read(raw, off, raw.length - off);
                    if (r <= 0) break;
                    off += r;
                }
            }
            // Only encrypt if it is currently a plaintext ELF
            if (raw[0] != 0x7F || raw[1] != 'E' || raw[2] != 'L' || raw[3] != 'F') {
                return;
            }
            int eiClass = raw[4] & 0xFF;
            for (int i = 0; i < raw.length; i++) {
                int k = (RAM_VAULT_KEY[i & 15] & 0xFF) ^ ((i * 31) & 0xFF) ^ 0xA7;
                raw[i] = (byte) ((raw[i] & 0xFF) ^ k);
            }
            soFile.setWritable(true, true);
            try (FileOutputStream fos = new FileOutputStream(soFile, false)) {
                fos.write(RAM_VAULT_MAGIC);
                fos.write((byte) eiClass);
                fos.write((byte) 0x5A);
                fos.write((byte) 0xC3);
                fos.write((byte) 0x1F);
                fos.write(raw);
                fos.flush();
            }
            java.util.Arrays.fill(raw, (byte) 0);
            // Lock to owner-only (0600)
            soFile.setReadable(false, false);
            soFile.setReadable(true, true);
            soFile.setWritable(false, false);
            soFile.setWritable(true, true);
            soFile.setExecutable(false, false);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Decrypts a vault .so (or reads a plain .so) strictly into a RAM byte[] buffer.
     */
    private static byte[] readAndDecryptSoToRam(File srcFile) {
        if (srcFile == null || !srcFile.isFile() || srcFile.length() < 20) return null;
        try {
            byte[] data = new byte[(int) srcFile.length()];
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                int off = 0;
                while (off < data.length) {
                    int r = fis.read(data, off, data.length - off);
                    if (r <= 0) break;
                    off += r;
                }
            }
            boolean isVault = data.length > 16;
            if (isVault) {
                for (int i = 0; i < RAM_VAULT_MAGIC.length; i++) {
                    if (data[i] != RAM_VAULT_MAGIC[i]) {
                        isVault = false;
                        break;
                    }
                }
            }
            if (!isVault) {
                return data;
            }
            int payloadLen = data.length - 16;
            byte[] plain = new byte[payloadLen];
            for (int i = 0; i < payloadLen; i++) {
                int k = (RAM_VAULT_KEY[i & 15] & 0xFF) ^ ((i * 31) & 0xFF) ^ 0xA7;
                plain[i] = (byte) ((data[i + 16] & 0xFF) ^ k);
            }
            java.util.Arrays.fill(data, (byte) 0);
            return plain;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Checks if `soFile` is a valid ELF or encrypted FBOX_RAM_V1 .so whose 32/64-bit class matches the
     * current process architecture (`FeaturesticBoxCore.is64Bit()`).
     */
    public static boolean isCompatibleElfSoFile(File soFile) {
        int bits = readSoOrVaultArchBit(soFile);
        if (bits != 64 && bits != 32) {
            return false;
        }
        boolean process64 = FeaturesticBoxCore.is64Bit();
        return process64 ? (bits == 64) : (bits == 32);
    }

    /**
     * Resolves the ordered list of `.so` files (Manual and/or Online) selected by the user
     * to be loaded into the virtual game process.
     */
    private List<File> resolveSelectedSoFilesToLoad() {
        Set<String> seenPaths = new LinkedHashSet<>();
        List<File> result = new ArrayList<>();
        try {
            File soDir = new File(getFilesDir(), "custom_so");
            File listConf = new File(soDir, "active_so_list.conf");
            if (listConf.isFile()) {
                boolean enabled = true;
                List<File> fromList = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(listConf))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (line.startsWith("ENABLED=")) {
                            enabled = "1".equals(line.substring("ENABLED=".length()).trim());
                        } else if (line.startsWith("PATH=")) {
                            String p = line.substring("PATH=".length()).trim();
                            File f = new File(p);
                            if (isCompatibleElfSoFile(f) && seenPaths.add(f.getAbsolutePath())) {
                                fromList.add(f);
                            }
                        }
                    }
                }
                if (!enabled) {
                    return Collections.emptyList();
                }
                if (!fromList.isEmpty()) {
                    return fromList;
                }
            }

            // Fallback 1: Legacy single active_so.conf + active_custom.so
            File confFile = new File(soDir, "active_so.conf");
            File activeSoFile = new File(soDir, "active_custom.so");
            if (confFile.isFile() && isCompatibleElfSoFile(activeSoFile)) {
                boolean enabled = false;
                try (BufferedReader br = new BufferedReader(new FileReader(confFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("ENABLED=")) {
                            enabled = "1".equals(line.substring("ENABLED=".length()).trim());
                        }
                    }
                }
                if (enabled && seenPaths.add(activeSoFile.getAbsolutePath())) {
                    result.add(activeSoFile);
                    return result;
                }
            }

            // Clean Mode by default: if the user has not explicitly enabled a .so in active_so_list.conf
            // or active_so.conf, do NOT auto-inject files/loader/*.so so the game works 100% in Clean Mode.
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static void collectCompatibleSoInDir(File dir, List<File> out, Set<String> seen) {
        if (dir == null || !dir.isDirectory()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f == null) continue;
            if (f.isDirectory()) {
                collectCompatibleSoInDir(f, out, seen);
            } else if (f.getName().toLowerCase(Locale.US).endsWith(".so")
                    && isCompatibleElfSoFile(f)
                    && seen.add(f.getAbsolutePath())) {
                out.add(f);
            }
        }
    }

    /**
     * Stages and loads all user-selected `.so` plugins (Manual and/or Online) inside the
     * virtual game process (`:pN`):
     * 1. Stages all selected `.so` files into BOTH `featurestic_so` AND `BEnvironment.getAppLibDir(pkg)`
     *    with Android 10–16 W^X-compliant `0555` (`r-xr-xr-x`) permissions before calling `System.load`
     *    so any inter-library `DT_NEEDED` dependencies resolve automatically.
     * 2. Registers the staging directory with `DexFileCompat.addNativeLibraryDirectories`.
     * 3. Loads each `.so` once per process in the user's chosen order.
     */
    public void loadUserSelectedCustomSoIfEnabled(Context targetCtx) {
        try {
            List<File> selectedSoFiles = resolveSelectedSoFilesToLoad();
            if (selectedSoFiles.isEmpty()) {
                return;
            }

            Context workCtx = targetCtx != null ? targetCtx : this;
            File execDir = workCtx.getDir("featurestic_so", Context.MODE_PRIVATE);
            if (!execDir.exists()) {
                execDir.mkdirs();
            }
            execDir.setReadable(true, false);
            execDir.setExecutable(true, false);

            String virtualPkg = BActivityThread.isThreadInit()
                    ? BActivityThread.getAppPackageName() : null;
            File appLibDir = (virtualPkg != null && !virtualPkg.isEmpty())
                    ? BEnvironment.getAppLibDir(virtualPkg) : null;
            if (appLibDir != null && !appLibDir.exists()) {
                appLibDir.mkdirs();
            }

            try {
                if (workCtx.getClassLoader() != null) {
                    if (appLibDir != null) {
                        DexFileCompat.addNativeLibraryDirectories(workCtx.getClassLoader(), execDir, appLibDir);
                    } else {
                        DexFileCompat.addNativeLibraryDirectories(workCtx.getClassLoader(), execDir);
                    }
                }
            } catch (Throwable ignored) {
            }

            // Encrypt any unencrypted online .so files in loader/ or online_so/ automatically so disk copies cannot be stolen
            for (File srcSo : selectedSoFiles) {
                if (srcSo != null && srcSo.isFile()) {
                    String abs = srcSo.getAbsolutePath();
                    if (abs.contains("/loader/") || abs.contains("/online_so/")) {
                        encryptOnlineSoInPlace(srcSo);
                    }
                }
            }

            // Preload libc++_shared.so if present in guest or host lib dir so NDK C++ STL symbols resolve on all Android versions
            preloadCppSharedIfPresent(appLibDir);

            // Step 2: Wait for game engine (libUE4.so + libanogs.so) to map & settle, then decrypt in RAM,
            // load via memfd / ephemeral unlinked inode, and immediately wipe disk staging so .so stays in RAM only!
            if (!selectedSoFiles.isEmpty()) {
                scheduleEngineReadySoRetry(selectedSoFiles);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Custom .so load warning: " + t.getMessage());
        }
    }

    private static File stageSoFileWxCompliant(File src, File dst) {
        try {
            if (dst.exists()) {
                dst.setWritable(true, true);
                dst.delete();
            }
            try (FileInputStream fis = new FileInputStream(src);
                 FileOutputStream fos = new FileOutputStream(dst)) {
                byte[] buf = new byte[32768];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
                fos.flush();
            }
            // Enforce Android 10-16 W^X policy: read+execute (0555), NOT writable during dlopen
            dst.setReadable(true, false);
            dst.setExecutable(true, false);
            dst.setWritable(false, false);
            return dst;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void preloadCppSharedIfPresent(File appLibDir) {
        try {
            if (appLibDir != null) {
                File guestCpp = new File(appLibDir, "libc++_shared.so");
                if (guestCpp.isFile()) {
                    System.load(guestCpp.getAbsolutePath());
                    return;
                }
            }
            String hostLibDir = getApplicationInfo() != null ? getApplicationInfo().nativeLibraryDir : null;
            if (hostLibDir != null) {
                File hostCpp = new File(hostLibDir, "libc++_shared.so");
                if (hostCpp.isFile()) {
                    System.load(hostCpp.getAbsolutePath());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean tryLoadStagedSo(File srcSoFile) {
        if (srcSoFile == null || !srcSoFile.isFile()) return false;
        String rawName = srcSoFile.getName();
        String cleanName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!cleanName.startsWith("lib")) {
            cleanName = "lib" + cleanName;
        }
        if (!cleanName.endsWith(".so")) {
            cleanName = cleanName + ".so";
        }

        byte[] plainElf = readAndDecryptSoToRam(srcSoFile);
        if (plainElf == null || plainElf.length < 64) return false;

        try {
            Context ctx = FeaturesticBoxCore.getContext();
            String virtualPkg = BActivityThread.isThreadInit()
                    ? BActivityThread.getAppPackageName() : null;
            File appLibDir = (virtualPkg != null && !virtualPkg.isEmpty())
                    ? BEnvironment.getAppLibDir(virtualPkg) : null;
            File baseDir = (appLibDir != null)
                    ? appLibDir
                    : ((ctx != null) ? ctx.getDir("featurestic_so", Context.MODE_PRIVATE) : srcSoFile.getParentFile());
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            // Stage with a clean standard library path and NEVER truncate (O_TRUNC) or unlink (delete)
            // while mapped in the game process:
            // 1. Truncating a mapped .so triggers kernel unmap_mapping_range() -> fatal SIGBUS after sleep(2).
            // 2. Unlinking a mapped .so creates "(deleted)" entries in /proc/self/maps -> flagged by libanogs.so.
            File stagedSo = new File(baseDir, cleanName);
            if (stagedSo.exists()) {
                stagedSo.setWritable(true, true);
            }
            try (FileOutputStream fos = new FileOutputStream(stagedSo, false)) {
                fos.write(plainElf);
                fos.flush();
            }
            stagedSo.setReadable(true, false);
            stagedSo.setExecutable(true, false);
            stagedSo.setWritable(false, false);

            try {
                File cf = getSharedOffsetCrashLogFile();
                if (cf != null) {
                    installNativeOffsetCrashShield(cf.getAbsolutePath());
                }
            } catch (Throwable ignored) {
            }

            System.load(stagedSo.getAbsolutePath());

            try {
                scrubLoadedPluginElfHeaderInRam(stagedSo.getAbsolutePath());
                File cf = getSharedOffsetCrashLogFile();
                if (cf != null) {
                    installNativeOffsetCrashShield(cf.getAbsolutePath());
                }
            } catch (Throwable ignored) {
            }

            Log.i(TAG, "Loaded custom .so cleanly with XOM/SELinux shield: " + cleanName);
            return true;
        } catch (Throwable firstErr) {
            Log.w(TAG, "Deferred .so plugin until game engine loads (" + rawName + "): " + firstErr.getMessage());
            return false;
        } finally {
            // Wipe decrypted ELF bytes from Java heap immediately
            java.util.Arrays.fill(plainElf, (byte) 0);
        }
    }

    private static void scheduleEngineReadySoRetry(final List<File> pendingFiles) {
        Thread watcher = new Thread(() -> {
            // Poll every 50ms with ZERO artificial 2.8s delay!
            // As soon as libanogs.so (or libUE4.so) maps into /proc/self/maps, normalize its Segment 0 (0x0)
            // + Segment 1 (.text) to r-xp and load the custom .so immediately so anogs startup offsets
            // apply on the true base address (0x0) before libanogs.so finishes startup checks!
            for (int attempt = 0; attempt < 200; attempt++) {
                if (isGameEngineMappedInMemory() || attempt >= 12) {
                    try {
                        File cf = getSharedOffsetCrashLogFile();
                        if (cf != null) installNativeOffsetCrashShield(cf.getAbsolutePath());
                    } catch (Throwable ignored) {
                    }
                    boolean allDone = true;
                    for (File f : pendingFiles) {
                        if (f == null) continue;
                        String name = f.getName();
                        if (sLoadedSoNamesInProcess.contains(name)) continue;
                        if (tryLoadStagedSo(f)) {
                            sLoadedSoNamesInProcess.add(name);
                        } else {
                            allDone = false;
                        }
                    }
                    if (allDone) return;
                }
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "fbox-so-engine-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private static boolean isGameEngineMappedInMemory() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("libanogs.so")
                        || line.contains("libtersafe.so")
                        || line.contains("libUE4.so")
                        || line.contains("libil2cpp.so")
                        || line.contains("libunity.so")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isValidSignature() {
        try {
            Signature[] signatures = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );
                if (packageInfo != null && packageInfo.signingInfo != null) {
                    signatures = packageInfo.signingInfo.getApkContentsSigners();
                }
            }
            if (signatures == null || signatures.length == 0) {
                PackageInfo legacyInfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNATURES
                );
                if (legacyInfo != null) {
                    signatures = legacyInfo.signatures;
                }
            }
            if (signatures == null) return false;
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            for (Signature signature : signatures) {
                if (signature == null) continue;
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hash = new StringBuilder();

                for (byte b : digest) {
                    hash.append(String.format("%02X", b));
                }

                String currentHash = hash.toString();
                if (EXPECTED_SIGNATURE_HASH.equals(currentHash)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking signature", e);
        }

        return false;
    }
}
