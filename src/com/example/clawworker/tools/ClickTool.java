package com.example.clawworker.tools;

import android.graphics.Rect;
import com.example.clawworker.action.ActionResult;
import com.example.clawworker.parser.UiElement;
import java.util.ArrayList;
import java.util.List;

public class ClickTool {
    private final InputInjector injector;

    public ClickTool(InputInjector injector) {
        this.injector = injector;
    }

    public ActionResult click(UiElement element, int index) {
        if (element == null) {
            return new ActionResult(false, "element not found", "click",
                    0, 0, 0, 0, index);
        }
        Rect bounds = element.getBounds();
        float[] center = center(bounds);
        boolean ok = injector.injectClick(center[0], center[1]);
        if (!ok) {
            for (float[] alt : alternativePoints(bounds)) {
                if (injector.injectClick(alt[0], alt[1])) {
                    return new ActionResult(true, "clicked alternative point", "click",
                            alt[0], alt[1], alt[0], alt[1], index);
                }
            }
            return new ActionResult(false, "click failed", "click",
                    center[0], center[1], center[0], center[1], index);
        }
        return new ActionResult(true, "click ok", "click",
                center[0], center[1], center[0], center[1], index);
    }

    public ActionResult clickAt(float x, float y) {
        boolean ok = injector.injectClick(x, y);
        return new ActionResult(ok, ok ? "click ok" : "click failed", "click",
                x, y, x, y, -1);
    }

    private float[] center(Rect bounds) {
        return new float[] { bounds.exactCenterX(), bounds.exactCenterY() };
    }

    private List<float[]> alternativePoints(Rect bounds) {
        List<float[]> points = new ArrayList<>();
        float left = bounds.left + bounds.width() * 0.25f;
        float right = bounds.right - bounds.width() * 0.25f;
        float top = bounds.top + bounds.height() * 0.25f;
        float bottom = bounds.bottom - bounds.height() * 0.25f;
        points.add(new float[] { left, top });
        points.add(new float[] { right, top });
        points.add(new float[] { left, bottom });
        points.add(new float[] { right, bottom });
        return points;
    }
}
