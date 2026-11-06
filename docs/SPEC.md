GID V1 SPEC
Personal productivity app for a college student. Sign in once; after that,
tasks, timetable, alarms, sleep and history work offline and stay on the
device. The cloud only holds the sign-up profile.
1. NAVIGATION
- Launch: no session -> Auth. Session exists -> Home (stay signed in; works
offline).
- First sign-in or sign-up -> Settings in setup mode if name or DOB is
missing, else Home. 'Go to home' is always allowed.
- Bottom bar on Home, Calendar, Library, History and Settings: Home |
Calendar | Library | History | Settings. The Settings icon shows a dot while
name or DOB is missing.
- Library opens full-screen SubjectDetail and CaptureDetail pages (back arrow
returns to Library). Exams and ExamDetail are full-screen pages too, opened
from Home's 'Next up' card, Library and exam reminders.
- Popups are dialogs over Home, not separate screens: AlarmPopup and
TimetablePopup. History day details are also a dialog. Each has a back arrow
top-left that closes it.
- Motion: switching tabs slides the pages sideways, as if the five tabs sit
side by side (left to right in bottom-bar order), fast (about 0.2 s). Pages
and popups (Exams, ExamDetail, SubjectDetail, CaptureDetail, AlarmPopup,
TimetablePopup, Debug) slide up from the bottom over the screen below and
slide back down on Back. No cross-fades, so two screens never show at once.
2. SCREENS
2.1 Auth (placement from the reference screenshot, our own colors)
- Curved header with logo mark; title 'Sign in to GID'; Email field;
Password field with show/hide; wide pill button 'Sign In'; social row (Google
only); 'Don't have an account? Sign up'; 'Forgot password'.
- Sign Up mode: same layout plus confirm password and one line of privacy
text saying what is stored (name, DOB, email, phone, institution) and that
productivity data stays on the device.
- Inline error messages. Forgot password sends the Supabase reset email.
2.2 Home
- Top row: 'Hello <Name>' (fallback 'Hello there') and a circular profile
picture (initial letter if none), centred on the 'Hello,' line. Avatar tap ->
Settings.
- Tap the greeting -> Personalise sheet: the name shown on Home (default:
first word of the profile name; only changes Home, not the profile), its font
(GID, DM Sans, Serif, Mono, Script, Light) and colour (text, cyan, coral,
mustard, periwinkle, sage, green), and the app background.
- Background: 'Choose photo' picks an image; the app blurs it once and keeps
only the blurred copy on the device. Text with no card behind it (titles, search,
section headings) turns dark on a light photo and stays light on a dark one,
judged from the photo's average colour after dimming. A 'Dim' slider (0-85%, default 45%)
darkens it so cards stay readable. 'Remove' goes back to the plain
background. The photo shows only behind Home, Calendar, Library and History;
it fades out on Settings, on full-screen pages (Exams, subject and capture
pages) and while AlarmPopup or TimetablePopup is open.
- Calendar widget below, headed by the current month's name: 2 rows x 7
columns (this week + next week). Today
highlighted, a dot under dates that have tasks, green for holidays. Tap ->
Calendar tab (tapped date preselected).
- Left column: Alarm card (periwinkle). Heading 'Alarm' opens AlarmPopup;
shows the next alarm time and type, or 'No alarms'. Below it, Upcoming
Classes card (mustard). Heading 'Timetable' opens TimetablePopup; shows the
next 3 classes today (or the next class day); 'No classes' on holidays.
- Right column, tall: Tasks card (coral). Today's tasks by priority then
time. Row = checkbox, title, priority dot, percent done. Checkbox sets 100%.
'+' quick add. Tap a row -> task detail sheet (same component as Calendar).
- Inbox row, then a 'Next up' card: the next exam or assignment with its
countdown, time and room ('Nothing due' when empty). Tap -> Exams.
2.3 Calendar
- Month grid with previous/next month. Today ringed, selected day filled
coral. Dot under days with tasks. Holidays green. Long-press a date -> mark
or unmark holiday (optional name).
- A purple dot under days with an exam or assignment; the selected day's
exams are listed above its tasks (tap -> ExamDetail).
- Below the grid: left = list 'Tasks on <date>'; right top = selected task
detail (title, description, time, estimated minutes, percent-done slider 0-
100); right bottom = priority (Low/Medium/High, editable).
- '+' adds a task for the selected date: title, description, date, optionaltime, priority, estimated minutes. Task actions: edit, delete, 'Move to
tomorrow'.
2.4 History (week selector with arrows, default this week; three cards)
- Productivity: Mon-Sun bars of day score. A weekly line with total focus
time. Tap a bar -> DayDetailDialog: date, day score, tasks with percent,
focus time per activity type.
- Sleep: Mon-Sun bars of total sleep hours. Tap a bar -> dialog: nap time,
long sleep, total sleep, and the list of sessions with start and end.
- Weekly score: big 0-100 number, change vs the previous week (points and %,
or 'No data last week'), tasks completed / registered, average percent done,
focus hours. No AI: deterministic functions only.
2.5 Settings
- Profile picture with 'Edit' under it (system photo picker, stored on device
only).
- Name*, Date of birth* (date picker) with computed Age (read-only), Email
(read-only, from the account), Phone (optional), College/institution.
- Buttons: 'Go to home', 'Contact us' (opens an email to YOUR_EMAIL,
placeholder), 'Sign out' (confirm dialog). Plus a 'Reliability' section (see
section 7).
- Save validates name and DOB, writes locally first, then upserts the
Supabase profile. If offline, set needsSync and retry on next launch.
- Profile photo: a picked photo opens a preview to zoom and drag inside a circle.
Nothing is saved until 'Use photo'; the chosen square is saved at up to 512 px.
2.5a Help
- Near the bottom of Settings, 'How to use GID' opens a page (slides up)
explaining every feature, one expandable section each.
- 'Contact us' opens the mail app addressed to adhieshjain@gmail.com.
2.6 AlarmPopup (floating dialog)
- Segmented control: Alarms | Timers.
- Alarm row: time, label, type chip, repeat days, on/off switch. Add/edit:
time picker, label, repeat days ('All days' chip, on by default for a new
alarm; off means it rings once). Alarms have no activity type. Timers do, and a
finished timer rings until stopped (no snooze), even with the app closed.
- 'Test alarm' rings in 30 seconds through the same path as a real alarm, and an
'Alarm log' shows what happened (set, fired, sound, any failure).
- Nothing may end in silence: a wake lock from the moment the alarm fires, the
sound starts within 1.5 s even if the database is slow, the alarm volume is
lifted to at least 70% for that ring and restored after, each tone falls back to
the next, then to a beep. If Android refuses the service, a loud backup
notification opens the ringing screen.
- Timer: duration, type, Start. A running timer shows a live countdown.
- Per alarm: ringtone (system picker; default = the phone's alarm sound), snooze
length (1, 2, 5, 10, 15 or 30 minutes; default 5) and 'Draw a pattern to stop or
snooze' (default on).
- Pattern to stop: the ringing screen shows a random 5-dot path on a 3x3 grid,
numbered in order, different every ring. Dismiss and Snooze appear only after
that path is drawn on the grid below it, and the alarm rings until one is
tapped. A wrong path buzzes and can be retried. The notification has no buttons
while a pattern is required. With the option off, both buttons show at once.
- Bottom: 'Activity types' manager: name, color, category. Add/edit/delete
(defaults can be renamed, not deleted).
- Default types: Sleep (SLEEP), Nap (NAP), Study session, Vibecode, DSA,
Learning on YouTube (all FOCUS), Class (CLASS), Free time (FREE).
2.7 TimetablePopup (floating dialog)
- Current weekly timetable grouped by day: subject, start-end, room. 'Change
timetable' switches to edit mode: add/edit/delete classes, reminder minutes
before (default 10), enable/disable.
- No reminders on holiday dates.
- 'Paste from VTOP' (empty state and edit mode): paste VIT VTOP's whole Time
Table page. GID reads the THEORY and LAB time rows and each day's cells, so
every class gets VTOP's exact times; a registered cell looks like
A2-BACSE202-ETH-PRP576-ALL. Course names come from the course list above the
grid. Neighbouring cells of one course (L3+L4) become one class. Labs show
'<venue> · Lab'. A preview shows the semester, the class count and courses
with no slot (projects, MOOCs) before it replaces the timetable. Past
attendance marks are kept.
- Saving a class links it to a Subject with the same name (case and spaces
ignored), creating the subject if needed.
2.8 Library (V1.1: Capture Inbox, on the device only, no AI)
- Search bar at the top: matches capture titles, notes and links, and
subject and heading names.
- Inbox section: count of unsorted captures, then one card per capture
(newest first): thumbnail or link/text preview, capture time, and the
suggested subject as a chip, and a delete button (with confirm). A PDF's
title is its file name. Tap -> Sort sheet. Long-press starts multi-select;
'Sort N' opens the Sort sheet for all selected, 'Delete N' deletes them.
- Subjects section: '+ New subject', then one row per non-archived subject
with its item count and a menu (Archive, Delete). Tap -> SubjectDetail.
'Archived (N)' shows archived subjects with Restore and Delete.
- Deleting a subject deletes its headings; its captures go back to the Inbox
and classes using it are unlinked (saving the class again re-creates it).
- '+' (also on Home) opens Capture: Camera, Photo from gallery, Paste
link or text.
- Sort sheet: type chips (Exam question, Resource, Note, Deadline, Idea,
Notice), subject chips (suggested one preselected), heading chips of that
subject plus 'New heading', optional title and note. Deadline adds a date and
'Also create a task'. 'Save' marks the capture SORTED.
- Categories: the Sort sheet's types are editable. Defaults: Exam question,
Resource, Note, Deadline, Idea, Notice. Add ('+ New category' in the Sort
sheet or the manager), rename or remove any of them, except that Deadline can
only be renamed because it carries the due date. Removing a category keeps its
items; they stay under All with no category.
- SubjectDetail tabs (swipe or tap): 'All' first, then one tab per category
with its item count. All also holds the Syllabus and Attendance cards,
upcoming exams and '+ New heading'. A tune icon by the tabs opens the
category manager.
- SubjectDetail: subject name and color; headings as sections in order, then
'Unfiled'. Rename/recolor/archive/delete the subject; add, rename, reorder and
delete headings (items in a deleted heading become Unfiled).
- CaptureDetail: the full photo, the PDF (opens in another app), or the
link (opens the browser and records openedAt) and the text; type, subject,
heading; 'Sort again', 'Delete' (confirm; also deletes the stored file).
- Share sheet: GID appears when sharing text, links, images or PDFs from any
app. The capture is saved straight away and a small dialog says 'Saved to
Inbox' with the suggested subject, plus 'Sort now' and 'Done'.
- Photos, PDFs, Word (.doc, .docx) and PowerPoint (.ppt, .pptx) files can be
captured by sharing or with the file picker; Word and PowerPoint open in
another app and are titled with their file name.
- Files are copied into GID (shared links from other apps stop working, and
the original may be moved or deleted). The same file captured again (same
contents) reuses the existing copy instead of storing a second one; the file
is deleted only when no capture uses it any more.
2.9 Backup (in Settings)
- A 'Storage' line shows how much space Library files use (size and count).
- 'Export backup' writes one .zip (database, captured files, profile photo,
manifest.json with app and database versions) to a place the user picks.
- 'Import backup' reads such a .zip, refuses one from a newer database
version, asks to confirm overwriting everything, then restarts the app.
2.10 Exams (V1.2, on the device only)
- Exams page: 'Upcoming' cards (soonest first): subject dot, title, kind chip
(Exam, Test, Assignment, Project), date, time, room and a countdown chip
(coral when today or tomorrow, cyan within 7 days). A collapsed 'Past & done'
section below. '+' opens the editor.
- Editor (bottom sheet): kind, subject (chips plus 'New subject'), title,
date, optional time, room, notes.
- ExamDetail: everything above, 'Mark done' ('Mark submitted' for
assignments and projects; undo allowed), Edit, Delete (confirm), and the
subject's saved Library items so revision starts from one place.
- SubjectDetail shows the subject's upcoming exams at the top. Library has an
'Exams' button next to its title.
- Sort sheet: a Deadline can also create a Task, an Exam or an Assignment.
2.11 Widget and Quick Settings tile
- Home-screen widget (resizable): Camera and Paste buttons and the Inbox
count; when wider, also Photo and an Inbox button, plus the next exam with its
countdown. Refreshed on every capture or exam change and once a day.
- Quick Settings tile 'GID capture': opens a small menu (Camera, Photo,
Paste). Settings > Reliability has an 'Add capture tile' button (Android 13+).
- Both save through the same path as the share sheet and show the same 'Saved
to Inbox' card.
2.12 Attendance (V1.3, on the device only)
- Each class can be marked Present, Absent or Cancelled for a date.
Cancelled classes never count. Marking the same class on the same date again
replaces the mark.
- When a class ends, a notification asks 'Were you in <subject>?' with
Present / Absent / Cancelled buttons; tapping one saves the mark and shows
'Marked present - DBMS 82%'. No prompt on holidays, for disabled classes, or
when the class is already marked.
- Home's Upcoming Classes card: today's classes that have started show
three small buttons (present, absent, cancelled) with the current mark; an
'Attendance' link opens the Attendance page.
- Attendance page (slides up): one card per subject with classes or marks:
percent, a bar (coral below the target, mustard within 5 points above it,
green otherwise) and advice: 'You can miss N more' or 'Attend the next N to
reach 75%'. Tap -> subject sheet: 'Before GID: attended X of Y' (classes before
the app was used) and a target override for that subject.
- SubjectDetail shows the subject's attendance line at the top.
- Settings: 'Attendance target' (50-100%, default 75), used by every subject
without its own target.
2.13 Syllabus (V1.4, on the device only)
- Each subject has units (e.g. 'Unit 1: Intro'), each with topics that are
ticked off when covered. Percent per unit and per subject.
- SubjectDetail shows a 'Syllabus' card (topics done of total, percent, bar);
tap -> Syllabus page (slides up).
- Syllabus page: units as sections with their percent; topic rows with a
checkbox and a menu (Rename, Move up, Move down, Delete); '+ Topic' under each
unit, '+ Unit' at the bottom; unit menu (Rename, Move up, Move down, Delete;
deleting a unit deletes its topics).
- 'Paste list': a box to paste a syllabus. A line starting with Unit, Module
or Chapter, or ending with ':', starts a unit; every other line is a topic
(leading bullets and numbers removed); topics before any unit go into
'General'. A preview ('2 units, 7 topics') shows before saving. A unit with
the same name as an existing one adds its topics to it.
- Exam editor: after picking the subject, chips to tick the units the exam
covers. None ticked = the whole subject.
- ExamDetail: a syllabus card (topics done of total for the exam's units, a
bar) listing the topics still to do, tickable there; link to the Syllabus
page. Home's 'Next up' shows a small 'N% syllabus' chip when the subject has
topics.
- Bar colour: green when done or on track, mustard when the exam is within 14
days and under 70% done, coral when within 7 days and under 70% done.
3. BEHAVIOUR RULES
Sessions: one table (ActivitySession) feeds both sleep and focus stats.
- Timer: start creates a RUNNING session with endsAt; finishing or stopping
closes it with the real duration.
- Sleep and naps are counted from SLEEP and NAP timers. Dismissing a ringing alarm
closes any of them still running.
- A session still RUNNING after 16 hours is closed automatically and flagged.
- Sleep counts on the day the session ENDS. Long sleep = SLEEP sessions, nap
= NAP sessions, total = both. Focus time = FOCUS sessions grouped by type.
Tasks:
- Percent done drives status: 0 = PENDING, 1-99 = IN_PROGRESS, 100 = DONE
(set completedAt). Overdue = date before today and under 100.
- Evening prompt at 21:00 (configurable): a notification opens 'Update
today's progress' with a slider per task and 'Move unfinished to tomorrow'.
- 'Move to tomorrow' moves the SAME task to the next day: it leaves the
previous day's list and keeps its percent, so the user carries on from it.
(carriedFromId is only set on copies made by older versions.)
Exams (plain Kotlin, unit tested):
- Reminders: Exam and Test at 19:00 seven days before, 19:00 the day before,
and 07:00 on the day (or 2 hours before the start, if earlier). Assignment
and Project: 19:00 the day before and 07:00 on the day. Past times and done
items get none; at most 60 are scheduled, re-planned after every exam change,
reboot, time change and once a day.
- Countdown: 'Today', 'Tomorrow', 'in N days' (up to 13), then 'in N weeks'.
Score (plain Kotlin, unit tested):
- Weights: HIGH 3, MEDIUM 2, LOW 1.
- DayScore = sum(weight x percent) / sum(weight). No tasks that day = null,
shown as 'no data', never 0.
- WeekScore = average of the non-null DayScores. Change vs last week =
difference in points and in percent.
4. ARCHITECTURE
Package by feature, one Gradle module: app/ (MainActivity, navigation,
AppContainer), core/ (database, designsystem, notifications, scheduling,
util), feature/ (auth, home, calendar, history, settings, alarm, timetable,
tasks, sessions). Inside a feature: ui/, viewmodel/, data/ (repository, dao),
domain/ for plain logic.
Repositories are interfaces so a cloud-sync version can be added in V2
without touching screens.
5. DESIGN TOKENS (dark theme only; colors estimated from the reference image)
Colors: background #161618 | surface #232326 | surfaceHigh #2E2E32 | text
#F4F3EE | mutedText #9B9BA1 | sage #C6DAD0 | periwinkle #6E6BF2 | coral
#F2675A | mustard #F5C842 | cyan #7FE3E6 | holidayGreen #5ED393 | error
#FF5C5C
Use: Alarm card periwinkle, Tasks card coral, Classes card mustard. History:
productivity coral, sleep periwinkle, score mustard. Priority High coral,
Medium mustard, Low periwinkle. Active nav icon cyan. Text on colored cards
is near-black (#161618).
Type: titles in Space Grotesk Bold, uppercase for screen titles, tight letter
spacing; body in DM Sans. Bundle the font files in res/font (no runtime
download). Scale: title 28, section 20, body 16, caption 12.
Shape: cards 28dp radius, fully rounded pill buttons, a floating pill-shaped
bottom bar, dialogs 28dp radius over a dim scrim. Material rounded icons; no
illustrations in V1.
All tokens live in core/designsystem (Color.kt, Type.kt, Shape.kt, Theme.kt).
6. DATA
Room entities (all ids are UUID strings, plus createdAt and updatedAt):
- Task: title, description, dateEpochDay, timeMinutes?, priority,
estimatedMinutes?, category?, progressPercent, status, completedAt?,
carriedFromId?
- TimetableEntry: subject, subjectId?, dayOfWeek, startMinutes, endMinutes,
room?, reminderMinutes, enabled
- Subject: name, nameKey (lowercase, trimmed, unique), colorHex?, archived
- Heading: subjectId, name, sortOrder
- Capture: kind (PHOTO, LINK, TEXT, PDF), type? (EXAM_QUESTION, RESOURCE,
NOTE, DEADLINE, IDEA, NOTICE), status (INBOX, SORTED, ARCHIVED), title, note,
url?, filePath?, mimeType?, capturedAt, subjectId?, suggestedSubjectId?,
headingId?, dueEpochDay?, linkedTaskId?, openedAt?. Full-text index over
title, note and url. Files live in the app's private files/captures folder.
- Exam: subjectId, title, kind (EXAM, TEST, ASSIGNMENT, PROJECT),
dateEpochDay, timeMinutes?, room?, notes, completedAt?, sourceCaptureId?
- Attendance: subjectId, timetableEntryId?, dateEpochDay, startMinutes,
status (PRESENT, ABSENT, CANCELLED); unique per (timetableEntryId,
dateEpochDay, startMinutes).
- Subject also has attendanceTarget? (percent; null = the Settings target),
priorPresent and priorTotal (classes before GID). Capture also has
contentHash? (SHA-256 of the file, to spot the same file captured twice).
- SyllabusUnit: subjectId, name, sortOrder
- SyllabusTopic: unitId, subjectId, name, sortOrder, doneAt?
- ExamUnit: examId, unitId (which units an exam covers)
Database version 2: migration 1->2 creates one Subject per distinct timetable
class name and links the classes to it. Version 3 adds the exams table
(automatic migration, no data changes). Deleting a subject deletes its exams.
Version 4 adds the attendance table and the new Subject and Capture columns
(automatic migration). Deleting a subject deletes its attendance marks.
No build may wipe data on a schema change: there is no destructive fallback.
Version 6 adds capture categories (seeded with the defaults) and the alarm
ringtone, snooze and pattern columns (automatic migration).
Version 5 adds the syllabus tables and exam-unit links (automatic
migration). Deleting a subject deletes its syllabus; deleting an exam or unit
deletes its links.
Attendance rules (plain Kotlin, unit tested):
- percent = present / (present + absent), including the 'before GID' counts.
- canMiss = the most absences in a row that keep percent >= target.
- mustAttend = the fewest presents in a row that bring percent back to the
target (impossible when the target is 100% and a class was missed).
Capture rules (plain Kotlin, unit tested):
- capturedAt = the photo's EXIF time, else the gallery's date taken, else
now; never 0 or in the future.
- Suggested subject = the enabled class running at capturedAt, from 10
minutes before it starts to 15 minutes after it ends; none on holidays.
- Holiday: dateEpochDay, name?
- ActivityType: name, colorHex, category (SLEEP, NAP, FOCUS, CLASS, FREE,
OTHER), isDefault
- Alarm: label, typeId, hour, minute, repeatDaysMask, enabled, ringtoneUri?
(null = phone default), snoozeMinutes (default 5), requirePattern (default on)
- CaptureCategory: name, sortOrder. The defaults use their old type name as id
(RESOURCE, ...), so Capture.type holds a category id.
- ActivitySession: typeId, category, startAt, endAt?, plannedMinutes?, source
(ALARM, TIMER, MANUAL), status (RUNNING, COMPLETED, CANCELLED), flagged
- Profile (local copy): name, dob, phone, institution, needsSync
DataStore (not Room): eveningPromptTime, defaultReminderMinutes,
profilePhotoPath, homeName, homeNameFont, homeNameColor, backgroundPath
(blurred copy in app files), backgroundDim, attendanceTarget (default 75). Appearance is local only and is
not part of the backup.
Supabase SQL (run once in the SQL Editor):
create table public.profiles (
id uuid primary key references auth.users(id) on delete cascade,
email text,
full_name text,
date_of_birth date,
phone text,
institution text,
created_at timestamptz default now(),
updated_at timestamptz default now()
);
alter table public.profiles enable row level security;
create policy profiles_select_own on public.profiles for select using
(auth.uid() = id);
create policy profiles_insert_own on public.profiles for insert with check
(auth.uid() = id);
create policy profiles_update_own on public.profiles for update using
(auth.uid() = id);
create function public.handle_new_user() returns triggerlanguage plpgsql security definer set search_path = ''
as $$
begin
insert into public.profiles (id, email) values (new.id, new.email);
return new;
end;
$$;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();
As the owner you see sign-ups under Authentication > Users and Table Editor >
profiles. Each user can read and change only their own row.
7. PERMISSIONS AND RELIABILITY
Manifest: INTERNET, POST_NOTIFICATIONS (runtime), SCHEDULE_EXACT_ALARM (if
canScheduleExactAlarms() is false, send the user to the system screen to
allow it), USE_FULL_SCREEN_INTENT (ringing screen over the lock screen),
RECEIVE_BOOT_COMPLETED, VIBRATE, WAKE_LOCK, and a foreground service with the
correct type.
Store absolute trigger times. Timers persist endsAt and never rely on a
running coroutine. Reschedule everything after reboot, time or timezone
change, app update, and any edit.
Settings > Reliability shows live status of notifications, exact alarms and
battery optimisation, with buttons that open the right system screens, plus a
note that some phones (Xiaomi, Oppo, Vivo, Realme, Samsung) need 'don't
restrict battery' or autostart turned on.
Test every alarm with the screen off, on the lock screen, with Do Not Disturb
on, and after a reboot.
8. OUT OF SCOPE FOR V1
Screen-time tracking, AI features (including reading text from photos),
friends and leaderboards, cloud sync of productivity data or captures, link
previews, exam revision packs and spaced-repetition quizzes (planned for a
later version), Facebook/Instagram login, recurring tasks, iOS.
