# GID — Architecture

How the app is put together. Update this file whenever a layer, table, scheduler or
library changes. The product rules live in [../SPEC.md](../SPEC.md) and the coding rules in
[../RULES.md](../RULES.md).

## At a glance

- **Platform:** Android only, minSdk 26, compileSdk 36. Kotlin, Jetpack Compose and
  Material 3, with a dark theme only.
- **Modules:** one Gradle module (`app`), packaged by feature.
- **Dependency injection:** manual, through `AppContainer` (no Hilt).
- **Navigation:** Navigation 3 (`NavDisplay` with a back stack of `NavKey`s).
- **Storage:**
  - Room (SQLite) for all app data;
  - DataStore for small settings;
  - private files for photos and PDFs.
- **Cloud:** Supabase, for sign-in and the profile row only.

```
┌──────────────── UI (Compose) ─────────────────┐
│  Screen  ──observes──▶  ViewModel (StateFlow)  │
└──────────────────────────────┬────────────────┘
                               │ calls
                     Repository (interface + Impl)
                     │            │            │
                    DAO        DataStore   File store
                (Room/SQLite) (GIDPreferences) (filesDir)
                     │
            domain/ rules (plain Kotlin, unit tested)

Receivers / Worker / Services ──▶ AppContainer ──▶ Repositories & Schedulers
```

**The rule:** Screen → ViewModel → Repository → DAO. Screens never touch Room or
Supabase directly.

## Package layout

```
com.adheesh.gid
├── MainActivity, GIDApplication
├── app/          AppContainer (DI), GIDNavHost, NavDestinations, NavTransitions, GIDBottomBar
├── core/
│   ├── database/       Room entities + DAOs, GIDDatabase, Enums, GIDPreferences, SampleDataSeeder
│   ├── designsystem/   Color, Type, Shape, Theme tokens and shared composables (GIDCard, GIDDialog…)
│   ├── notifications/  Notifications (channels, builders, ids), permission prompt
│   ├── scheduling/     Schedulers (AlarmManager), Receivers, ring service, DailyMaintenanceWorker
│   └── util/           time helpers (epochDay, minutes-from-midnight, formatting), ids
└── feature/<name>/
    ├── ui/         Compose screens and components
    ├── viewmodel/  ViewModels exposing StateFlow
    ├── data/       repositories
    └── domain/     plain-Kotlin rules with unit tests
```

**Features:**
- alarm, appearance, attendance, auth, calendar, capture, debug, exams;
- history, home, library, sessions, settings, subjects, syllabus;
- tasks, timetable, widget.

## Dependency injection: `AppContainer`
- Built once in `GIDApplication`. It creates:
  - the database;
  - the preferences;
  - every repository;
  - every scheduler;
  - the Supabase client.
- **Screens:** ViewModels get their repositories from it through `viewModel { … }`
  factories in `GIDNavHost`.
- **Receivers and workers** reach it through `context.applicationContext as GIDApplication`.
- **Change hooks:** repositories take callbacks for side effects. For example, exam
  changes re-plan the exam reminders and refresh the widget, and attendance changes
  re-plan the after-class prompts.
- **`rescheduleEverything()`** rebuilds all alarms:
  - when it runs: on app start, after a reboot, a clock or time-zone change, an app
    update, and once a day;
  - what it rebuilds: alarms, class reminders, the evening prompt, exam reminders and
    attendance prompts. It also repairs running sessions and refreshes the widget.

## Navigation and motion
- `GIDNavHost` holds the back stack:
  - Home is always the base, and a tab switch replaces whatever is above it;
  - full-screen pages are pushed on top.
- **Tabs:** Home, Calendar, Library, History, Settings.
- **Pages:**
  - Exams, ExamDetail, SubjectDetail, CaptureDetail;
  - Assessments, Marks, MarksDetail, Attendance, Syllabus, Help, Debug.
- **`NavTransitions.kt`:** every entry is tagged with metadata, either a tab with its
  index or a page.
  - Tab to tab slides sideways, in the direction of the index.
  - A page slides up over the screen below and slides down on Back.
  - Nothing cross-fades.
- **Pop-ups:** `GIDDialog` (Alarm, Timetable) animates its own slide up and down inside
  the dialog window.
- **Background photo:** `AppBackdrop` sits behind the Scaffold. It fades in only on the
  four photo tabs. Pages and Settings are wrapped in an opaque `Page {}`.
- **Text over the photo:** `BackdropContrast` compares the photo's average colour after
  dimming, and `LocalBackdropColors` gives the result to text with no card behind it.

## Semesters
- `SemesterRepository` owns the semester list. `GIDPreferences` holds two ids: **viewed**
  (what screens filter by) and **running** (whose classes drive reminders, prompts, Home's
  class cards and subject suggestions).
- Subjects, exams and the timetable filter inside their repositories, so screens need no
  changes. Attendance, syllabus, marks and captures follow their subject.
- `VtopImporter` turns pasted pages into a semester's subjects, classes, marks and assessments.

## Data: Room (`GIDDatabase`, version 7)

| Entity | What it holds |
| --- | --- |
| Task | title, date, time, priority, estimate, progress, status, carriedFromId |
| TimetableEntry | class: subject, subjectId, day, start/end, room, reminder minutes, enabled |
| Holiday | date, name |
| ActivityType | name, colour, category (SLEEP, NAP, FOCUS, CLASS, FREE, OTHER) |
| Alarm | time, repeat days, enabled, ringtoneUri, snoozeMinutes, requirePattern (typeId is unused, kept so old data needs no migration) |
| ActivitySession | sleep, focus and other sessions with start/end and status |
| LocalProfile | local copy of the cloud profile, needsSync |
| Subject | name, nameKey (unique), colour, archived, attendanceTarget, priorPresent/Total |
| Heading | sections inside a subject |
| Capture (+ CaptureFts) | photo, link, text or PDF; type, status, subject, heading, filePath, contentHash; full-text search |
| CaptureCategory | editable categories; Capture.type holds a category id, and defaults use their old type name as id |
| Semester | name, sortKey |
| MarkComponent / MarkEntry | a subject's parts with credits and expectation; each uploaded mark |
| Exam | exam, test, assignment or project per subject; date, time, room, completedAt |
| Attendance | one class on one date: PRESENT, ABSENT or CANCELLED (unique per class, date and start) |
| SyllabusUnit / SyllabusTopic | units and topics per subject; doneAt on topics |
| ExamUnit | which units an exam covers |

**Every record** has a UUID string id plus `createdAt` and `updatedAt`. Enums are stored
as strings and dates as epoch days.

**Migrations.** All are automatic, and schemas are exported to `app/schemas/`.
`MigrationTest` checks each step on a device.
- 1 → 2: subjects and headings, captures and their search index. A hook turns timetable
  class names into subjects.
- 2 → 3: the exams table.
- 3 → 4: the attendance table, attendance columns on subjects, and `contentHash` on
  captures.
- 4 → 5: the syllabus units, topics and exam-unit links.
- 6 → 7: semesters, marks, `Subject.semesterId` (unique with the name),
  `TimetableEntry.semesterId`, `Exam.importKey`.
- 5 → 6: capture categories and the alarm options. A hook seeds the default categories, and
  a `RoomDatabase.Callback` does the same on a fresh install.

No build may drop the database on a schema change: a missing migration fails loudly instead.

## Data: DataStore (`GIDPreferences`)
These are small settings only, kept outside Room.
- **Times:** evening prompt time, default reminder minutes.
- **Account:** the signed-in user's id and email, cached so the app opens offline.
- **Profile photo:** the path to the profile photo.
- **Scheduled-alarm counts:** class reminders, exam reminders and attendance prompts. A
  re-plan cancels exactly the old alarms before setting new ones.
- **Home look:** name, font, colour, background path, background dim.
- **Attendance:** the attendance target.
- **One-time flags:** first launch, notification rationale shown, capture hashes filled.

## Files
- `filesDir/captures/` holds the captured photos and PDFs:
  - shared files are copied in, because links from other apps expire;
  - a SHA-256 `contentHash` is worked out during the copy, so the same file shares one
    copy;
  - a file is deleted only when no capture points to it any more.
- The profile photo and the blurred background photo are also in `filesDir`.
- A `FileProvider` gives the camera app a place to write photos.

## Scheduling and notifications
- **The replan pattern:** each scheduler cancels the alarms it set last time (using the
  stored count), works out the next plan with a pure planner, then sets the new alarms
  with `ExactAlarms.setWhileIdle`.

| Scheduler | Planner | Receiver |
| --- | --- | --- |
| AlarmScheduler (`setAlarmClock`) | AlarmTimeCalculator | AlarmReceiver → AlarmRingService (foreground, full-screen) |
| ClassReminderScheduler | ReminderPlanner | ClassReminderReceiver |
| ExamReminderScheduler | ExamReminderPlanner | ExamReminderReceiver |
| AttendancePromptScheduler | AttendancePromptPlanner | AttendancePromptReceiver, AttendanceActionReceiver (buttons) |
| EveningPromptScheduler | AlarmTimeCalculator | EveningPromptReceiver |
| TimerScheduler | — | TimerReceiver |

- **Receivers re-read the data** when they fire, so edits made after planning are
  respected.
- **`SystemEventsReceiver`** handles boot, time and time-zone changes, app updates and
  changes to the exact-alarm permission. `DailyMaintenanceWorker` runs once a day.
- **Notification channels:** alarms, timers, class reminders, sessions, evening prompt,
  exam reminders, attendance. The `Notifications` object owns the ids and the builders.

## Alarm ringing
- `AlarmReceiver` takes a wake lock first, because Android only keeps the phone awake until
  a receiver returns. If Android refuses the service, a backup notification rings instead.
- `AlarmRingService` owns the sound, for alarms and finished timers. It waits at most 1.5 s
  for the database, lifts the alarm volume to at least 70% for that ring, and falls back
  through several tones to a beep.
- `AlarmDiagnostics` keeps a short on-phone log of each step, shown under Test alarm.
- `AlarmRingActivity` shows the ringing screen. When `requirePattern` is on it uses
  `AlarmPattern` to make a random path, shows it with `PatternTarget` and checks the drawing
  from `PatternInput`; Dismiss and Snooze appear after that. The notification drops both
  actions, so the shade can't skip it.

## Capture entry points
- **`ShareReceiverActivity`:** the share sheet. It's a see-through activity that shows a
  "Saved to Inbox" card.
- **`QuickCaptureActivity`:** Camera, Photo, Paste or a menu. The widget and the tile
  both use it.
- **`GIDWidget`:** a Jetpack Glance home-screen widget, refreshed on any Inbox or exam
  change and once a day.
- **`CaptureTileService`:** the Quick Settings tile.

## Backup
- `BackupManager` writes one zip containing:
  - `manifest.json` with the app and database versions;
  - the database files;
  - `captures/`;
  - the profile photo.
- **Import:**
  - refuses a backup from a newer database version;
  - asks you to confirm before replacing everything;
  - restarts the app.

## Cloud (Supabase)
- **What's stored:**
  - Auth: email and password, or Google;
  - one `profiles` row per user, protected by row-level security.
- **What's never uploaded:** tasks, timetable, Library, exams, attendance, syllabus,
  sessions, photos.
- **Keys:** they come from `local.properties` through `BuildConfig`. They are never
  committed, and the `service_role` key is never used.

## Tested rules (`src/test`)
These are plain Kotlin with no Android code:
- ScoreCalculator, SessionCalculator, SessionRules, TaskRules;
- AlarmTimeCalculator, ReminderPlanner, HistoryCalculator, AgeCalculator;
- CaptureTimeResolver, SubjectSuggester, LinkExtractor, ContentHash;
- CaptureFileTypes, BackdropContrast, PhotoCrop, MarksMath, VtopMarksParser, VtopAssessmentParser;
- VtopParser (checked against real Fall and Winter VTOP grids), AlarmPattern;
- ExamReminderPlanner, Countdown;
- Appearance and BoxBlur;
- AttendanceMath, AttendancePromptPlanner;
- SyllabusParser, SyllabusProgress.

Instrumented tests (`src/androidTest`) cover the database migrations.

## Build
- **Gradle tasks:** `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleRelease`
  (signed with the keystore from `local.properties`).
- **`BUILD_DIR`:** set in `local.properties`, it moves the build output outside OneDrive.
  APKs then land in `<BUILD_DIR>/app/outputs/apk/...`.
- **Main libraries:** Compose BOM, Navigation 3, Room 2.7, DataStore, WorkManager,
  Glance 1.2, Coil 3, the Supabase Kotlin client, and ExifInterface.
