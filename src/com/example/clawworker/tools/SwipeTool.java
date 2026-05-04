package com.example.clawworker.tools;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.example.clawworker.action.ActionResult;
import com.example.clawworker.parser.UiElement;
import com.example.clawworker.parser.UiParseResult;
import java.util.List;

public class SwipeTool {
    private final InputInjector injector;

    public SwipeTool(InputInjector injector) {
        this.injector = injector;
    }

    public ActionResult swipe(UiElement start, UiElement end, int startIndex) {
        if (start == null || end == null) {
            return new ActionResult(false, "element not found", "swipe",
                    0, 0, 0, 0, -1);
        }
        float[] s = center(start.getBounds());
        float[] e = center(end.getBounds());
        boolean ok = injector.injectSwipe(s[0], s[1], e[0], e[1]);
        return new ActionResult(ok, ok ? "swipe ok" : "swipe failed", "swipe",
                s[0], s[1], e[0], e[1], startIndex);
    }

    public ActionResult swipe(float startX, float startY, float endX, float endY) {
        boolean ok = injector.injectSwipe(startX, startY, endX, endY);
        return new ActionResult(ok, ok ? "swipe ok" : "swipe failed", "swipe",
                startX, startY, endX, endY, -1);
    }

    public ActionResult swipeDirection(UiParseResult result, String direction) {
        Rect screen = inferScreenBounds(result);
        float cx = screen.exactCenterX();
        float cy = screen.exactCenterY();
        float distance = Math.min(screen.width(), screen.height()) * 0.35f;
        float startX = cx;
        float startY = cy;
        float endX = cx;
        float endY = cy;
        String dir = direction == null ? "" : direction.toLowerCase();
        if (dir.contains("up")) {
            endY = cy - distance;
        } else if (dir.contains("down")) {
            endY = cy + distance;
        } else if (dir.contains("left")) {
            endX = cx - distance;
        } else if (dir.contains("right")) {
            endX = cx + distance;
        } else {
            return new ActionResult(false, "unknown direction", "swipe",
                    startX, startY, endX, endY, -1);
        }
        boolean ok = injector.injectSwipe(startX, startY, endX, endY);
        return new ActionResult(ok, ok ? "swipe ok" : "swipe failed", "swipe",
                startX, startY, endX, endY, -1);
    }

    private float[] center(Rect bounds) {
        return new float[] { bounds.exactCenterX(), bounds.exactCenterY() };
    }

    private Rect inferScreenBounds(UiParseResult result) {
        if (result != null) {
            Bitmap bmp = result.getScreenshot();
            if (bmp != null) {
                return new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
            }
            List<UiElement> elements = result.getElements();
            if (elements != null && !elements.isEmpty()) {
                int right = 0;
                int bottom = 0;
                for (UiElement e : elements) {
                    Rect b = e.getBounds();
                    right = Math.max(right, b.right);
                    bottom = Math.max(bottom, b.bottom);
                }
                return new Rect(0, 0, Math.max(1, right), Math.max(1, bottom));
            }
        }
        return new Rect(0, 0, 1080, 1920);
    }
}
