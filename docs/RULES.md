PROJECT RULES: GID (Android)
1. Read docs/SPEC.md before every task. It is the source of truth. If the
spec is unclear or conflicts with the code, ask me. Do not guess.
2. Do only what the current prompt asks. No extra features, no unrelated
refactors.
3. Stack: Kotlin, Jetpack Compose + Material 3, MVVM (ViewModel + StateFlow),
Navigation Compose, Room, DataStore, Coroutines/Flow, supabase-kt (auth and
profile only). Single Gradle module, version catalog (libs.versions.toml).
Use current stable versions and check the official docs rather than relying
on memory. No Hilt in V1: use a simple AppContainer for manual dependency
injection.
4. Layers: Screen -> ViewModel -> Repository -> DAO or remote source. UI code
never calls Room or Supabase directly. Business logic (score, sleep math,
session rules) lives in plain Kotlin classes with unit tests.
5. Data: every record has a UUID string id plus createdAt and updatedAt
(epoch millis). Use java.time. LocalDate is stored as epochDay, clock times
as minutes from midnight.
6. Privacy: tasks, sleep, alarms, timetable and history NEVER go over the
network. Supabase stores only the profile row defined in the spec. No
analytics or crash-reporting SDKs.
7. Secrets: read from local.properties through BuildConfig. Never hard-code
or commit them. Never use the Supabase service_role key.
8. Design: use only the tokens in spec section 5 (colors, type, shapes). No
hard-coded colors or font sizes inside screens.
9. Alarms and notifications: use AlarmManager.setAlarmClock for alarms,
request permissions properly, and reschedule after reboot, time change,
timezone change and app update. Follow the current Android documentation.
10. After each task: build with ./gradlew assembleDebug, run unit tests, fix
every error and warning you introduced, then tell me in plain language what
changed and exactly how to test it on my phone.
11. I am learning Android. Keep code readable, name things clearly, and add
short comments explaining why, not what. Explain any new concept in two
sentences in your summary.
12. Do not run git commit. Suggest a commit message at the end.