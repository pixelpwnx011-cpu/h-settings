# Geneo Hidden Settings

A shortcut launcher straight into specific Android system settings screens — for
boards whose custom launcher hides normal navigation (e.g. no visible "Apps"
section in Settings).

## What it does
Tapping any shortcut jumps directly to that system screen via Android's own
Settings intents, bypassing whatever custom Settings UI is hiding it. If a
specific screen isn't available on this device/Android version, it falls back to
the next best option, and finally to the general Settings screen.

**System shortcuts included:** All Apps, Default Apps, Home App/Launcher switcher,
Display Over Other Apps, Battery Optimization List, Security Settings,
Accessibility Settings, Date & Time, Wi-Fi, Storage, Developer Options, All
Settings.

**Per-app shortcuts:** type (or quick-fill) a package name, then jump straight to
that app's App Info page, its "Display over other apps" toggle, or trigger the
one-tap battery-optimization-exemption request dialog for it.

No special permissions needed — this app only opens system settings screens, it
doesn't modify anything itself.

## Build the APK
Same GitHub Actions setup as the other Geneo apps:
1. Create a new GitHub repo, upload everything in this folder (including the
   hidden `.github` folder — reveal hidden files in your file manager first, or
   use "Create new file" and type the full path if drag-and-drop skips it).
2. Check the **Actions** tab → download from **Artifacts** once the build succeeds.

## Project layout
```
app/src/main/java/com/geneo/hiddensettings/
  MainActivity.kt   – builds the shortcut list and launches each Settings intent
app/src/main/res/
  layout/            – main screen + reusable shortcut row
  values/            – dark theme matching the other Geneo apps
```
