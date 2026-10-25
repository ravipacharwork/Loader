package com.bgmi;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static com.bgmi.server.ApiServer.mainURL;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bgmi.server.DownloadZip;
import com.bgmi.utils.AppManager;
import com.bgmi.utils.Downtwo;
import com.bgmi.utils.Prefs;
import com.featurestic.box.FeaturesticAntiCrackVault;
import com.featurestic.box.FeaturesticBoxCore;
import com.featurestic.box.core.env.BEnvironment;
import com.featurestic.box.entity.pm.InstallResult;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Obfuscate
public class MAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("zenin");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    private static String PKG_BGMI = "com.pubg.imobile";
    private static int USER_ID = 0;

    private static final String[][] SUPPORTED_GAMES = new String[][]{
            {"com.pubg.imobile", "BGMI"},
            {"com.tencent.ig", "PUBG Global"},
            {"com.pubg.krmobile", "PUBG Korea"},
            {"com.vng.pubgmobile", "PUBG VNG"},
            {"com.rekoo.pubgm", "PUBG Taiwan"},
            {"com.tencent.iglite", "PUBG Lite"},
            {"com.dts.freefiremax", "Free Fire MAX"},
            {"com.dts.freefireth", "Free Fire"},
            {"com.activision.callofduty.shooter", "COD Mobile"},
            {"com.mobile.legends", "Mobile Legends"},
            {"com.miniclip.eightballpool", "8 Ball Pool"},
            {"com.proximabeta.mf.uamo", "Delta Force"},
            {"com.axlebolt.standoff2", "Standoff 2"},
            {"com.farlightgames.farlight84.gp", "Farlight 84"}
    };

    private static final int REQUEST_PICK_GG_APK = 5001;
    private static final int REQUEST_PICK_SO_FILE = 5002;

    private static final String PREF_GG_PKG = "featurestic_gg_pkg";
    private static final String PREF_GG_LABEL = "featurestic_gg_label";
    private static final String PREF_SO_NAME = "featurestic_custom_so_name";
    private static final String PREF_SO_ENABLED = "featurestic_custom_so_enabled";
    private static final String PREF_ONLINE_SO_URL = "featurestic_online_so_url";
    private static final String PREF_TARGET_GAME_PKG = "featurestic_target_game_pkg";
    private static final String PREF_ACTIVE_SLOT = "featurestic_active_user_slot";

    public static String modeselect;
    private ProgressBar progressBar;
    private TextView progressText;
    private boolean isObbCopied = false;
    private AppManager appManager;
    private Prefs prefs;
    public static boolean Ischeck = false;
    public static int gameint = 1;
    public static int bitversi = 64;
    public static boolean modestatus = false;

    private TextView startBgmi;
    private TextView tvLoaderInfo;
    private TextView btnSwitchGameBar;
    private LinearLayout progressCard;
    public static String game = "com.pubg.imobile";
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    public static String socket;
    public static String daemonPath;

    // Countdown text views
    private TextView tvD, tvH, tvM, tvS;

    // Apple HIG Module rows (GameGuardian & Custom .SO)
    private LinearLayout cardGgModule;
    private TextView tvGgStatus;
    private TextView btnAddGg;
    private TextView btnLaunchGg;

    private LinearLayout cardSoModule;
    private TextView tvSoStatus;
    private TextView btnAddSo;
    private TextView btnToggleSo;

    private static MAct instance;

    public static Context get() {
        return instance != null ? instance : null;
    }

    public static native String pp();
    public static native String ZENINOP();

    /**
     * Represents an available `.so` plugin from either Online Server (`files/loader/` / `files/online_so/`)
     * or Manual Phone Storage (`files/custom_so/manual/`).
     */
    private static final class SoPluginItem {
        final File file;
        final String displayName;
        final boolean isOnline;
        final boolean is64Bit;
        final long sizeKb;

        SoPluginItem(File file, String displayName, boolean isOnline, boolean is64Bit) {
            this.file = file;
            this.displayName = displayName;
            this.isOnline = isOnline;
            this.is64Bit = is64Bit;
            this.sizeKb = file != null ? Math.max(1L, file.length() / 1024L) : 0L;
        }

        String getBadge() {
            String src = isOnline ? "ONLINE" : "MANUAL";
            String arch = is64Bit ? "64-BIT" : "32-BIT";
            return "[" + src + " • " + arch + "]";
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        prefs = new Prefs(this);
        LoaderLog.init(this);
        LoaderLog.i("MAct", "onCreate activity started");

        if (!ischicken()) {
            LoaderLog.e("MAct", "signature integrity check FAILED");
            Toast.makeText(this, "Security or signature integrity check failed!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!verifyAuthenticatedSessionAndPanelGate() || !isExpiryDateValid()) {
            LoaderLog.w("MAct", "session gate failed (auth=" + verifyAuthenticatedSessionAndPanelGate() + ")");
            showToast("Session unauthorized or expired! Please login again.");
            startActivity(new Intent(this, LogAct.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        appManager = new AppManager(this);

        startBgmi = findViewById(R.id.startBgmi);
        progressCard = findViewById(R.id.progressCard);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        tvD = findViewById(R.id.tvD);
        tvH = findViewById(R.id.tvH);
        tvM = findViewById(R.id.tvM);
        tvS = findViewById(R.id.tvS);

        cardGgModule = findViewById(R.id.cardGgModule);
        tvGgStatus = findViewById(R.id.tvGgStatus);
        btnAddGg = findViewById(R.id.btnAddGg);
        btnLaunchGg = findViewById(R.id.btnLaunchGg);

        cardSoModule = findViewById(R.id.cardSoModule);
        tvSoStatus = findViewById(R.id.tvSoStatus);
        btnAddSo = findViewById(R.id.btnAddSo);
        btnToggleSo = findViewById(R.id.btnToggleSo);

        TextView btnSignOut = findViewById(R.id.btnSignOut);
        TextView btnClearLogin = findViewById(R.id.btnClearLogin);
        Switch hideEspSwitch = findViewById(R.id.hiderecording);
        TextView tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvLoaderInfo = findViewById(R.id.tvLoaderInfo);

        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL + " • Android " + Build.VERSION.RELEASE;
        if (tvDeviceInfo != null) {
            tvDeviceInfo.setText(deviceInfo);
            tvDeviceInfo.setLongClickable(true);
            tvDeviceInfo.setOnLongClickListener(v -> {
                showLogsDialog();
                return true;
            });
        }
        if (tvLoaderInfo != null) {
            tvLoaderInfo.setText("Target Game & Multi-.SO Ready");
            tvLoaderInfo.setOnClickListener(v -> showTargetGameAndSlotDialog(false));
        }
        if (hideEspSwitch != null) {
            hideEspSwitch.setChecked(false);
        }

        refreshGgModuleStatus();
        refreshSoModuleStatus();

        if (btnAddGg != null) {
            btnAddGg.setOnClickListener(v -> showGgSelectorDialog());
        }
        if (cardGgModule != null) {
            cardGgModule.setOnClickListener(v -> showGgSelectorDialog());
        }
        if (btnLaunchGg != null) {
            btnLaunchGg.setOnClickListener(v -> handleOneClickLaunchOrAddGg());
        }

        if (btnAddSo != null) {
            btnAddSo.setOnClickListener(v -> showSoSelectorDialog());
        }
        if (cardSoModule != null) {
            cardSoModule.setOnClickListener(v -> showSoSelectorDialog());
        }
        if (btnToggleSo != null) {
            btnToggleSo.setOnClickListener(v -> handleOneClickLoadOrAddSo());
        }

        btnSwitchGameBar = findViewById(R.id.btnSwitchGameBar);
        if (btnSwitchGameBar != null) {
            btnSwitchGameBar.setOnClickListener(v -> showTargetGameAndSlotDialog(false));
        }
        resolveActiveTargetGameAndSlot();
        if (startBgmi != null) {
            startBgmi.setLongClickable(true);
            startBgmi.setOnClickListener(v -> handleStartBgmiWithSoCheck());
            startBgmi.setOnLongClickListener(v -> {
                showTargetGameAndSlotDialog(false);
                return true;
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                prefs.setSt("USER", "");
                LogAct.hidesp = false;
                startActivity(new Intent(MAct.this, LogAct.class));
                finish();
            });
        }

        if (btnClearLogin != null) {
            btnClearLogin.setOnClickListener(v -> {
                prefs.setSt("USER", "");
                startActivity(new Intent(MAct.this, LogAct.class));
                finish();
            });
        }

        startCountdownTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoaderLog.i("MAct", "onResume");
        resolveActiveTargetGameAndSlot();
        refreshGgModuleStatus();
        refreshSoModuleStatus();
        checkAndShowPendingOffsetCrashPopup();
        if (progressBar != null && progressCard != null && progressText != null) {
            String cur = progressText.getText() != null ? progressText.getText().toString() : "";
            if (cur.contains("Launching Game") || cur.contains("Starting Game")) {
                progressBar.setIndeterminate(false);
                progressCard.setVisibility(View.GONE);
            }
        }
    }

    // =========================================================================================
    // 1-Click GG APK Selector (+ Button from Phone Storage / Download / Installed Apps) & Launch
    // =========================================================================================

    private String resolveInstalledGgPackage() {
        try {
            String savedPkg = prefs.getSt(PREF_GG_PKG, "");
            if (savedPkg != null && !savedPkg.isEmpty()
                    && FeaturesticBoxCore.get().isInstalled(savedPkg, USER_ID)) {
                return savedPkg;
            }
            List<PackageInfo> installed = FeaturesticBoxCore.get().getInstalledPackages(0, USER_ID);
            if (installed != null) {
                for (PackageInfo pi : installed) {
                    if (pi == null || pi.packageName == null) continue;
                    String p = pi.packageName;
                    if (!FboxCompat.isExcludedGameOrSystemPackage(p)) {
                        prefs.setSt(PREF_GG_PKG, p);
                        return p;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void refreshGgModuleStatus() {
        if (tvGgStatus == null || btnLaunchGg == null) return;
        String installedPkg = resolveInstalledGgPackage();
        if (installedPkg != null && !installedPkg.isEmpty()) {
            String savedLabel = prefs.getSt(PREF_GG_LABEL, "");
            String display = (savedLabel != null && !savedLabel.isEmpty())
                    ? savedLabel + " (" + installedPkg + ")"
                    : installedPkg;
            tvGgStatus.setText("Ready • " + display);
            tvGgStatus.setTextColor(Color.parseColor("#30D158"));
            btnLaunchGg.setText("OPEN");
        } else {
            FboxCompat.DownloadApkCandidate best =
                    FboxCompat.findBestApkInDownloads();
            if (best != null && best.file != null) {
                tvGgStatus.setText("Found " + best.file.getName() + " • Tap +");
                tvGgStatus.setTextColor(Color.parseColor("#8E8E93"));
                btnLaunchGg.setText("ADD");
            } else {
                tvGgStatus.setText("Tap + to add GG APK from phone");
                tvGgStatus.setTextColor(Color.parseColor("#8E8E93"));
                btnLaunchGg.setText("ADD");
            }
        }
    }

    private void handleOneClickLaunchOrAddGg() {
        String installedPkg = resolveInstalledGgPackage();
        if (installedPkg != null && !installedPkg.isEmpty()) {
            String label = prefs.getSt(PREF_GG_LABEL, installedPkg);
            showToast("Opening " + label + "...");
            launchCompanionAppCleanly(installedPkg);
            return;
        }
        FboxCompat.DownloadApkCandidate best =
                FboxCompat.findBestApkInDownloads();
        if (best != null && best.file != null && best.file.isFile()) {
            installGgApkFileAndLaunch(best.file, best.label, true);
        } else {
            showGgSelectorDialog();
        }
    }

    private void showGgSelectorDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B3000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText("Add GameGuardian APK");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        TextView sub = new TextView(this);
        String currentGg = resolveInstalledGgPackage();
        sub.setText(currentGg != null
                ? "Installed: " + currentGg
                : "Choose an APK from your phone storage or installed apps.");
        sub.setTextColor(Color.parseColor("#8E8E93"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(12));
        card.addView(sub);

        TextView btnPickStorage = buildAppleSheetButton(
                "Choose APK from Files", "#0A84FF", "#FFFFFF", boldTf);
        btnPickStorage.setOnClickListener(v -> {
            dialog.dismiss();
            openSystemFilePickerForGgApk();
        });
        card.addView(btnPickStorage);

        TextView btnPickInstalled = buildAppleSheetButton(
                "Select from Installed Apps", "#2C2C2E", "#FFFFFF", boldTf);
        btnPickInstalled.setOnClickListener(v -> {
            dialog.dismiss();
            showInstalledAppsPickerDialog();
        });
        card.addView(btnPickInstalled);

        List<FboxCompat.DownloadApkCandidate> candidates =
                FboxCompat.scanAllCandidateApksInDownloads();
        if (!candidates.isEmpty()) {
            TextView dlHeader = new TextView(this);
            dlHeader.setText("FOUND IN DOWNLOADS");
            dlHeader.setTextColor(Color.parseColor("#8E8E93"));
            dlHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            dlHeader.setTypeface(boldTf);
            dlHeader.setPadding(0, dpToPx(12), 0, dpToPx(4));
            card.addView(dlHeader);

            ScrollView sv = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            int max = Math.min(candidates.size(), 4);
            for (int i = 0; i < max; i++) {
                final FboxCompat.DownloadApkCandidate cand = candidates.get(i);
                TextView itemBtn = buildAppleSheetButton(
                        cand.file.getName() + " (" + cand.getArchBadge() + ")",
                        "#2C2C2E", "#0A84FF", regTf);
                itemBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    installGgApkFileAndLaunch(cand.file, cand.label, true);
                });
                list.addView(itemBtn);
            }
            sv.addView(list);
            card.addView(sv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (currentGg != null) {
            final String pkgToRun = currentGg;
            TextView btnRunNow = buildAppleSheetButton(
                    "Open Installed GG", "#2C2C2E", "#30D158", boldTf);
            btnRunNow.setOnClickListener(v -> {
                dialog.dismiss();
                launchCompanionAppCleanly(pkgToRun);
            });
            card.addView(btnRunNow);
        }

        TextView btnResetId = buildAppleSheetButton(
                "Reset Virtual Device ID (New IMEI / Android ID)", "#2C2C2E", "#FFD60A", boldTf);
        btnResetId.setOnClickListener(v -> {
            String newAid = FeaturesticBoxCore.get().resetVirtualDeviceIdentity(USER_ID);
            showToast("New Virtual Device ID: " + newAid);
        });
        card.addView(btnResetId);

        TextView btnSlotSwitch = buildAppleSheetButton(
                "Switch Game / Account Slot (Slot " + (USER_ID + 1) + ")", "#2C2C2E", "#5E5CE6", boldTf);
        btnSlotSwitch.setOnClickListener(v -> {
            dialog.dismiss();
            showTargetGameAndSlotDialog(false);
        });
        card.addView(btnSlotSwitch);

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(340), (int) (getResources().getDisplayMetrics().widthPixels * 0.90f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    private void openSystemFilePickerForGgApk() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/vnd.android.package-archive",
                    "application/octet-stream",
                    "*/*"
            });
            startActivityForResult(Intent.createChooser(intent, "Select GG APK"), REQUEST_PICK_GG_APK);
        } catch (Throwable e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.addCategory(Intent.CATEGORY_OPENABLE);
                fallback.setType("*/*");
                startActivityForResult(Intent.createChooser(fallback, "Select GG APK"), REQUEST_PICK_GG_APK);
            } catch (Throwable ex) {
                showToast("Unable to open file picker: " + ex.getMessage());
            }
        }
    }

    private void showInstalledAppsPickerDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B3000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText("Installed Apps");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        PackageManager pm = getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        List<PackageInfo> userApps = new ArrayList<>();
        if (pkgs != null) {
            for (PackageInfo pi : pkgs) {
                if (pi == null || pi.applicationInfo == null || pi.packageName == null) continue;
                if (pi.packageName.equals(getPackageName()) || pi.packageName.equals(PKG_BGMI)) continue;
                boolean isSystem = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (isSystem) continue;
                userApps.add(pi);
            }
        }

        Collections.sort(userApps, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo a, PackageInfo b) {
                boolean aGg = FboxCompat.isKnownCompanionPackage(a.packageName);
                boolean bGg = FboxCompat.isKnownCompanionPackage(b.packageName);
                if (aGg != bGg) return aGg ? -1 : 1;
                return a.packageName.compareToIgnoreCase(b.packageName);
            }
        });

        int shown = 0;
        for (final PackageInfo pi : userApps) {
            if (shown >= 30) break;
            shown++;
            CharSequence cs = pi.applicationInfo.loadLabel(pm);
            final String label = cs != null ? cs.toString() : pi.packageName;
            final String srcDir = pi.applicationInfo.sourceDir;
            if (srcDir == null) continue;
            TextView btn = buildAppleSheetButton(
                    label + " (" + pi.packageName + ")",
                    "#2C2C2E", "#FFFFFF", regTf);
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                installGgApkFileAndLaunch(new File(srcDir), label, true);
            });
            list.addView(btn);
        }

        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("No third-party apps found. Use 'Choose APK from Files'.");
            empty.setTextColor(Color.parseColor("#8E8E93"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            empty.setPadding(0, dpToPx(10), 0, dpToPx(10));
            list.addView(empty);
        }

        sv.addView(list);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(250));
        svLp.topMargin = dpToPx(10);
        card.addView(sv, svLp);

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(340), (int) (getResources().getDisplayMetrics().widthPixels * 0.90f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    private void installGgApkFileAndLaunch(
            final File apkFile, final String displayHint, final boolean autoLaunch) {
        if (apkFile == null || !apkFile.isFile()) {
            showToast("Selected APK file is not accessible");
            return;
        }
        LoaderLog.i("GG", "install requested: " + apkFile.getAbsolutePath() + " autoLaunch=" + autoLaunch);
        progressCard.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressText.setText("Installing " + apkFile.getName() + "...");
        tvGgStatus.setText("Installing " + apkFile.getName() + "...");

        new Thread(() -> {
            try {
                String resolvedLabel = displayHint;
                try {
                    PackageManager pm = getPackageManager();
                    PackageInfo archivePi = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
                    if (archivePi != null && archivePi.applicationInfo != null) {
                        archivePi.applicationInfo.sourceDir = apkFile.getAbsolutePath();
                        archivePi.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
                        CharSequence cs = archivePi.applicationInfo.loadLabel(pm);
                        if (cs != null && cs.length() > 0) {
                            resolvedLabel = cs.toString();
                        }
                    }
                } catch (Throwable ignored) {
                }

                InstallResult res = FeaturesticBoxCore.get().installPackageAsUser(apkFile, USER_ID);
                LoaderLog.i("GG", "installPackageAsUser -> " + (res == null ? "null"
                        : ("success=" + res.success + " pkg=" + res.packageName + " msg=" + res.msg)));
                if (res == null || !res.success || res.packageName == null) {
                    res = FeaturesticBoxCore.get().installBundleAsUser(apkFile, USER_ID);
                    LoaderLog.i("GG", "installBundleAsUser -> " + (res == null ? "null"
                            : ("success=" + res.success + " pkg=" + res.packageName + " msg=" + res.msg)));
                }
                final InstallResult finalRes = res;
                final String finalLabel = (resolvedLabel != null && !resolvedLabel.isEmpty())
                        ? resolvedLabel : apkFile.getName();

                runOnUiThread(() -> {
                    progressBar.setIndeterminate(false);
                    if (finalRes != null && finalRes.success && finalRes.packageName != null) {
                        prefs.setSt(PREF_GG_PKG, finalRes.packageName);
                        prefs.setSt(PREF_GG_LABEL, finalLabel);
                        FboxCompat.notifyCompanionPackageInstalled(finalRes.packageName);
                        progressText.setText("Installed " + finalLabel);
                        refreshGgModuleStatus();
                        showToast("Added " + finalLabel);
                        if (autoLaunch) {
                            launchCompanionAppCleanly(finalRes.packageName);
                        }
                    } else {
                        String msg = (finalRes != null && finalRes.msg != null)
                                ? finalRes.msg : "Invalid APK";
                        progressText.setText("Install failed: " + msg);
                        refreshGgModuleStatus();
                        showToast("Install failed: " + msg);
                    }
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    progressBar.setIndeterminate(false);
                    progressText.setText("Error: " + t.getMessage());
                    refreshGgModuleStatus();
                });
            }
        }).start();
    }

    // =========================================================================================
    // Dual Manual + Online Multi-.SO Manager & Pre-Launch .SO Selector Dialog
    // =========================================================================================

    private File getCustomSoDir() {
        File dir = new File(getFilesDir(), "custom_so");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getManualSoLibraryDir() {
        File dir = new File(getCustomSoDir(), "manual");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getOnlineSoLibraryDir() {
        File dir = new File(getFilesDir(), "online_so");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getServerLoaderDir() {
        File dir = new File(getFilesDir(), "loader");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getActiveCustomSoFile() {
        return new File(getCustomSoDir(), "active_custom.so");
    }

    private File getActiveCustomSoConf() {
        return new File(getCustomSoDir(), "active_so.conf");
    }

    private File getActiveSoListConf() {
        return new File(getCustomSoDir(), "active_so_list.conf");
    }

    private static int readElfArchBit(File file) {
        return BoxApplication.readSoOrVaultArchBit(file);
    }

    /**
     * Collects all available valid `.so` plugins from BOTH:
     * 1. Online Server directories (`files/loader/` from `Downtwo`, `files/online_so/` from URL sync)
     * 2. Manual Phone Storage directory (`files/custom_so/manual/` + legacy `active_custom.so`)
     */
    private List<SoPluginItem> collectAllAvailableSoPlugins() {
        List<SoPluginItem> items = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        Set<String> seenNamesBySource = new HashSet<>();

        // 1. Online .so files (extracted by Downtwo into files/loader/ or downloaded into files/online_so/)
        scanDirForSoPlugins(getServerLoaderDir(), true, items, seenPaths, seenNamesBySource);
        scanDirForSoPlugins(getOnlineSoLibraryDir(), true, items, seenPaths, seenNamesBySource);

        // 2. Manual .so files added by the user from phone storage / Downloads
        scanDirForSoPlugins(getManualSoLibraryDir(), false, items, seenPaths, seenNamesBySource);

        // 3. Legacy single active_custom.so if not already in manual/
        File legacyActive = getActiveCustomSoFile();
        String legacyName = prefs.getSt(PREF_SO_NAME, "libcustom.so");
        if (legacyActive.isFile() && seenPaths.add(legacyActive.getAbsolutePath())) {
            int bits = readElfArchBit(legacyActive);
            if (bits == 64 || bits == 32) {
                String key = "M:" + legacyName.toLowerCase(Locale.US);
                if (seenNamesBySource.add(key)) {
                    items.add(new SoPluginItem(legacyActive, legacyName, false, bits == 64));
                }
            }
        }
        return items;
    }

    private void scanDirForSoPlugins(
            File dir,
            boolean isOnline,
            List<SoPluginItem> out,
            Set<String> seenPaths,
            Set<String> seenNamesBySource) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f == null) continue;
            if (f.isDirectory()) {
                scanDirForSoPlugins(f, isOnline, out, seenPaths, seenNamesBySource);
                continue;
            }
            if (!f.getName().toLowerCase(Locale.US).endsWith(".so")) continue;
            int bits = readElfArchBit(f);
            if (bits != 64 && bits != 32) continue;
            // Only list .so files compatible with the current 32/64-bit container mode
            boolean want64 = FeaturesticBoxCore.is64Bit();
            if (want64 && bits != 64) continue;
            if (!want64 && bits != 32) continue;
            if (!seenPaths.add(f.getAbsolutePath())) continue;
            String key = (isOnline ? "O:" : "M:") + f.getName().toLowerCase(Locale.US);
            if (seenNamesBySource.add(key)) {
                out.add(new SoPluginItem(f, f.getName(), isOnline, bits == 64));
            }
        }
    }

    private Set<String> readSelectedSoPaths() {
        Set<String> selected = new LinkedHashSet<>();
        File conf = getActiveSoListConf();
        if (conf.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(conf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("PATH=")) {
                        String p = line.substring("PATH=".length()).trim();
                        if (!p.isEmpty() && new File(p).isFile()) {
                            selected.add(p);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (selected.isEmpty()) {
            List<SoPluginItem> all = collectAllAvailableSoPlugins();
            if (all.size() == 1) {
                selected.add(all.get(0).file.getAbsolutePath());
            }
        }
        return selected;
    }

    private void saveSelectedSoPaths(boolean enabled, List<SoPluginItem> chosenItems) {
        try {
            File listConf = getActiveSoListConf();
            StringBuilder sb = new StringBuilder();
            sb.append("ENABLED=").append(enabled ? "1" : "0").append("\n");
            if (chosenItems != null) {
                for (SoPluginItem item : chosenItems) {
                    if (item != null && item.file != null && item.file.isFile()) {
                        sb.append("PATH=").append(item.file.getAbsolutePath()).append("\n");
                        sb.append("NAME=").append(item.displayName).append("\n");
                    }
                }
            }
            try (FileOutputStream fos = new FileOutputStream(listConf)) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }

            if (enabled && chosenItems != null && !chosenItems.isEmpty()) {
                SoPluginItem first = chosenItems.get(0);
                prefs.setSt(PREF_SO_NAME, first.displayName);
                prefs.setBool(PREF_SO_ENABLED, true);
                writeCustomSoConf(true, first.displayName);
            } else {
                prefs.setBool(PREF_SO_ENABLED, false);
                writeCustomSoConf(false, "");
            }
        } catch (Throwable ignored) {
        }
    }

    private void writeCustomSoConf(boolean enabled, String soName) {
        try {
            File conf = getActiveCustomSoConf();
            String content = "ENABLED=" + (enabled ? "1" : "0") + "\nNAME=" + (soName != null ? soName : "libcustom.so") + "\n";
            try (FileOutputStream fos = new FileOutputStream(conf)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean isSoInjectionEnabledByUser() {
        File listConf = getActiveSoListConf();
        if (listConf.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(listConf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("ENABLED=")) {
                        return "1".equals(line.substring("ENABLED=".length()).trim());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return prefs != null && prefs.getBool(PREF_SO_ENABLED, false);
    }

    private void refreshSoModuleStatus() {
        if (tvSoStatus == null || btnToggleSo == null) return;
        List<SoPluginItem> allPlugins = collectAllAvailableSoPlugins();
        Set<String> selectedPaths = readSelectedSoPaths();
        boolean isEnabled = isSoInjectionEnabledByUser() && !selectedPaths.isEmpty();

        if (!allPlugins.isEmpty()) {
            int onlineCount = 0;
            int manualCount = 0;
            for (SoPluginItem item : allPlugins) {
                if (item.isOnline) onlineCount++;
                else manualCount++;
            }
            if (allPlugins.size() == 1) {
                SoPluginItem single = allPlugins.get(0);
                if (isEnabled) {
                    tvSoStatus.setText("ACTIVE " + single.getBadge() + " • " + single.displayName);
                    tvSoStatus.setTextColor(Color.parseColor("#30D158"));
                    btnToggleSo.setText("ON ✓");
                } else {
                    tvSoStatus.setText("OFF (Clean Mode) • " + single.displayName);
                    tvSoStatus.setTextColor(Color.parseColor("#8E8E93"));
                    btnToggleSo.setText("LOAD");
                }
            } else {
                if (isEnabled) {
                    tvSoStatus.setText(selectedPaths.size() + "/" + allPlugins.size() + " .SO Active (" + onlineCount + " Online, " + manualCount + " Manual)");
                    tvSoStatus.setTextColor(Color.parseColor("#30D158"));
                    btnToggleSo.setText("SELECT");
                } else {
                    tvSoStatus.setText("OFF (Clean Mode) • " + allPlugins.size() + " .SO Available");
                    tvSoStatus.setTextColor(Color.parseColor("#8E8E93"));
                    btnToggleSo.setText("SELECT");
                }
            }
        } else {
            tvSoStatus.setText("Clean Mode (No .SO) • Tap + to add .SO");
            tvSoStatus.setTextColor(Color.parseColor("#8E8E93"));
            btnToggleSo.setText("ADD");
        }
    }

    private void handleOneClickLoadOrAddSo() {
        List<SoPluginItem> allPlugins = collectAllAvailableSoPlugins();
        if (allPlugins.isEmpty()) {
            showSoSelectorDialog();
            return;
        }
        if (allPlugins.size() > 1) {
            showPreLaunchSoChooserDialog(allPlugins, false);
            return;
        }
        SoPluginItem single = allPlugins.get(0);
        boolean currentlyOn = isSoInjectionEnabledByUser();
        if (currentlyOn) {
            saveSelectedSoPaths(false, Collections.emptyList());
            refreshSoModuleStatus();
            showToast("Clean Mode: .SO Disabled");
        } else {
            saveSelectedSoPaths(true, Collections.singletonList(single));
            refreshSoModuleStatus();
            showToast("Enabled .SO: " + single.displayName);
        }
    }

    /**
     * Triggered when the user taps `START BGMI`.
     * - If 2 or more `.so` plugins exist (from Online and/or Manual), asks the user which
     *   `.so` plugin(s) they want to load before launching the game!
     * - If 1 `.so` plugin exists, activates it and starts the game immediately.
     */
    private void handleStartBgmiWithSoCheck() {
        List<String[]> installedGames = detectAllInstalledGamesOnHost();
        if (installedGames.size() > 1) {
            showTargetGameAndSlotDialog(true);
            return;
        }
        if (installedGames.size() == 1) {
            String[] singleGame = installedGames.get(0);
            PKG_BGMI = singleGame[0];
            game = singleGame[0];
            prefs.setSt(PREF_TARGET_GAME_PKG, singleGame[0]);
            updateStartButtonText();
        }
        continueStartAfterGameSelected();
    }

    private void continueStartAfterGameSelected() {
        List<SoPluginItem> allPlugins = collectAllAvailableSoPlugins();
        if (allPlugins.size() > 1) {
            showPreLaunchSoChooserDialog(allPlugins, true);
            return;
        }
        if (allPlugins.size() == 1 && isSoInjectionEnabledByUser()) {
            saveSelectedSoPaths(true, Collections.singletonList(allPlugins.get(0)));
            refreshSoModuleStatus();
        }
        proceedToStartGameNow();
    }

    private void proceedToStartGameNow() {
        progressCard.setVisibility(View.VISIBLE);
        progressText.setText("Checking game...");
        startGameSequence();
    }

    /**
     * Pre-Launch Multi-.SO Chooser Dialog:
     * Shown automatically before launching BGMI whenever multiple `.so` files (Online & Manual)
     * are available, or when the user taps `SELECT` on the `.SO` card.
     */
    private void showPreLaunchSoChooserDialog(final List<SoPluginItem> plugins, final boolean launchAfterSelect) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B8000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText(launchAfterSelect ? "Choose .SO to Load Before Game" : "Select Active .SO Plugins");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText(launchAfterSelect
                ? "Multiple .SO files detected (Online & Manual). Tap one to load & launch immediately, or select multiple:"
                : "Tap any .SO plugin below to toggle it ON/OFF for the next game launch:");
        sub.setTextColor(Color.parseColor("#8E8E93"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(10));
        card.addView(sub);

        final Set<String> activePaths = readSelectedSoPaths();
        if (activePaths.isEmpty() && !plugins.isEmpty()) {
            activePaths.add(plugins.get(0).file.getAbsolutePath());
        }

        ScrollView sv = new ScrollView(this);
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        final List<TextView> itemViews = new ArrayList<>();
        for (int i = 0; i < plugins.size(); i++) {
            final SoPluginItem item = plugins.get(i);
            final String path = item.file.getAbsolutePath();
            boolean isChecked = activePaths.contains(path);

            final TextView rowBtn = buildAppleSheetButton(
                    formatSoRowLabel(item, isChecked),
                    isChecked ? "#1A30D158" : "#2C2C2E",
                    isChecked ? "#30D158" : "#FFFFFF",
                    boldTf);
            itemViews.add(rowBtn);

            rowBtn.setOnClickListener(v -> {
                if (activePaths.contains(path)) {
                    activePaths.remove(path);
                } else {
                    activePaths.add(path);
                }
                boolean nowChecked = activePaths.contains(path);
                rowBtn.setText(formatSoRowLabel(item, nowChecked));
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor(nowChecked ? "#1A30D158" : "#2C2C2E"));
                bg.setCornerRadius(dpToPx(12));
                if (nowChecked) {
                    bg.setStroke(dpToPx(1), Color.parseColor("#30D158"));
                }
                rowBtn.setBackground(bg);
                rowBtn.setTextColor(Color.parseColor(nowChecked ? "#30D158" : "#FFFFFF"));
            });

            // Long-press on any single .SO immediately selects ONLY that .SO and launches
            rowBtn.setOnLongClickListener(v -> {
                dialog.dismiss();
                saveSelectedSoPaths(true, Collections.singletonList(item));
                refreshSoModuleStatus();
                showToast("Selected " + item.displayName);
                if (launchAfterSelect) {
                    proceedToStartGameNow();
                }
                return true;
            });

            list.addView(rowBtn);
        }

        sv.addView(list);
        int maxScrollH = Math.min(dpToPx(220), (int) (getResources().getDisplayMetrics().heightPixels * 0.38f));
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView btnLoadSelected = buildAppleSheetButton(
                launchAfterSelect ? "Load Checked .SO & Start BGMI" : "Save Checked .SO Selection",
                "#30D158", "#000000", boldTf);
        btnLoadSelected.setOnClickListener(v -> {
            List<SoPluginItem> chosen = new ArrayList<>();
            for (SoPluginItem item : plugins) {
                if (activePaths.contains(item.file.getAbsolutePath())) {
                    chosen.add(item);
                }
            }
            if (chosen.isEmpty() && !plugins.isEmpty()) {
                chosen.add(plugins.get(0));
            }
            dialog.dismiss();
            saveSelectedSoPaths(true, chosen);
            refreshSoModuleStatus();
            showToast("Loading " + chosen.size() + " .SO plugin(s)");
            if (launchAfterSelect) {
                proceedToStartGameNow();
            }
        });
        card.addView(btnLoadSelected);

        if (launchAfterSelect) {
            TextView btnLoadAll = buildAppleSheetButton(
                    "Load All (" + plugins.size() + " .SO) & Start BGMI",
                    "#0A84FF", "#FFFFFF", boldTf);
            btnLoadAll.setOnClickListener(v -> {
                dialog.dismiss();
                saveSelectedSoPaths(true, plugins);
                refreshSoModuleStatus();
                showToast("Loading all " + plugins.size() + " .SO plugins");
                proceedToStartGameNow();
            });
            card.addView(btnLoadAll);

            TextView btnSkipSo = buildAppleSheetButton(
                    "Start Game Without .SO (Clean Mode)",
                    "#2C2C2E", "#FF9F0A", regTf);
            btnSkipSo.setOnClickListener(v -> {
                dialog.dismiss();
                saveSelectedSoPaths(false, Collections.emptyList());
                refreshSoModuleStatus();
                proceedToStartGameNow();
            });
            card.addView(btnSkipSo);
        }

        TextView btnManage = buildAppleSheetButton(
                "Manage / Add Manual or Online .SO", "#2C2C2E", "#0A84FF", regTf);
        btnManage.setOnClickListener(v -> {
            dialog.dismiss();
            showSoSelectorDialog();
        });
        card.addView(btnManage);

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(350), (int) (getResources().getDisplayMetrics().widthPixels * 0.92f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    private static String formatSoRowLabel(SoPluginItem item, boolean checked) {
        String mark = checked ? "[✓] " : "[  ] ";
        return mark + item.displayName + "\n" + item.getBadge() + " • " + item.sizeKb + " KB";
    }

    private void showSoSelectorDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B3000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText("Manual & Online .SO Manager");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        List<SoPluginItem> available = collectAllAvailableSoPlugins();

        TextView sub = new TextView(this);
        sub.setText(available.isEmpty()
                ? "Add a Manual .so from your phone or sync an Online .so from the server."
                : available.size() + " .SO plugin(s) ready. If multiple .SOs are added, you can pick which one to load before starting the game.");
        sub.setTextColor(Color.parseColor("#8E8E93"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(10));
        card.addView(sub);

        // Option 1: Manual .SO from Phone Storage
        TextView btnPickSo = buildAppleSheetButton(
                "+ Add Manual .SO from Phone Files", "#0A84FF", "#FFFFFF", boldTf);
        btnPickSo.setOnClickListener(v -> {
            dialog.dismiss();
            openSystemFilePickerForSo();
        });
        card.addView(btnPickSo);

        // Option 2: Online .SO Sync / URL Download
        TextView btnOnlineSo = buildAppleSheetButton(
                "↓ Sync / Download Online .SO (Server / URL)", "#30D158", "#000000", boldTf);
        btnOnlineSo.setOnClickListener(v -> {
            dialog.dismiss();
            showOnlineSoSyncDialog();
        });
        card.addView(btnOnlineSo);

        // Option 3: If 1+ .SO plugins exist, open selector
        if (!available.isEmpty()) {
            TextView btnSelectActive = buildAppleSheetButton(
                    "Choose Which .SO to Load (" + available.size() + " Available)",
                    "#2C2C2E", "#30D158", boldTf);
            btnSelectActive.setOnClickListener(v -> {
                dialog.dismiss();
                showPreLaunchSoChooserDialog(available, false);
            });
            card.addView(btnSelectActive);
        }

        // Option 4: Quick import from Phone Downloads
        List<File> downloadSoFiles = scanDownloadFoldersForSoFiles();
        if (!downloadSoFiles.isEmpty()) {
            TextView dlHeader = new TextView(this);
            dlHeader.setText("FOUND IN PHONE DOWNLOADS (MANUAL)");
            dlHeader.setTextColor(Color.parseColor("#8E8E93"));
            dlHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            dlHeader.setTypeface(boldTf);
            dlHeader.setPadding(0, dpToPx(12), 0, dpToPx(4));
            card.addView(dlHeader);

            ScrollView sv = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            int max = Math.min(downloadSoFiles.size(), 4);
            for (int i = 0; i < max; i++) {
                final File soFile = downloadSoFiles.get(i);
                long kb = Math.max(1L, soFile.length() / 1024L);
                TextView itemBtn = buildAppleSheetButton(
                        soFile.getName() + " (" + kb + " KB)",
                        "#2C2C2E", "#0A84FF", regTf);
                itemBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    importCustomSoFromFile(soFile, soFile.getName());
                });
                list.addView(itemBtn);
            }
            sv.addView(list);
            card.addView(sv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (!available.isEmpty()) {
            TextView btnClearAll = buildAppleSheetButton(
                    "Remove Manual .SO Plugins", "#2C2C2E", "#FF453A", regTf);
            btnClearAll.setOnClickListener(v -> {
                dialog.dismiss();
                File[] manualFiles = getManualSoLibraryDir().listFiles();
                if (manualFiles != null) {
                    for (File mf : manualFiles) {
                        if (mf != null) mf.delete();
                    }
                }
                getActiveCustomSoFile().delete();
                getActiveCustomSoConf().delete();
                getActiveSoListConf().delete();
                prefs.setSt(PREF_SO_NAME, "");
                prefs.setBool(PREF_SO_ENABLED, false);
                refreshSoModuleStatus();
                showToast("Cleared manual .SO plugins");
            });
            card.addView(btnClearAll);
        }

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(345), (int) (getResources().getDisplayMetrics().widthPixels * 0.92f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    /**
     * Dialog for syncing Online `.so` plugins from the built-in server (`Downtwo.Link()`)
     * or downloading a `.so` directly from a custom URL into `files/online_so/`.
     */
    private void showOnlineSoSyncDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B8000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText("Online .SO Cloud Sync");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Sync the latest .SO plugins from the official loader server or enter a direct .SO file URL:");
        sub.setTextColor(Color.parseColor("#8E8E93"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(10));
        card.addView(sub);

        TextView btnOfficialSync = buildAppleSheetButton(
                "1-Tap Sync from Official Server", "#30D158", "#000000", boldTf);
        btnOfficialSync.setOnClickListener(v -> {
            dialog.dismiss();
            syncOnlineSoFromOfficialServer();
        });
        card.addView(btnOfficialSync);

        final EditText urlInput = new EditText(this);
        urlInput.setHint("https://example.com/libplugin.so");
        urlInput.setHintTextColor(Color.parseColor("#636366"));
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        urlInput.setSingleLine(true);
        urlInput.setText(prefs.getSt(PREF_ONLINE_SO_URL, ""));
        int inputPad = dpToPx(12);
        urlInput.setPadding(inputPad, inputPad, inputPad, inputPad);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#2C2C2E"));
        inputBg.setCornerRadius(dpToPx(10));
        urlInput.setBackground(inputBg);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = dpToPx(12);
        card.addView(urlInput, inputLp);

        TextView btnDownloadUrl = buildAppleSheetButton(
                "Download .SO from URL", "#0A84FF", "#FFFFFF", boldTf);
        btnDownloadUrl.setOnClickListener(v -> {
            String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
            if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                showToast("Enter a valid http:// or https:// .so URL");
                return;
            }
            prefs.setSt(PREF_ONLINE_SO_URL, url);
            dialog.dismiss();
            downloadOnlineSoFromDirectUrl(url);
        });
        card.addView(btnDownloadUrl);

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(345), (int) (getResources().getDisplayMetrics().widthPixels * 0.92f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    private void syncOnlineSoFromOfficialServer() {
        progressCard.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressText.setText("Syncing Online .SO from server...");
        try {
            // Force fresh version check on manual sync
            getSharedPreferences("com.bgmi.download", MODE_PRIVATE).edit().putString("version", "0.0").apply();
            Downtwo task = new Downtwo(this, success -> runOnUiThread(() -> {
                progressBar.setIndeterminate(false);
                refreshSoModuleStatus();
                List<SoPluginItem> found = collectAllAvailableSoPlugins();
                if (success && !found.isEmpty()) {
                    progressBar.setProgress(100);
                    progressText.setText("Online .SO Synced (" + found.size() + " plugin(s) ready)");
                    showToast("Online .SO Synced!");
                    if (found.size() > 1) {
                        showPreLaunchSoChooserDialog(found, false);
                    }
                } else {
                    progressText.setText(success ? "Server sync completed" : "Online sync failed");
                }
            }));
            task.setProgressListener(percent -> runOnUiThread(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(percent);
                progressText.setText("Downloading Online .SO: " + percent + "%");
            }));
            task.execute(Downtwo.Link());
        } catch (Throwable t) {
            progressBar.setIndeterminate(false);
            progressText.setText("Online sync error: " + t.getMessage());
        }
    }

    private void downloadOnlineSoFromDirectUrl(final String urlString) {
        progressCard.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressText.setText("Downloading Online .SO...");
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                String pathPart = url.getPath();
                String rawName = (pathPart != null && pathPart.contains("/"))
                        ? pathPart.substring(pathPart.lastIndexOf('/') + 1)
                        : "libonline.so";
                rawName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!rawName.toLowerCase(Locale.US).endsWith(".so")) {
                    rawName = rawName + ".so";
                }

                final String uiRawName = rawName;
                File tmpFile = new File(getCacheDir(), "dl_" + System.currentTimeMillis() + "_" + rawName);
                int total = conn.getContentLength();
                long downloaded = 0;
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    byte[] buf = new byte[32768];
                    int r;
                    int lastProg = -1;
                    while ((r = is.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                        downloaded += r;
                        if (total > 0) {
                            final int prog = (int) ((downloaded * 100L) / total);
                            if (prog != lastProg) {
                                lastProg = prog;
                                runOnUiThread(() -> {
                                    progressBar.setIndeterminate(false);
                                    progressBar.setProgress(prog);
                                    progressText.setText("Downloading " + finalNameForUi(uiRawName) + ": " + prog + "%");
                                });
                            }
                        }
                    }
                    fos.flush();
                }

                int bits = readElfArchBit(tmpFile);
                if (bits != 64 && bits != 32) {
                    tmpFile.delete();
                    runOnUiThread(() -> {
                        progressBar.setIndeterminate(false);
                        progressText.setText("Downloaded file is not a valid ELF .so");
                        showToast("Invalid .so file from URL");
                    });
                    return;
                }

                File dstFile = new File(getOnlineSoLibraryDir(), rawName);
                if (dstFile.exists()) dstFile.delete();
                try (FileInputStream fis = new FileInputStream(tmpFile);
                     FileOutputStream fos = new FileOutputStream(dstFile)) {
                    byte[] buf = new byte[32768];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                    }
                    fos.flush();
                }
                tmpFile.delete();
                BoxApplication.encryptOnlineSoInPlace(dstFile);

                final String savedName = rawName;
                final SoPluginItem newItem = new SoPluginItem(dstFile, savedName, true, bits == 64);
                saveSelectedSoPaths(true, Collections.singletonList(newItem));

                runOnUiThread(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(100);
                    progressText.setText("Online .SO Ready: " + savedName);
                    refreshSoModuleStatus();
                    showToast("Downloaded Online .SO: " + savedName);
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    progressBar.setIndeterminate(false);
                    progressText.setText("Online .SO failed: " + t.getMessage());
                    showToast("Download failed: " + t.getMessage());
                });
            }
        }).start();
    }

    private static String finalNameForUi(String name) {
        return name != null ? name : "plugin.so";
    }

    private void openSystemFilePickerForSo() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select .SO Library"), REQUEST_PICK_SO_FILE);
        } catch (Throwable e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.addCategory(Intent.CATEGORY_OPENABLE);
                fallback.setType("*/*");
                startActivityForResult(Intent.createChooser(fallback, "Select .SO Library"), REQUEST_PICK_SO_FILE);
            } catch (Throwable ex) {
                showToast("Unable to open file picker: " + ex.getMessage());
            }
        }
    }

    private List<File> scanDownloadFoldersForSoFiles() {
        List<File> dirs = new ArrayList<>();
        try {
            File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (pub != null) dirs.add(pub);
        } catch (Throwable ignored) {
        }
        dirs.add(new File("/sdcard/Download"));
        dirs.add(new File("/storage/emulated/0/Download"));
        dirs.add(new File("/sdcard/Download/Telegram"));
        dirs.add(new File("/storage/emulated/0/Download/Telegram"));

        Set<String> seen = new HashSet<>();
        List<File> found = new ArrayList<>();
        for (File d : dirs) {
            if (d == null || !d.isDirectory()) continue;
            File[] children = d.listFiles();
            if (children == null) continue;
            for (File f : children) {
                if (f == null || !f.isFile()) continue;
                if (f.getName().toLowerCase(Locale.US).endsWith(".so") && f.length() > 64) {
                    if (seen.add(f.getAbsolutePath())) {
                        found.add(f);
                    }
                }
            }
        }
        return found;
    }

    private void importCustomSoFromFile(final File srcFile, final String originalName) {
        if (srcFile == null || !srcFile.isFile()) {
            showToast("Selected .so file is not readable");
            return;
        }
        new Thread(() -> {
            try {
                int bits = readElfArchBit(srcFile);
                if (bits != 64 && bits != 32) {
                    runOnUiThread(() -> showToast("Invalid .so file (Not a valid ELF library)"));
                    return;
                }
                if (FeaturesticBoxCore.is64Bit() && bits != 64) {
                    runOnUiThread(() -> showToast("Warning: 32-bit .so cannot load into 64-bit BGMI! Use a 64-bit .so."));
                    return;
                }

                String cleanName = (originalName != null && !originalName.isEmpty())
                        ? originalName : srcFile.getName();
                cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!cleanName.toLowerCase(Locale.US).endsWith(".so")) {
                    cleanName = cleanName + ".so";
                }

                // Store inside files/custom_so/manual/<cleanName> so multiple manual .so files coexist!
                File manualTarget = new File(getManualSoLibraryDir(), cleanName);
                try (FileInputStream fis = new FileInputStream(srcFile);
                     FileOutputStream fos = new FileOutputStream(manualTarget)) {
                    byte[] buf = new byte[32768];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                    }
                    fos.flush();
                }
                manualTarget.setReadable(true, false);
                manualTarget.setExecutable(true, false);

                // Also copy to legacy active_custom.so for backward compatibility
                File activeSo = getActiveCustomSoFile();
                try (FileInputStream fis = new FileInputStream(manualTarget);
                     FileOutputStream fos = new FileOutputStream(activeSo)) {
                    byte[] buf = new byte[32768];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                    }
                    fos.flush();
                }
                activeSo.setReadable(true, false);
                activeSo.setExecutable(true, false);

                final String finalName = cleanName;
                final SoPluginItem addedItem = new SoPluginItem(manualTarget, finalName, false, bits == 64);
                saveSelectedSoPaths(true, Collections.singletonList(addedItem));

                runOnUiThread(() -> {
                    List<SoPluginItem> allNow = collectAllAvailableSoPlugins();
                    refreshSoModuleStatus();
                    progressCard.setVisibility(View.VISIBLE);
                    progressBar.setProgress(100);
                    progressText.setText("Added " + finalName + " (" + allNow.size() + " .SO Ready)");
                    showToast("Added Manual .SO: " + finalName);
                });
            } catch (Throwable t) {
                runOnUiThread(() -> showToast("Failed to add .so: " + t.getMessage()));
            }
        }).start();
    }

    private String queryFileNameFromUri(Uri uri) {
        if (uri == null) return null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = cursor.getString(idx);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        String seg = uri.getLastPathSegment();
        if (seg != null) {
            int slash = seg.lastIndexOf('/');
            return slash >= 0 ? seg.substring(slash + 1) : seg;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final Uri pickedUri = data.getData();
        final String displayName = queryFileNameFromUri(pickedUri);

        if (requestCode == REQUEST_PICK_GG_APK) {
            progressCard.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            progressText.setText("Copying selected GG APK...");
            new Thread(() -> {
                try {
                    String fileName = (displayName != null && displayName.toLowerCase(Locale.US).endsWith(".apk"))
                            ? displayName : "selected_gg.apk";
                    File tempApk = new File(getCacheDir(), fileName);
                    try (InputStream is = getContentResolver().openInputStream(pickedUri);
                         FileOutputStream fos = new FileOutputStream(tempApk)) {
                        if (is == null) throw new IOException("Cannot open selected APK");
                        byte[] buf = new byte[32768];
                        int r;
                        while ((r = is.read(buf)) != -1) {
                            fos.write(buf, 0, r);
                        }
                        fos.flush();
                    }
                    runOnUiThread(() -> installGgApkFileAndLaunch(tempApk, displayName, true));
                } catch (Throwable t) {
                    runOnUiThread(() -> {
                        progressBar.setIndeterminate(false);
                        progressText.setText("Failed to read APK: " + t.getMessage());
                        showToast("Failed to read APK: " + t.getMessage());
                    });
                }
            }).start();
        } else if (requestCode == REQUEST_PICK_SO_FILE) {
            new Thread(() -> {
                try {
                    String fileName = (displayName != null && !displayName.isEmpty())
                            ? displayName : "libcustom.so";
                    File tempSo = new File(getCacheDir(), "picked_" + System.currentTimeMillis() + ".so");
                    try (InputStream is = getContentResolver().openInputStream(pickedUri);
                         FileOutputStream fos = new FileOutputStream(tempSo)) {
                        if (is == null) throw new IOException("Cannot open selected .so file");
                        byte[] buf = new byte[32768];
                        int r;
                        while ((r = is.read(buf)) != -1) {
                            fos.write(buf, 0, r);
                        }
                        fos.flush();
                    }
                    importCustomSoFromFile(tempSo, fileName);
                } catch (Throwable t) {
                    runOnUiThread(() -> showToast("Failed to read .so file: " + t.getMessage()));
                }
            }).start();
        }
    }

    private TextView buildAppleSheetButton(String text, String fillHex, String textHex, Typeface tf) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(textHex));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tv.setTypeface(tf);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(14), dpToPx(11), dpToPx(14), dpToPx(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(fillHex));
        bg.setCornerRadius(dpToPx(12));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void startCountdownTimer() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String expiryStr = pp();
                    if (expiryStr == null || expiryStr.isEmpty()) {
                        setCountdown(0, 0, 0, 0);
                        return;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    Date expiry = sdf.parse(expiryStr);
                    long diff = expiry.getTime() - System.currentTimeMillis();
                    if (diff <= 0) {
                        setCountdown(0, 0, 0, 0);
                        showToast("License expired!");
                        prefs.setSt("USER", "");
                        startActivity(new Intent(MAct.this, LogAct.class));
                        finish();
                        return;
                    }
                    long days = TimeUnit.MILLISECONDS.toDays(diff);
                    long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    setCountdown(days, hours, minutes, seconds);
                } catch (Exception e) {
                    setCountdown(0, 0, 0, 0);
                }
                countdownHandler.postDelayed(this, 1000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void setCountdown(long d, long h, long m, long s) {
        if (tvD != null) tvD.setText(String.valueOf(d));
        if (tvH != null) tvH.setText(String.format(Locale.US, "%02d", h));
        if (tvM != null) tvM.setText(String.format(Locale.US, "%02d", m));
        if (tvS != null) tvS.setText(String.format(Locale.US, "%02d", s));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LoaderLog.i("MAct", "onPause (leaving foreground)");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoaderLog.i("MAct", "onDestroy");
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        instance = null;
    }

    private boolean isExpiryDateValid() {
        try {
            String expiryDateStr = pp();
            if (expiryDateStr == null || expiryDateStr.isEmpty() || !expiryDateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return false;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date expiryDate = dateFormat.parse(expiryDateStr);
            long now = System.currentTimeMillis();
            return expiryDate.getTime() > now;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean ensureAllFilesAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true;
        }
        if (Environment.isExternalStorageManager()) {
            return true;
        }
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Storage permission needed")
                    .setMessage("Allow \"All files access\" so the game reuses your existing OBB/data instead of downloading again.")
                    .setPositiveButton("Allow", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 9001);
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            } catch (Exception ignored) {
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e("MAct", "All files access dialog failed", e);
        }
        return false;
    }

    private boolean needsVirtualGameSync() {
        try {
            if (!FeaturesticBoxCore.get().isInstalled(PKG_BGMI, USER_ID)) {
                return true;
            }
            PackageInfo hostPi = getPackageManager().getPackageInfo(PKG_BGMI, 0);
            PackageInfo virtPi = FeaturesticBoxCore.getBPackageManager().getPackageInfo(PKG_BGMI, 0, USER_ID);
            if (hostPi != null && virtPi != null && hostPi.versionCode != virtPi.versionCode) {
                return true;
            }
            File appLibDir = BEnvironment.getAppLibDir(PKG_BGMI);
            File[] libs = appLibDir != null ? appLibDir.listFiles((d, n) -> n != null && n.endsWith(".so")) : null;
            if (libs == null || libs.length == 0) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void startGameSequence() {
        LoaderLog.i("GAME", "startGameSequence pkg=" + PKG_BGMI + " needsSync=" + needsVirtualGameSync());
        if (!ensureAllFilesAccess()) {
            LoaderLog.e("GAME", "all-files access missing, aborting launch");
            progressText.setText("Allow 'All files access' to reuse game OBB/data");
            return;
        }
        if (needsVirtualGameSync()) {
            LoaderLog.i("GAME", "cloning game into sandbox...");
            progressText.setText("Cloning & Optimizing Game Engine...");
            progressBar.setIndeterminate(true);
            new Thread(() -> {
                InstallResult result = FeaturesticBoxCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
                LoaderLog.i("GAME", "sandbox install result=" + (result == null ? "null"
                        : ("success=" + result.success + " pkg=" + result.packageName + " msg=" + result.msg)));
                runOnUiThread(() -> {
                    progressBar.setIndeterminate(false);
                    if (result != null && result.success) {
                        checkAndCopyObb();
                    } else {
                        progressText.setText("Install Failed: " + (result != null ? result.msg : "Host game not found"));
                    }
                });
            }).start();
        } else {
            checkAndCopyObb();
        }
    }

    private void checkAndCopyObb() {
        if (!isObbCopied) {
            copyObbFiles();
        } else {
            launchGameNow();
        }
    }

    private void launchGameNow() {
        progressText.setText("Launching Game...");
        progressBar.setIndeterminate(true);
        new Handler(Looper.getMainLooper()).postDelayed(this::launchGame, 350);
    }

    private void showLogsDialog() {
        String content = LoaderLog.readTail(150000);
        if (content.isEmpty()) {
            content = "(log is empty)";
        }
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(content);
        tv.setTextSize(9f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(Color.parseColor("#E5E5EA"));
        tv.setBackgroundColor(Color.parseColor("#0B0E21"));
        int p = dpToPx(10);
        tv.setPadding(p, p, p, p);
        tv.setTextIsSelectable(true);
        sv.addView(tv);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Loader Logs • " + LoaderLog.getDirPath())
                .setView(sv)
                .setPositiveButton("Share", (d, w) -> shareLogs())
                .setNeutralButton("Logcat", (d, w) -> {
                    LoaderLog.captureLogcat();
                    showToast("Capturing logcat...");
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareLogs() {
        try {
            String text = LoaderLog.readTail(250000);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Featurestic Loader Logs");
            share.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(share, "Share Loader Logs"));
        } catch (Throwable t) {
            showToast("Share failed: " + t.getMessage());
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void copyObbFiles() {
        progressText.setText("Checking OBB Files...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";

                File obbDestDir = BEnvironment.getExternalObbDir(PKG_BGMI);
                if (!obbDestDir.exists()) obbDestDir.mkdirs();

                File obbSource = null;
                StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);

                for (StorageVolume storageVolume : sm.getStorageVolumes()) {
                    File obbDir;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        obbDir = storageVolume.getDirectory();
                    } else {
                        try {
                            Method getPathFile = StorageVolume.class.getMethod("getPathFile");
                            obbDir = (File) getPathFile.invoke(storageVolume);
                        } catch (Exception e) {
                            obbDir = null;
                        }
                    }

                    if (obbDir != null) {
                        File obbSubDir = new File(obbDir, "Android/obb/" + PKG_BGMI);
                        File obbFile = new File(obbSubDir, gameObb);
                        if (obbFile.exists() && obbFile.canRead()) {
                            obbSource = obbFile;
                            break;
                        }
                    }
                }

                File obbDestFile = new File(obbDestDir, gameObb);
                if (obbDestFile.exists() && obbSource != null && obbDestFile.length() != obbSource.length()) {
                    try {
                        obbDestFile.delete();
                    } catch (Throwable ignored) {
                    }
                }
                com.featurestic.box.utils.compat.FeaturesticZeroCopyLinker.linkGameObbAndMapPaks(PKG_BGMI, USER_ID);
                final File finalObbSource = obbSource;
                runOnUiThread(() -> {
                    isObbCopied = true;
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(100);
                    progressText.setText(finalObbSource != null
                            ? "Zero-Copy OBB Mounted • Starting Game..."
                            : "Storage Redirect Active • Starting Game...");
                    launchGameNow();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isObbCopied = true;
                    progressText.setText("Starting Game...");
                    launchGameNow();
                });
            }
        }).start();
    }

    public void Checking() {
        new DownloadZip(this).execute("1", mainURL());
    }

    @SuppressWarnings("deprecation")
    private boolean ischicken() {
        try {
            Signature[] signatures = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo != null && packageInfo.signingInfo != null) {
                    signatures = packageInfo.signingInfo.getApkContentsSigners();
                }
            }
            if (signatures == null || signatures.length == 0) {
                PackageInfo legacyPi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                if (legacyPi != null) {
                    signatures = legacyPi.signatures;
                }
            }
            if (signatures == null) return false;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) {
                byte[] digest = md.digest(signature.toByteArray());
                StringBuilder hash = new StringBuilder();
                for (byte b : digest) {
                    hash.append(String.format("%02X", b));
                }
                if (hash.toString().equals("E545B00E31AF8FC008566DE0B87C0B17FC467A90AEED71B00755B98698AE58BC")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void launchGame() {
        try {
            LoaderLog.i("GAME", "launchGame pkg=" + PKG_BGMI + " userId=" + USER_ID);
            if (!ischicken() || !verifyAuthenticatedSessionAndPanelGate() || !isExpiryDateValid()) {
                LoaderLog.e("GAME", "launch aborted: security/auth gate failed");
                showToast("Security error: Unauthorized session or login bypass detected!");
                startActivity(new Intent(this, LogAct.class));
                finish();
                return;
            }
            autoVerifyAndHealGameEnvironment(PKG_BGMI);
            String ggPkg = resolveInstalledGgPackage();
            if (ggPkg != null && !ggPkg.isEmpty()) {
                
            }
            String label = getGameLabelForPkg(PKG_BGMI);
            boolean ok = FeaturesticBoxCore.get().launchApk(PKG_BGMI, USER_ID);
            LoaderLog.i("GAME", "launchApk(" + PKG_BGMI + ") ok=" + ok);
            if (ok) {
                showToast("Launching " + label + "...");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (progressBar != null) progressBar.setIndeterminate(false);
                    if (progressCard != null) progressCard.setVisibility(View.GONE);
                }, 1200L);
            } else {
                if (progressBar != null) progressBar.setIndeterminate(false);
                if (progressText != null) progressText.setText("Launch failed for " + label + ". Re-cloning...");
                new Thread(() -> {
                    InstallResult res = FeaturesticBoxCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
                    runOnUiThread(() -> {
                        if (res != null && res.success) {
                            FeaturesticBoxCore.get().launchApk(PKG_BGMI, USER_ID);
                        } else {
                            showToast("Could not launch " + label);
                        }
                    });
                }).start();
            }
        } catch (Exception e) {
            LoaderLog.e("GAME", "launchGame exception", e);
            if (progressBar != null) progressBar.setIndeterminate(false);
            showToast("Launch failed: " + e.getMessage());
        }
    }

    private GradientDrawable createRoundRectDrawable(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }

    private String getGameLabelForPkg(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "Game";
        for (String[] pair : SUPPORTED_GAMES) {
            if (pair[0].equalsIgnoreCase(pkg)) return pair[1];
        }
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            if (ai != null) {
                CharSequence cs = ai.loadLabel(pm);
                if (cs != null && cs.length() > 0) {
                    return cs.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return pkg;
    }

    /**
     * Automatically detects ALL games installed on the user's phone:
     * 1. Checks known popular games (`SUPPORTED_GAMES`) first.
     * 2. Scans all third-party installed packages for `ApplicationInfo.CATEGORY_GAME`,
     *    native game engines (`libUE4.so`, `libil2cpp.so`, `libunity.so`, `libcocos2dcpp.so`, `libmain.so`, `libanogs.so`),
     *    or existing OBB directories (`/sdcard/Android/obb/<pkg>`).
     */
    private List<String[]> detectAllInstalledGamesOnHost() {
        List<String[]> found = new ArrayList<>();
        Set<String> seenPkgs = new HashSet<>();
        PackageManager pm = getPackageManager();

        for (String[] pair : SUPPORTED_GAMES) {
            try {
                pm.getPackageInfo(pair[0], 0);
                if (seenPkgs.add(pair[0])) {
                    found.add(new String[]{pair[0], pair[1]});
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            List<PackageInfo> allPkgs = pm.getInstalledPackages(0);
            if (allPkgs != null) {
                for (PackageInfo pi : allPkgs) {
                    if (pi == null || pi.packageName == null || pi.applicationInfo == null) continue;
                    String pkg = pi.packageName;
                    if (seenPkgs.contains(pkg)) continue;
                    if (pkg.equals(getPackageName())
                            || FboxCompat.isKnownCompanionPackage(pkg)
                            || pkg.startsWith("com.google.android.")
                            || pkg.startsWith("com.android.")) {
                        continue;
                    }
                    boolean isSystem = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            && (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                    if (isSystem) continue;

                    boolean isGame = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && pi.applicationInfo.category == ApplicationInfo.CATEGORY_GAME) {
                        isGame = true;
                    }
                    if (!isGame && pi.applicationInfo.nativeLibraryDir != null) {
                        File libDir = new File(pi.applicationInfo.nativeLibraryDir);
                        if (new File(libDir, "libUE4.so").exists()
                                || new File(libDir, "libil2cpp.so").exists()
                                || new File(libDir, "libunity.so").exists()
                                || new File(libDir, "libanogs.so").exists()
                                || new File(libDir, "libcocos2dcpp.so").exists()
                                || new File(libDir, "libmain.so").exists()) {
                            isGame = true;
                        }
                    }
                    if (!isGame) {
                        File obbDir = new File("/sdcard/Android/obb/" + pkg);
                        if (obbDir.isDirectory()) {
                            File[] obbs = obbDir.listFiles((d, n) -> n != null && n.endsWith(".obb"));
                            if (obbs != null && obbs.length > 0) {
                                isGame = true;
                            }
                        }
                    }
                    if (isGame && seenPkgs.add(pkg)) {
                        CharSequence cs = pi.applicationInfo.loadLabel(pm);
                        String label = (cs != null && cs.length() > 0) ? cs.toString() : pkg;
                        found.add(new String[]{pkg, label});
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return found;
    }

    private void resolveActiveTargetGameAndSlot() {
        try {
            String slotStr = prefs.getSt(PREF_ACTIVE_SLOT, "0");
            int parsedSlot = Integer.parseInt(slotStr);
            if (parsedSlot >= 0 && parsedSlot <= 4) {
                USER_ID = parsedSlot;
            }
        } catch (Throwable ignored) {
            USER_ID = 0;
        }
        List<String[]> installedGames = detectAllInstalledGamesOnHost();
        String savedPkg = prefs.getSt(PREF_TARGET_GAME_PKG, "");
        PackageManager pm = getPackageManager();
        if (savedPkg != null && !savedPkg.isEmpty()) {
            try {
                pm.getPackageInfo(savedPkg, 0);
                PKG_BGMI = savedPkg;
                game = savedPkg;
                updateStartButtonText();
                return;
            } catch (Throwable ignored) {
            }
        }
        if (!installedGames.isEmpty()) {
            String[] first = installedGames.get(0);
            PKG_BGMI = first[0];
            game = first[0];
            prefs.setSt(PREF_TARGET_GAME_PKG, first[0]);
        }
        updateStartButtonText();
    }

    private void updateStartButtonText() {
        String label = getGameLabelForPkg(PKG_BGMI);
        List<String[]> installedGames = detectAllInstalledGamesOnHost();
        if (startBgmi != null) {
            String upper = label.toUpperCase(Locale.US);
            if (USER_ID > 0) {
                startBgmi.setText("START " + upper + " (SLOT " + (USER_ID + 1) + ")");
            } else {
                startBgmi.setText("START " + upper);
            }
        }
        if (tvLoaderInfo != null) {
            if (installedGames.size() > 1) {
                tvLoaderInfo.setText("Target: " + label + " (" + installedGames.size() + " Games • Tap to Switch)");
            } else {
                tvLoaderInfo.setText("Target: " + label + " • Slot " + (USER_ID + 1) + " (Tap to Change)");
            }
        }
        if (btnSwitchGameBar != null) {
            btnSwitchGameBar.setText("Target Game: " + label + " • Slot " + (USER_ID + 1) + "   [SWITCH GAME / SLOT]");
        }
    }

    private void showTargetGameAndSlotDialog(final boolean launchAfterSelect) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B8000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface regTf = Typeface.create("sans-serif", Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText(launchAfterSelect ? "Choose Target Game to Launch" : "Target Game & Account Slot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        List<String[]> installedGames = detectAllInstalledGamesOnHost();

        TextView sub = new TextView(this);
        sub.setText(launchAfterSelect
                ? installedGames.size() + " games detected on your phone. Tap the game you want to launch:"
                : "Active: " + getGameLabelForPkg(PKG_BGMI) + " • Account Slot " + (USER_ID + 1));
        sub.setTextColor(Color.parseColor("#30D158"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(10));
        card.addView(sub);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        for (final String[] pair : installedGames) {
            boolean activeGame = pair[0].equalsIgnoreCase(PKG_BGMI);
            TextView gameBtn = buildAppleSheetButton(
                    (activeGame ? "[✓] " : "") + pair[1] + " (" + pair[0] + ")",
                    activeGame ? "#1A30D158" : "#2C2C2E",
                    activeGame ? "#30D158" : "#FFFFFF",
                    boldTf);
            gameBtn.setOnClickListener(v -> {
                PKG_BGMI = pair[0];
                game = pair[0];
                isObbCopied = false;
                prefs.setSt(PREF_TARGET_GAME_PKG, pair[0]);
                updateStartButtonText();
                dialog.dismiss();
                showToast("Target Game: " + pair[1]);
                if (launchAfterSelect) {
                    continueStartAfterGameSelected();
                }
            });
            list.addView(gameBtn);
        }

        TextView slotHdr = new TextView(this);
        slotHdr.setText("VIRTUAL ACCOUNT SLOT (MULTI-ID)");
        slotHdr.setTextColor(Color.parseColor("#8E8E93"));
        slotHdr.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        slotHdr.setTypeface(boldTf);
        slotHdr.setPadding(0, dpToPx(12), 0, dpToPx(4));
        list.addView(slotHdr);

        for (int slot = 0; slot < 3; slot++) {
            final int targetSlot = slot;
            boolean isCurrent = (USER_ID == slot);
            TextView slotBtn = buildAppleSheetButton(
                    (isCurrent ? "[✓] " : "") + "Account Slot " + (slot + 1) + (slot == 0 ? " (Main ID)" : " (Clone ID #" + (slot + 1) + ")"),
                    isCurrent ? "#1A0A84FF" : "#2C2C2E",
                    isCurrent ? "#0A84FF" : "#FFFFFF",
                    regTf);
            slotBtn.setOnClickListener(v -> {
                USER_ID = targetSlot;
                prefs.setSt(PREF_ACTIVE_SLOT, String.valueOf(targetSlot));
                FeaturesticBoxCore.get().switchAccountSlot(PKG_BGMI, targetSlot, false);
                updateStartButtonText();
                dialog.dismiss();
                showToast("Switched to Account Slot " + (targetSlot + 1));
                if (launchAfterSelect) {
                    continueStartAfterGameSelected();
                }
            });
            list.addView(slotBtn);
        }

        sv.addView(list);
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView btnClose = buildAppleSheetButton("Cancel", "#2C2C2E", "#8E8E93", regTf);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        card.addView(btnClose);

        int w = Math.min(dpToPx(345), (int) (getResources().getDisplayMetrics().widthPixels * 0.92f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private boolean verifyAuthenticatedSessionAndPanelGate() {
        try {
            String savedKey = prefs != null ? prefs.getSt("USER", "") : "";
            if (savedKey == null || savedKey.trim().isEmpty()) {
                return false;
            }
            if (!LogAct.isSessionAuthenticatedForKey(this, savedKey.trim())) {
                return false;
            }
            String nativeToken = "";
            try {
                nativeToken = ZENINOP();
            } catch (Throwable ignored) {
            }
            if (nativeToken != null && !nativeToken.isEmpty()) {
                String expected = LogAct.computeJavaSessionToken(this, savedKey.trim());
                if (!expected.startsWith(nativeToken) && !nativeToken.startsWith(expected.substring(0, Math.min(32, expected.length())))) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void autoVerifyAndHealGameEnvironment(String targetPkg) {
        if (targetPkg == null || targetPkg.isEmpty()) return;
        try {
            // 1. Enforce Android 14-16 Read-Only DEX/APK guard on virtual app directory
            File virtualAppDir = BEnvironment.getAppDir(targetPkg);
            if (virtualAppDir != null && virtualAppDir.isDirectory()) {
                enforceReadOnlyApksAndDexRecursive(virtualAppDir);
            }
            // 2. Clean stale cache/ and code_cache/ files older than 24h or temporary .tmp files
            File virtualDataDir = BEnvironment.getDataDir(targetPkg, USER_ID);
            if (virtualDataDir != null && virtualDataDir.isDirectory()) {
                cleanDirectoryContentsSafely(new File(virtualDataDir, "cache"), false);
                cleanDirectoryContentsSafely(new File(virtualDataDir, "code_cache"), false);
            }
            // 3. Verify host OBB directory & ensure virtual OBB mount directory exists
            File hostObbDir = new File(Environment.getExternalStorageDirectory(), "Android/obb/" + targetPkg);
            if (hostObbDir.isDirectory()) {
                File[] obbs = hostObbDir.listFiles((dir, name) -> name != null && name.endsWith(".obb"));
                if (obbs != null) {
                    for (File obb : obbs) {
                        if (obb != null && obb.isFile() && obb.length() == 0L) {
                            runOnUiThread(() -> showToast("Warning: Corrupted (0-byte) OBB detected for " + targetPkg));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void enforceReadOnlyApksAndDexRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f == null) continue;
            if (f.isDirectory()) {
                enforceReadOnlyApksAndDexRecursive(f);
            } else {
                String n = f.getName().toLowerCase(Locale.US);
                if (n.endsWith(".apk") || n.endsWith(".dex") || n.endsWith(".odex") || n.endsWith(".vdex")) {
                    try {
                        f.setReadable(true, false);
                        if (Build.VERSION.SDK_INT >= 34) {
                            f.setReadOnly();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    private void cleanDirectoryContentsSafely(File dir, boolean deleteSelf) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f == null) continue;
                if (f.isDirectory()) {
                    cleanDirectoryContentsSafely(f, true);
                } else {
                    try {
                        f.delete();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (deleteSelf) {
            try {
                dir.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private void preWarmVirtualSandboxAsync() {
        Thread warmThread = new Thread(() -> {
            try {
                String targetPkg = PKG_BGMI;
                autoVerifyAndHealGameEnvironment(targetPkg);
                FeaturesticBoxCore.getBPackageManager().isInstalled(targetPkg, USER_ID);
                FeaturesticBoxCore.getBActivityManager();
            } catch (Throwable ignored) {
            }
        }, "fbox-sandbox-prewarm");
        warmThread.setDaemon(true);
        warmThread.start();
    }


    private void launchCompanionAppCleanly(final String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        LoaderLog.i("GG", "launchCompanionAppCleanly pkg=" + pkg);
        try {
            FboxCompat.notifyCompanionPackageInstalled(pkg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                showToast("Please allow 'Display over other apps' for the floating icon!");
                try {
                    Intent ov = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(ov);
                } catch (Throwable ignored) {
                }
            }
            new Thread(() -> {
                try {
                    Intent launchIntent = FeaturesticBoxCore.getBPackageManager().getLaunchIntentForPackage(pkg, USER_ID);
                    LoaderLog.i("GG", "launch intent=" + (launchIntent == null ? "null" : launchIntent.getComponent()));
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        FeaturesticBoxCore.getBActivityManager().startActivity(launchIntent, USER_ID);
                        LoaderLog.i("GG", "startActivity issued for " + pkg);
                    } else {
                        boolean ok = FeaturesticBoxCore.get().launchApk(pkg, USER_ID);
                        LoaderLog.i("GG", "launchApk fallback ok=" + ok);
                    }
                } catch (Throwable e) {
                    LoaderLog.e("GG", "companion launch failed", e);
                    runOnUiThread(() -> showToast("Failed to launch " + pkg + ": " + e.getMessage()));
                }
            }, "fbox-clean-gg-launch").start();
        } catch (Throwable e) {
            showToast("Launch error: " + e.getMessage());
        }
    }


    private void checkAndShowPendingOffsetCrashPopup() {
        try {
            File crashFile = BoxApplication.getSharedOffsetCrashLogFile();
            if (crashFile == null || !crashFile.isFile() || crashFile.length() == 0) {
                crashFile = new File(getFilesDir(), "featurestic_offset_crash.log");
            }
            if (!crashFile.isFile() || crashFile.length() == 0) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(crashFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            crashFile.delete();
            final String reportText = sb.toString().trim();
            if (reportText.isEmpty()) return;
            showOffsetCrashPopupDialog(reportText);
        } catch (Throwable ignored) {
        }
    }

    private void showOffsetCrashPopupDialog(final String crashReport) {
        if (isFinishing() || isDestroyed()) return;
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.parseColor("#B8000000"));
        int pad = dpToPx(20);
        outer.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1C1C1E"));
        cardBg.setCornerRadius(dpToPx(20));
        cardBg.setStroke(dpToPx(1), Color.parseColor("#FF453A"));
        card.setBackground(cardBg);

        Typeface boldTf = Typeface.create("sans-serif-medium", Typeface.BOLD);
        Typeface monoTf = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

        TextView title = new TextView(this);
        title.setText("Offset Crash Shield Report");
        title.setTextColor(Color.parseColor("#FF453A"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        title.setTypeface(boldTf);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText("A faulting .so offset was caught by the Crash Shield. Copy the offset below to fix or disable it:");
        sub.setTextColor(Color.parseColor("#AEAEB2"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setPadding(0, dpToPx(4), 0, dpToPx(10));
        card.addView(sub);

        ScrollView sv = new ScrollView(this);
        TextView reportTv = new TextView(this);
        reportTv.setText(crashReport);
        reportTv.setTextColor(Color.parseColor("#30D158"));
        reportTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        reportTv.setTypeface(monoTf);
        int boxPad = dpToPx(12);
        reportTv.setPadding(boxPad, boxPad, boxPad, boxPad);
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.parseColor("#121214"));
        boxBg.setCornerRadius(dpToPx(12));
        boxBg.setStroke(dpToPx(1), Color.parseColor("#2C2C2E"));
        reportTv.setBackground(boxBg);
        sv.addView(reportTv);

        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.bottomMargin = dpToPx(14);
        card.addView(sv, svLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCopy = buildAppleSheetButton("Copy Offset", "#0A84FF", "#FFFFFF", boldTf);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        copyLp.rightMargin = dpToPx(6);
        btnCopy.setLayoutParams(copyLp);
        btnCopy.setOnClickListener(v -> {
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Crash Offset Report", crashReport));
                    showToast("Crash Offset Copied!");
                }
            } catch (Throwable ignored) {
            }
        });

        TextView btnOk = buildAppleSheetButton("OK", "#30D158", "#000000", boldTf);
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        okLp.leftMargin = dpToPx(6);
        btnOk.setLayoutParams(okLp);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        btnRow.addView(btnCopy);
        btnRow.addView(btnOk);
        card.addView(btnRow);

        int w = Math.min(dpToPx(350), (int) (getResources().getDisplayMetrics().widthPixels * 0.92f));
        outer.addView(card, new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(outer);
        dialog.show();
    }

}
