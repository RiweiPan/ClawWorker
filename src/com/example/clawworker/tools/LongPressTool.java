package com.example.clawworker.tools;

import android.graphics.Rect;
import com.example.clawworker.action.ActionResult;
import com.example.clawworker.parser.UiElement;

public class LongPressTool {
    private final InputInjector injector;

    public LongPressTool(InputInjector injector) {
        this.injector = injector;
    }

    public ActionResult longPress(UiElement element, int index, long durationMs) {
        if (element == null) {
            return new ActionResult(false, "element not found", "long_press",
                    0, 0, 0, 0, index);
        }
        Rect bounds = element.getBounds();
        float x = bounds.exactCenterX();
        float y = bounds.exactCenterY();
        boolean ok = injector.injectLongPress(x, y, durationMs);
        return new ActionResult(ok, ok ? "long_press ok" : "long_press failed", "long_press",
                x, y, x, y, index);
    }

    public ActionResult longPressAt(float x, float y, long durationMs) {
        boolean ok = injector.injectLongPress(x, y, durationMs);
        return new ActionResult(ok, ok ? "long_press ok" : "long_press failed", "long_press",
                x, y, x, y, -1);
    }
}
