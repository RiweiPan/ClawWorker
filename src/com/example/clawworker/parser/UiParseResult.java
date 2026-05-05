package com.example.clawworker.parser;

import android.graphics.Bitmap;
import java.util.List;

public class UiParseResult {
    private final List<UiElement> elements;
    private final String textDump;
    private final Bitmap screenshot;
    private final String packageName;

    public UiParseResult(List<UiElement> elements, String textDump, Bitmap screenshot,
                         String packageName) {
        this.elements = elements;
        this.textDump = textDump == null ? "" : textDump;
        this.screenshot = screenshot;
        this.packageName = packageName == null ? "" : packageName;
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

    public String getPackageName() {
        return packageName;
    }
}
