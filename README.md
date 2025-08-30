# WorktimeCompose – Full Starter (Calendar + Editor)

Enthält:
- Room DB (Projects, Sessions, Pauses, SessionEdits) – v3 mit Migration.
- DataStore `WEEK_TARGET_HOURS` (Default 40).
- Calendar mit Wochensummen links (Farbcodiert ggü. Wochenziel).
- Session-Editor (Start/Ende/Pausen) mit Audit-Trail.
- Foreground Service + Receiver-Stubs.
- Wear OS Minimal-App.

## Import in Android Studio
1. ZIP entpacken.
2. **Open**  Ordner `worktime-full` wählen.
3. Gradle sync abwarten, dann `:app` ausführen.

## Wichtige Abhängigkeiten (bereits gesetzt)
- Compose BOMs manuell angegeben (ui/material3 1.7.x/1.3.x).
- Room 2.6.1 (+kapt), DataStore 1.1.1, WorkManager 2.9.1, Play Services Location 21.3.0, Wearable 18.2.0.
- `compileSdk=34`, Kotlin 1.9.24, AGP 8.5.1, `jvmTarget=17`.

## Manifest-Permissions
- FINE/COARSE/BACKGROUND LOCATION, WIFI STATE/CHANGE, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.

## To-Do (optional)
- Geofence/Wi-Fi Trigger, Settings-UI komplettieren (WLAN/GPS/Rundung/Feiertage).
- Notification-Buttons & Service-Intents.
- Migrations erweitern (falls du Tabellen änderst).

Viel Spaß!
