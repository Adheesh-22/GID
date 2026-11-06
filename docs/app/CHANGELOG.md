# GID — Change history

Every version, newest first, from the first commit. Each entry gives the date, the commit
and what changed in plain language. Add to **Unreleased** while working; move it under a
version when it's committed.

---

## Unreleased (not committed yet)

### V1.7: Semesters, Marks, separate Assessments
- **Semesters:** subjects and everything attached to them are split by semester. Pasting a
  Time Table page sorts existing subjects by name. A running semester drives reminders.
- **Marks:** paste Marks View; per-subject page with uploaded, left and expected marks, totals
  out of 100 weighted by credits.
- **Assessments:** own page, separate from Exams, with Assignment Upload paste. Home shows the
  next exam and next assessment side by side.
- **Database:** version 7 (automatic migration).

### V1.6: Reliable alarms, simpler alarms, photo crop
- **Alarm not ringing:** several real gaps fixed:
  - a wake lock from the moment the alarm fires (the sound had been waiting on a database
    read with nothing keeping the phone awake);
  - the sound no longer waits more than 1.5 s for that read;
  - alarm volume lifted to at least 70% for the ring, then restored;
  - more fallback tones, then a beep;
  - a backup notification if Android refuses the service (before, the app crashed).
- **Test alarm and Alarm log:** ring in 30 seconds and see what happened.
- **Timers:** a finished timer now rings until stopped, instead of one notification beep.
- **Alarms are plain:** no activity types, no "Start sleep now", no session offer after
  dismissing. **All days** button, on by default.
- **Settings:** a new profile photo opens a preview and crop screen before saving.
- **Attendance:** the marks list is gone from the subject sheet.
- **Home:** Attendance today card layout; Snooze also needs the pattern.

---

## 4 Nov 2026

### `cd7a54f` V1.5: Subject tabs, alarm pattern, VTOP import
- **Library:** each subject opens with tabs: All, then one per category. Categories can be
  added, renamed and removed (Deadline can only be renamed).
- **Alarms:**
  - a ringtone per alarm;
  - snooze length per alarm, 5 minutes by default (it was fixed at 2);
  - **Draw a pattern to stop or snooze:** a random 3×3 pattern, on by default.
- **Timetable:** Paste from VTOP builds the whole timetable from VIT's Time Table page.
- **Settings:**
  - **How to use GID**, a guide to every feature;
  - Contact us now fills in the To address (it held a placeholder).
- **Home:** the month's name above the calendar strip.
- **Library files:** Word and PowerPoint can be captured, by sharing or with the picker.
- **Wallpaper text:** text with no card behind it turns dark on a light photo.
- **Data safety:** removed the debug-only fallback that could wipe the database on a schema
  problem.
- **Database:** version 6 (automatic migration).

---

## 3 Nov 2026

### `95ecac6` V1.3 and V1.4: Attendance, Syllabus, app docs

#### V1.4: Syllabus tracker
- **Units and topics:** each subject gets units and topics you tick off, with a percent
  per unit and per subject.
- **Syllabus page:**
  - add, rename, reorder and delete units and topics;
  - **Paste list** turns a pasted syllabus into units and topics, with a preview first.
- **Exams:**
  - an exam can cover chosen units;
  - the exam page shows the topics still to do, tickable there;
  - Home's Next up shows "N% syllabus".
- **Database:** version 5 (automatic migration) adds the syllabus tables.
- **Docs:** the `docs/app/` folder: FEATURES.md, ARCHITECTURE.md and this changelog.

#### V1.3: Attendance and no duplicate files
- **After-class notification:** "Were you in …?" with Present / Absent / Cancelled
  buttons.
- **Home:** "Attendance today" buttons.
- **Attendance page:**
  - one card per subject with the percent against your target and "can miss / must
    attend" advice;
  - "Before GID" counts and a target per subject.
- **Settings:** the attendance target (75% by default).
- **Duplicate files:** captured files get a SHA-256 fingerprint, so the same file is
  stored once and deleted only when no capture uses it. Settings shows how much space
  Library files use.
- **Database:** version 4 (automatic migration).

---

## 2 Nov 2026

### `436e4f7` README updated
- The README covers the Personalise sheet, the slide transitions, where APKs land with
  `BUILD_DIR`, and the new rule classes.

### `5a8e2f7` V1.2: Exams, widget, tile; motion and personalisation
- **Exams and assignments:**
  - exams, tests, assignments and projects per subject;
  - an Exams page, an editor and an exam page with Mark done / Mark submitted;
  - countdown chips;
  - Home's Next up card, purple Calendar dots, and upcoming exams on subject pages;
  - a Deadline capture can create an exam or assignment.
- **Exam reminders:** 19:00 a week before, 19:00 the day before, and the morning of.
- **Database:** version 3 (automatic migration).
- **Home-screen widget (Glance):** Camera, Photo, Paste, the Inbox count and the next
  exam.
- **Quick Settings tile "GID capture":** opens a capture menu.
- **Motion:**
  - tabs slide sideways;
  - pages and the Alarm and Timetable popups slide up from the bottom;
  - this fixed the overlapping cross-fade.
- **Personalise:**
  - tap the greeting to set the name, font and colour;
  - a background photo, blurred on the phone and dimmable, shown only on the four main
    tabs;
  - the avatar is now level with the greeting.

### `a83fbed` Library fixes and new icon
- You can delete Inbox items, including several at once.
- PDFs show their file name.
- Subjects can be added, archived and deleted from the Library.
- New app icon (the swirl logo).

### `7401082` V1.1: Library (Capture Inbox)
- **Capture:**
  - share photos, links, text and PDFs to GID from any app, or capture with the camera,
    the gallery or a paste;
  - GID suggests the subject from the class that was on.
- **Sorting:**
  - a Sort sheet (type, subject, heading, note); a Deadline can create a task;
  - subjects, headings and subject pages;
  - full-text search.
- **Evening Inbox reminder.**
- **Backup:** export and import of one .zip (database and files).
- **Database:** version 2. The automatic migration turns timetable class names into
  subjects.
- **Tasks:** "Move to tomorrow" now moves the task instead of copying it (this also fixed
  a TaskRulesTest compile error).

---

## 23 Oct 2026

### `7f29b7a` Move-to-tomorrow keeps progress
- Moving a task to the next day removes it from the old day and keeps its progress.

---

## 20 Oct 2026

### `636cea0` Snooze is 2 minutes
- Alarm snooze changed from 10 minutes to 2.

### `8f18dfc` Home layout
- The welcome text and the calendar strip were placed to match the design.

### `aeea9d6` Merge PR #1: V1.0, phases 2–9 complete
- Merged `feature/phases-2-9` into main. Still open at that point: setting up Google
  sign-in.

### `992af4b` V1.0: phases 2–9
- **Phase 2, sign-in:**
  - build fixes (AGP 9, KSP);
  - a session that works offline;
  - sign-up and email confirmation, deep links, Google sign-in;
  - routing to Home or Settings.
- **Phase 3, Settings:**
  - profile, with the photo kept on the phone;
  - saved locally, then uploaded to Supabase, with a retry when offline.
- **Phase 4, Calendar:** month grid, holidays, adding and editing tasks, percent done,
  priority, Move to tomorrow.
- **Phase 5, Timetable:** the Timetable popup and class reminders (a 7-day plan that
  skips holidays).
- **Phase 6, Alarms:**
  - alarms, a ringing screen over the lock screen and snooze;
  - timers;
  - sleep sessions with a 16-hour auto-close;
  - everything re-planned after a reboot or a time change.
- **Phase 7, Home:** greeting, the 2-week strip, and the alarm, classes and tasks cards.
- **Phase 8, History:** productivity and sleep bars, day details, the weekly score.
- **Phase 9, polish:**
  - the evening progress prompt and the Reliability checklist;
  - strings moved to resources and lint cleaned up;
  - the README and release signing.
- 53 unit tests.

---

## 19 Oct 2026

### `5b933b5` V1: Phase 1 (foundation)
- **Project setup:** Compose, Material 3, the design tokens (colours, type, shapes) and
  the bottom bar.
- **Database:** Room with tasks, timetable, holidays, activity types, alarms, sessions
  and profile.
- **Settings storage:** DataStore preferences.
- **Wiring:** `AppContainer` (manual DI) and the Navigation 3 host.

---

## 18 Oct 2026

### `1e64488` Initial commit
- An empty README to start the repository.
