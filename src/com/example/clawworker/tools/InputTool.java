package com.example.clawworker.tools;

import android.graphics.Rect;
import com.example.clawworker.action.ActionResult;
import com.example.clawworker.parser.UiElement;

public class InputTool {
    private final InputInjector injector;

    public InputTool(InputInjector injector) {
        this.injector = injector;
    }

    public ActionResult input(UiElement element, int index, String text) {
        if (element == null) {
            return new ActionResult(false, "element not found", "input",
                    0, 0, 0, 0, index);
        }
        Rect bounds = element.getBounds();
        float[] center = new float[] { bounds.exactCenterX(), bounds.exactCenterY() };
        boolean focused = injector.injectClick(center[0], center[1]);
        if (!focused) {
            return new ActionResult(false, "focus failed", "input",
                    center[0], center[1], center[0], center[1], index);
        }
        try {
            Thread.sleep(120L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String existingText = element.getText();
        if (existingText != null && !existingText.isEmpty()) {
            injector.clearText();
        }
        String safeText = text == null ? "" : text;
        boolean ok = injector.injectText(safeText);
        if (!ok) {
            ok = injector.injectTextByShell(safeText);
        }
        return new ActionResult(ok, ok ? "input ok" : "input failed", "input",
                center[0], center[1], center[0], center[1], index);
    }

    public ActionResult inputAt(float x, float y, String text) {
        boolean focused = injector.injectClick(x, y);
        if (!focused) {
            return new ActionResult(false, "focus failed", "input",
                    x, y, x, y, -1);
        }
        try {
            Thread.sleep(120L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        injector.clearText();
        String safeText = text == null ? "" : text;
        boolean ok = injector.injectText(safeText);
        if (!ok) {
            ok = injector.injectTextByShell(safeText);
        }
        return new ActionResult(ok, ok ? "input ok" : "input failed", "input",
                x, y, x, y, -1);
    }
}
