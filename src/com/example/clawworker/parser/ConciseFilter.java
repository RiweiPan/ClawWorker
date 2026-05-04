package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class ConciseFilter implements TreeFilter {
    @Override
    public UiElement filter(UiElement node, Rect screenBounds) {
        if (node == null || screenBounds == null) {
            return null;
        }
        Rect bounds = node.getBounds();
        if (!Rect.intersects(screenBounds, bounds)) {
            return null;
        }
        if (bounds.width() <= 5 || bounds.height() <= 5) {
            return null;
        }
        List<UiElement> keptChildren = new ArrayList<>();
        for (UiElement child : node.getChildren()) {
            UiElement kept = filter(child, screenBounds);
            if (kept != null) {
                keptChildren.add(kept);
            }
        }
        return new UiElement(node.getClassName(), node.getText(), node.getResourceId(),
                node.isClickable(), node.isEditable(), bounds, keptChildren);
    }
}
