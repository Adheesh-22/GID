# GID

A personal productivity app for a college student, for Android (Kotlin + Jetpack Compose).
You sign in once. After that, tasks, timetable, alarms, sleep and history all work offline
and stay on the phone. The cloud (Supabase) only holds your sign-up profile: name, date of
birth, email, phone and institution.

The full product spec is in [docs/SPEC.md](docs/SPEC.md) and the coding rules are in
[docs/RULES.md](docs/RULES.md). The [docs/app](docs/app) folder describes the app as it is now:
[FEATURES.md](docs/app/FEATURES.md) (everything it does),
[ARCHITECTURE.md](docs/app/ARCHITECTURE.md) (how it is built) and
[CHANGELOG.md](docs/app/CHANGELOG.md) (every change since the first commit).

## What it does

| Screen | What you can do |
| --- | --- |
| **Auth** | Sign in or sign up with email and password, or with Google. Forgot-password emails a reset link. |
| **Home** | A greeting and your photo, a two-week calendar strip, your next alarm, upcoming classes, and today's tasks (tick them off or add new ones). Tap the greeting to personalise it. |
| **Personalise** | Change the name Home greets you with, its font (GID, DM Sans, Serif, Mono, Script, Light) and colour. Pick a background photo: GID blurs it on the phone, lets you dim it, and shows it only behind Home, Calendar, Library and History. |
| **Calendar** | A month grid with task dots and holidays (long-press a date to mark one). Add, edit, delete, track percent done, change priority and move tasks to tomorrow. |
| **Library** | Each subject opens with tabs: All, then one per category (add, rename or remove categories). The Capture Inbox. Share photos, links, text, PDFs, Word or PowerPoint files to GID from any app (or tap +). GID suggests the subject from your timetable; you sort each item into a subject, heading and category. Search, subject pages, and a deadline can become a task. Files are copied into GID once; capturing the same file again reuses that copy. All on the phone. |
| **Exams & Assessments** | Separate pages for exams and tests, and for assignments and projects (paste VTOP's Assignment Upload pages), per subject, with countdowns, reminders (a week before, the evening before, the morning of) and the subject's saved Library items. Shown on Home ("Next up"), the Calendar and subject pages. |
| **Semesters** | Everything follows the semester you pick. Pasting a VTOP Time Table page sorts subjects into it; the one marked Classes now sends class reminders. |
| **Marks** | Paste VTOP's Marks View. Each subject shows uploaded and remaining marks; type what you expect and see the total out of 100, weighted by credits. |
| **Attendance** | Mark each class Present, Absent or Cancelled, from the notification that appears when a class ends or from Home. The Attendance page shows each subject's percent against your target (75% by default, changeable in Settings or per subject) and says how many classes you can miss or must attend. Classes from before you used GID can be added. |
| **Syllabus** | Units and topics per subject that you tick off, with percent per unit and subject. Paste a whole syllabus and GID splits it into units and topics. An exam can cover chosen units; its page and Home's Next up show progress for just those. |
| **Widget & tile** | A home-screen widget (Camera, Photo, Paste, Inbox count, next exam) and a Quick Settings tile that opens a capture menu. |
| **History** | Week by week: productivity bars, sleep bars and a weekly score with the change from last week. Tap a bar to see that day's details. |
| **Settings** | Profile (photo stays on the phone), evening check-in time, attendance target, a **How to use GID** guide, sign out, **Backup** export/import (one .zip you keep) with how much space Library files use, and the **Reliability** checklist. |
| **Motion** | Tabs slide sideways in bottom-bar order. Full-screen pages (Exams, subject and capture pages) and the Alarm and Timetable popups slide up from the bottom and back down on Back. |
| **Alarm popup** | Alarms with repeat days, an activity type, their own ringtone and snooze length, and a random pattern to draw to stop them, a "Start sleep now" option, timers, and the activity-types manager. |
| **Timetable popup** | Your weekly classes, built by pasting VIT VTOP's Time Table page or by hand, with reminders N minutes before each one (skipped on holidays). |

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

# Optional: build output outside the project folder. Use this when the project
# sits in OneDrive (or another synced folder) and Gradle can't clean app/build.
BUILD_DIR=C\:/Users/<you>/gid-build
```

With `BUILD_DIR` set, APKs land in `<BUILD_DIR>/app/outputs/apk/...` instead of `app/build/outputs/apk/...`.

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
If `BUILD_DIR` is set, take the APK from `<BUILD_DIR>/app/outputs/apk/...` instead; the
`app/build` folder is not updated.

## Architecture

One Gradle module, packaged by feature (SPEC §4):

```
app/        MainActivity's nav host (Navigation 3), page transitions, bottom bar, AppContainer (manual DI, no Hilt)
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
  `AlarmTimeCalculator`, `ReminderPlanner`, `HistoryCalculator`, `AgeCalculator`,
  `ExamReminderPlanner`, `Countdown`, `Appearance`, `BoxBlur`, `AttendanceMath`,
  `AttendancePromptPlanner`, `ContentHash`, `SyllabusParser`, `SyllabusProgress`, `VtopParser` and
  `AlarmPattern`.
- **Motion** lives in `app/NavTransitions.kt`: each nav entry is tagged as a tab (with its
  position) or a page, and one rule picks a sideways slide or a slide up/down.
- **Offline first.** The signed-in account is cached in DataStore, so a saved session opens Home
  without internet. A profile saved offline is marked `needsSync` and pushed on the next launch.
- **Alarms** use `AlarmManager.setAlarmClock`. A foreground service plays the sound with
  alarm audio attributes and shows a full-screen ringing activity over the lock screen.
  Everything is re-planned after a reboot, a clock or time-zone change, an app update, every
  edit, and once a day.
- **Privacy.** Only the `profiles` row goes to Supabase. No analytics or crash-reporting SDKs.
  Library files, the profile photo and the blurred background photo stay in the app's private
  storage. Shared files are copied in (links from other apps expire); identical files are
  stored once and deleted only when no capture uses them.

## Reliability tips

Open **Settings → Reliability** and make every row green. On Xiaomi, Oppo, Vivo, Realme and
Samsung phones, also open the app info page and allow *Autostart* or *Don't restrict battery*.
Otherwise the phone may stop alarms while the app is closed.

## Known limitations (V1)

- Data lives only on the phone. Export a backup regularly (**Settings → Backup**); without
  one, uninstalling the app or losing the phone erases everything.
- The backup does not include the Home look (name, font, colour, background photo).
- The "Contact us" address is `contact_email` in `res/values/strings.xml`.
