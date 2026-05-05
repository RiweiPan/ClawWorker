package com.example.clawworker.action;

import com.example.clawworker.parser.UiElement;
import com.example.clawworker.parser.UiParseResult;
import com.example.clawworker.tools.CalendarReminderTool;
import com.example.clawworker.tools.ClickTool;
import com.example.clawworker.tools.ClipboardTool;
import com.example.clawworker.tools.CurrentTimeTool;
import com.example.clawworker.tools.InputTool;
import com.example.clawworker.tools.KeyEventTool;
import com.example.clawworker.tools.LongPressTool;
import com.example.clawworker.tools.OpenAppTool;
import com.example.clawworker.tools.SetAlarmTool;
import com.example.clawworker.tools.SwipeTool;
import com.example.clawworker.tools.WaitTool;
import java.util.List;

public class ActionExecutor {
    private final ClickTool clickTool;
    private final InputTool inputTool;
    private final SwipeTool swipeTool;
    private final LongPressTool longPressTool;
    private final OpenAppTool openAppTool;
    private final KeyEventTool keyEventTool;
    private final SetAlarmTool setAlarmTool;
    private final CurrentTimeTool currentTimeTool;
    private final CalendarReminderTool calendarReminderTool;
    private final ClipboardTool clipboardTool;
    private final WaitTool waitTool;

    public ActionExecutor(ClickTool clickTool, InputTool inputTool, SwipeTool swipeTool,
                          LongPressTool longPressTool, OpenAppTool openAppTool,
                          KeyEventTool keyEventTool, SetAlarmTool setAlarmTool,
                          CurrentTimeTool currentTimeTool, CalendarReminderTool calendarReminderTool,
                          ClipboardTool clipboardTool, WaitTool waitTool) {
        this.clickTool = clickTool;
        this.inputTool = inputTool;
        this.swipeTool = swipeTool;
        this.longPressTool = longPressTool;
        this.openAppTool = openAppTool;
        this.keyEventTool = keyEventTool;
        this.setAlarmTool = setAlarmTool;
        this.currentTimeTool = currentTimeTool;
        this.calendarReminderTool = calendarReminderTool;
        this.clipboardTool = clipboardTool;
        this.waitTool = waitTool;
    }

    public ActionResult click(UiParseResult result, int index) {
        return clickTool.click(findElement(result, index), index);
    }

    public ActionResult clickAt(float x, float y) {
        return clickTool.clickAt(x, y);
    }

    public ActionResult input(UiParseResult result, int index, String text) {
        return inputTool.input(findElement(result, index), index, text);
    }

    public ActionResult inputAt(float x, float y, String text) {
        return inputTool.inputAt(x, y, text);
    }

    public ActionResult swipe(UiParseResult result, int startIndex, int endIndex) {
        UiElement start = findElement(result, startIndex);
        UiElement end = findElement(result, endIndex);
        return swipeTool.swipe(start, end, startIndex);
    }

    public ActionResult swipeAt(float startX, float startY, float endX, float endY) {
        return swipeTool.swipe(startX, startY, endX, endY);
    }

    public ActionResult swipeDirection(UiParseResult result, String direction) {
        return swipeTool.swipeDirection(result, direction);
    }

    public ActionResult longPress(UiParseResult result, int index, long durationMs) {
        return longPressTool.longPress(findElement(result, index), index, durationMs);
    }

    public ActionResult longPressAt(float x, float y, long durationMs) {
        return longPressTool.longPressAt(x, y, durationMs);
    }

    public ActionResult openAppByName(String name) {
        return openAppTool.openAppByName(name);
    }

    public ActionResult pressKey(int keyCode) {
        return keyEventTool.pressKey(keyCode);
    }

    public ActionResult setAlarm(int hour, int minutes, String message, List<Integer> daysOfWeek,
                                 boolean repeat) {
        return setAlarmTool.setAlarm(hour, minutes, message, daysOfWeek, repeat);
    }

    public ActionResult getCurrentTime() {
        return currentTimeTool.getCurrentTime();
    }

    public ActionResult createCalendarReminder(long startEpochMs, long endEpochMs, String title,
                                               String description, int reminderMinutes) {
        return calendarReminderTool.createReminder(startEpochMs, endEpochMs, title,
                description, reminderMinutes);
    }

    public ActionResult waitMillis(long milliseconds) {
        return waitTool.waitMillis(milliseconds);
    }

    public ActionResult setClipboard(String text) {
        return clipboardTool.setText(text);
    }

    public ActionResult clipboardPaste() {
        return clipboardTool.paste();
    }

    public ActionResult clipboardPasteAt(float x, float y) {
        return clipboardTool.pasteAt(x, y);
    }

    private UiElement findElement(UiParseResult result, int index) {
        if (result == null || result.getElements() == null) {
            return null;
        }
        for (UiElement e : result.getElements()) {
            if (e.getIndex() == index) {
                return e;
            }
        }
        return null;
    }
}
