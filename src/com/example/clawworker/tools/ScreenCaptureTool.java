package com.example.clawworker.tools;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManagerGlobal;
import android.window.ScreenCapture;

public class ScreenCaptureTool {
    private final DisplayManager displayManager;
    private final String tag;

    public ScreenCaptureTool(DisplayManager displayManager, String tag) {
        this.displayManager = displayManager;
        this.tag = tag == null ? "ScreenCaptureTool" : tag;
    }

    public Bitmap capture() {
        Log.d(tag, "captureScreenBitmap start");
        Bitmap bitmap = tryCaptureDisplayWithArgs();
        Log.d(tag, "captureScreenBitmap result=" + (bitmap != null));
        return bitmap;
    }

    private Bitmap tryCaptureDisplayWithArgs() {
        Display display = displayManager != null
                ? displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                : null;
        if (display == null) {
            return null;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        Rect rect = new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        ScreenCapture.CaptureArgs captureArgs = new ScreenCapture.CaptureArgs.Builder()
                .setSourceCrop(rect)
                .build();
        ScreenCapture.SynchronousScreenCaptureListener syncCapture =
                ScreenCapture.createSyncCaptureListener();
        try {
            WindowManagerGlobal.getWindowManagerService()
                    .captureDisplay(display.getDisplayId(), captureArgs, syncCapture);
        } catch (Exception e) {
            Log.d(tag, "capture display error=" + e.getMessage());
            return null;
        }
        ScreenCapture.ScreenshotHardwareBuffer buffer = syncCapture.getBuffer();
        if (buffer == null) {
            return null;
        }
        return buffer.asBitmap();
    }
}
