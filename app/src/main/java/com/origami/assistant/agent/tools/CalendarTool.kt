package com.origami.assistant.agent.tools

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarTool @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseTool() {

    override val definition = Tool(
        name = "calendar",
        description = "Read or create calendar events on the device calendar.",
        parameters = listOf(
            ToolParameter("action", "string", "Action: 'list_today', 'list_range', 'create_event'", required = true),
            ToolParameter("title", "string", "Event title (for create_event)", required = false),
            ToolParameter("start_time", "string", "ISO 8601 start time (for create_event/list_range)", required = false),
            ToolParameter("end_time", "string", "ISO 8601 end time (for create_event/list_range)", required = false),
            ToolParameter("description", "string", "Event description (for create_event)", required = false)
        )
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    override suspend fun execute(params: Map<String, Any>): String {
        val action = param(params, "action")
        val cr = context.contentResolver

        return try {
            when (action) {
                "list_today" -> listEvents(cr, todayStart(), todayEnd())
                "list_range" -> {
                    val start = dateFormat.parse(param(params, "start_time"))?.time ?: todayStart()
                    val end = dateFormat.parse(param(params, "end_time"))?.time ?: todayEnd()
                    listEvents(cr, start, end)
                }
                "create_event" -> createEvent(cr, params)
                else -> "Unknown calendar action: $action"
            }
        } catch (e: SecurityException) {
            "Calendar access denied: please grant calendar permissions in Settings."
        } catch (e: Exception) {
            "Calendar error: ${e.message}"
        }
    }

    private fun listEvents(cr: ContentResolver, start: Long, end: Long): String {
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION
        )
        val cursor: Cursor? = cr.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?",
            arrayOf(start.toString(), end.toString()),
            CalendarContract.Events.DTSTART + " ASC"
        )

        return buildString {
            var count = 0
            cursor?.use {
                while (it.moveToNext() && count < 20) {
                    val title = it.getString(0) ?: "Untitled"
                    val dtStart = it.getLong(1)
                    val dtEnd = it.getLong(2)
                    appendLine("• $title")
                    appendLine("  ${displayFormat.format(Date(dtStart))} → ${displayFormat.format(Date(dtEnd))}")
                    count++
                }
            }
            if (count == 0) appendLine("No events found in the specified range.")
        }
    }

    private fun createEvent(cr: ContentResolver, params: Map<String, Any>): String {
        val title = param(params, "title")
        val start = dateFormat.parse(param(params, "start_time"))?.time
            ?: return "Invalid start_time format. Use ISO 8601."
        val end = dateFormat.parse(param(params, "end_time"))?.time
            ?: return "Invalid end_time format. Use ISO 8601."
        val description = paramOrNull(params, "description") ?: ""

        // Find default calendar
        val calCursor = cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.IS_PRIMARY} = 1",
            null, null
        )
        val calendarId = calCursor?.use {
            if (it.moveToFirst()) it.getLong(0) else null
        } ?: return "No primary calendar found."

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri: Uri? = cr.insert(CalendarContract.Events.CONTENT_URI, values)
        return if (uri != null) {
            "Event created: \"$title\" on ${displayFormat.format(Date(start))}"
        } else {
            "Failed to create event."
        }
    }

    private fun todayStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun todayEnd(): Long = todayStart() + 86_400_000L - 1
}
