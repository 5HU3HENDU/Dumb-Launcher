# Dumb Launcher (Android)

Native Android home launcher matching the Dumb Launcher webpage: one viewport-filling
grid of installed apps grouped by category (A-Z within category), real app icons,
no scrolling (tiles shrink to fit), clock/date header and category legend.

## Build
Requires JDK 17, Android SDK platform 34 + build-tools 34, Gradle 8.5.

    gradle assembleRelease

Unsigned release APK lands in app/build/outputs/apk/release/. To sign, place a
keystore.properties next to settings.gradle (see .gitignore note) and rebuild.

## Layout
- MainActivity.java: home UI, grid layout math, clock, Personal/Work profile toggle,
  and the Inactive folder tile (greyish folder with a clock badge, last in the grid).
- ProfileApps.java: per-profile app enumeration via LauncherApps, profile-aware keys.
- CategoryMap.java: category names and package-to-category seed map (editable data).
- CategoryStore.java: persisted category order, renames, deletions and app membership.
- InactiveStore.java: inactivity threshold (week/month), last-used/first-seen
  tracking and the manual hide set. Inactivity is measured by launches through
  this launcher; apps never launched here age from first sighting. Auto-inactive
  apps return to their original category when opened; hidden apps stay until
  unhidden. Real category membership is never rewritten by this feature.
- InactiveActivity.java: the Inactive folder screen - auto-inactive and manually
  hidden apps together, A-Z, same no-scroll shrink-to-fit grid.
- SettingsActivity.java: two settings tabs - Apps (category dropdown per app,
  Hide/Unhide button, inactivity threshold), Categories (add, reorder, rename,
  delete; deleted categories' apps move to Other).
