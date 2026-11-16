package com.dumb.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Process;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InactiveStoreTest {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private Context c;
    private long now;

    @Before
    public void setUp() {
        c = RuntimeEnvironment.getApplication();
        now = System.currentTimeMillis();
        InactiveStore.setThreshold(c, InactiveStore.WEEK);
    }

    @Test
    public void unseenAppIsNeverInactive() {
        assertFalse(InactiveStore.isAutoInactive(c, "com.never.seen", now,
                Collections.<String, Long>emptyMap(), false));
    }

    @Test
    public void noAutoMoveWithoutUsageAccess() {
        // Launcher launches cannot prove an app went unused: without Usage
        // access nothing is ever auto-moved, however old it is.
        InactiveStore.noteSeen(c, "com.old.app", now - 90 * DAY);
        assertFalse(InactiveStore.isAutoInactive(c, "com.old.app", now,
                Collections.<String, Long>emptyMap(), false));
    }

    @Test
    public void recentUseStaysActive() {
        InactiveStore.noteSeen(c, "com.new.app", now - 30 * DAY);
        Map<String, Long> usage = new HashMap<>();
        usage.put("com.new.app", now - 2 * DAY);
        assertFalse(InactiveStore.isAutoInactive(c, "com.new.app", now,
                usage, true));
    }

    @Test
    public void monthThresholdKeepsWeekOldUseActive() {
        InactiveStore.setThreshold(c, InactiveStore.MONTH);
        InactiveStore.noteSeen(c, "com.month.app", now - 90 * DAY);
        Map<String, Long> usage = new HashMap<>();
        usage.put("com.month.app", now - 10 * DAY);
        assertFalse(InactiveStore.isAutoInactive(c, "com.month.app", now,
                usage, true));
    }

    @Test
    public void staleUseGoesInactiveAfterThreshold() {
        InactiveStore.noteSeen(c, "com.stale.app", now - 90 * DAY);
        Map<String, Long> usage = new HashMap<>();
        usage.put("com.stale.app", now - 10 * DAY);
        assertTrue(InactiveStore.isAutoInactive(c, "com.stale.app", now,
                usage, true));
    }

    @Test
    public void usageStatsUseKeepsAppActiveWithoutLauncherLaunch() {
        // The mismatch fix: app used outside the launcher stays active.
        InactiveStore.noteSeen(c, "com.notify.app", now - 30 * DAY);
        Map<String, Long> usage = new HashMap<>();
        usage.put("com.notify.app", now - DAY);
        assertFalse(InactiveStore.isAutoInactive(c, "com.notify.app", now,
                usage, true));
    }

    @Test
    public void usageStatsAbsentStillFallsBackToFirstSeen() {
        InactiveStore.noteSeen(c, "com.absent.app", now - 30 * DAY);
        assertTrue(InactiveStore.isAutoInactive(c, "com.absent.app", now,
                Collections.<String, Long>emptyMap(), true));
    }

    @Test
    public void workKeyIsNeverAutoMoved() {
        // No cross-profile usage exists: the personal profile's stats for the
        // same package must not decide a work app's fate, and guessing is
        // worse than leaving it alone - so work apps are never auto-moved.
        InactiveStore.noteSeen(c, "com.duo.app" + ProfileApps.WORK_SUFFIX,
                now - 90 * DAY);
        assertFalse(InactiveStore.isAutoInactive(c,
                "com.duo.app" + ProfileApps.WORK_SUFFIX, now,
                Collections.<String, Long>emptyMap(), true));
    }

    @Test
    public void hiddenAppStaysInFolderEvenWithRecentUsage() {
        InactiveStore.noteSeen(c, "com.hidden.app", now - 30 * DAY);
        InactiveStore.setHidden(c, "com.hidden.app", true);
        Map<String, Long> usage = new HashMap<>();
        usage.put("com.hidden.app", now);
        assertTrue(InactiveStore.inInactiveFolder(c, "com.hidden.app", now,
                usage, true));
        InactiveStore.setHidden(c, "com.hidden.app", false);
        assertFalse(InactiveStore.inInactiveFolder(c, "com.hidden.app", now,
                usage, true));
    }

    @Test
    public void usageAccessReflectsAppOpsMode() {
        AppOpsManager aom =
                (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        Shadows.shadowOf(aom).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), c.getPackageName(), AppOpsManager.MODE_ALLOWED);
        assertTrue(InactiveStore.hasUsageAccess(c));
        Shadows.shadowOf(aom).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), c.getPackageName(), AppOpsManager.MODE_IGNORED);
        assertFalse(InactiveStore.hasUsageAccess(c));
    }
}
