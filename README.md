# GID

A personal productivity app for a college student, for Android (Kotlin + Jetpack Compose).
You sign in once. After that, tasks, timetable, alarms, sleep and history all work offline
and stay on the phone. The cloud (Supabase) only holds your sign-up profile: name, date of
birth, email, phone and institution.

The full product spec is in [docs/SPEC.md](docs/SPEC.md) and the coding rules are in
[docs/RULES.md](docs/RULES.md).

## What it does

| Screen | What you can do |
| --- | --- |
| **Auth** | Sign in or sign up with email and password, or with Google. Forgot-password emails a reset link. |
| **Home** | A greeting and your photo, a two-week calendar strip, your next alarm, upcoming classes, and today's tasks (tick them off or add new ones). |
| **Calendar** | A month grid with task dots and holidays (long-press a date to mark one). Add, edit, delete, track percent done, change priority and move tasks to tomorrow. |
| **History** | Week by week: productivity bars, sleep bars and a weekly score with the change from last week. Tap a bar to see that day's details. |
| **Settings** | Profile (photo stays on the phone), evening check-in time, sign out, and the **Reliability** checklist. |
| **Alarm popup** | Alarms with repeat days and an activity type, a "Start sleep now" option, timers, and the activity-types manager. |
| **Timetable popup** | Your weekly classes, with reminders N minutes before each one (skipped on holidays). |

## One-time setup

### 1. Supabase

1. Create a free project at <https://supabase.com>.
2. Open **SQL Editor** and run the SQL in `docs/SPEC.md` section 6. It creates the
   `profiles` table, row-level security and the sign-up trigger.
3. **Authentication → Providers → Email**: enable it. While testing you can turn
   *Confirm email* off. Turn it back on before you share the app.
4. **Authentication → URL Configuration → Redirect URLs**: add `com.adheesh.gid://callback`.
   This lets the confirmation and password-reset links open the app.
5. **Project Settings → API**: copy the **Project URL** and the **anon public** key.
   Never use the `service_role` key.

### 2. Google sign-in (optional)

1. In <https://console.cloud.google.com>, create a project and set up the **OAuth consent screen**.
2. **Credentials → Create credentials → OAuth client ID**:
   - Create a **Web application** client and copy its Client ID and secret.
   - Create an **Android** client with package `com.adheesh.gid` and the SHA-1 of the key
     that signs your APK. Make one for each key you use (debug and release). To print a
     key's SHA-1, run:

     ```bash
     keytool -list -v -keystore C:/Users/<you>/.android/gid-release.jks -alias gid
     ```

3. In Supabase, go to **Authentication → Providers → Google**, enable it, paste the *Web*
   client ID and secret, and leave **Skip nonce checks** turned off.

### 3. `local.properties` (never commit this file)

```properties
sdk.dir=C\:/Users/<you>/AppData/Local/Android/Sdk
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
GOOGLE_OAUTH_WEB_CLIENT_ID=1234-abc.apps.googleusercontent.com

# Release signing (the keystore lives outside the repo; back it up)
RELEASE_STORE_FILE=C\:/Users/<you>/.android/gid-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=gid
RELEASE_KEY_PASSWORD=...
```

The build reads these through `BuildConfig`. Without Supabase keys the app still opens,
but sign-in says it isn't set up.

## Build and run

```bash
./gradlew assembleDebug        # debug APK, with the seed-data screen (bug button on Home)
./gradlew testDebugUnitTest    # unit tests for scores, sleep maths, sessions, alarms, reminders
./gradlew lintDebug
./gradlew assembleRelease      # signed, shareable APK in app/build/outputs/apk/release/
```

Install on a phone with `adb install -r app/build/outputs/apk/release/app-release.apk`,
or copy the APK to the phone and open it (allow "Install unknown apps" for your file manager).

## Architecture

One Gradle module, packaged by feature (SPEC §4):

```
app/        MainActivity's nav host (Navigation 3), bottom bar, AppContainer (manual DI, no Hilt)
core/
  database/      Room entities + DAOs, DataStore preferences, enums, sample-data seeder
  designsystem/  Color, Type, Shape, Theme tokens and shared composables
  notifications/ channels, builders, permission prompt
  scheduling/    AlarmManager schedulers, receivers, ringing service + full-screen activity,
                 daily WorkManager job, reschedule-after-reboot receiver
  util/          time helpers (epochDay, minutes-from-midnight, formatting)
feature/<name>/  ui/ (Compose), viewmodel/ (StateFlow), data/ (repositories), domain/ (plain Kotlin rules)
```

- **Screen → ViewModel → Repository → DAO / Supabase.** Screens never touch Room or Supabase.
- **Business rules are plain Kotlin with unit tests:** `ScoreCalculator`, `SessionCalculator`,
  `SessionRules` (closing, the midnight rule, 16-hour auto-close), `TaskRules`,
  `AlarmTimeCalculator`, `ReminderPlanner`, `HistoryCalculator`, `AgeCalculator`.
- **Offline first.** The signed-in account is cached in DataStore, so a saved session opens Home
  without internet. A profile saved offline is marked `needsSync` and pushed on the next launch.
- **Alarms** use `AlarmManager.setAlarmClock`. A foreground service plays the sound with
  alarm audio attributes and shows a full-screen ringing activity over the lock screen.
  Everything is re-planned after a reboot, a clock or time-zone change, an app update, every
  edit, and once a day.
- **Privacy.** Only the `profiles` row goes to Supabase. No analytics or crash-reporting SDKs.

## Reliability tips

Open **Settings → Reliability** and make every row green. On Xiaomi, Oppo, Vivo, Realme and
Samsung phones, also open the app info page and allow *Autostart* or *Don't restrict battery*.
Otherwise the phone may stop alarms while the app is closed.

## Known limitations (V1)

- Data lives only on the phone. Uninstalling the app or losing the phone erases history.
  A JSON export is the planned first addition.
- The "Contact us" address is a placeholder (`contact_email` in `res/values/strings.xml`).
