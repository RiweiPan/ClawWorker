package com.example.clawworker.parser;

import android.graphics.Rect;

public interface TreeFilter {
    UiElement filter(UiElement node, Rect screenBounds);
}
