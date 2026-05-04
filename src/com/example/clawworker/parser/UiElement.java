package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class UiElement {
    private int index;
    private final String className;
    private final String text;
    private final String resourceId;
    private final boolean clickable;
    private final boolean editable;
    private final Rect bounds;
    private final List<UiElement> children;

    public UiElement(String className, String text, String resourceId, Rect bounds,
                     List<UiElement> children) {
        this(className, text, resourceId, false, false, bounds, children);
    }

    public UiElement(String className, String text, String resourceId,
                     boolean clickable, boolean editable, Rect bounds,
                     List<UiElement> children) {
        this.index = -1;
        this.className = className == null ? "" : className;
        this.text = text == null ? "" : text;
        this.resourceId = resourceId == null ? "" : resourceId;
        this.clickable = clickable;
        this.editable = editable;
        this.bounds = bounds == null ? new Rect() : new Rect(bounds);
        this.children = children == null ? new ArrayList<>() : children;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }

    public String getResourceId() {
        return resourceId;
    }

    public boolean isClickable() {
        return clickable;
    }

    public boolean isEditable() {
        return editable;
    }

    public Rect getBounds() {
        return new Rect(bounds);
    }

    public List<UiElement> getChildren() {
        return children;
    }
}
