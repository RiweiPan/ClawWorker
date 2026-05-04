package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class IndexedFormatter {
    public static FormatResult format(UiElement root) {
        List<UiElement> flat = new ArrayList<>();
        StringBuilder dump = new StringBuilder();
        if (root != null) {
            formatRecursive(root, flat, dump, 0);
        }
        return new FormatResult(flat, dump.toString());
    }

    private static int formatRecursive(UiElement node, List<UiElement> flat,
                                       StringBuilder dump, int index) {
        int next = index;
        if (node.isClickable() || node.isEditable()) {
            node.setIndex(index);
            flat.add(node);
            Rect b = node.getBounds();
            dump.append(index)
                    .append(". ")
                    .append(node.getClassName())
                    .append(": ")
                    .append(node.getResourceId())
                    .append(", ")
                    .append(node.getText())
                    .append(" - (")
                    .append(b.left).append(",")
                    .append(b.top).append(",")
                    .append(b.right).append(",")
                    .append(b.bottom)
                    .append(")")
                    .append("\n");
            next = index + 1;
        }
        for (UiElement child : node.getChildren()) {
            next = formatRecursive(child, flat, dump, next);
        }
        return next;
    }

    public static class FormatResult {
        private final List<UiElement> elements;
        private final String textDump;

        public FormatResult(List<UiElement> elements, String textDump) {
            this.elements = elements;
            this.textDump = textDump == null ? "" : textDump;
        }

        public List<UiElement> getElements() {
            return elements;
        }

        public String getTextDump() {
            return textDump;
        }
    }
}
