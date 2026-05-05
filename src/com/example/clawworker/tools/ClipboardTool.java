package com.example.clawworker.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import com.example.clawworker.action.ActionResult;

public class ClipboardTool {
    private final ClipboardManager clipboardManager;
    private final InputInjector injector;

    public ClipboardTool(Context context, InputInjector injector) {
        this.injector = injector;
        this.clipboardManager = context == null ? null
                : context.getSystemService(ClipboardManager.class);
    }

    public ActionResult setText(String text) {
        if (clipboardManager == null) {
            return new ActionResult(false, "clipboard service unavailable",
                    "clipboard_set", 0, 0, 0, 0, -1);
        }
        String safeText = text == null ? "" : text;
        try {
            ClipData clip = ClipData.newPlainText("claw_worker", safeText);
            clipboardManager.setPrimaryClip(clip);
        } catch (SecurityException e) {
            return new ActionResult(false, "clipboard permission denied",
                    "clipboard_set", 0, 0, 0, 0, -1);
        }
        return new ActionResult(true, "clipboard set ok",
                "clipboard_set", 0, 0, 0, 0, -1);
    }

    public ActionResult paste() {
        boolean ok = injector.injectPaste();
        return new ActionResult(ok, ok ? "paste ok" : "paste failed",
                "clipboard_paste", 0, 0, 0, 0, -1);
    }

    public ActionResult pasteAt(float x, float y) {
        injector.injectClick(x, y);
        try {
            Thread.sleep(80L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean ok = injector.injectPaste();
        return new ActionResult(ok, ok ? "paste ok" : "paste failed",
                "clipboard_paste", x, y, x, y, -1);
    }
}
