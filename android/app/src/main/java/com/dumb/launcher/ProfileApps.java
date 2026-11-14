package com.dumb.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates launchable apps across every profile the launcher can access
 * (personal plus any work/managed profile), via LauncherApps - the official
 * Android API "mainly for use by launchers". Identity is profile-aware: the
 * personal copy and the work copy of the same package are two distinct
 * Installed entries with different keys and different UserHandles, so each
 * can be categorized and launched separately. Work copies are listed exactly
 * as the platform exposes them; nothing here bypasses employer policy, quiet
 * mode, or profile locks - an inaccessible profile is simply skipped.
 */
public final class ProfileApps {

    /**
     * Suffix that makes a work-profile copy's key distinct from the personal
     * one. Personal keys stay exactly the bare package name, so memberships
     * stored before profile support keep applying to the personal copy.
     */
    public static final String WORK_SUFFIX = "@work";

    private ProfileApps() {
    }

    /** One launchable app in one profile. */
    public static final class Installed {
        /** Base label, e.g. "Slack" (used for A-Z sorting). */
        public final String label;
        /** Settings label: base label plus " (Work)" on work copies. */
        public final String display;
        /** Bare package name (seed-category lookups). */
        public final String pkg;
        /** Profile-aware identity: pkg for personal, pkg+"@work" for work. */
        public final String key;
        public final boolean work;
        public final ComponentName component;
        public final UserHandle user;
        /** Platform icon; work copies carry the official briefcase badge. */
        public final Drawable icon;

        Installed(String label, String pkg, String key, boolean work,
                  ComponentName component, UserHandle user, Drawable icon) {
            this.label = label;
            this.display = work ? label + " (Work)" : label;
            this.pkg = pkg;
            this.key = key;
            this.work = work;
            this.component = component;
            this.user = user;
            this.icon = icon;
        }
    }

    /** All launchable apps in every accessible profile, this launcher excluded. */
    public static List<Installed> list(Context context) {
        List<Installed> out = new ArrayList<>();
        LauncherApps la = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (la == null) {
            return out;
        }
        String self = context.getPackageName();
        int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        List<UserHandle> profiles;
        try {
            profiles = la.getProfiles();
        } catch (RuntimeException e) {
            return out;
        }
        for (UserHandle u : profiles) {
            boolean work = !u.equals(Process.myUserHandle());
            List<LauncherActivityInfo> infos;
            try {
                infos = la.getActivityList(null, u);
            } catch (RuntimeException e) {
                // Profile locked, not running, or not accessible: skip it.
                continue;
            }
            if (infos == null) {
                continue;
            }
            for (LauncherActivityInfo info : infos) {
                String pkg = info.getComponentName().getPackageName();
                if (pkg == null || pkg.equals(self)) {
                    continue;
                }
                CharSequence cs = info.getLabel();
                String label = cs == null ? pkg : cs.toString();
                String key = work ? pkg + WORK_SUFFIX : pkg;
                out.add(new Installed(label, pkg, key, work,
                        info.getComponentName(), u, info.getBadgedIcon(densityDpi)));
            }
        }
        return out;
    }

    /** Strip the work suffix, returning the bare package name. */
    public static String basePackage(String key) {
        if (key != null && key.endsWith(WORK_SUFFIX)) {
            return key.substring(0, key.length() - WORK_SUFFIX.length());
        }
        return key;
    }
}
