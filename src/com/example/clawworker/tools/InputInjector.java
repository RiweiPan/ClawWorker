package com.example.clawworker.tools;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.IOException;

public class InputInjector {
    private final InputManager inputManager;
    private final int injectMode;

    public InputInjector(InputManager inputManager, int injectMode) {
        this.inputManager = inputManager;
        this.injectMode = injectMode;
    }

    public boolean injectClick(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
        );
        downEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        long upTime = SystemClock.uptimeMillis();
        MotionEvent upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );
        upEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean downResult = injectInputEvent(downEvent);
        boolean upResult = injectInputEvent(upEvent);
        downEvent.recycle();
        upEvent.recycle();
        return downResult && upResult;
    }

    public boolean injectSwipe(float startX, float startY, float endX, float endY) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                startX,
                startY,
                0
        );
        downEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean result = injectInputEvent(downEvent);
        downEvent.recycle();

        long durationMs = 300L;
        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            long eventTime = downTime + (durationMs * i / steps);
            float x = startX + (endX - startX) * i / steps;
            float y = startY + (endY - startY) * i / steps;
            MotionEvent moveEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_MOVE,
                    x,
                    y,
                    0
            );
            moveEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            result = result && injectInputEvent(moveEvent);
            moveEvent.recycle();
        }

        long upTime = downTime + durationMs + 10;
        MotionEvent upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                endX,
                endY,
                0
        );
        upEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        result = result && injectInputEvent(upEvent);
        upEvent.recycle();
        return result;
    }

    public boolean injectLongPress(float x, float y, long durationMs) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
        );
        downEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean result = injectInputEvent(downEvent);
        downEvent.recycle();
        if (!result) {
            return false;
        }
        long holdMs = Math.max(0L, durationMs);
        try {
            Thread.sleep(holdMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long upTime = SystemClock.uptimeMillis();
        MotionEvent upEvent = MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );
        upEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean upResult = injectInputEvent(upEvent);
        upEvent.recycle();
        return upResult;
    }

    public boolean injectText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] events = keyCharacterMap.getEvents(text.toCharArray());
        if (events == null) {
            return false;
        }
        boolean result = true;
        for (KeyEvent event : events) {
            event.setSource(InputDevice.SOURCE_KEYBOARD);
            result = result && injectInputEvent(event);
        }
        return result;
    }

    public boolean injectTextByShell(String text) {
        if (text == null) {
            text = "";
        }
        String escaped = text
                .replace("%", "\\%")
                .replace(" ", "%s")
                .replace("\n", "%s");
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/input", "text", escaped).start();
            int code = process.waitFor();
            return code == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public boolean injectKeyCode(int keyCode) {
        long downTime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(
                downTime,
                downTime,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        );
        boolean downResult = injectInputEvent(downEvent);
        long upTime = SystemClock.uptimeMillis();
        KeyEvent upEvent = new KeyEvent(
                downTime,
                upTime,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        );
        boolean upResult = injectInputEvent(upEvent);
        return downResult && upResult;
    }

    public boolean injectPaste() {
        long downTime = SystemClock.uptimeMillis();
        boolean ctrlDown = injectKeyEvent(new KeyEvent(
                downTime, downTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_CTRL_LEFT, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD));
        long eventTime = SystemClock.uptimeMillis();
        boolean vDown = injectKeyEvent(new KeyEvent(
                downTime, eventTime, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD));
        long upTime = SystemClock.uptimeMillis();
        boolean vUp = injectKeyEvent(new KeyEvent(
                downTime, upTime, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD));
        long ctrlUpTime = SystemClock.uptimeMillis();
        boolean ctrlUp = injectKeyEvent(new KeyEvent(
                downTime, ctrlUpTime, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_CTRL_LEFT, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD));
        boolean ok = ctrlDown && vDown && vUp && ctrlUp;
        if (!ok) {
            ok = injectPasteByShell();
        }
        return ok;
    }

    private boolean injectPasteByShell() {
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/input", "keyevent", "279").start();
            int code = process.waitFor();
            return code == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public boolean clearText() {
        long downTime = SystemClock.uptimeMillis();
        boolean ctrlDown = injectKeyEvent(new KeyEvent(
                downTime,
                downTime,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_CTRL_LEFT,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        ));
        long eventTime = SystemClock.uptimeMillis();
        boolean selectAllDown = injectKeyEvent(new KeyEvent(
                downTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A,
                0,
                KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        ));
        long upTime = SystemClock.uptimeMillis();
        boolean selectAllUp = injectKeyEvent(new KeyEvent(
                downTime,
                upTime,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_A,
                0,
                KeyEvent.META_CTRL_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        ));
        long ctrlUpTime = SystemClock.uptimeMillis();
        boolean ctrlUp = injectKeyEvent(new KeyEvent(
                downTime,
                ctrlUpTime,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_CTRL_LEFT,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD
        ));
        boolean deleteOk = injectKeyCode(KeyEvent.KEYCODE_DEL);
        return ctrlDown && selectAllDown && selectAllUp && ctrlUp && deleteOk;
    }

    private boolean injectKeyEvent(KeyEvent event) {
        return injectInputEvent(event);
    }

    private boolean injectInputEvent(InputEvent event) {
        if (inputManager == null || event == null) {
            return false;
        }
        return inputManager.injectInputEvent(event, injectMode);
    }
}
