package com.dumb.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Inactive-folder data: the inactivity threshold (week or month), per-app
 * last-used and first-seen times, and the manual hide set. Persisted in its
 * own SharedPreferences file so it survives process death and app updates.
 *
 * An app appears in the Inactive folder when it is manually hidden, or when
 * it has not been opened through the launcher within the threshold. Usage is
 * measured by launches from this launcher (no usage-access permission);
 * apps never launched through the launcher age from when they were first
 * seen, so nothing becomes inactive right after install or upgrade.
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

    private static long thresholdMillis(Context c) {
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

    /**
     * True when the app has out-aged the inactivity threshold. The clock
     * starts at the last launch through the launcher, or at first sighting
     * when it was never launched here.
     */
    public static boolean isAutoInactive(Context c, String key, long now) {
        SharedPreferences p = prefs(c);
        long last = readTime(p, key, KEY_LAST_USED);
        if (last == 0L) {
            last = readTime(p, key, KEY_FIRST_SEEN);
        }
        if (last == 0L) {
            return false;
        }
        return now - last > thresholdMillis(c);
    }

    /** Folder membership: manually hidden, or auto-inactive. */
    public static boolean inInactiveFolder(Context c, String key, long now) {
        return isHidden(c, key) || isAutoInactive(c, key, now);
    }
}
