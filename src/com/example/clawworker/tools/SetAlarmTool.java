package com.example.clawworker.tools;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import com.example.clawworker.action.ActionResult;
import java.util.ArrayList;
import java.util.List;

public class SetAlarmTool {
    private final Context context;

    public SetAlarmTool(Context context) {
        this.context = context;
    }

    public ActionResult setAlarm(int hour, int minutes, String message, List<Integer> daysOfWeek,
                                 boolean repeat) {
        if (context == null) {
            return new ActionResult(false, "context not ready", "set_alarm",
                    0, 0, 0, 0, -1);
        }
        if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
            return new ActionResult(false, "invalid time", "set_alarm",
                    0, 0, 0, 0, -1);
        }
        if (repeat && (daysOfWeek == null || daysOfWeek.isEmpty())) {
            return new ActionResult(false, "repeat days required", "set_alarm",
                    0, 0, 0, 0, -1);
        }
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        if (message != null && !message.trim().isEmpty()) {
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, message.trim());
        }
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            intent.putExtra(AlarmClock.EXTRA_DAYS, new ArrayList<>(daysOfWeek));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return new ActionResult(true, "alarm ui shown", "set_alarm",
                    0, 0, 0, 0, -1);
        } catch (ActivityNotFoundException e) {
            return new ActionResult(false, "alarm app not found", "set_alarm",
                    0, 0, 0, 0, -1);
        } catch (SecurityException e) {
            return new ActionResult(false, "set alarm denied", "set_alarm",
                    0, 0, 0, 0, -1);
        }
    }
}
