package com.adheesh.gid.feature.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.MainActivity
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.capture.ui.QuickCaptureActivity
import com.adheesh.gid.feature.exams.domain.Countdown

// ── Home-screen widget (SPEC §2.11) ──────────────────────────────────────────
// Glance turns these Compose-style functions into the RemoteViews a launcher
// can draw. Small: Camera, Paste and the Inbox count. Wider: Photo and Inbox
// too, plus the next exam with its countdown. While it is on screen it
// follows the database live; refresh() wakes it after changes made elsewhere.
class GIDWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as GIDApplication).container
        val firstCount = container.captureRepository.inboxCount()
        val firstExams = container.examRepository.getUpcoming()
        provideContent {
            val inbox by container.captureRepository.observeInboxCount().collectAsState(initial = firstCount)
            val exams by container.examRepository.observeUpcoming().collectAsState(initial = firstExams)
            val next = exams.firstOrNull()
            val subjectName by produceState<String?>(null, next?.subjectId) {
                value = next?.let { container.subjectRepository.get(it.subjectId)?.name }
            }
            Content(inbox, next, subjectName)
        }
    }

    @Composable
    private fun Content(inbox: Int, next: ExamEntity?, subjectName: String?) {
        val context = LocalContext.current
        val wide = LocalSize.current.width >= WIDE.width
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GIDSurface)
                .cornerRadius(20.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                IconButton(R.drawable.ic_widget_camera, context.getString(R.string.capture_camera), capture(context, QuickCaptureActivity.MODE_CAMERA))
                Spacer(GlanceModifier.width(8.dp))
                if (wide) {
                    IconButton(R.drawable.ic_widget_photo, context.getString(R.string.capture_photo), capture(context, QuickCaptureActivity.MODE_PHOTO))
                    Spacer(GlanceModifier.width(8.dp))
                }
                IconButton(R.drawable.ic_widget_paste, context.getString(R.string.capture_paste), capture(context, QuickCaptureActivity.MODE_PASTE))
                Spacer(GlanceModifier.width(8.dp))
                InboxButton(context, inbox, wide)
            }
            if (wide) {
                Spacer(GlanceModifier.height(10.dp))
                NextUp(context, next, subjectName)
            }
        }
    }

    @Composable
    private fun IconButton(@DrawableRes icon: Int, label: String, onClick: Action) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier.size(44.dp).background(GIDSurfaceHigh).cornerRadius(14.dp).clickable(onClick),
        ) {
            Image(
                provider           = ImageProvider(icon),
                contentDescription = label,
                colorFilter        = ColorFilter.tint(ColorProvider(GIDCyan)),
                modifier           = GlanceModifier.size(22.dp),
            )
        }
    }

    /** The Inbox icon with its count; opens Library. */
    @Composable
    private fun InboxButton(context: Context, inbox: Int, wide: Boolean) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .height(44.dp)
                .background(GIDSurfaceHigh)
                .cornerRadius(14.dp)
                .padding(horizontal = 10.dp)
                .clickable(actionStartActivity(openApp(context, Notifications.OPEN_LIBRARY))),
        ) {
            Image(
                provider           = ImageProvider(R.drawable.ic_widget_inbox),
                contentDescription = context.getString(R.string.library_inbox),
                colorFilter        = ColorFilter.tint(ColorProvider(if (inbox > 0) GIDCoral else GIDMutedText)),
                modifier           = GlanceModifier.size(20.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                if (wide && inbox > 0) context.resources.getQuantityString(R.plurals.library_unsorted_chip, inbox, inbox) else inbox.toString(),
                style = TextStyle(color = ColorProvider(if (inbox > 0) GIDCoral else GIDMutedText), fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
        }
    }

    /** "Next: DBMS · Mid-sem · in 4 days", or that nothing is due. */
    @Composable
    private fun NextUp(context: Context, next: ExamEntity?, subjectName: String?) {
        val line = if (next == null) context.getString(R.string.home_nothing_due) else {
            val title = next.title.ifBlank { context.getString(kindLabel(ExamKind.of(next.kind))) }
            val countdown = countdownText(context, Countdown.of(next.dateEpochDay, today().toEpochDay()))
            context.getString(R.string.widget_next, listOfNotNull(subjectName, title, countdown).joinToString(" · "))
        }
        val open = if (next == null) openApp(context, null) else openApp(context, Notifications.OPEN_EXAM).putExtra(Notifications.EXTRA_EXAM_ID, next.id)
        Text(
            line,
            maxLines = 1,
            style    = TextStyle(color = ColorProvider(if (next == null) GIDMutedText else GIDText), fontSize = 14.sp),
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp).clickable(actionStartActivity(open)),
        )
    }

    companion object {
        private val SMALL = DpSize(110.dp, 48.dp)
        private val WIDE  = DpSize(250.dp, 100.dp)

        /** Redraws every GID widget on the home screen. Never fails the caller. */
        suspend fun refresh(context: Context) {
            runCatching { GIDWidget().updateAll(context) }
        }

        private fun capture(context: Context, mode: String): Action =
            actionStartActivity(QuickCaptureActivity.intent(context, mode))

        private fun openApp(context: Context, open: String?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { if (open != null) putExtra(Notifications.EXTRA_OPEN, open) }

        private fun kindLabel(kind: ExamKind): Int = when (kind) {
            ExamKind.EXAM       -> R.string.exam_kind_exam
            ExamKind.TEST       -> R.string.exam_kind_test
            ExamKind.ASSIGNMENT -> R.string.exam_kind_assignment
            ExamKind.PROJECT    -> R.string.exam_kind_project
        }

        private fun countdownText(context: Context, c: Countdown): String {
            val res = context.resources
            return when (c.unit) {
                Countdown.Unit.PAST     -> res.getQuantityString(R.plurals.countdown_past, c.value.toInt(), c.value.toInt())
                Countdown.Unit.TODAY    -> context.getString(R.string.countdown_today)
                Countdown.Unit.TOMORROW -> context.getString(R.string.countdown_tomorrow)
                Countdown.Unit.DAYS     -> res.getQuantityString(R.plurals.countdown_days, c.value.toInt(), c.value.toInt())
                Countdown.Unit.WEEKS    -> res.getQuantityString(R.plurals.countdown_weeks, c.value.toInt(), c.value.toInt())
            }
        }
    }
}

/** The launcher talks to this receiver; Glance does the rest. */
class GIDWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GIDWidget()
}

