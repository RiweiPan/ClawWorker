package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class DetailedFilter implements TreeFilter {
    private final boolean filterKeyboard;
    private final boolean clipBounds;

    public DetailedFilter(boolean filterKeyboard, boolean clipBounds) {
        this.filterKeyboard = filterKeyboard;
        this.clipBounds = clipBounds;
    }

    @Override
    public UiElement filter(UiElement node, Rect screenBounds) {
        if (node == null || screenBounds == null) {
            return null;
        }
        if (filterKeyboard && node.getResourceId().startsWith(
                "com.google.android.inputmethod.latin:id/")) {
            return null;
        }
        Rect bounds = node.getBounds();
        int nodeArea = bounds.width() * bounds.height();
        if (nodeArea <= 0) {
            return null;
        }
        Rect intersection = new Rect(bounds);
        boolean intersects = intersection.intersect(screenBounds);
        if (!intersects) {
            return null;
        }
        int visibleArea = intersection.width() * intersection.height();
        float ratio = visibleArea / (float) nodeArea;
        if (ratio < 0.1f) {
            return null;
        }
        Rect finalBounds = clipBounds ? intersection : bounds;
        List<UiElement> keptChildren = new ArrayList<>();
        for (UiElement child : node.getChildren()) {
            UiElement kept = filter(child, screenBounds);
            if (kept != null) {
                keptChildren.add(kept);
            }
        }
        return new UiElement(node.getClassName(), node.getText(), node.getResourceId(),
                node.isClickable(), node.isEditable(), finalBounds, keptChildren);
    }
}
