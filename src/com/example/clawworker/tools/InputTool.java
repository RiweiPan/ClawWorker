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
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        boolean clicked = injector.injectClick(cx, cy);
        if (!clicked) {
            float[] alt = tryAlternativePoints(bounds);
            if (alt == null) {
                return new ActionResult(false, "focus failed", "input",
                        cx, cy, cx, cy, index);
            }
            cx = alt[0];
            cy = alt[1];
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
                cx, cy, cx, cy, index);
    }

    public ActionResult inputAt(float x, float y, String text) {
        boolean clicked = injector.injectClick(x, y);
        float usedX = x;
        float usedY = y;
        if (!clicked) {
            float[] alt = tryNearbyPoints(x, y);
            if (alt == null) {
                return new ActionResult(false, "focus failed", "input",
                        x, y, x, y, -1);
            }
            usedX = alt[0];
            usedY = alt[1];
        }
        try {
            Thread.sleep(120L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String safeText = text == null ? "" : text;
        boolean ok = injector.injectText(safeText);
        if (!ok) {
            ok = injector.injectTextByShell(safeText);
        }
        return new ActionResult(ok, ok ? "input ok" : "input failed", "input",
                usedX, usedY, usedX, usedY, -1);
    }

    private float[] tryAlternativePoints(Rect bounds) {
        float left = bounds.left + bounds.width() * 0.25f;
        float right = bounds.right - bounds.width() * 0.25f;
        float top = bounds.top + bounds.height() * 0.25f;
        float bottom = bounds.bottom - bounds.height() * 0.25f;
        float[][] candidates = {
                {left, top},
                {right, top},
                {left, bottom},
                {right, bottom},
        };
        for (float[] pt : candidates) {
            if (injector.injectClick(pt[0], pt[1])) {
                return pt;
            }
        }
        return null;
    }

    private float[] tryNearbyPoints(float x, float y) {
        float[] offsets = {-8f, 8f, -16f, 16f};
        for (float dx : offsets) {
            for (float dy : offsets) {
                if (dx == 0f && dy == 0f) {
                    continue;
                }
                if (injector.injectClick(x + dx, y + dy)) {
                    return new float[] {x + dx, y + dy};
                }
            }
        }
        return null;
    }
}
