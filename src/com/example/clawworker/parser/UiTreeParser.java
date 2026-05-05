package com.example.clawworker.parser;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class UiTreeParser {
    public static UiParseResult parse(AccessibilityNodeInfo root, int screenW, int screenH,
                                      boolean visionEnabled, Bitmap screenshot) {
        if (root == null || screenW <= 0 || screenH <= 0) {
            return new UiParseResult(new ArrayList<>(), "", null, "");
        }
        String packageName = "";
        CharSequence pkg = root.getPackageName();
        if (pkg != null) {
            packageName = pkg.toString();
        }
        UiElement tree = buildElementTree(root);
        Rect screenBounds = new Rect(0, 0, screenW, screenH);
        TreeFilter filter = visionEnabled
                ? new ConciseFilter()
                : new DetailedFilter(true, true);
        UiElement filtered = filter.filter(tree, screenBounds);
        if (filtered == null) {
            return new UiParseResult(new ArrayList<>(), "", visionEnabled ? screenshot : null,
                    packageName);
        }
        IndexedFormatter.FormatResult formatted = IndexedFormatter.format(filtered);
        Bitmap resultBitmap = visionEnabled ? screenshot : null;
        return new UiParseResult(formatted.getElements(), formatted.getTextDump(), resultBitmap,
                packageName);
    }

    private static UiElement buildElementTree(AccessibilityNodeInfo node) {
        String className = extractClassName(node);
        String resourceId = node.getViewIdResourceName();
        String text = extractText(node, resourceId, className);
        boolean clickable = node.isClickable();
        boolean editable = node.isEditable();
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        List<UiElement> children = new ArrayList<>();
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                children.add(buildElementTree(child));
                child.recycle();
            }
        }
        return new UiElement(className, text, resourceId, clickable, editable, bounds, children);
    }

    private static String extractText(AccessibilityNodeInfo node, String resourceId,
                                      String className) {
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            return text.toString();
        }
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.length() > 0) {
            return desc.toString();
        }
        if (resourceId != null && !resourceId.isEmpty()) {
            return resourceId;
        }
        return className == null ? "" : className;
    }

    private static String extractClassName(AccessibilityNodeInfo node) {
        CharSequence raw = node.getClassName();
        if (raw == null) {
            return "";
        }
        String name = raw.toString();
        int idx = name.lastIndexOf('.');
        if (idx >= 0 && idx + 1 < name.length()) {
            return name.substring(idx + 1);
        }
        return name;
    }
}
