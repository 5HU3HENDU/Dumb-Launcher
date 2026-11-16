package com.dumb.launcher;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Inactive-folder data: the inactivity threshold (week or month), per-app
 * last-used and first-seen times, and the manual hide set. Persisted in its
 * own SharedPreferences file so it survives process death and app updates.
 *
 * An app appears in the Inactive folder when it is manually hidden, or when
 * Usage access is granted and real device usage (UsageStats) shows the app
 * unused for the threshold. Without Usage access, or for work-profile apps
 * (the system reports no cross-profile usage stats), apps are never
 * auto-moved: launches through this launcher cannot prove an app went
 * unused, so auto-moving could wrongly hide apps opened via notifications,
 * links or widgets. Apps with no usage signal age from when they were first
 * seen, so nothing becomes inactive right after granting access.
 *
 * Membership in real categories is never touched: an auto-inactive app
 * returns to its original category the moment it is opened, and a manually
 * hidden app keeps its category while it stays hidden.
 */
public final class InactiveStore {

    private static final String PREFS = "dumb_launcher_inactive";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_LAST_USED = "last_used";
    private static final String KEY_FIRST_SEEN = "first_seen";
    private static final String KEY_HIDDEN = "hidden";

    public static final String WEEK = "week";
    public static final String MONTH = "month";

    private static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final long MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private InactiveStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The chosen inactivity threshold: WEEK (default) or MONTH. */
    public static String threshold(Context c) {
        return prefs(c).getString(KEY_THRESHOLD, WEEK);
    }

    public static void setThreshold(Context c, String value) {
        if (!WEEK.equals(value) && !MONTH.equals(value)) {
            return;
        }
        prefs(c).edit().putString(KEY_THRESHOLD, value).apply();
    }

    static long thresholdMillis(Context c) {
        return MONTH.equals(threshold(c)) ? MONTH_MILLIS : WEEK_MILLIS;
    }

    private static long readTime(SharedPreferences p, String key, String mapKey) {
        try {
            JSONObject obj = new JSONObject(p.getString(mapKey, "{}"));
            return obj.optLong(key, 0L);
        } catch (JSONException e) {
            return 0L;
        }
    }

    private static void writeTime(Context c, String mapKey, String key, long value) {
        SharedPreferences p = prefs(c);
        JSONObject obj;
        try {
            obj = new JSONObject(p.getString(mapKey, "{}"));
        } catch (JSONException e) {
            obj = new JSONObject();
        }
        try {
            obj.put(key, value);
        } catch (JSONException ignored) {
            return;
        }
        p.edit().putString(mapKey, obj.toString()).apply();
    }

    /**
     * Baseline an app the first time enumeration sees it. Only the first
     * sighting counts; later calls are no-ops for that key.
     */
    public static void noteSeen(Context c, String key, long now) {
        if (readTime(prefs(c), key, KEY_FIRST_SEEN) == 0L) {
            writeTime(c, KEY_FIRST_SEEN, key, now);
        }
    }

    /** Record that the app was opened through the launcher just now. */
    public static void recordLaunch(Context c, String key) {
        writeTime(c, KEY_LAST_USED, key, System.currentTimeMillis());
    }

    /** True while the app is manually hidden (stays hidden even when used). */
    public static boolean isHidden(Context c, String key) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_HIDDEN, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                if (key.equals(arr.getString(i))) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
            // treat a corrupt set as empty
        }
        return false;
    }

    public static void setHidden(Context c, String key, boolean hidden) {
        SharedPreferences p = prefs(c);
        JSONArray next = new JSONArray();
        try {
            JSONArray arr = new JSONArray(p.getString(KEY_HIDDEN, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String k = arr.getString(i);
                if (!key.equals(k)) {
                    next.put(k);
                }
            }
        } catch (JSONException ignored) {
            // rebuild from scratch
        }
        if (hidden) {
            next.put(key);
        }
        p.edit().putString(KEY_HIDDEN, next.toString()).apply();
    }

    /** True when the user has granted this app Usage access. */
    public static boolean hasUsageAccess(Context c) {
        AppOpsManager aom = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        if (aom == null) {
            return false;
        }
        int mode;
        if (Build.VERSION.SDK_INT >= 29) {
            mode = aom.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
        } else {
            mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
        }
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return c.checkCallingOrSelfPermission(
                    android.Manifest.permission.PACKAGE_USAGE_STATS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Per-package last-used times from UsageStats for apps used at least
     * once in [windowStart, now]. Empty when Usage access is not granted or
     * the service is unavailable. Covers this profile only.
     */
    public static Map<String, Long> usageLastUsed(Context c, long windowStart) {
        Map<String, Long> out = new HashMap<>();
        UsageStatsManager usm =
                (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return out;
        }
        List<UsageStats> stats;
        try {
            stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                    windowStart, System.currentTimeMillis());
        } catch (RuntimeException e) {
            return out;
        }
        if (stats == null) {
            return out;
        }
        for (UsageStats st : stats) {
            Long prev = out.get(st.getPackageName());
            long t = st.getLastTimeUsed();
            if (prev == null || t > prev) {
                out.put(st.getPackageName(), t);
            }
        }
        return out;
    }

    /**
     * True when the app has out-aged the inactivity threshold. The clock
     * starts at the most recent of: last launch through the launcher, last
     * UsageStats use (usageGranted, personal-profile keys only - the system
     * reports no work-profile usage), or first sighting when neither exists.
     * Pass a windowed map from usageLastUsed(now - thresholdMillis) and an
     * empty map with usageGranted=false when Usage access is not granted.
     */
    public static boolean isAutoInactive(Context c, String key, long now,
            Map<String, Long> usage, boolean usageGranted) {
        if (!usageGranted || key.endsWith(ProfileApps.WORK_SUFFIX)) {
            // Never auto-move an app whose real usage cannot be checked.
            // Launches through this launcher cannot prove an app went unused
            // (it may be opened via notifications, links or widgets), and the
            // system reports no cross-profile usage stats for work apps.
            // Manual Hide still works in both cases.
            return false;
        }
        SharedPreferences p = prefs(c);
        long last = readTime(p, key, KEY_LAST_USED);
        Long u = usage.get(key);
        if (u != null && u > last) {
            last = u;
        }
        if (last == 0L) {
            last = readTime(p, key, KEY_FIRST_SEEN);
        }
        if (last == 0L) {
            return false;
        }
        return now - last > thresholdMillis(c);
    }

    /** Folder membership: manually hidden, or auto-inactive. */
    public static boolean inInactiveFolder(Context c, String key, long now,
            Map<String, Long> usage, boolean usageGranted) {
        return isHidden(c, key) || isAutoInactive(c, key, now, usage, usageGranted);
    }
}
