package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class IndexedFormatter {
    private static final String INDENT = "  ";

    public static FormatResult format(UiElement root) {
        List<UiElement> flat = new ArrayList<>();
        StringBuilder dump = new StringBuilder();
        if (root != null) {
            formatRecursive(root, flat, dump, 0, 0);
        }
        return new FormatResult(flat, dump.toString());
    }

    private static int formatRecursive(UiElement node, List<UiElement> flat,
                                       StringBuilder dump, int depth, int index) {
        boolean interactive = node.isClickable() || node.isEditable();
        boolean hasText = hasVisibleText(node);
        boolean isLeaf = node.getChildren().isEmpty();
        boolean skipSelf = interactive && !hasText && hasInteractiveDescendant(node);

        int nextIndex = index;

        if (skipSelf) {
            // Container is clickable but children already provide interaction points;
            // don't emit this node, process children at the same depth.
        } else if (interactive || (hasText && isLeaf)) {
            node.setIndex(nextIndex);
            flat.add(node);
            appendLine(dump, depth, nextIndex, node);
            nextIndex = nextIndex + 1;
        } else if (hasText) {
            appendLine(dump, depth, -1, node);
        }

        int childDepth = skipSelf ? depth : depth + 1;
        for (UiElement child : node.getChildren()) {
            nextIndex = formatRecursive(child, flat, dump, childDepth, nextIndex);
        }

        return nextIndex;
    }

    private static boolean hasVisibleText(UiElement node) {
        String text = node.getText();
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.equals(node.getResourceId()) || text.equals(node.getClassName())) {
            return false;
        }
        return true;
    }

    private static boolean hasInteractiveDescendant(UiElement node) {
        if (node.getChildren().isEmpty()) {
            return false;
        }
        for (UiElement child : node.getChildren()) {
            if (child.isClickable() || child.isEditable()) {
                return true;
            }
            if (hasInteractiveDescendant(child)) {
                return true;
            }
        }
        return false;
    }

    private static void appendLine(StringBuilder dump, int depth, int index, UiElement node) {
        for (int i = 0; i < depth; i++) {
            dump.append(INDENT);
        }
        if (index >= 0) {
            dump.append(index).append(". ");
        } else {
            dump.append("· ");
        }
        dump.append(node.getClassName())
                .append(": ")
                .append(node.getResourceId())
                .append(", \"")
                .append(displayText(node))
                .append("\" - (")
                .append(boundsStr(node))
                .append(")\n");
    }

    private static String displayText(UiElement node) {
        String text = node.getText();
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.equals(node.getResourceId()) || text.equals(node.getClassName())) {
            return "";
        }
        return text;
    }

    private static String boundsStr(UiElement node) {
        Rect b = node.getBounds();
        return b.left + "," + b.top + "," + b.right + "," + b.bottom;
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
