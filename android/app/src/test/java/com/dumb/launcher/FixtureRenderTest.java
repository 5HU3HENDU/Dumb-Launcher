package com.dumb.launcher;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowUsageStatsManager;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

/**
 * Populates the shipped views with clearly synthetic fixture apps and renders
 * real native pixels: the home grid with the Inactive folder tile last, the
 * populated folder, Settings rows with Hide/Unhide, and the open-to-restore
 * flow. Fixture data only - never user-facing content.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w412dp-h892dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FixtureRenderTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private Context c;
    private long now;

    @Before
    public void fixtures() {
        c = RuntimeEnvironment.getApplication();
        now = System.currentTimeMillis();

        // Synthetic installed apps.
        addApp("com.fake.alpha", "Alpha");
        addApp("com.fake.beeper", "Beeper");
        addApp("com.fake.comet", "Comet");
        addApp("com.fake.delta", "Delta");
        addApp("com.fake.ember", "Ember");
        addApp("com.fake.flux", "Flux");

        // Categories for a representative grid.
        CategoryStore.setMembership(c, "com.fake.alpha",
                Collections.singleton("Social"));
        CategoryStore.setMembership(c, "com.fake.beeper",
                Collections.singleton("Chat"));
        CategoryStore.setMembership(c, "com.fake.comet",
                Collections.singleton("Utilities"));
        CategoryStore.setMembership(c, "com.fake.delta",
                Collections.singleton("Social"));
        CategoryStore.setMembership(c, "com.fake.ember",
                Collections.singleton("Video"));

        // Usage access granted; everyone but Ember used today.
        grantUsageAccess(true);
        UsageStatsManager usm =
                (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        for (String pkg : new String[]{"com.fake.alpha", "com.fake.beeper",
                "com.fake.comet", "com.fake.delta", "com.fake.flux"}) {
            UsageStats stats = ShadowUsageStatsManager.UsageStatsBuilder.newBuilder()
                    .setPackageName(pkg)
                    .setFirstTimeStamp(now - 90 * DAY)
                    .setLastTimeStamp(now)
                    .setLastTimeUsed(now)
                    .setTotalTimeInForeground(60000)
                    .build();
            Shadows.shadowOf(usm).addUsageStats(UsageStatsManager.INTERVAL_BEST, stats);
        }

        // Delta: manually hidden. Ember: unseen since long before the threshold.
        InactiveStore.setHidden(c, "com.fake.delta", true);
        InactiveStore.noteSeen(c, "com.fake.ember", now - 40 * DAY);
    }

    private void addApp(String pkg, String label) {
        PackageManager pm = c.getPackageManager();
        ApplicationInfo app = new ApplicationInfo();
        app.packageName = pkg;
        app.nonLocalizedLabel = label;
        app.icon = 0;
        ActivityInfo ai = new ActivityInfo();
        ai.packageName = pkg;
        ai.name = pkg + ".MainActivity";
        ai.nonLocalizedLabel = label;
        ai.applicationInfo = app;
        Object internal;
        LauncherActivityInfo info;
        try {
            Class<?> internalCls =
                    Class.forName("android.content.pm.LauncherActivityInfoInternal");
            Class<?> incrementalCls =
                    Class.forName("android.content.pm.IncrementalStatesInfo");
            Object incremental = ReflectionHelpers.callConstructor(incrementalCls,
                    ReflectionHelpers.ClassParameter.from(boolean.class, false),
                    ReflectionHelpers.ClassParameter.from(float.class, 1f),
                    ReflectionHelpers.ClassParameter.from(long.class, 0L));
            internal = ReflectionHelpers.callConstructor(internalCls,
                    ReflectionHelpers.ClassParameter.from(ActivityInfo.class, ai),
                    ReflectionHelpers.ClassParameter.from(incrementalCls, incremental),
                    ReflectionHelpers.ClassParameter.from(UserHandle.class,
                            Process.myUserHandle()));
            info = ReflectionHelpers.callConstructor(LauncherActivityInfo.class,
                    ReflectionHelpers.ClassParameter.from(Context.class, c),
                    ReflectionHelpers.ClassParameter.from(internalCls, internal));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        LauncherApps la = (LauncherApps) c.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        Shadows.shadowOf(la).addActivity(Process.myUserHandle(), info);
    }

    private void grantUsageAccess(boolean granted) {
        AppOpsManager aom =
                (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        Shadows.shadowOf(aom).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), c.getPackageName(),
                granted ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }

    private void draw(View root, String name) throws Exception {
        int w = c.getResources().getDisplayMetrics().widthPixels;
        int h = c.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        File out = new File("build/render/" + name + ".png");
        out.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(out);
        b.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
    }

    @Test
    public void homePopulatedThenRestore() throws Exception {
        // Populated home: 5 visible apps in categories + folder tile last (2 inside).
        ActivityController<MainActivity> home =
                Robolectric.buildActivity(MainActivity.class);
        home.setup();
        draw(home.get().findViewById(android.R.id.content), "home_populated");
        home.destroy();

        // Populated folder: Delta (hidden) + Ember (auto-inactive).
        ActivityController<InactiveActivity> folder =
                Robolectric.buildActivity(InactiveActivity.class);
        folder.setup();
        draw(folder.get().findViewById(android.R.id.content), "inactive_populated");
        folder.destroy();

        // Open-to-restore: launching Ember from the folder records the launch;
        // the next home render must show it back in Video, folder down to 1.
        InactiveStore.recordLaunch(c, "com.fake.ember");
        ActivityController<MainActivity> home2 =
                Robolectric.buildActivity(MainActivity.class);
        home2.setup();
        draw(home2.get().findViewById(android.R.id.content), "home_restored");
        home2.destroy();
    }

    @Test
    public void settingsPopulated() throws Exception {
        ActivityController<SettingsActivity> controller =
                Robolectric.buildActivity(SettingsActivity.class);
        controller.setup();
        View content = controller.get().findViewById(android.R.id.content);
        UiRenderTest.clickText(content, "Apps");
        draw(content, "settings_populated");

        // The row button really wires through: tapping Alpha's HIDE moves it
        // to the folder; the re-rendered row must offer UNHIDE.
        org.junit.Assert.assertFalse(
                InactiveStore.isHidden(c, "com.fake.alpha"));
        org.junit.Assert.assertTrue(UiRenderTest.clickText(content, "Hide"));
        org.junit.Assert.assertTrue(
                InactiveStore.isHidden(c, "com.fake.alpha"));
        org.junit.Assert.assertTrue(UiRenderTest.clickText(content, "Unhide"));
        org.junit.Assert.assertFalse(
                InactiveStore.isHidden(c, "com.fake.alpha"));
        controller.destroy();
    }
}
