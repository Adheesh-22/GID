GID V1 SPEC
Personal productivity app for a college student. Sign in once; after that,
tasks, timetable, alarms, sleep and history work offline and stay on the
device. The cloud only holds the sign-up profile.
1. NAVIGATION
- Launch: no session -> Auth. Session exists -> Home (stay signed in; works
offline).
- First sign-in or sign-up -> Settings in setup mode if name or DOB is
missing, else Home. 'Go to home' is always allowed.
- Bottom bar on Home, Calendar, History and Settings: Home | Calendar |
History | Settings. The Settings icon shows a dot while name or DOB is
missing.
- Popups are dialogs over Home, not separate screens: AlarmPopup and
TimetablePopup. History day details are also a dialog. Each has a back arrow
top-left that closes it.
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
picture (initial letter if none). Tap -> Settings.
- Calendar widget below: 2 rows x 7 columns (this week + next week). Today
highlighted, a dot under dates that have tasks, green for holidays. Tap ->
Calendar tab (tapped date preselected).
- Left column: Alarm card (periwinkle). Heading 'Alarm' opens AlarmPopup;
shows the next alarm time and type, or 'No alarms'. Below it, Upcoming
Classes card (mustard). Heading 'Timetable' opens TimetablePopup; shows the
next 3 classes today (or the next class day); 'No classes' on holidays.
- Right column, tall: Tasks card (coral). Today's tasks by priority then
time. Row = checkbox, title, priority dot, percent done. Checkbox sets 100%.
'+' quick add. Tap a row -> task detail sheet (same component as Calendar).
2.3 Calendar
- Month grid with previous/next month. Today ringed, selected day filled
coral. Dot under days with tasks. Holidays green. Long-press a date -> mark
or unmark holiday (optional name).
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
2.6 AlarmPopup (floating dialog)
- Segmented control: Alarms | Timers.
- Alarm row: time, label, type chip, repeat days, on/off switch. Add/edit:
time picker, label, type, repeat days. For SLEEP/NAP types an extra toggle
'Start sleep now'.
- Timer: duration, type, Start. A running timer shows a live countdown.
- Bottom: 'Activity types' manager: name, color, category. Add/edit/delete
(defaults can be renamed, not deleted).
- Default types: Sleep (SLEEP), Nap (NAP), Study session, Vibecode, DSA,
Learning on YouTube (all FOCUS), Class (CLASS), Free time (FREE).
2.7 TimetablePopup (floating dialog)
- Current weekly timetable grouped by day: subject, start-end, room. 'Change
timetable' switches to edit mode: add/edit/delete classes, reminder minutes
before (default 10), enable/disable.
- No reminders on holiday dates.
3. BEHAVIOUR RULES
Sessions: one table (ActivitySession) feeds both sleep and focus stats.
- Timer: start creates a RUNNING session with endsAt; finishing or stopping
closes it with the real duration.
- SLEEP/NAP alarm: 'Start sleep now' creates a RUNNING session at the current
time. Dismissing the ringing alarm, or tapping 'I'm awake' on the ongoing
notification, closes it at that moment. An alarm without a session just
rings.
- FOCUS alarm: rings; on dismiss offer 'Start a session' with quick durations
(25/45/60 min).
- A session still RUNNING after 16 hours is closed automatically and flagged.
- Sleep counts on the day the session ENDS. Long sleep = SLEEP sessions, nap
= NAP sessions, total = both. Focus time = FOCUS sessions grouped by type.
Tasks:
- Percent done drives status: 0 = PENDING, 1-99 = IN_PROGRESS, 100 = DONE
(set completedAt). Overdue = date before today and under 100.
- Evening prompt at 21:00 (configurable): a notification opens 'Update
today's progress' with a slider per task and 'Move unfinished to tomorrow'.
- 'Move to tomorrow' creates a NEW task on the next day (carriedFromId set)
and leaves the original on its day with its final percent, so history stays
true.
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
- TimetableEntry: subject, dayOfWeek, startMinutes, endMinutes, room?,
reminderMinutes, enabled
- Holiday: dateEpochDay, name?
- ActivityType: name, colorHex, category (SLEEP, NAP, FOCUS, CLASS, FREE,
OTHER), isDefault
- Alarm: label, typeId, hour, minute, repeatDaysMask, enabled
- ActivitySession: typeId, category, startAt, endAt?, plannedMinutes?, source
(ALARM, TIMER, MANUAL), status (RUNNING, COMPLETED, CANCELLED), flagged
- Profile (local copy): name, dob, phone, institution, needsSync
DataStore (not Room): eveningPromptTime, defaultReminderMinutes,
profilePhotoPath
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
Screen-time tracking, AI features, friends and leaderboards, cloud sync of
productivity data, home-screen widgets, Facebook/Instagram login, recurring
tasks, iOS.
