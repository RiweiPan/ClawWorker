package com.example.clawworker.parser;

import android.graphics.Bitmap;
import java.util.List;

public class UiParseResult {
    private final List<UiElement> elements;
    private final String textDump;
    private final Bitmap screenshot;

    public UiParseResult(List<UiElement> elements, String textDump, Bitmap screenshot) {
        this.elements = elements;
        this.textDump = textDump == null ? "" : textDump;
        this.screenshot = screenshot;
    }

    public List<UiElement> getElements() {
        return elements;
    }

    public String getTextDump() {
        return textDump;
    }

    public Bitmap getScreenshot() {
        return screenshot;
    }
}
