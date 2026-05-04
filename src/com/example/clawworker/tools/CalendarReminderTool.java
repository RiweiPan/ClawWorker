package com.example.clawworker.tools;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import com.example.clawworker.action.ActionResult;
import java.time.ZoneId;

public class CalendarReminderTool {
    private final Context context;

    public CalendarReminderTool(Context context) {
        this.context = context;
    }

    public ActionResult createReminder(long startEpochMs, long endEpochMs, String title,
                                       String description, int reminderMinutes) {
        if (context == null) {
            return new ActionResult(false, "context not ready", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        if (startEpochMs <= 0) {
            return new ActionResult(false, "invalid start time", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        if (endEpochMs <= startEpochMs) {
            endEpochMs = startEpochMs + 30 * 60 * 1000L;
        }
        if (title == null || title.trim().isEmpty()) {
            return new ActionResult(false, "title required", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        if (!hasCalendarPermission()) {
            return new ActionResult(false, "calendar permission required", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        ContentResolver resolver = context.getContentResolver();
        long calendarId = resolveWritableCalendarId(resolver);
        if (calendarId <= 0) {
            return new ActionResult(false, "no writable calendar", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startEpochMs);
        values.put(CalendarContract.Events.DTEND, endEpochMs);
        values.put(CalendarContract.Events.TITLE, title.trim());
        if (description != null && !description.trim().isEmpty()) {
            values.put(CalendarContract.Events.DESCRIPTION, description.trim());
        }
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().getId());
        values.put(CalendarContract.Events.HAS_ALARM, reminderMinutes >= 0 ? 1 : 0);
        Uri eventUri;
        try {
            eventUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
        } catch (SecurityException e) {
            return new ActionResult(false, "calendar permission denied", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        if (eventUri == null) {
            return new ActionResult(false, "calendar insert failed", "calendar_reminder",
                    0, 0, 0, 0, -1);
        }
        long eventId = ContentUris.parseId(eventUri);
        if (reminderMinutes >= 0) {
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.MINUTES, reminderMinutes);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            try {
                resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
            } catch (SecurityException e) {
                return new ActionResult(false, "reminder permission denied", "calendar_reminder",
                        0, 0, 0, 0, -1);
            }
        }
        String message = "calendar event created id=" + eventId;
        return new ActionResult(true, message, "calendar_reminder", 0, 0, 0, 0, -1);
    }

    private long resolveWritableCalendarId(ContentResolver resolver) {
        Cursor cursor = null;
        try {
            String[] projection = new String[] {
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.IS_PRIMARY
            };
            String selection = CalendarContract.Calendars.VISIBLE + "=1";
            String orderBy = CalendarContract.Calendars.IS_PRIMARY + " DESC";
            cursor = resolver.query(CalendarContract.Calendars.CONTENT_URI, projection,
                    selection, null, orderBy);
            if (cursor == null) {
                return -1;
            }
            long fallbackId = -1;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                int access = cursor.getInt(1);
                int primary = cursor.getInt(2);
                if (fallbackId <= 0) {
                    fallbackId = id;
                }
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    if (primary == 1) {
                        return id;
                    }
                    if (fallbackId <= 0) {
                        fallbackId = id;
                    }
                }
            }
            return fallbackId;
        } catch (SecurityException e) {
            return -1;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean hasCalendarPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }
}
