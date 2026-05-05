package com.example.clawworker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.util.DisplayMetrics;
import android.os.Environment;
import android.view.Display;
import com.example.clawworker.action.ActionExecutor;
import com.example.clawworker.action.ActionResult;
import com.example.clawworker.parser.UiElement;
import com.example.clawworker.parser.UiParseResult;
import com.example.clawworker.parser.UiTreeManager;
import com.example.clawworker.parser.UiTreeParser;
import com.example.clawworker.tools.CalendarReminderTool;
import com.example.clawworker.tools.ClickTool;
import com.example.clawworker.tools.ClipboardTool;
import com.example.clawworker.tools.CurrentTimeTool;
import com.example.clawworker.tools.InputInjector;
import com.example.clawworker.tools.InputTool;
import com.example.clawworker.tools.KeyEventTool;
import com.example.clawworker.tools.LongPressTool;
import com.example.clawworker.tools.OpenAppTool;
import com.example.clawworker.tools.ScreenCaptureTool;
import com.example.clawworker.tools.SetAlarmTool;
import com.example.clawworker.tools.SystemStatusTool;
import com.example.clawworker.tools.SwipeTool;
import com.example.clawworker.tools.WaitTool;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ToolDispatcher {
    private static final String DEFAULT_SCREENSHOT_PREFIX = "clawworker_";
    private static final String DEFAULT_SCREENSHOT_DIR = "/sdcard/ClawWorkerScreens";
    private static final int QUERY_UI_MAX_ATTEMPTS = 3;
    private static final long QUERY_UI_RETRY_DELAY_MS = 120L;
    private final Context context;
    private final Gson gson = new Gson();
    private final UiTreeManager uiTreeManager;
    private final ScreenCaptureTool screenCaptureTool;
    private final SystemStatusTool systemStatusTool;
    private final ActionExecutor actionExecutor;
    private final DisplayManager displayManager;

    public ToolDispatcher(Context context) {
        this.context = context;
        InputManager inputManager = context == null ? null : context.getSystemService(InputManager.class);
        displayManager = context == null ? null : context.getSystemService(DisplayManager.class);
        uiTreeManager = new UiTreeManager(context);
        screenCaptureTool = new ScreenCaptureTool(displayManager, "ClawWorkerScreen");
        systemStatusTool = new SystemStatusTool(context);
        InputInjector injector = new InputInjector(inputManager,
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
        ClickTool clickTool = new ClickTool(injector);
        InputTool inputTool = new InputTool(injector);
        SwipeTool swipeTool = new SwipeTool(injector);
        LongPressTool longPressTool = new LongPressTool(injector);
        OpenAppTool openAppTool = new OpenAppTool(context);
        KeyEventTool keyEventTool = new KeyEventTool(injector);
        SetAlarmTool setAlarmTool = new SetAlarmTool(context);
        CurrentTimeTool currentTimeTool = new CurrentTimeTool();
        CalendarReminderTool calendarReminderTool = new CalendarReminderTool(context);
        ClipboardTool clipboardTool = new ClipboardTool(context, injector);
        WaitTool waitTool = new WaitTool();
        actionExecutor = new ActionExecutor(clickTool, inputTool, swipeTool, longPressTool,
                openAppTool, keyEventTool, setAlarmTool, currentTimeTool, calendarReminderTool,
                clipboardTool, waitTool);
    }

    public ClawResponse handleRequest(String json) {
        ClawRequest request;
        try {
            request = gson.fromJson(json, ClawRequest.class);
        } catch (JsonSyntaxException e) {
            return errorResponse("", "invalid_json");
        }
        if (request == null || request.action == null || request.action.trim().isEmpty()) {
            return errorResponse(request == null ? "" : request.id, "missing_action");
        }
        String action = request.action.trim().toLowerCase(Locale.US);
        Map<String, Object> params = request.params == null ? new HashMap<>() : request.params;
        switch (action) {
            case "click":
                return handleClick(request, params);
            case "input":
                return handleInput(request, params);
            case "swipe":
                return handleSwipe(request, params);
            case "swipe_direction":
                return handleSwipeDirection(request, params);
            case "long_press":
                return handleLongPress(request, params);
            case "open_app":
                return handleOpenApp(request, params);
            case "key_event":
                return handleKeyEvent(request, params);
            case "set_alarm":
                return handleSetAlarm(request, params);
            case "current_time":
                return handleCurrentTime(request);
            case "calendar_reminder":
                return handleCalendarReminder(request, params);
            case "wait":
                return handleWait(request, params);
            case "system_status":
                return handleSystemStatus(request);
            case "screen_capture":
                return handleScreenCapture(request, params);
            case "query_ui":
                return handleQueryUi(request, params);
            case "clipboard_set":
                return handleClipboardSet(request, params);
            case "clipboard_paste":
                return handleClipboardPaste(request, params);
            default:
                return errorResponse(request.id, "unknown_action");
        }
    }

    private ClawResponse handleClick(ClawRequest request, Map<String, Object> params) {
        if (!hasKey(params, "x") || !hasKey(params, "y")) {
            return errorResponse(request == null ? "" : request.id, "missing_coordinates");
        }
        float x = getFloat(params, "x", -1f);
        float y = getFloat(params, "y", -1f);
        ActionResult actionResult = actionExecutor.clickAt(x, y);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleInput(ClawRequest request, Map<String, Object> params) {
        String text = getString(params, "text", "");
        ActionResult actionResult;
        if (hasKey(params, "x") && hasKey(params, "y")) {
            float x = getFloat(params, "x", -1f);
            float y = getFloat(params, "y", -1f);
            actionResult = actionExecutor.inputAt(x, y, text);
        } else {
            UiParseResult result = buildUiParseResult(getBoolean(params, "vision_enabled", false), null);
            int index = getInt(params, "index", -1);
            actionResult = actionExecutor.input(result, index, text);
        }
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleSwipe(ClawRequest request, Map<String, Object> params) {
        if (!hasKey(params, "start_x") || !hasKey(params, "start_y")
                || !hasKey(params, "end_x") || !hasKey(params, "end_y")) {
            return errorResponse(request == null ? "" : request.id, "missing_coordinates");
        }
        float startX = getFloat(params, "start_x", -1f);
        float startY = getFloat(params, "start_y", -1f);
        float endX = getFloat(params, "end_x", -1f);
        float endY = getFloat(params, "end_y", -1f);
        ActionResult actionResult = actionExecutor.swipeAt(startX, startY, endX, endY);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleSwipeDirection(ClawRequest request, Map<String, Object> params) {
        UiParseResult result = buildUiParseResult(getBoolean(params, "vision_enabled", false), null);
        String direction = getString(params, "direction", "");
        ActionResult actionResult = actionExecutor.swipeDirection(result, direction);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleLongPress(ClawRequest request, Map<String, Object> params) {
        long durationMs = getLong(params, "duration_ms", 0L);
        if (!hasKey(params, "x") || !hasKey(params, "y")) {
            return errorResponse(request == null ? "" : request.id, "missing_coordinates");
        }
        float x = getFloat(params, "x", -1f);
        float y = getFloat(params, "y", -1f);
        ActionResult actionResult = actionExecutor.longPressAt(x, y, durationMs);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleOpenApp(ClawRequest request, Map<String, Object> params) {
        String name = getString(params, "name", "");
        ActionResult actionResult = actionExecutor.openAppByName(name);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleKeyEvent(ClawRequest request, Map<String, Object> params) {
        int keyCode = getInt(params, "key_code", -1);
        ActionResult actionResult = actionExecutor.pressKey(keyCode);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleSetAlarm(ClawRequest request, Map<String, Object> params) {
        int hour = getInt(params, "hour", -1);
        int minute = getInt(params, "minute", -1);
        String message = getString(params, "message", "");
        List<Integer> days = getIntegerList(params, "days_of_week");
        boolean repeat = getBoolean(params, "repeat", false);
        ActionResult actionResult = actionExecutor.setAlarm(hour, minute, message, days, repeat);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleCurrentTime(ClawRequest request) {
        ActionResult actionResult = actionExecutor.getCurrentTime();
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleCalendarReminder(ClawRequest request, Map<String, Object> params) {
        long start = getLong(params, "start_epoch_ms", -1L);
        long end = getLong(params, "end_epoch_ms", -1L);
        String title = getString(params, "title", "");
        String description = getString(params, "description", "");
        int reminder = getInt(params, "reminder_minutes", -1);
        ActionResult actionResult = actionExecutor.createCalendarReminder(start, end, title,
                description, reminder);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleWait(ClawRequest request, Map<String, Object> params) {
        long ms = getLong(params, "milliseconds", 0L);
        ActionResult actionResult = actionExecutor.waitMillis(ms);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleSystemStatus(ClawRequest request) {
        ClawResponse response = baseResponse(request);
        response.status = "success";
        response.data.put("system_state", systemStatusTool.buildSystemState());
        return response;
    }

    private ClawResponse handleScreenCapture(ClawRequest request, Map<String, Object> params) {
        ClawResponse response = baseResponse(request);
        Bitmap bitmap = screenCaptureTool.capture();
        if (bitmap == null) {
            response.status = "error";
            response.error_msg = "capture_failed";
            return response;
        }
        String path = getString(params, "path", "");
        String saved = saveBitmap(bitmap, path);
        bitmap.recycle();
        if (saved == null) {
            response.status = "error";
            response.error_msg = "save_failed";
            return response;
        }
        response.status = "success";
        response.data.put("screenshot_path", saved);
        return response;
    }

    private ClawResponse handleQueryUi(ClawRequest request, Map<String, Object> params) {
        boolean includeScreenshot = getBoolean(params, "include_screenshot", false);
        boolean visionEnabled = getBoolean(params, "vision_enabled", includeScreenshot);
        Bitmap screenshot = includeScreenshot ? screenCaptureTool.capture() : null;
        UiParseResult result = null;
        int attempts = 0;
        while (attempts < QUERY_UI_MAX_ATTEMPTS) {
            attempts++;
            result = buildUiParseResult(visionEnabled, screenshot);
            if (hasUiElements(result)) {
                break;
            }
            if (attempts < QUERY_UI_MAX_ATTEMPTS) {
                try {
                    Thread.sleep(QUERY_UI_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        ClawResponse response = baseResponse(request);
        if (!hasUiElements(result)) {
            response.status = "error";
            response.error_msg = "ui_tree_empty_after_retries";
            response.data.put("ui_dump", "");
            response.data.put("package_name", result == null ? "" : result.getPackageName());
            response.data.put("element_count", 0);
            response.data.put("ui_points", new ArrayList<>());
            response.data.put("retry_attempts", attempts);
            if (includeScreenshot && screenshot != null) {
                String path = getString(params, "screenshot_path", "");
                String saved = saveBitmap(screenshot, path);
                response.data.put("screenshot_path", saved == null ? "" : saved);
            }
            if (screenshot != null && !screenshot.isRecycled()) {
                screenshot.recycle();
            }
            return response;
        }
        response.status = "success";
        response.data.put("ui_dump", result == null ? "" : result.getTextDump());
        response.data.put("package_name", result == null ? "" : result.getPackageName());
        response.data.put("element_count", result == null || result.getElements() == null
                ? 0 : result.getElements().size());
        response.data.put("ui_points", buildUiPoints(result));
        response.data.put("retry_attempts", attempts);
        if (includeScreenshot && screenshot != null) {
            String path = getString(params, "screenshot_path", "");
            String saved = saveBitmap(screenshot, path);
            response.data.put("screenshot_path", saved == null ? "" : saved);
        }
        if (screenshot != null && !screenshot.isRecycled()) {
            screenshot.recycle();
        }
        return response;
    }

    private ClawResponse handleClipboardSet(ClawRequest request, Map<String, Object> params) {
        String text = getString(params, "text", "");
        ActionResult actionResult = actionExecutor.setClipboard(text);
        return buildActionResponse(request, actionResult);
    }

    private ClawResponse handleClipboardPaste(ClawRequest request, Map<String, Object> params) {
        if (hasKey(params, "x") && hasKey(params, "y")) {
            float x = getFloat(params, "x", -1f);
            float y = getFloat(params, "y", -1f);
            ActionResult actionResult = actionExecutor.clipboardPasteAt(x, y);
            return buildActionResponse(request, actionResult);
        }
        ActionResult actionResult = actionExecutor.clipboardPaste();
        return buildActionResponse(request, actionResult);
    }

    private boolean hasUiElements(UiParseResult result) {
        return result != null && result.getElements() != null && !result.getElements().isEmpty();
    }

    private UiParseResult buildUiParseResult(boolean visionEnabled, Bitmap screenshot) {
        if (uiTreeManager == null || !uiTreeManager.init()) {
            return new UiParseResult(new ArrayList<>(), "", null, "");
        }
        int[] size = getDisplaySize();
        int width = size[0];
        int height = size[1];
        UiParseResult result;
        android.view.accessibility.AccessibilityNodeInfo root = uiTreeManager.getRootNode();
        try {
            if (screenshot != null) {
                width = screenshot.getWidth();
                height = screenshot.getHeight();
            }
            result = UiTreeParser.parse(root, width, height, visionEnabled, visionEnabled ? screenshot : null);
        } finally {
            if (root != null) {
                root.recycle();
            }
        }
        return result;
    }

    private int[] getDisplaySize() {
        Display display = displayManager == null ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return new int[] {1080, 1920};
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        return new int[] {metrics.widthPixels, metrics.heightPixels};
    }

    private ClawResponse buildActionResponse(ClawRequest request, ActionResult result) {
        ClawResponse response = baseResponse(request);
        if (result == null) {
            response.status = "error";
            response.error_msg = "action_failed";
            return response;
        }
        response.status = result.isSuccess() ? "success" : "error";
        response.data.put("action_result", toMap(result));
        if (!result.isSuccess()) {
            response.error_msg = result.getMessage();
        }
        return response;
    }

    private Map<String, Object> toMap(ActionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("message", result.getMessage());
        map.put("action_type", result.getActionType());
        map.put("start_x", result.getStartX());
        map.put("start_y", result.getStartY());
        map.put("end_x", result.getEndX());
        map.put("end_y", result.getEndY());
        map.put("target_index", result.getTargetElementIndex());
        return map;
    }

    private List<Map<String, Object>> buildUiPoints(UiParseResult result) {
        List<Map<String, Object>> points = new ArrayList<>();
        if (result == null || result.getElements() == null) {
            return points;
        }
        for (UiElement element : result.getElements()) {
            if (element == null) {
                continue;
            }
            Rect bounds = element.getBounds();
            Map<String, Object> item = new HashMap<>();
            item.put("index", element.getIndex());
            item.put("center_x", bounds.exactCenterX());
            item.put("center_y", bounds.exactCenterY());
            item.put("left", bounds.left);
            item.put("top", bounds.top);
            item.put("right", bounds.right);
            item.put("bottom", bounds.bottom);
            item.put("class_name", element.getClassName());
            item.put("resource_id", element.getResourceId());
            item.put("text", element.getText());
            points.add(item);
        }
        return points;
    }

    private String saveBitmap(Bitmap bitmap, String path) {
        String outputPath = path == null || path.trim().isEmpty()
                ? buildDefaultScreenshotPath()
                : path.trim();
        // First attempt: requested or default path
        if (tryWriteBitmap(bitmap, outputPath)) {
            return outputPath;
        }
        // Fallback: external shared directory
        File ext = Environment.getExternalStorageDirectory();
        String base = (ext != null ? ext.getAbsolutePath() : "/sdcard") + "/ClawWorkerScreens";
        String fallback = base + "/" + DEFAULT_SCREENSHOT_PREFIX + System.currentTimeMillis() + ".png";
        if (!fallback.equals(outputPath) && tryWriteBitmap(bitmap, fallback)) {
            return fallback;
        }
        return null;
    }

    private boolean tryWriteBitmap(Bitmap bitmap, String outputPath) {
        File file = new File(outputPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String buildDefaultScreenshotPath() {
        long now = System.currentTimeMillis();
        return DEFAULT_SCREENSHOT_DIR + "/" + DEFAULT_SCREENSHOT_PREFIX + now + ".png";
    }

    private ClawResponse baseResponse(ClawRequest request) {
        ClawResponse response = new ClawResponse();
        response.id = request == null || request.id == null ? "" : request.id;
        response.data = new HashMap<>();
        response.error_msg = "";
        return response;
    }

    private ClawResponse errorResponse(String id, String error) {
        ClawResponse response = new ClawResponse();
        response.id = id == null ? "" : id;
        response.status = "error";
        response.error_msg = error == null ? "" : error;
        response.data = new HashMap<>();
        return response;
    }

    private int getInt(Map<String, Object> params, String key, int def) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private float getFloat(Map<String, Object> params, String key, float def) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat(((String) value).trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private long getLong(Map<String, Object> params, String key, long def) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private boolean getBoolean(Map<String, Object> params, String key, boolean def) {
        Object value = params.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean(((String) value).trim());
        }
        return def;
    }

    private String getString(Map<String, Object> params, String key, String def) {
        Object value = params.get(key);
        if (value == null) {
            return def;
        }
        return String.valueOf(value);
    }

    private boolean hasKey(Map<String, Object> params, String key) {
        return params != null && params.containsKey(key) && params.get(key) != null;
    }

    private List<Integer> getIntegerList(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) value;
        List<Integer> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number) {
                result.add(((Number) item).intValue());
            } else if (item instanceof String) {
                try {
                    result.add(Integer.parseInt(((String) item).trim()));
                } catch (NumberFormatException e) {
                    return result;
                }
            }
        }
        return result;
    }
}
