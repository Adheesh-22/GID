# GID — Features

Everything the app does today, screen by screen, in plain language.
Update this file whenever a feature is added, changed or removed.

**Privacy in one line:** everything below stays on the phone. The only thing that goes
online is your sign-up profile (name, date of birth, email, phone, institution) in Supabase.
There is no AI and no analytics.

---

## Sign in (Auth)
- Sign in or sign up with email and password, or with Google.
- Forgot password sends a reset email; the link opens the app to set a new password.
- You stay signed in, even offline. A profile saved offline is uploaded on the next launch.

## Home
- **Greeting:** "Hello, <name>" with your profile photo beside it (tap the photo → Settings).
- **Personalise** (tap the greeting):
  - the name shown on Home (it changes only the greeting, not your profile);
  - its font (GID, DM Sans, Serif, Mono, Script, Light) and colour;
  - a background photo, blurred on the phone, with a Dim slider. It shows only on Home,
    Calendar, Library and History. Text over it turns dark on a light photo and light on a
    dark one.
- **Two-week calendar strip:** headed by the month's name; this week and next, with task
  dots and holidays. Tap a date → Calendar on that date.
- **Inbox row:** how many captures still need sorting, plus a quick-capture button.
- **Next up:** the next exam or assignment with its countdown, and "N% syllabus" when its
  subject has a syllabus. Tap → Exams.
- **Attendance today:** today's classes that have started, each with Present / Absent /
  Cancelled buttons. "All subjects" → Attendance.
- **Alarm card:** your next alarm (tap → Alarm popup).
- **Timetable card:** your next classes (tap → Timetable popup).
- **Tasks card:** today's tasks; tick them off, add new ones, or open one for details.

## Calendar
- A month grid. Today is ringed and the selected day is filled.
- Days with tasks get a dot, exam days a purple dot, and holidays are green.
- Long-press a date to mark it as a holiday or unmark it.
- The selected day lists its exams and tasks.
- **Task actions:** add, edit, delete, set percent done, change priority, and Move to
  tomorrow (this moves the task and keeps its progress).

## Library (Capture Inbox)
- **Capture from anywhere:**
  - the share sheet in any app (photos, PDFs, Word and PowerPoint files, links, text);
  - the + button: Camera, Photo, Paste;
  - the home-screen widget or the Quick Settings tile.
- **Subject suggestion:** GID suggests a subject from the class that was on when the
  photo was taken.
- **Inbox:**
  - shows unsorted items, with a delete button on each;
  - long-press to select several, then Sort or Delete them all at once.
- **Sort sheet:** pick a category, a subject, a heading, a title and a note. A Deadline can
  also create a Task, an Exam or an Assignment.
- **Categories:** Exam question, Resource, Note, Deadline, Idea and Notice to start with.
  - Add, rename or remove them (tune icon on a subject page, or "New category" in the Sort
    sheet).
  - Removing one keeps its items under All. Deadline can only be renamed, because it holds
    the due date.
- **Subjects:**
  - add, archive, restore and delete;
  - each subject's page has tabs: **All** first, then one per category with its count.
    Swipe or tap between them;
  - headings: add, rename, reorder, delete.
- **Search:** searches titles, notes, links, subjects and headings.
- **Files and duplicates:**
  - files are copied into GID (they keep working if the original is moved or deleted);
  - capturing the same file again reuses the copy GID already has.
- **Item page:** opens the photo, PDF, Word or PowerPoint file (in another app), or link. From there you can sort it again or delete it.

## Exams and assignments
- **Exams page:** upcoming exams, tests, assignments and projects, soonest first. Past
  and done ones are folded away below.
- **Countdown chips:** coral for today or tomorrow, cyan within a week.
- **Editor:** kind, subject, title, date, time, room, notes, and which syllabus units the
  exam covers.
- **Exam page:**
  - Mark done or Mark submitted (with undo), Edit and Delete;
  - the syllabus topics still to do, which you can tick off right there;
  - the subject's saved Library items.
- **Reminders:**
  - exams and tests: 19:00 a week before (with how many items you've saved for that
    subject), 19:00 the day before, and 07:00 on the day (or 2 hours before an early
    start);
  - assignments and projects: the day before and the morning of.

## Attendance
- **Notification after each class:** "Were you in DBMS?" with Present / Absent /
  Cancelled buttons. Tapping one saves the mark and shows your new percent.
- **On Home:** mark today's classes; tap the selected button again to undo it.
- **Attendance page:**
  - one card per subject: the percent, a bar (coral below the target, mustard close to
    it, green when safe), and advice such as "You can miss 2 more" or "Attend the next 3
    to reach 75%";
  - tap a subject to add the classes from before you used GID ("Before GID"), or set a
    target just for that subject.
- **Target:** 75% by default, changeable in Settings. Cancelled classes never count.
- **Subject pages** show their attendance card.

## Syllabus
- **Structure:** each subject has units, and each unit has topics you tick off. You see
  a percent per unit and per subject.
- **Syllabus page** (from a subject page or an exam page):
  - add, rename, reorder and delete units and topics;
  - **Paste list:** paste a whole syllabus. Lines starting with Unit, Module or Chapter
    (or ending with ":") become units; every other line becomes a topic. A preview shows
    how it will be split before you save.
- **Exams:** an exam can cover chosen units. Its page and Home's Next up then show
  progress for just those units.
- **Bar colours:** green when on track; mustard or coral when the exam is close and
  under 70% is done.

## Home-screen widget and Quick Settings tile
- **Widget** (resizable):
  - small: Camera, Paste and the Inbox count;
  - wider: also Photo, and the next exam with its countdown.
- **Tile "GID capture":** opens a small Camera / Photo / Paste menu from the pull-down
  shade. Settings → Reliability has an "Add tile" button (Android 13+).

## History
- Week by week: productivity bars, sleep bars and a weekly score with the change from last
  week.
- Tap a bar for that day's details.

## Settings
- **Profile:** name, date of birth, phone, institution and photo. A new photo opens a
  preview where you zoom and drag it inside a circle before saving. It stays on the phone.
- **Evening check-in time:** the 21:00 prompt to update today's progress and sort the
  Inbox.
- **Attendance target**, from 50% to 100%.
- **Backup:**
  - export or import one .zip (database, Library files, profile photo);
  - shows how much space Library files use;
  - your Home look (name, font, colour, background) isn't included.
- **Reliability checklist:** exact alarms, notifications, battery optimisation, and the
  capture tile.
- **How to use GID:** a guide to every feature.
- **Contact us:** opens your mail app addressed to the developer.
- Sign out.

## Alarm popup
- Plain alarms: time, label and repeat days, with an **All days** button (on for a new
  alarm).
- **Test alarm:** rings in 30 seconds so you can check this phone really rings, with an
  **Alarm log** of what happened.
- Each alarm has its own ringtone (from the phone's picker) and snooze length (1, 2, 5, 10,
  15 or 30 minutes; 5 by default).
- Timers with an activity type, and the activity-types manager. A finished timer rings until
  you stop it.
- Ringing:
  - a full-screen alarm over the lock screen;
  - **Draw a pattern to stop or snooze** (on by default, per alarm): a random 5-dot pattern
    on a 3×3 grid, different every ring, has to be drawn before Dismiss and Snooze appear.
    The notification has no buttons meanwhile;
  - with the option off, both buttons show at once.

## Timetable popup
- Your weekly classes (subject, time, room).
- **Paste from VTOP:** paste VIT VTOP's whole Time Table page. GID builds the timetable with
  the page's exact times, rooms and course names, and previews it first. Neighbouring lab
  slots become one class. Past attendance is kept.
- Edit mode to add, change or delete classes.
- A reminder N minutes before each class (10 by default), skipped on holidays.

## Motion
- Tabs slide sideways in bottom-bar order.
- Pages (Exams, Attendance, Syllabus, Help, subject and capture pages) and the Alarm and
  Timetable popups slide up from the bottom, and slide back down on Back.

## Notifications
- Class reminders, exam reminders, after-class attendance check-ins, the evening
  check-in, timers and the alarm.
- Everything is re-planned after a reboot, a clock or time-zone change, an app update,
  and once a day.
