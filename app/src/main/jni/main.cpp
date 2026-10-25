
//#include <obfuscate.h>
#include "oxorany.h"
#include <openssl/sha.h>
#include "StrEnc.h"
#include <zlib.h>  // For CRC32
#include <pthread.h>
#include <jni.h>
#include <string>
#include "ESP.h"
#include "Hacks.h"

ESP espOverlay;
int type = 1, utype = 2;



extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_Overlay_DrawOn(JNIEnv *env, jclass , jobject espView, jobject canvas) {
    espOverlay = ESP(env, espView, canvas);
    if (espOverlay.isValid()){
        DrawESP(espOverlay, espOverlay.getWidth(), espOverlay.getHeight());
    }
    espOverlay.Release();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_pp(JNIEnv *env, jclass clazz) {
    if (!xConnected || !xServerConnection || !memek || ZENINOP[0] == '\0') {
        return env->NewStringUTF("NULL");
    }
    return env->NewStringUTF(exdate.c_str());
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_bgmi_MAct_ZENINOP(JNIEnv *env, jobject activityObject) {
    if (!xConnected || !xServerConnection || !memek) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(ZENINOP);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_LogAct_GetKey(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(oxorany("https://t.me/")); // Link Channel
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_utils_Downtwo_Version(JNIEnv *env, jclass clazz) {
    // return URL to version file
    const char *versionUrl = (oxorany("https://github.com/BGMILITEOP558/Op098/releases/download/Op773/version.txt"));// can put GitHub too
    return env->NewStringUTF(versionUrl);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bgmi_utils_Downtwo_Link(JNIEnv *env, jclass clazz) {
    // return URL to your update ZIP file
    const char *downloadUrl = (oxorany("https://github.com/BGMILITEOP558/Op098/releases/download/Op773/PRIVATE_SRC_FILES.zip"));// can put GitHub too
    return env->NewStringUTF(downloadUrl);
}


extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_Overlay_Close(JNIEnv *, jobject) {
    Close();
    options.openState = -1;
    options.aimBullet = -1;
    options.aimT = -1;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_bgmi_floating_Overlay_getReady(JNIEnv *, jobject thiz) {
    int sockCheck = 1;

    if (!Create()) {
        perror("Creation failed");
        return false;
    }
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &sockCheck, sizeof(int));
    if (!Bind()) {
        perror("Bind failed");
        return false;
    }

    if (!Listen()) {
        perror("Listen failed");
        return false;
    }
    if (Accept()) {
        return true;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleAim_ToggleAim(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.openState = 0;
    else
        options.openState = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleBullet_ToggleBullet(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimBullet = 0;
    else
        options.aimBullet = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_ToggleSimulation_ToggleSimulation(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimT = 0;
    else
        options.aimT = -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingValue(JNIEnv *, jobject, jint code, jboolean jboolean1) {

    switch ((int) code) {
        case 2:
            isPlayerLine = jboolean1;
            break;
        case 3:
            isPlayerBox = jboolean1;
            break;
        case 4:
            isSkeleton = jboolean1;
            break;
        case 5:
            isPlayerDistance = jboolean1;
            break;
        case 6:
            isPlayerHealth = jboolean1;
            break;
        case 7:
            isPlayerName = jboolean1;
            break;
        case 8:
            isPlayerHead = jboolean1;
            break;
        case 9:
            is360Alertv2 = jboolean1;
            break;
        case 10:
            isPlayerWeapon = jboolean1;
            break;
        case 11:
            isGrenadeWarning = jboolean1;
            break;
        case 12:
          //  isVehicles = jboolean1;
            break;
        case 13:
          //  isItems = jboolean1;
            break;
        case 14:
           // isLootBox = jboolean1;
            break;
        case 15:
            options.ignoreAi = jboolean1;
            break;
        case 16:
            isPlayerWeaponIcon = jboolean1;
            break;
        
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingAim(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            options.openState = -1;
            break;
        case 2:
            options.aimBullet = -1;
            break;
        case 3:
            options.pour = value;
            break;
        case 4:
            options.ignoreBot = value;
            break;
        case 5:
            options.InputInversion = value;
            break;
        case 6:
            options.tracingStatus = value;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SettingMemory(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.LessRecoil = value;
            break;
        case 2:
            otherFeature.SmallCrosshair = value;
            break;
        case 3:
            otherFeature.Aimbot = value;
            break;
        case 4:
            otherFeature.WideView = value;
            break;
          
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Range(JNIEnv *, jobject, jint range) {
    options.aimingRange = 1 + range;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_distances(JNIEnv *, jobject, jint distances) {
    options.aimingDist = distances;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil2(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe1 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_recoil3(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe2 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Bulletspeed(JNIEnv *env, jobject thiz, jint bulletspeed) {
    options.aimingSpeed = bulletspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimingSpeed(JNIEnv *env, jobject thiz, jint aimingspeed) {
    options.touchSpeed = aimingspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Smoothness(JNIEnv *env, jobject thiz, jint smoothness) {
    options.Smoothing = smoothness;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchSize(JNIEnv *env, jobject thiz, jint touchsize) {
    options.touchSize = touchsize;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchPosX(JNIEnv *env, jobject thiz, jint touchposx) {
    options.touchX = touchposx;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_TouchPosY(JNIEnv *env, jobject thiz, jint touchposy) {
    options.touchY = touchposy;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_WideView(JNIEnv *env, jobject thiz, jint wideview) {
    otherFeature.WideView = wideview;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Target(JNIEnv *, jobject, jint target) {
    options.aimbotmode = target;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimWhen(JNIEnv *, jobject, jint state) {
    options.aimingState = state;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_AimBy(JNIEnv *, jobject, jint aimby) {
    options.priority = aimby;
}
extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_RadarSize(JNIEnv *, jobject, jint size) {
    request.radarSize = size;
}





extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_SkinHack(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:  otherFeature.clothes = 1; break;   // Blood Raven
        case 2:  otherFeature.clothes = 2; break;   // Golden Pharaoh
        case 3:  otherFeature.clothes = 3; break;   // Avalanche
        case 4:  otherFeature.clothes = 4; break;   // Poseidon
        case 5:  otherFeature.clothes = 5; break;   // Arcane Jester
        case 6:  otherFeature.clothes = 6; break;   // Silvanus
        case 7:  otherFeature.clothes = 7; break;   // Marmoris
        case 8:  otherFeature.clothes = 8; break;   // Fiore
        case 9:  otherFeature.clothes = 9; break;   // Ignis
        case 10: otherFeature.clothes = 10; break;  // White Mummy (no)
        case 11: otherFeature.clothes = 11; break;  // Galadria
        case 12: otherFeature.clothes = 12; break;  // Flamewraith Set
        case 13: otherFeature.clothes = 13; break;  // Majestic Cavalry Set
        case 14: otherFeature.clothes = 14; break;  // Bramble Overlord Set
        case 15: otherFeature.clothes = 15; break;  // Nether Visage Set
        case 16: otherFeature.clothes = 16; break;  // Spectral Swan Set
        case 17: otherFeature.clothes = 17; break;  // Untamed Celestial
        case 18: otherFeature.clothes = 18; break;  // Snowstar Sweetheart
        case 19: otherFeature.clothes = 19; break;  // Arctic Conqueror
        case 20: otherFeature.clothes = 20; break;  // Feral Ravager
        case 21: otherFeature.clothes = 21; break;  // Vampyra Countess
        case 22: otherFeature.clothes = 22; break;  // Serene Lumina
        case 23: otherFeature.clothes = 23; break;  // Mercury Soldier
        case 24: otherFeature.clothes = 24; break;  // Luminous Muse
        case 25: otherFeature.clothes = 25; break;  // Origin Lumen
        case 26: otherFeature.clothes = 26; break;  // Serpengleam
        case 27: otherFeature.clothes = 27; break;  // Shinobi Spirit
        case 28: otherFeature.clothes = 28; break;  // Foxy Flare
        case 29: otherFeature.clothes = 29; break;  // Glacial Bride
        case 30: otherFeature.clothes = 30; break;  // Boxerbolt
        case 31: otherFeature.clothes = 31; break;  // Dandy Groovster
        case 32: otherFeature.clothes = 32; break;  // Wrathful Neptune
        case 33: otherFeature.clothes = 33; break;  // Noctum Sunder
        case 34: otherFeature.clothes = 34; break;  // Crimson Ephialtes
        default:
            otherFeature.clothes = 0; // reset or no selection
            break;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Skinbag(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.bag = 1; // Poseidon
            break;
        case 2:
            otherFeature.bag = 2; // Mystique Splendor
            break;
        case 3:
            otherFeature.bag = 3; // Ancient Civilization
            break;
        case 4:
            otherFeature.bag = 4; // Galadria
            break;
        case 5:
            otherFeature.bag = 5; // Alfheim Wonder
            break;
        
        
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bgmi_floating_FloatService_Skinhelmet(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:
            otherFeature.helmet = 1; // Inferno Rider
            break;
        case 2:
            otherFeature.helmet = 2; // Auric Sentinel
            break;
        case 3:
            otherFeature.helmet = 3; // Galadria
            break;
        case 4:
            otherFeature.helmet = 4; // Shining Eagle
            break;
        case 5:
            otherFeature.helmet = 5; // Eternal Kingdom
            break;
        
    }
}














#include <signal.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <dlfcn.h>
#include <link.h>
#include <elf.h>
#include <fcntl.h>
#include <errno.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>

static char gCrashReportFilePath[512] = {0};
static struct sigaction gOldSegvSa;
static struct sigaction gOldBusSa;
static struct sigaction gOldIllSa;
static volatile bool gWatchdogStarted = false;

static volatile uintptr_t gLastFaultPc = 0;
static volatile uintptr_t gLastFaultAddr = 0;
static volatile int gRepeatFaultCount = 0;
static volatile bool gPopupReportWrittenForCurrentPc = false;
static volatile uintptr_t gLastUnlockedPage = 0;

static long getSystemPageSize() {
    long ps = sysconf(_SC_PAGESIZE);
    return (ps > 0) ? ps : 4096;
}

/**
 * Unlocks a single page or range to PROT_READ | PROT_WRITE | PROT_EXEC (rwxp).
 * On Android 10-16, virtualized game libraries (libanogs.so, libUE4.so) reside inside
 * /data/user/0/<host_pkg>/... which carries SELinux label `app_data_file`.
 * Kernel SELinux blocks `execmod` (EACCES) when calling mprotect(RWX) or mprotect(RX) on a
 * COW-modified `app_data_file` page. If a .so plugin falls back to mprotect(RW), it strips
 * PROT_EXEC from libanogs.so code pages, causing an immediate SEGV_ACCERR crash 2-3 seconds
 * later when libanogs.so executes any function on that 4KB page!
 *
 * When direct syscall(__NR_mprotect, ..., RWX) returns EACCES, we copy the page bytes into a
 * buffer and replace that page in-place with an identical MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED
 * mapping (vm_file == NULL -> SELinux checks `execmem` instead of `app_data_file:execmod`),
 * preserving both the patched bytes AND PROT_EXEC!
 */
static bool unlockOrRemapPageRwx(uintptr_t pageBase, size_t pageSize) {
    if (pageBase < 0x1000 || pageSize == 0) return false;
    if (syscall(__NR_mprotect, reinterpret_cast<void *>(pageBase), pageSize,
                PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
        __builtin___clear_cache(reinterpret_cast<char *>(pageBase),
                                reinterpret_cast<char *>(pageBase + pageSize));
        return true;
    }

    // Ensure the page is readable before backing up its current contents (including any patch already written)
    syscall(__NR_mprotect, reinterpret_cast<void *>(pageBase), pageSize, PROT_READ | PROT_EXEC);
    syscall(__NR_mprotect, reinterpret_cast<void *>(pageBase), pageSize, PROT_READ);

    uint8_t stackBuf[16384];
    uint8_t *backup = (pageSize <= sizeof(stackBuf)) ? stackBuf : static_cast<uint8_t *>(malloc(pageSize));
    if (!backup) return false;

    memcpy(backup, reinterpret_cast<const void *>(pageBase), pageSize);

    void *remapped = mmap(reinterpret_cast<void *>(pageBase), pageSize,
                          PROT_READ | PROT_WRITE | PROT_EXEC,
                          MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0);
    if (remapped != MAP_FAILED) {
        memcpy(reinterpret_cast<void *>(pageBase), backup, pageSize);
        __builtin___clear_cache(reinterpret_cast<char *>(pageBase),
                                reinterpret_cast<char *>(pageBase + pageSize));
        if (backup != stackBuf) free(backup);
        return true;
    }

    if (backup != stackBuf) free(backup);
    // Last-resort fallback: restore PROT_READ | PROT_EXEC so code execution never crashes
    syscall(__NR_mprotect, reinterpret_cast<void *>(pageBase), pageSize, PROT_READ | PROT_EXEC);
    return false;
}

/**
 * Normalizes both Segment 0 (r--p at file_offset 0x0) and Segment 1 (--xp execute-only .text)
 * of libanogs.so, libUE4.so, libtersafe.so, etc. to PROT_READ | PROT_EXEC (r-xp).
 * Why this is critical for anogs offsets:
 * 1. In libUE4.so, Segment 0 (offset 0x0) is already r-xp.
 * 2. In libanogs.so, Segment 0 (offset 0x0) is r--p and Segment 1 (offset +0x4c000/+0x164000) is --xp.
 *    If only Segment 1 is unlocked to r-xp while Segment 0 stays r--p, .so plugins that search
 *    /proc/self/maps for "libanogs.so" + "r-xp" match Segment 1 instead of Segment 0 (0x0),
 *    shifting every anogs offset by +0x4c000/+0x164000 and crashing in 2-3 seconds!
 * 3. By setting both Segment 0 (r--p) and Segment 1 (--xp) to r-xp, the Linux kernel VMA merger
 *    combines them into a single r-xp VMA starting at file_offset 00000000 (identical to libUE4.so!).
 */
static void normalizeAnogsAndEngineSegments() {
    FILE *fp = fopen("/proc/self/maps", "rt");
    if (!fp) return;
    long pageSize = getSystemPageSize();
    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "libanogs.so") == nullptr
                && strstr(line, "libUE4.so") == nullptr
                && strstr(line, "libtersafe.so") == nullptr
                && strstr(line, "libgcloud.so") == nullptr
                && strstr(line, "libTDataMaster.so") == nullptr
                && strstr(line, "libil2cpp.so") == nullptr
                && strstr(line, "libunity.so") == nullptr) {
            continue;
        }
        unsigned long long startAddr = 0, endAddr = 0;
        char perms[8] = {0};
        if (sscanf(line, "%llx-%llx %7s", &startAddr, &endAddr, perms) == 3) {
            if (strstr(perms, "---") != nullptr) continue;
            // Skip writable data segments (rw-p); convert r--p and --xp to r-xp (PROT_READ | PROT_EXEC)
            if (strchr(perms, 'w') == nullptr && (strchr(perms, 'r') == nullptr || strchr(perms, 'x') == nullptr)) {
                uintptr_t alignedStart = static_cast<uintptr_t>(startAddr) & ~static_cast<uintptr_t>(pageSize - 1);
                uintptr_t alignedEnd = (static_cast<uintptr_t>(endAddr) + pageSize - 1) & ~static_cast<uintptr_t>(pageSize - 1);
                syscall(__NR_mprotect, reinterpret_cast<void *>(alignedStart),
                        alignedEnd - alignedStart, PROT_READ | PROT_EXEC);
            }
        }
    }
    fclose(fp);
}

/**
 * GOT-level mprotect wrapper for custom .so plugins (KittyMemory / MemoryPatch / A64HookFunction / Dobby).
 * Prevents Android 10-16 SELinux `app_data_file:execmod` failures and ensures PROT_EXEC is never
 * stripped from live libanogs.so or libUE4.so code pages during runtime patching.
 */
static int safe_mprotect_wrapper(void *addr, size_t len, int prot) {
    if (addr == nullptr || len == 0) {
        return static_cast<int>(syscall(__NR_mprotect, addr, len, prot));
    }
    long pageSize = getSystemPageSize();
    uintptr_t startPage = reinterpret_cast<uintptr_t>(addr) & ~static_cast<uintptr_t>(pageSize - 1);
    uintptr_t endPage = (reinterpret_cast<uintptr_t>(addr) + len + pageSize - 1) & ~static_cast<uintptr_t>(pageSize - 1);
    size_t alignedLen = endPage - startPage;

    // Always keep PROT_READ | PROT_EXEC when a plugin modifies or restores protections on code pages
    int desiredProt = prot | PROT_READ | PROT_EXEC;
    int directRes = static_cast<int>(syscall(__NR_mprotect, reinterpret_cast<void *>(startPage), alignedLen, desiredProt));
    if (directRes == 0) {
        return 0;
    }

    // If kernel mprotect failed with EACCES (SELinux app_data_file execmod), remap each page as anonymous RWX
    if (alignedLen <= 4 * 1024 * 1024) {
        bool allOk = true;
        for (uintptr_t p = startPage; p < endPage; p += static_cast<uintptr_t>(pageSize)) {
            if (!unlockOrRemapPageRwx(p, static_cast<size_t>(pageSize))) {
                allOk = false;
            }
        }
        if (allOk) {
            return 0;
        }
    }

    return static_cast<int>(syscall(__NR_mprotect, reinterpret_cast<void *>(startPage), alignedLen, prot));
}

#if defined(__LP64__)
typedef Elf64_Dyn Elf_Dyn_t;
typedef Elf64_Sym Elf_Sym_t;
typedef Elf64_Rela Elf_Rela_t;
typedef Elf64_Rel Elf_Rel_t;
#define ELF_R_SYM_FN ELF64_R_SYM
#else
typedef Elf32_Dyn Elf_Dyn_t;
typedef Elf32_Sym Elf_Sym_t;
typedef Elf32_Rela Elf_Rela_t;
typedef Elf32_Rel Elf_Rel_t;
#define ELF_R_SYM_FN ELF32_R_SYM
#endif

static int phdrHookMprotectCallback(struct dl_phdr_info *info, size_t, void *) {
    if (!info || !info->dlpi_name || info->dlpi_name[0] == 0) return 0;
    const char *name = info->dlpi_name;
    // Only hook custom loaded .so plugins in /data/; NEVER touch system libs or game/anti-cheat libs!
    if (strstr(name, "/data/") == nullptr) return 0;
    if (strstr(name, "libanogs.so") != nullptr
            || strstr(name, "libUE4.so") != nullptr
            || strstr(name, "libtersafe.so") != nullptr
            || strstr(name, "libgcloud.so") != nullptr
            || strstr(name, "libTDataMaster.so") != nullptr
            || strstr(name, "libil2cpp.so") != nullptr
            || strstr(name, "libunity.so") != nullptr
            || strstr(name, "libFeaturesticCore.so") != nullptr
            || strstr(name, "libc++_shared.so") != nullptr) {
        return 0;
    }

    uintptr_t base = static_cast<uintptr_t>(info->dlpi_addr);
    const Elf_Dyn_t *dyn = nullptr;
    for (int i = 0; i < info->dlpi_phnum; i++) {
        if (info->dlpi_phdr[i].p_type == PT_DYNAMIC) {
            dyn = reinterpret_cast<const Elf_Dyn_t *>(base + info->dlpi_phdr[i].p_vaddr);
            break;
        }
    }
    if (!dyn) return 0;

    const Elf_Sym_t *symtab = nullptr;
    const char *strtab = nullptr;
    uintptr_t jmprel = 0;
    size_t pltrelsz = 0;
    int pltrelType = 0;
    uintptr_t relaDyn = 0;
    size_t relaSz = 0;
    uintptr_t relDyn = 0;
    size_t relSz = 0;

    for (const Elf_Dyn_t *d = dyn; d->d_tag != DT_NULL; ++d) {
        switch (d->d_tag) {
            case DT_SYMTAB:
                symtab = reinterpret_cast<const Elf_Sym_t *>(d->d_un.d_ptr > base ? d->d_un.d_ptr : (base + d->d_un.d_ptr));
                break;
            case DT_STRTAB:
                strtab = reinterpret_cast<const char *>(d->d_un.d_ptr > base ? d->d_un.d_ptr : (base + d->d_un.d_ptr));
                break;
            case DT_JMPREL:
                jmprel = (d->d_un.d_ptr > base) ? d->d_un.d_ptr : (base + d->d_un.d_ptr);
                break;
            case DT_PLTRELSZ:
                pltrelsz = static_cast<size_t>(d->d_un.d_val);
                break;
            case DT_PLTREL:
                pltrelType = static_cast<int>(d->d_un.d_val);
                break;
            case DT_RELA:
                relaDyn = (d->d_un.d_ptr > base) ? d->d_un.d_ptr : (base + d->d_un.d_ptr);
                break;
            case DT_RELASZ:
                relaSz = static_cast<size_t>(d->d_un.d_val);
                break;
            case DT_REL:
                relDyn = (d->d_un.d_ptr > base) ? d->d_un.d_ptr : (base + d->d_un.d_ptr);
                break;
            case DT_RELSZ:
                relSz = static_cast<size_t>(d->d_un.d_val);
                break;
            default:
                break;
        }
    }
    if (!symtab || !strtab) return 0;

    long pageSize = getSystemPageSize();
    auto patchRelocSlot = [&](uintptr_t rOffset, size_t symIdx) {
        if (symIdx == 0) return;
        const char *symName = strtab + symtab[symIdx].st_name;
        if (strcmp(symName, "mprotect") != 0) return;
        void **gotSlot = reinterpret_cast<void **>(base + rOffset);
        if (*gotSlot == reinterpret_cast<void *>(&safe_mprotect_wrapper)) return;
        uintptr_t slotPage = reinterpret_cast<uintptr_t>(gotSlot) & ~static_cast<uintptr_t>(pageSize - 1);
        syscall(__NR_mprotect, reinterpret_cast<void *>(slotPage), static_cast<size_t>(pageSize),
                PROT_READ | PROT_WRITE);
        *gotSlot = reinterpret_cast<void *>(&safe_mprotect_wrapper);
    };

    if (jmprel && pltrelsz > 0) {
        if (pltrelType == DT_RELA) {
            size_t count = pltrelsz / sizeof(Elf_Rela_t);
            const Elf_Rela_t *entries = reinterpret_cast<const Elf_Rela_t *>(jmprel);
            for (size_t i = 0; i < count; i++) {
                patchRelocSlot(entries[i].r_offset, ELF_R_SYM_FN(entries[i].r_info));
            }
        } else {
            size_t count = pltrelsz / sizeof(Elf_Rel_t);
            const Elf_Rel_t *entries = reinterpret_cast<const Elf_Rel_t *>(jmprel);
            for (size_t i = 0; i < count; i++) {
                patchRelocSlot(entries[i].r_offset, ELF_R_SYM_FN(entries[i].r_info));
            }
        }
    }
    if (relaDyn && relaSz > 0) {
        size_t count = relaSz / sizeof(Elf_Rela_t);
        const Elf_Rela_t *entries = reinterpret_cast<const Elf_Rela_t *>(relaDyn);
        for (size_t i = 0; i < count; i++) {
            patchRelocSlot(entries[i].r_offset, ELF_R_SYM_FN(entries[i].r_info));
        }
    }
    if (relDyn && relSz > 0) {
        size_t count = relSz / sizeof(Elf_Rel_t);
        const Elf_Rel_t *entries = reinterpret_cast<const Elf_Rel_t *>(relDyn);
        for (size_t i = 0; i < count; i++) {
            patchRelocSlot(entries[i].r_offset, ELF_R_SYM_FN(entries[i].r_info));
        }
    }
    return 0;
}

static void hookCustomPluginMprotectGot() {
    dl_iterate_phdr(phdrHookMprotectCallback, nullptr);
}

static void resolveAddressModuleAndOffset(uintptr_t addr, char *outMod, size_t modCap, uintptr_t *outRelOffset, char *outPerms, size_t permsCap) {
    strncpy(outMod, "unknown.so", modCap - 1);
    outMod[modCap - 1] = 0;
    *outRelOffset = addr;
    if (outPerms && permsCap > 0) {
        strncpy(outPerms, "----", permsCap - 1);
        outPerms[permsCap - 1] = 0;
    }
    if (addr < 0x1000) return;

    Dl_info info;
    if (dladdr(reinterpret_cast<void *>(addr), &info) != 0 && info.dli_fname != nullptr && info.dli_fbase != nullptr) {
        const char *slash = strrchr(info.dli_fname, '/');
        strncpy(outMod, slash ? (slash + 1) : info.dli_fname, modCap - 1);
        outMod[modCap - 1] = 0;
        uintptr_t base = reinterpret_cast<uintptr_t>(info.dli_fbase);
        if (addr >= base) {
            *outRelOffset = addr - base;
        }
    }

    FILE *fp = fopen("/proc/self/maps", "rt");
    if (!fp) return;
    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        unsigned long long startA = 0, endA = 0, fileOff = 0;
        char perms[8] = {0};
        char pathBuf[256] = {0};
        int n = sscanf(line, "%llx-%llx %7s %llx %*s %*s %255s", &startA, &endA, perms, &fileOff, pathBuf);
        if (n >= 4 && addr >= startA && addr < endA) {
            if (outPerms && permsCap > 0) {
                strncpy(outPerms, perms, permsCap - 1);
                outPerms[permsCap - 1] = 0;
            }
            if (n == 5 && pathBuf[0] != 0) {
                const char *slash = strrchr(pathBuf, '/');
                strncpy(outMod, slash ? (slash + 1) : pathBuf, modCap - 1);
                outMod[modCap - 1] = 0;
            }
            uintptr_t modBase = static_cast<uintptr_t>(startA - fileOff);
            if (addr >= modBase) {
                *outRelOffset = addr - modBase;
            }
            break;
        }
    }
    fclose(fp);
}

static void writeOffsetCrashReport(int sig, int siCode, uintptr_t pcAddr, uintptr_t faultAddr, int hitCount) {
    if (gCrashReportFilePath[0] == 0) return;
    char pcMod[128] = {0};
    char pcPerms[16] = {0};
    uintptr_t pcRel = 0;
    resolveAddressModuleAndOffset(pcAddr, pcMod, sizeof(pcMod), &pcRel, pcPerms, sizeof(pcPerms));

    char addrMod[128] = {0};
    char addrPerms[16] = {0};
    uintptr_t addrRel = 0;
    resolveAddressModuleAndOffset(faultAddr, addrMod, sizeof(addrMod), &addrRel, addrPerms, sizeof(addrPerms));

    const char *sigName = (sig == SIGSEGV) ? "SIGSEGV" : (sig == SIGBUS ? "SIGBUS" : (sig == SIGILL ? "SIGILL" : "SIGNAL"));
    const char *codeName = (sig == SIGSEGV && siCode == SEGV_ACCERR) ? "SEGV_ACCERR (Protected/Read-Only Offset)"
                         : (sig == SIGSEGV && siCode == SEGV_MAPERR) ? "SEGV_MAPERR (Invalid/Unmapped Offset)"
                         : "Instruction/Memory Fault";

    FILE *out = fopen(gCrashReportFilePath, "wt");
    if (out) {
        fprintf(out,
                "Crash Offset : %s + 0x%llX\n"
                "Target Addr  : %s + 0x%llX\n"
                "Signal Type  : %s (%s)\n"
                "Memory Perms : PC=%s | Target=%s\n"
                "Repeat Hits  : %d times (Game Crash Prevented)\n"
                "Raw PC / Addr: 0x%llX / 0x%llX\n",
                pcMod, static_cast<unsigned long long>(pcRel),
                addrMod, static_cast<unsigned long long>(addrRel),
                sigName, codeName,
                pcPerms, addrPerms,
                hitCount,
                static_cast<unsigned long long>(pcAddr),
                static_cast<unsigned long long>(faultAddr));
        fclose(out);
    }
}

static void liveOffsetCrashShieldHandler(int sig, siginfo_t *info, void *ucontext) {
    uintptr_t faultAddr = (info != nullptr) ? reinterpret_cast<uintptr_t>(info->si_addr) : 0;
    uintptr_t pcAddr = 0;
#if defined(__aarch64__)
    ucontext_t *uc = reinterpret_cast<ucontext_t *>(ucontext);
    if (uc != nullptr) {
        pcAddr = static_cast<uintptr_t>(uc->uc_mcontext.pc);
    }
#elif defined(__arm__)
    ucontext_t *uc = reinterpret_cast<ucontext_t *>(ucontext);
    if (uc != nullptr) {
        pcAddr = static_cast<uintptr_t>(uc->uc_mcontext.arm_pc);
    }
#endif

    if (pcAddr == gLastFaultPc && faultAddr == gLastFaultAddr) {
        gRepeatFaultCount++;
    } else {
        gLastFaultPc = pcAddr;
        gLastFaultAddr = faultAddr;
        gRepeatFaultCount = 1;
        gPopupReportWrittenForCurrentPc = false;
    }

    long pageSize = getSystemPageSize();

    // Stage 1: Permission fault (SEGV_ACCERR) on a mapped page (e.g. writing/reading/executing a
    // page in libanogs.so or libUE4.so). Unlock or anonymous-COW remap the page as RWX without
    // EVER stripping PROT_EXEC, and resume execution without advancing PC!
    if (sig == SIGSEGV && info != nullptr && info->si_code == SEGV_ACCERR && faultAddr >= 0x1000) {
        uintptr_t pageBase = faultAddr & ~static_cast<uintptr_t>(pageSize - 1);
        if (pageBase != gLastUnlockedPage || gRepeatFaultCount <= 2) {
            if (unlockOrRemapPageRwx(pageBase, static_cast<size_t>(pageSize))) {
                gLastUnlockedPage = pageBase;
                return;
            }
        }
    }

    // Stage 2: If an offset faults repeatedly after unlock or hits an unmapped address (SEGV_MAPERR),
    // log the exact module + offset so the In-Game / Loader Popup with [Copy] and [OK] buttons is shown!
    if (!gPopupReportWrittenForCurrentPc && (gRepeatFaultCount >= 2 || (info != nullptr && info->si_code == SEGV_MAPERR))) {
        gPopupReportWrittenForCurrentPc = true;
        writeOffsetCrashReport(sig, info ? info->si_code : 0, pcAddr, faultAddr, gRepeatFaultCount);
    }

    // Stage 3: Skip the faulting instruction (PC += 4 on ARM64, PC += 2/4 on ARM32) so the game does NOT crash!
    if (uc != nullptr && gRepeatFaultCount <= 16) {
#if defined(__aarch64__)
        uc->uc_mcontext.pc += 4;
        return;
#elif defined(__arm__)
        if (uc->uc_mcontext.arm_cpsr & (1 << 5)) {
            uint16_t insn16 = *reinterpret_cast<const uint16_t *>(pcAddr & ~1ULL);
            bool is32BitThumb = ((insn16 & 0xF800) == 0xE800) || ((insn16 & 0xF000) == 0xF000);
            uc->uc_mcontext.arm_pc += is32BitThumb ? 4 : 2;
        } else {
            uc->uc_mcontext.arm_pc += 4;
        }
        return;
#endif
    }

    // Stage 4: If a background thread is stuck in an infinite fault loop (> 16 hits at the same offset),
    // exit ONLY that background thread so the main Game process stays alive while the user sees the popup!
    pid_t myPid = getpid();
    pid_t myTid = static_cast<pid_t>(syscall(__NR_gettid));
    if (myTid != myPid) {
        writeOffsetCrashReport(sig, info ? info->si_code : 0, pcAddr, faultAddr, gRepeatFaultCount);
        pthread_exit(nullptr);
    }

    writeOffsetCrashReport(sig, info ? info->si_code : 0, pcAddr, faultAddr, gRepeatFaultCount);
    struct sigaction *prev = (sig == SIGSEGV) ? &gOldSegvSa : (sig == SIGBUS ? &gOldBusSa : &gOldIllSa);
    if (prev && (prev->sa_flags & SA_SIGINFO) && prev->sa_sigaction && prev->sa_sigaction != liveOffsetCrashShieldHandler) {
        prev->sa_sigaction(sig, info, ucontext);
        return;
    }
    signal(sig, SIG_DFL);
    raise(sig);
}

static void ensureSignalHandlerOnTop(int sig, struct sigaction *savedOld) {
    struct sigaction cur{};
    if (sigaction(sig, nullptr, &cur) == 0) {
        if (cur.sa_sigaction == liveOffsetCrashShieldHandler) {
            return;
        }
        if (savedOld != nullptr && cur.sa_sigaction != nullptr) {
            *savedOld = cur;
        }
    }
    struct sigaction sa{};
    sa.sa_sigaction = liveOffsetCrashShieldHandler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_NODEFER;
    sigaction(sig, &sa, nullptr);
}

static void *signalShieldWatchdogThread(void *) {
    // High-frequency startup loop (every 150ms for the first 12 seconds) so that the moment
    // libanogs.so or libUE4.so maps, Segment 0 (0x0) + Segment 1 (.text) are normalized to r-xp,
    // custom .so mprotect GOT slots are wrapped, and our signal shield stays on top!
    for (int i = 0; i < 80; i++) {
        usleep(150 * 1000);
        normalizeAnogsAndEngineSegments();
        hookCustomPluginMprotectGot();
        ensureSignalHandlerOnTop(SIGSEGV, &gOldSegvSa);
        ensureSignalHandlerOnTop(SIGBUS, &gOldBusSa);
        ensureSignalHandlerOnTop(SIGILL, &gOldIllSa);
    }
    while (true) {
        sleep(2);
        normalizeAnogsAndEngineSegments();
        hookCustomPluginMprotectGot();
        ensureSignalHandlerOnTop(SIGSEGV, &gOldSegvSa);
        ensureSignalHandlerOnTop(SIGBUS, &gOldBusSa);
        ensureSignalHandlerOnTop(SIGILL, &gOldIllSa);
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_BoxApplication_installNativeOffsetCrashShield(JNIEnv *env, jclass, jstring crashLogPath) {
    if (env != nullptr && crashLogPath != nullptr) {
        const char *cpath = env->GetStringUTFChars(crashLogPath, nullptr);
        if (cpath != nullptr) {
            strncpy(gCrashReportFilePath, cpath, sizeof(gCrashReportFilePath) - 1);
            gCrashReportFilePath[sizeof(gCrashReportFilePath) - 1] = 0;
            env->ReleaseStringUTFChars(crashLogPath, cpath);
        }
    }
    normalizeAnogsAndEngineSegments();
    hookCustomPluginMprotectGot();
    ensureSignalHandlerOnTop(SIGSEGV, &gOldSegvSa);
    ensureSignalHandlerOnTop(SIGBUS, &gOldBusSa);
    ensureSignalHandlerOnTop(SIGILL, &gOldIllSa);

    if (!gWatchdogStarted) {
        gWatchdogStarted = true;
        pthread_t tid;
        if (pthread_create(&tid, nullptr, signalShieldWatchdogThread, nullptr) == 0) {
            pthread_detach(tid);
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_bgmi_BoxApplication_scrubLoadedPluginElfHeaderInRam(JNIEnv *env, jclass, jstring soPath) {
    if (!env || !soPath) return;
    const char *cpath = env->GetStringUTFChars(soPath, nullptr);
    if (!cpath) return;
    const char *baseName = strrchr(cpath, '/');
    baseName = baseName ? (baseName + 1) : cpath;

    FILE *fp = fopen("/proc/self/maps", "rt");
    if (fp) {
        long pageSize = getSystemPageSize();
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, baseName) == nullptr) continue;
            unsigned long long startAddr = 0, endAddr = 0, fileOff = 0;
            char perms[8] = {0};
            if (sscanf(line, "%llx-%llx %7s %llx", &startAddr, &endAddr, perms, &fileOff) == 4 && fileOff == 0) {
                uintptr_t pageBase = static_cast<uintptr_t>(startAddr) & ~static_cast<uintptr_t>(pageSize - 1);
                if (unlockOrRemapPageRwx(pageBase, static_cast<size_t>(pageSize))) {
                    // Scrub section header table metadata (e_shoff, e_shnum, e_shentsize, e_shstrndx)
                    // in RAM so memory dumpers cannot reconstruct the .so from RAM, while keeping
                    // the file-backed code pages intact so the kernel never raises SIGBUS!
                    uint8_t *hdr = reinterpret_cast<uint8_t *>(startAddr);
                    if (hdr[0] == 0x7F && hdr[1] == 'E' && hdr[2] == 'L' && hdr[3] == 'F') {
                        memset(hdr + 0x28, 0, 24);
                    }
                    syscall(__NR_mprotect, reinterpret_cast<void *>(pageBase),
                            static_cast<size_t>(pageSize), PROT_READ | PROT_EXEC);
                }
                break;
            }
        }
        fclose(fp);
    }
    env->ReleaseStringUTFChars(soPath, cpath);
}
