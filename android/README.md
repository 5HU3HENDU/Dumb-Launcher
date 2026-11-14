# Dumb Launcher (Android source)

Native Android home launcher. One viewport-filling grid of installed apps,
grouped by category (A-Z inside each), no scrolling. Neutral tiles, real app
icons. Settings (gear or long-press): create categories, move apps between
them (one category per app), change category order. Edits persist in
SharedPreferences.

Build: JDK 17 + Android SDK (platform 34, build-tools 34.0.0) + Gradle 8.5.
`gradle assembleRelease`. Release signing reads `keystore.properties`
(keystoreFile/storePassword/keyAlias/keyPassword) from this directory; the
file and keystore are gitignored and live in the owner's private custody
(encrypted backup in Drive, passphrase in Instinct vault), never in this repo.

- CategoryMap.java: built-in seed category names/order and package->category map.
- CategoryStore.java: user overrides (order + membership), persisted.
- MainActivity.java: home grid. SettingsActivity/CategoryAppsActivity: settings.
